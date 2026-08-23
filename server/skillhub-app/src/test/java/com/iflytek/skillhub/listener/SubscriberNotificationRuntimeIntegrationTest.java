package com.iflytek.skillhub.listener;

import com.iflytek.skillhub.SkillhubApplication;
import com.iflytek.skillhub.TestRedisConfig;
import com.iflytek.skillhub.auth.rbac.RbacService;
import com.iflytek.skillhub.domain.event.SkillPublishedEvent;
import com.iflytek.skillhub.domain.event.SkillVersionYankedEvent;
import com.iflytek.skillhub.domain.namespace.Namespace;
import com.iflytek.skillhub.domain.namespace.NamespaceMemberRepository;
import com.iflytek.skillhub.domain.namespace.NamespaceRepository;
import com.iflytek.skillhub.domain.namespace.NamespaceType;
import com.iflytek.skillhub.domain.review.PromotionRequest;
import com.iflytek.skillhub.domain.review.PromotionRequestRepository;
import com.iflytek.skillhub.domain.review.PromotionService;
import com.iflytek.skillhub.domain.review.ReviewTaskStatus;
import com.iflytek.skillhub.domain.skill.Skill;
import com.iflytek.skillhub.domain.skill.SkillRepository;
import com.iflytek.skillhub.domain.skill.SkillVersion;
import com.iflytek.skillhub.domain.skill.SkillVersionRepository;
import com.iflytek.skillhub.domain.skill.SkillVersionStatus;
import com.iflytek.skillhub.domain.skill.SkillVisibility;
import com.iflytek.skillhub.domain.social.SkillSubscription;
import com.iflytek.skillhub.domain.social.SkillSubscriptionRepository;
import com.iflytek.skillhub.domain.user.UserAccount;
import com.iflytek.skillhub.domain.user.UserAccountRepository;
import com.iflytek.skillhub.domain.user.UserStatus;
import com.iflytek.skillhub.infra.jpa.NotificationJpaRepository;
import com.iflytek.skillhub.notification.domain.Notification;
import com.iflytek.skillhub.notification.domain.NotificationCategory;
import com.iflytek.skillhub.notification.service.NotificationDispatcher;
import com.iflytek.skillhub.notification.sse.RecordingSseEmitterTestConfiguration;
import com.iflytek.skillhub.notification.sse.RecordingSseEmitterTestConfiguration.RecordedNotification;
import com.iflytek.skillhub.notification.sse.RecordingSseEmitterTestConfiguration.RecordingSseProbe;
import com.iflytek.skillhub.notification.sse.SseEmitterManager;
import com.iflytek.skillhub.search.SearchEmbeddingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = SkillhubApplication.class)
@ActiveProfiles("test")
@Import({TestRedisConfig.class, RecordingSseEmitterTestConfiguration.class})
class SubscriberNotificationRuntimeIntegrationTest {

    private static final Duration EVENT_TIMEOUT = Duration.ofSeconds(10);

    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private PlatformTransactionManager transactionManager;
    @SpyBean private NamespaceRepository namespaceRepository;
    @Autowired private SkillRepository skillRepository;
    @Autowired private SkillVersionRepository skillVersionRepository;
    @Autowired private SkillSubscriptionRepository subscriptionRepository;
    @SpyBean private UserAccountRepository userAccountRepository;
    @SpyBean private NamespaceMemberRepository namespaceMemberRepository;
    @Autowired private PromotionRequestRepository promotionRequestRepository;
    @Autowired private PromotionService promotionService;
    @Autowired private NotificationJpaRepository notificationRepository;
    @Autowired private SseEmitterManager sseEmitterManager;
    @Autowired private RecordingSseProbe sseProbe;
    @Autowired private NotificationEventListener notificationEventListener;
    @Autowired
    @Qualifier("skillhubEventExecutor")
    private Executor eventExecutor;

    @SpyBean private NotificationDispatcher dispatcher;
    @SpyBean private RbacService rbacService;

    @MockBean private SearchEmbeddingService searchEmbeddingService;

    private TransactionTemplate transactionTemplate;
    private TransactionTemplate independentTransaction;

    @BeforeEach
    void setUp() throws Exception {
        transactionTemplate = new TransactionTemplate(transactionManager);
        independentTransaction = new TransactionTemplate(transactionManager);
        independentTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        sseProbe.clear();
        awaitExecutorIdle();
    }

    @AfterEach
    void tearDown() throws Exception {
        sseProbe.releaseBlockedNotification();
        sseProbe.clear();
        awaitExecutorIdle();
    }

    @Test
    void committedEvent_traversesProxyConfiguredAsync_andPersistsRowBeforeSse() throws Exception {
        SubscriberFixture fixture = subscriberFixture(List.of("recipient"), Set.of());
        sseEmitterManager.register(fixture.user("recipient"));
        sseProbe.observeRowVisibility(() -> hasSingleRow(
                fixture.user("recipient"), "SUBSCRIPTION_NEW_VERSION"));
        long publisherThread = Thread.currentThread().getId();

        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(
                new SkillPublishedEvent(
                        fixture.skillId(), fixture.versionId(), fixture.user("publisher"))));

        RecordedNotification sent = sseProbe.awaitNotificationCount(1, EVENT_TIMEOUT).get(0);
        List<Notification> rows = rowsFor(
                fixture.user("recipient"), "SUBSCRIPTION_NEW_VERSION");
        assertThat(rows).singleElement().satisfies(row -> assertTuple(
                row, fixture.user("recipient"), NotificationCategory.PUBLISH,
                "SUBSCRIPTION_NEW_VERSION", "SKILL", fixture.skillId()));
        assertThat(sent.rowVisibleBeforeSend()).isTrue();
        assertThat(sent.threadName()).startsWith("skillhub-event-");
        assertThat(sent.threadId()).isNotEqualTo(publisherThread);
        assertPayloadMatchesRow(sent.payload(), rows.get(0));
    }

    @Test
    void rollbackAndNoTransaction_skipRegisteredListenerAndAllSinks() throws Exception {
        SubscriberFixture fixture = subscriberFixture(List.of("recipient"), Set.of());
        String recipient = fixture.user("recipient");
        sseEmitterManager.register(recipient);

        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new SkillPublishedEvent(
                    fixture.skillId(), fixture.versionId(), fixture.user("publisher")));
            status.setRollbackOnly();
        });
        awaitExecutorIdle();
        assertThat(rowsFor(recipient, "SUBSCRIPTION_NEW_VERSION")).isEmpty();
        assertThat(sseProbe.notifications()).isEmpty();

        eventPublisher.publishEvent(new SkillPublishedEvent(
                fixture.skillId(), fixture.versionId(), fixture.user("publisher")));
        awaitExecutorIdle();
        Thread.sleep(200L);
        assertThat(rowsFor(recipient, "SUBSCRIPTION_NEW_VERSION")).isEmpty();
        assertThat(sseProbe.notifications()).isEmpty();
        verify(dispatcher, never()).dispatch(
                eq(recipient), any(), any(), any(), any(), any(), any());
    }

    @Test
    void saturatedConfiguredExecutor_runsAfterCommitOnPublisherThread_andBlocksUntilCommittedSink()
            throws Exception {
        SubscriberFixture fixture = subscriberFixture(List.of("recipient"), Set.of());
        String recipient = fixture.user("recipient");
        sseEmitterManager.register(recipient);
        sseProbe.blockEventType("SUBSCRIPTION_NEW_VERSION");
        sseProbe.observeRowVisibility(() -> hasSingleRow(recipient, "SUBSCRIPTION_NEW_VERSION"));

        try (ExecutorSaturation ignored = saturateConfiguredExecutor()) {
            AtomicLong publisherThreadId = new AtomicLong();
            try (ExecutorService publisher = Executors.newSingleThreadExecutor(
                    Thread.ofPlatform().name("hd30-subscriber-publisher").factory())) {
                Future<?> future = publisher.submit(() -> {
                    publisherThreadId.set(Thread.currentThread().getId());
                    transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(
                            new SkillPublishedEvent(
                                    fixture.skillId(), fixture.versionId(), fixture.user("publisher"))));
                });
                try {
                    RecordedNotification blocked = sseProbe.awaitBlockedNotification(EVENT_TIMEOUT);
                    assertThat(future.isDone()).isFalse();
                    assertThat(blocked.threadId()).isEqualTo(publisherThreadId.get());
                    assertThat(blocked.rowVisibleBeforeSend()).isTrue();
                    assertThat(rowsFor(recipient, "SUBSCRIPTION_NEW_VERSION"))
                            .as("CallerRuns afterCommit must expose a committed row before SSE")
                            .singleElement()
                            .satisfies(row -> assertTuple(
                                    row, recipient, NotificationCategory.PUBLISH,
                                    "SUBSCRIPTION_NEW_VERSION", "SKILL", fixture.skillId()));
                } finally {
                    sseProbe.releaseBlockedNotification();
                    future.get(10, TimeUnit.SECONDS);
                }
            }
        }
    }

    @Test
    void promotionApproved_saturatedConfiguredExecutor_persistsCommittedRowBeforeRealManagerSse()
            throws Exception {
        PromotionFixture fixture = promotionFixture();
        sseEmitterManager.register(fixture.submitterId());
        sseProbe.blockEventType("PROMOTION_APPROVED");
        sseProbe.observeRowVisibility(() -> hasSingleRow(
                fixture.submitterId(), "PROMOTION_APPROVED"));

        // No disabled PROMOTION/IN_APP preference is seeded; the production default-open gate is real.
        try (ExecutorSaturation ignored = saturateConfiguredExecutor()) {
            AtomicLong publisherThreadId = new AtomicLong();
            try (ExecutorService publisher = Executors.newSingleThreadExecutor(
                    Thread.ofPlatform().name("hd30-promotion-publisher").factory())) {
                Future<?> future = publisher.submit(() -> {
                    publisherThreadId.set(Thread.currentThread().getId());
                    promotionService.approvePromotion(
                            fixture.promotionId(), fixture.reviewerId(), "ship it",
                            Set.of("SKILL_ADMIN"));
                });
                try {
                    RecordedNotification blocked = sseProbe.awaitBlockedNotification(EVENT_TIMEOUT);
                    assertThat(future.isDone()).isFalse();
                    assertThat(blocked.threadId()).isEqualTo(publisherThreadId.get());

                    PromotionRequest approved = independentTransaction.execute(status ->
                            promotionRequestRepository.findById(fixture.promotionId()).orElseThrow());
                    assertThat(approved.getStatus()).isEqualTo(ReviewTaskStatus.APPROVED);
                    assertThat(approved.getTargetSkillId()).isNotNull();
                    assertThat(skillRepository.findById(approved.getTargetSkillId())).isPresent();

                    List<Notification> rows = rowsFor(
                            fixture.submitterId(), "PROMOTION_APPROVED");
                    assertThat(blocked.rowVisibleBeforeSend()).isTrue();
                    assertThat(rows)
                            .as("PROMOTION_APPROVED row must commit before real-manager SSE")
                            .singleElement()
                            .satisfies(row -> {
                                assertTuple(row, fixture.submitterId(), NotificationCategory.PROMOTION,
                                        "PROMOTION_APPROVED", "SKILL", fixture.sourceSkillId());
                                assertThat(row.getBodyJson())
                                        .contains("\"skillId\":" + fixture.sourceSkillId())
                                        .contains("\"promotionId\":" + fixture.promotionId())
                                        .contains("\"reviewerId\":\"" + fixture.reviewerId() + "\"");
                            });
                    assertPayloadMatchesRow(blocked.payload(), rows.get(0));
                } finally {
                    sseProbe.releaseBlockedNotification();
                    future.get(10, TimeUnit.SECONDS);
                }
            }
        }
    }

    @Test
    void publishedRecipients_preserveOrderThroughRealRowAndSse() throws Exception {
        assertFinalRecipientOrder(EventKind.PUBLISHED);
    }

    @Test
    void yankedRecipients_preserveOrderThroughRealRowAndSse() throws Exception {
        assertFinalRecipientOrder(EventKind.YANKED);
    }

    @Test
    void published_accountFailure_hasZeroFinalSink() throws Exception {
        assertAuthorityFailure(EventKind.PUBLISHED, FailurePoint.ACCOUNT);
    }

    @Test
    void published_membershipFailure_hasZeroFinalSink() throws Exception {
        assertAuthorityFailure(EventKind.PUBLISHED, FailurePoint.MEMBERSHIP);
    }

    @Test
    void published_platformRoleFailure_hasZeroFinalSink() throws Exception {
        assertAuthorityFailure(EventKind.PUBLISHED, FailurePoint.PLATFORM_ROLE);
    }

    @Test
    void published_namespaceFailure_hasZeroFinalSink() throws Exception {
        assertAuthorityFailure(EventKind.PUBLISHED, FailurePoint.NAMESPACE);
    }

    @Test
    void yanked_accountFailure_hasZeroFinalSink() throws Exception {
        assertAuthorityFailure(EventKind.YANKED, FailurePoint.ACCOUNT);
    }

    @Test
    void yanked_membershipFailure_hasZeroFinalSink() throws Exception {
        assertAuthorityFailure(EventKind.YANKED, FailurePoint.MEMBERSHIP);
    }

    @Test
    void yanked_platformRoleFailure_hasZeroFinalSink() throws Exception {
        assertAuthorityFailure(EventKind.YANKED, FailurePoint.PLATFORM_ROLE);
    }

    @Test
    void yanked_namespaceFailure_hasZeroFinalSink() throws Exception {
        assertAuthorityFailure(EventKind.YANKED, FailurePoint.NAMESPACE);
    }

    @Test
    void springRegistry_reportsSubscriberProxyPhaseExecutorAndAllListeners() throws Exception {
        assertThat(AopUtils.isAopProxy(notificationEventListener)).isTrue();
        Method published = NotificationEventListener.class.getMethod(
                "onSkillPublishedForSubscribers", SkillPublishedEvent.class);
        Method yanked = NotificationEventListener.class.getMethod(
                "onSkillVersionYankedForSubscribers", SkillVersionYankedEvent.class);
        assertRegisteredAfterCommitAsync(published);
        assertRegisteredAfterCommitAsync(yanked);

        ThreadPoolTaskExecutor executor = configuredExecutor();
        assertThat(executor.getCorePoolSize()).isEqualTo(2);
        assertThat(executor.getMaxPoolSize()).isEqualTo(4);
        assertThat(executor.getThreadPoolExecutor().getQueue().remainingCapacity()).isEqualTo(100);
        assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler())
                .isInstanceOf(java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy.class);
    }

    private void assertFinalRecipientOrder(EventKind kind) throws Exception {
        SubscriberFixture fixture = subscriberFixture(
                List.of("beta", "denied", "alpha", "actor"), Set.of("denied"));
        String beta = fixture.user("beta");
        String alpha = fixture.user("alpha");
        sseEmitterManager.register(beta);
        sseEmitterManager.register(alpha);

        publishInCommittedTransaction(fixture, kind, fixture.user("actor"));
        List<RecordedNotification> sent = sseProbe.awaitNotificationCount(2, EVENT_TIMEOUT);
        awaitExecutorIdle();

        String eventType = kind.eventType();
        InOrder order = inOrder(dispatcher);
        order.verify(dispatcher).dispatch(
                eq(beta), eq(NotificationCategory.PUBLISH), eq(eventType),
                any(), any(), eq("SKILL"), eq(fixture.skillId()));
        order.verify(dispatcher).dispatch(
                eq(alpha), eq(NotificationCategory.PUBLISH), eq(eventType),
                any(), any(), eq("SKILL"), eq(fixture.skillId()));
        assertThat(rowsForUsers(Set.of(beta, alpha), eventType).stream()
                .map(Notification::getRecipientId)).containsExactly(beta, alpha);
        assertThat(sent.stream().map(RecordedNotification::recipientId))
                .containsExactly(beta, alpha);
    }

    private void assertAuthorityFailure(EventKind kind, FailurePoint point) throws Exception {
        SubscriberFixture fixture = subscriberFixture(List.of("first", "second"), Set.of());
        List<String> recipients = List.of(fixture.user("first"), fixture.user("second"));
        recipients.forEach(sseEmitterManager::register);
        RuntimeException failure = new IllegalStateException(point.name().toLowerCase() + " failed");

        switch (point) {
            case ACCOUNT -> doThrow(failure).when(userAccountRepository).findByIdIn(anyList());
            case MEMBERSHIP -> doThrow(failure).when(namespaceMemberRepository)
                    .findByNamespaceIdAndUserIdIn(eq(fixture.namespaceId()), any());
            case NAMESPACE -> doThrow(failure).when(namespaceRepository)
                    .findById(fixture.namespaceId());
            case PLATFORM_ROLE -> stubBatchRbacFailure(recipients, failure);
        }

        publishInCommittedTransaction(fixture, kind, fixture.user("actor"));
        awaitExecutorIdle();
        Thread.sleep(100L);

        assertThat(rowsForUsers(Set.copyOf(recipients), kind.eventType())).isEmpty();
        assertThat(sseProbe.notifications()).isEmpty();
        recipients.forEach(recipient -> verify(dispatcher, never()).dispatch(
                eq(recipient), any(), any(), any(), any(), any(), any()));
    }

    private void stubBatchRbacFailure(List<String> recipients, RuntimeException failure) {
        doThrow(failure).when(rbacService).getUserRoleCodesByUserIds(recipients);
    }

    private void publishInCommittedTransaction(SubscriberFixture fixture,
                                               EventKind kind,
                                               String actor) {
        transactionTemplate.executeWithoutResult(status -> {
            if (kind == EventKind.PUBLISHED) {
                eventPublisher.publishEvent(new SkillPublishedEvent(
                        fixture.skillId(), fixture.versionId(), actor));
            } else {
                eventPublisher.publishEvent(new SkillVersionYankedEvent(
                        fixture.skillId(), fixture.versionId(), actor));
            }
        });
    }

    private SubscriberFixture subscriberFixture(List<String> labels, Set<String> disabledLabels) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String owner = "owner-" + suffix;
        Namespace namespace = namespaceRepository.save(
                new Namespace("runtime-" + suffix, "Runtime " + suffix, owner));
        Skill skill = new Skill(namespace.getId(), "runtime-skill-" + suffix,
                owner, SkillVisibility.PUBLIC);
        skill.setDisplayName("Runtime Skill " + suffix);
        skill.setCreatedBy(owner);
        skill.setUpdatedBy(owner);
        skill = skillRepository.save(skill);
        SkillVersion version = new SkillVersion(skill.getId(), "1.0.0", owner);
        version.setStatus(SkillVersionStatus.PUBLISHED);
        version.setPublishedAt(Instant.now());
        version.setRequestedVisibility(SkillVisibility.PUBLIC);
        version.setParsedMetadataJson("{\"name\":\"runtime\"}");
        version.setManifestJson("{\"version\":\"1.0.0\"}");
        version = skillVersionRepository.save(version);
        skill.setLatestVersionId(version.getId());
        skillRepository.save(skill);

        Map<String, String> users = new java.util.LinkedHashMap<>();
        for (String label : labels) {
            String userId = label + "-" + suffix;
            UserAccount account = new UserAccount(
                    userId, label, label + "-" + suffix + "@example.com", null);
            if (disabledLabels.contains(label)) {
                account.setStatus(UserStatus.DISABLED);
            }
            userAccountRepository.save(account);
            subscriptionRepository.save(new SkillSubscription(skill.getId(), userId));
            users.put(label, userId);
        }
        users.put("publisher", "publisher-" + suffix);
        users.put("actor", "actor-" + suffix);
        return new SubscriberFixture(namespace.getId(), skill.getId(), version.getId(), users);
    }

    private PromotionFixture promotionFixture() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String submitter = "promotion-submitter-" + suffix;
        String reviewer = "promotion-reviewer-" + suffix;
        userAccountRepository.save(new UserAccount(
                submitter, "Promotion Submitter", submitter + "@example.com", null));
        userAccountRepository.save(new UserAccount(
                reviewer, "Promotion Reviewer", reviewer + "@example.com", null));

        Namespace target = new Namespace("global-" + suffix, "Global " + suffix, reviewer);
        target.setType(NamespaceType.GLOBAL);
        target = namespaceRepository.save(target);
        Namespace sourceNamespace = namespaceRepository.save(
                new Namespace("team-" + suffix, "Team " + suffix, submitter));
        Skill source = new Skill(sourceNamespace.getId(), "promotion-skill-" + suffix,
                submitter, SkillVisibility.PUBLIC);
        source.setDisplayName("Promotion Skill " + suffix);
        source.setSummary("Runtime promotion evidence");
        source.setCreatedBy(submitter);
        source.setUpdatedBy(submitter);
        source = skillRepository.save(source);
        SkillVersion sourceVersion = new SkillVersion(source.getId(), "1.0.0", submitter);
        sourceVersion.setStatus(SkillVersionStatus.PUBLISHED);
        sourceVersion.setPublishedAt(Instant.now());
        sourceVersion.setRequestedVisibility(SkillVisibility.PUBLIC);
        sourceVersion.setParsedMetadataJson("{\"name\":\"promotion-runtime\"}");
        sourceVersion.setManifestJson("{\"version\":\"1.0.0\"}");
        sourceVersion = skillVersionRepository.save(sourceVersion);
        source.setLatestVersionId(sourceVersion.getId());
        skillRepository.save(source);
        PromotionRequest request = promotionRequestRepository.save(
                new PromotionRequest(
                        source.getId(), sourceVersion.getId(), target.getId(), submitter));
        return new PromotionFixture(
                request.getId(), source.getId(), submitter, reviewer);
    }

    private ExecutorSaturation saturateConfiguredExecutor() throws Exception {
        ThreadPoolTaskExecutor executor = configuredExecutor();
        awaitExecutorIdle();
        CountDownLatch release = new CountDownLatch(1);
        Thread saturator = Thread.currentThread();
        // How many fillers the pool absorbs depends on how many worker threads a previous test left
        // alive, so a fixed count can overshoot into CallerRunsPolicy and park the saturating thread
        // on the very latch only it can release. Submit until the pool reports saturation, and let a
        // filler handed back by CallerRunsPolicy return at once - being handed one already proves the
        // pool is full. The production executor keeps its own core/max/queue/policy untouched.
        Runnable filler = () -> {
            if (Thread.currentThread() == saturator) {
                return;
            }
            try {
                release.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        };
        try {
            Instant deadline = Instant.now().plus(EVENT_TIMEOUT);
            while (!isSaturated(executor) && Instant.now().isBefore(deadline)) {
                boolean queueHasRoom =
                        executor.getThreadPoolExecutor().getQueue().remainingCapacity() > 0;
                boolean poolCanGrow = executor.getPoolSize() < executor.getMaxPoolSize();
                if (queueHasRoom || poolCanGrow) {
                    // Submitting on a full queue is what makes the pool grow past its core size: only
                    // a failed offer spawns another worker. A filler count computed up front cannot
                    // converge, because every worker that wakes frees one queue slot again.
                    executor.execute(filler);
                } else {
                    Thread.sleep(10L);
                }
            }
            if (!isSaturated(executor)) {
                throw new AssertionError(
                        "configured executor did not reach 4 active workers and 100 queued tasks; observed "
                                + executorState(executor));
            }
            assertThat(executor.getThreadPoolExecutor().isShutdown()).isFalse();
        } catch (Throwable failure) {
            // Fillers already parked would outlive this test and make every later awaitExecutorIdle
            // fail, hiding the one real cause behind a cascade.
            release.countDown();
            throw failure;
        }
        return new ExecutorSaturation(executor, release);
    }

    private String executorState(ThreadPoolTaskExecutor executor) {
        return "active=" + executor.getActiveCount()
                + " pool=" + executor.getPoolSize()
                + " queued=" + executor.getThreadPoolExecutor().getQueue().size()
                + " remainingCapacity=" + executor.getThreadPoolExecutor().getQueue().remainingCapacity();
    }

    private boolean isSaturated(ThreadPoolTaskExecutor executor) {
        return executor.getActiveCount() == executor.getMaxPoolSize()
                && executor.getThreadPoolExecutor().getQueue().remainingCapacity() == 0;
    }

    private ThreadPoolTaskExecutor configuredExecutor() {
        assertThat(eventExecutor).isInstanceOf(ThreadPoolTaskExecutor.class);
        return (ThreadPoolTaskExecutor) eventExecutor;
    }

    private void awaitExecutorIdle() throws Exception {
        ThreadPoolTaskExecutor executor = configuredExecutor();
        awaitCondition(() -> executor.getActiveCount() == 0
                        && executor.getThreadPoolExecutor().getQueue().isEmpty(),
                EVENT_TIMEOUT,
                "configured executor did not become idle; observed " + executorState(executor));
    }

    private void awaitCondition(java.util.function.BooleanSupplier condition,
                                Duration timeout,
                                String message) throws Exception {
        Instant deadline = Instant.now().plus(timeout);
        while (!condition.getAsBoolean() && Instant.now().isBefore(deadline)) {
            Thread.sleep(10L);
        }
        if (!condition.getAsBoolean()) {
            throw new AssertionError(message);
        }
    }

    private List<Notification> rowsFor(String recipient, String eventType) {
        return rowsForUsers(Set.of(recipient), eventType);
    }

    private List<Notification> rowsForUsers(Set<String> recipients, String eventType) {
        List<Notification> rows = independentTransaction.execute(status ->
                notificationRepository.findAll().stream()
                        .filter(row -> recipients.contains(row.getRecipientId()))
                        .filter(row -> eventType.equals(row.getEventType()))
                        .sorted(Comparator.comparing(Notification::getId))
                        .toList());
        return rows != null ? rows : List.of();
    }

    private boolean hasSingleRow(String recipient, String eventType) {
        return rowsFor(recipient, eventType).size() == 1;
    }

    private void assertTuple(Notification row,
                             String recipient,
                             NotificationCategory category,
                             String eventType,
                             String entityType,
                             Long entityId) {
        assertThat(row.getRecipientId()).isEqualTo(recipient);
        assertThat(row.getCategory()).isEqualTo(category);
        assertThat(row.getEventType()).isEqualTo(eventType);
        assertThat(row.getEntityType()).isEqualTo(entityType);
        assertThat(row.getEntityId()).isEqualTo(entityId);
    }

    private void assertPayloadMatchesRow(Map<String, Object> payload, Notification row) {
        assertThat(payload)
                .containsEntry("id", row.getId())
                .containsEntry("category", row.getCategory().name())
                .containsEntry("eventType", row.getEventType())
                .containsEntry("bodyJson", Optional.ofNullable(row.getBodyJson()).orElse(""))
                .containsEntry("entityType", row.getEntityType())
                .containsEntry("entityId", row.getEntityId());
    }

    private void assertRegisteredAfterCommitAsync(Method method) {
        TransactionalEventListener transactional = method.getAnnotation(TransactionalEventListener.class);
        assertThat(transactional).isNotNull();
        assertThat(transactional.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(transactional.fallbackExecution()).isFalse();
        assertThat(method.getAnnotation(Async.class).value()).isEqualTo("skillhubEventExecutor");
    }

    private enum EventKind {
        PUBLISHED("SUBSCRIPTION_NEW_VERSION"),
        YANKED("SUBSCRIPTION_VERSION_YANKED");

        private final String eventType;

        EventKind(String eventType) {
            this.eventType = eventType;
        }

        String eventType() {
            return eventType;
        }
    }

    private enum FailurePoint {
        ACCOUNT,
        MEMBERSHIP,
        PLATFORM_ROLE,
        NAMESPACE
    }

    private record SubscriberFixture(Long namespaceId,
                                     Long skillId,
                                     Long versionId,
                                     Map<String, String> users) {
        String user(String label) {
            return users.get(label);
        }
    }

    private record PromotionFixture(Long promotionId,
                                    Long sourceSkillId,
                                    String submitterId,
                                    String reviewerId) {
    }

    private final class ExecutorSaturation implements AutoCloseable {
        private final ThreadPoolTaskExecutor executor;
        private final CountDownLatch release;

        private ExecutorSaturation(ThreadPoolTaskExecutor executor, CountDownLatch release) {
            this.executor = executor;
            this.release = release;
        }

        @Override
        public void close() throws Exception {
            release.countDown();
            awaitCondition(() -> executor.getActiveCount() == 0
                            && executor.getThreadPoolExecutor().getQueue().isEmpty(),
                    EVENT_TIMEOUT,
                    "configured executor fillers did not drain");
        }
    }
}
