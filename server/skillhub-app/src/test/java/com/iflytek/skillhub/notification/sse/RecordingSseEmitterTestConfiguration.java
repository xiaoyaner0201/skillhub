package com.iflytek.skillhub.notification.sse;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/** Test transport seam that keeps the production {@link SseEmitterManager} behavior real. */
@TestConfiguration
public class RecordingSseEmitterTestConfiguration {

    @Bean
    RecordingSseProbe recordingSseProbe() {
        return new RecordingSseProbe();
    }

    @Bean
    @Primary
    SseEmitterManager recordingSseEmitterManager(RecordingSseProbe probe) {
        return new SseEmitterManager(probe::newEmitter);
    }

    public static final class RecordingSseProbe {
        private final Object eventMonitor = new Object();
        private final AtomicLong sequence = new AtomicLong();
        private final CopyOnWriteArrayList<RecordedNotification> notifications =
                new CopyOnWriteArrayList<>();
        private final ConcurrentHashMap<String, RecordingSseEmitter> emitters =
                new ConcurrentHashMap<>();
        private final AtomicReference<Blocker> blocker = new AtomicReference<>();
        private final AtomicReference<BooleanSupplier> rowVisibilityCheck =
                new AtomicReference<>(() -> true);

        SseEmitter newEmitter(String userId) {
            RecordingSseEmitter emitter = new RecordingSseEmitter(userId, this);
            emitters.put(userId, emitter);
            return emitter;
        }

        public void clear() {
            releaseBlockedNotification();
            notifications.clear();
            sequence.set(0L);
            emitters.values().forEach(RecordingSseEmitter::complete);
            emitters.clear();
            blocker.set(null);
            rowVisibilityCheck.set(() -> true);
        }

        public void blockEventType(String eventType) {
            releaseBlockedNotification();
            blocker.set(new Blocker(eventType, new CountDownLatch(1), new CountDownLatch(1)));
        }

        public void observeRowVisibility(BooleanSupplier visibilityCheck) {
            rowVisibilityCheck.set(visibilityCheck);
        }

        public RecordedNotification awaitBlockedNotification(Duration timeout)
                throws InterruptedException {
            Blocker active = blocker.get();
            if (active == null || !active.entered().await(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new AssertionError("Timed out waiting for blocked SSE notification");
            }
            return notifications.stream()
                    .filter(notification -> active.eventType().equals(notification.eventType()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Blocked SSE notification was not recorded"));
        }

        public void releaseBlockedNotification() {
            Blocker active = blocker.get();
            if (active != null) {
                active.release().countDown();
            }
        }

        public List<RecordedNotification> awaitNotificationCount(int expected, Duration timeout)
                throws InterruptedException {
            Instant deadline = Instant.now().plus(timeout);
            synchronized (eventMonitor) {
                while (notifications.size() < expected && Instant.now().isBefore(deadline)) {
                    long remaining = Math.max(1L, Duration.between(Instant.now(), deadline).toMillis());
                    eventMonitor.wait(remaining);
                }
            }
            if (notifications.size() < expected) {
                throw new AssertionError(
                        "Expected " + expected + " SSE notifications but observed " + notifications.size());
            }
            return List.copyOf(notifications);
        }

        public List<RecordedNotification> notifications() {
            return List.copyOf(notifications);
        }

        public void complete(String userId) {
            RecordingSseEmitter emitter = emitters.remove(userId);
            if (emitter != null) {
                emitter.complete();
            }
        }

        private void record(String userId, Map<String, Object> payload) throws IOException {
            String eventType = String.valueOf(payload.get("eventType"));
            notifications.add(new RecordedNotification(
                    sequence.incrementAndGet(),
                    userId,
                    Thread.currentThread().getId(),
                    Thread.currentThread().getName(),
                    eventType,
                    rowVisibilityCheck.get().getAsBoolean(),
                    Map.copyOf(payload)));
            synchronized (eventMonitor) {
                eventMonitor.notifyAll();
            }

            Blocker active = blocker.get();
            if (active != null && active.eventType().equals(eventType)) {
                active.entered().countDown();
                try {
                    if (!active.release().await(15, TimeUnit.SECONDS)) {
                        throw new IOException("Timed out waiting to release recording SSE transport");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while blocking recording SSE transport", exception);
                }
            }
        }
    }

    public record RecordedNotification(long sequence,
                                       String recipientId,
                                       long threadId,
                                       String threadName,
                                       String eventType,
                                       boolean rowVisibleBeforeSend,
                                       Map<String, Object> payload) {
    }

    private record Blocker(String eventType, CountDownLatch entered, CountDownLatch release) {
    }

    private static final class RecordingSseEmitter extends SseEmitter {
        private final String userId;
        private final RecordingSseProbe probe;
        private Runnable completionCallback = () -> { };
        private Runnable timeoutCallback = () -> { };
        private java.util.function.Consumer<Throwable> errorCallback = error -> { };

        private RecordingSseEmitter(String userId, RecordingSseProbe probe) {
            super(60_000L);
            this.userId = userId;
            this.probe = probe;
        }

        @Override
        public synchronized void onCompletion(Runnable callback) {
            completionCallback = callback;
        }

        @Override
        public synchronized void onTimeout(Runnable callback) {
            timeoutCallback = callback;
        }

        @Override
        public synchronized void onError(java.util.function.Consumer<Throwable> callback) {
            errorCallback = callback;
        }

        @Override
        public void complete() {
            completionCallback.run();
        }

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            List<Object> data = new ArrayList<>();
            for (ResponseBodyEmitter.DataWithMediaType item : builder.build()) {
                data.add(item.getData());
            }
            boolean notificationEvent = data.stream()
                    .anyMatch(value -> value.toString().contains("event:notification"));
            if (!notificationEvent) {
                return;
            }
            Object rawPayload = data.stream().filter(Map.class::isInstance).findFirst().orElse(null);
            if (!(rawPayload instanceof Map<?, ?> rawMap)) {
                throw new IOException("Notification SSE event did not contain a map payload");
            }
            Map<String, Object> payload = new java.util.LinkedHashMap<>();
            rawMap.forEach((key, value) -> payload.put(String.valueOf(key), value));
            try {
                probe.record(userId, payload);
            } catch (IOException exception) {
                errorCallback.accept(exception);
                throw exception;
            }
        }
    }
}
