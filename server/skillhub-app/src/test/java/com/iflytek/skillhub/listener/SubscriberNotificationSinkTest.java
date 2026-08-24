package com.iflytek.skillhub.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.skillhub.domain.event.SkillPublishedEvent;
import com.iflytek.skillhub.domain.event.SkillVersionYankedEvent;
import com.iflytek.skillhub.domain.namespace.Namespace;
import com.iflytek.skillhub.domain.namespace.NamespaceRepository;
import com.iflytek.skillhub.domain.skill.Skill;
import com.iflytek.skillhub.domain.skill.SkillRepository;
import com.iflytek.skillhub.domain.skill.SkillVersionRepository;
import com.iflytek.skillhub.domain.skill.SkillVisibility;
import com.iflytek.skillhub.domain.social.SkillSubscriptionService;
import com.iflytek.skillhub.notification.domain.Notification;
import com.iflytek.skillhub.notification.domain.NotificationCategory;
import com.iflytek.skillhub.notification.domain.NotificationChannel;
import com.iflytek.skillhub.notification.domain.NotificationPreference;
import com.iflytek.skillhub.notification.domain.NotificationPreferenceRepository;
import com.iflytek.skillhub.notification.domain.NotificationRepository;
import com.iflytek.skillhub.notification.domain.NotificationStatus;
import com.iflytek.skillhub.notification.service.NotificationDispatcher;
import com.iflytek.skillhub.notification.service.NotificationPreferenceService;
import com.iflytek.skillhub.notification.service.NotificationService;
import com.iflytek.skillhub.notification.sse.SseEmitterManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HD-32 path subscriber-fanout: notification side-effect sink probes.
 *
 * <p>The subject is the real production fanout chain, not a helper: the two
 * {@code NotificationEventListener} subscriber entry points drive a real
 * {@link NotificationDispatcher} over a real {@link NotificationService} and a real
 * {@link NotificationPreferenceService}. Only the two terminal sinks are observed - the durable
 * notification row ({@link NotificationRepository#save}) and the SSE transport
 * ({@link SseEmitterManager#push}). A recipient who may no longer read the skill must reach
 * neither sink; the allowed control in the same run must reach both exactly once.
 */
@ExtendWith(MockitoExtension.class)
class SubscriberNotificationSinkTest {

    private static final Long SKILL_ID = 42L;
    private static final Long VERSION_ID = 90L;
    private static final Long NAMESPACE_ID = 5L;

    private static final String OWNER = "owner-1";
    private static final String PUBLISHER = "publisher-1";
    private static final String ACTOR = "actor-1";
    private static final String ORDINARY_MEMBER = "ordinary-1";
    private static final String FORMER_MEMBER = "former-member-1";

    private static final String PUBLISH_EVENT_TYPE = "SUBSCRIPTION_NEW_VERSION";
    private static final String YANK_EVENT_TYPE = "SUBSCRIPTION_VERSION_YANKED";

    @Mock private SkillRepository skillRepository;
    @Mock private SkillVersionRepository skillVersionRepository;
    @Mock private NamespaceRepository namespaceRepository;
    @Mock private RecipientResolver recipientResolver;
    @Mock private SkillSubscriptionService skillSubscriptionService;
    @Mock private NotificationPreferenceRepository preferenceRepository;
    @Mock private SseEmitterManager sseEmitterManager;

    private RecordingNotificationRepository notificationRepository;
    private NotificationEventListener listener;

    @BeforeEach
    void setUp() {
        notificationRepository = new RecordingNotificationRepository();
        NotificationService notificationService = new NotificationService(
                notificationRepository, Clock.fixed(Instant.parse("2026-08-24T00:00:00Z"), ZoneOffset.UTC));
        NotificationPreferenceService preferenceService = new NotificationPreferenceService(preferenceRepository);
        NotificationDispatcher dispatcher =
                new NotificationDispatcher(notificationService, preferenceService, sseEmitterManager);
        listener = new NotificationEventListener(skillRepository, skillVersionRepository, namespaceRepository,
                recipientResolver, dispatcher, skillSubscriptionService, new ObjectMapper());
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private void givenSkill(SkillVisibility visibility, boolean hidden) {
        Skill skill = new Skill(NAMESPACE_ID, "test-skill", OWNER, visibility);
        skill.setDisplayName("Test Skill");
        skill.setLatestVersionId(VERSION_ID);
        skill.setHidden(hidden);
        ReflectionTestUtils.setField(skill, "id", SKILL_ID);
        when(skillRepository.findById(SKILL_ID)).thenReturn(Optional.of(skill));
        lenient().when(namespaceRepository.findById(NAMESPACE_ID))
                .thenReturn(Optional.of(new Namespace("team-ai", "Team AI", OWNER)));
        lenient().when(skillVersionRepository.findById(VERSION_ID)).thenReturn(Optional.empty());
    }

    private void givenSubscribers(String... subscriberIds) {
        when(skillSubscriptionService.findSubscribersBySkillId(SKILL_ID)).thenReturn(List.of(subscriberIds));
    }

    private void publish() {
        listener.onSkillPublishedForSubscribers(new SkillPublishedEvent(SKILL_ID, VERSION_ID, PUBLISHER));
    }

    private void yank() {
        listener.onSkillVersionYankedForSubscribers(new SkillVersionYankedEvent(SKILL_ID, VERSION_ID, ACTOR));
    }

    // ------------------------------------------------------------------
    // Sink assertions - the durable row and the SSE push, nothing else
    // ------------------------------------------------------------------

    private void assertNoSinkReached(String recipientId) {
        assertThat(notificationRepository.saved)
                .as("no durable notification row may be written for %s", recipientId)
                .noneMatch(notification -> recipientId.equals(notification.getRecipientId()));
        verify(sseEmitterManager, never()).push(eq(recipientId), any());
    }

    private void assertBothSinksReachedOnce(String recipientId, String eventType) {
        assertThat(notificationRepository.saved)
                .as("control recipient %s must still get exactly one durable row", recipientId)
                .filteredOn(notification -> recipientId.equals(notification.getRecipientId()))
                .singleElement()
                .satisfies(notification -> {
                    assertThat(notification.getEventType()).isEqualTo(eventType);
                    assertThat(notification.getCategory()).isEqualTo(NotificationCategory.PUBLISH);
                });
        verify(sseEmitterManager).push(eq(recipientId), any());
    }

    // ------------------------------------------------------------------
    // Probes published-*-denied
    // ------------------------------------------------------------------

    /** Probe published-private-denied. */
    @Test
    void publish_privateOrdinaryMember_reachesNeitherSink() {
        givenSkill(SkillVisibility.PRIVATE, false);
        givenSubscribers(ORDINARY_MEMBER, OWNER);

        publish();

        assertNoSinkReached(ORDINARY_MEMBER);
        assertBothSinksReachedOnce(OWNER, PUBLISH_EVENT_TYPE);
    }

    /** Probe published-hidden-denied. */
    @Test
    void publish_hiddenOrdinaryMember_reachesNeitherSink() {
        givenSkill(SkillVisibility.PUBLIC, true);
        givenSubscribers(ORDINARY_MEMBER, OWNER);

        publish();

        assertNoSinkReached(ORDINARY_MEMBER);
        assertBothSinksReachedOnce(OWNER, PUBLISH_EVENT_TYPE);
    }

    /**
     * Probe published-removed-denied.
     *
     * <p>The control is the owner rather than a current MEMBER: the listener takes no membership
     * collaborator on base, so "current MEMBER" is not expressible from this entry point yet
     * (reported as a Stage 1 plan gap).
     */
    @Test
    void publish_namespaceOnlyRemovedMember_reachesNeitherSink() {
        givenSkill(SkillVisibility.NAMESPACE_ONLY, false);
        givenSubscribers(FORMER_MEMBER, OWNER);

        publish();

        assertNoSinkReached(FORMER_MEMBER);
        assertBothSinksReachedOnce(OWNER, PUBLISH_EVENT_TYPE);
    }

    // ------------------------------------------------------------------
    // Probes yanked-*-denied
    // ------------------------------------------------------------------

    /** Probe yanked-private-denied. */
    @Test
    void yank_privateOrdinaryMember_reachesNeitherSink() {
        givenSkill(SkillVisibility.PRIVATE, false);
        givenSubscribers(ORDINARY_MEMBER, OWNER);

        yank();

        assertNoSinkReached(ORDINARY_MEMBER);
        assertBothSinksReachedOnce(OWNER, YANK_EVENT_TYPE);
    }

    /** Probe yanked-hidden-denied. */
    @Test
    void yank_hiddenOrdinaryMember_reachesNeitherSink() {
        givenSkill(SkillVisibility.PUBLIC, true);
        givenSubscribers(ORDINARY_MEMBER, OWNER);

        yank();

        assertNoSinkReached(ORDINARY_MEMBER);
        assertBothSinksReachedOnce(OWNER, YANK_EVENT_TYPE);
    }

    /** Probe yanked-removed-denied; same owner-as-control deviation as the publish variant. */
    @Test
    void yank_namespaceOnlyRemovedMember_reachesNeitherSink() {
        givenSkill(SkillVisibility.NAMESPACE_ONLY, false);
        givenSubscribers(FORMER_MEMBER, OWNER);

        yank();

        assertNoSinkReached(FORMER_MEMBER);
        assertBothSinksReachedOnce(OWNER, YANK_EVENT_TYPE);
    }

    // ------------------------------------------------------------------
    // Probe notification-ineligible-zero
    // ------------------------------------------------------------------

    /**
     * Probe notification-ineligible-zero.
     *
     * <p>An explicitly ENABLED in-app preference must not be able to override the access decision:
     * preference is a "do you want this" switch, not a "may you see this" one. The ineligible
     * recipient opts in and still reaches zero sinks.
     */
    @Test
    void ineligibleRecipient_withPreferenceExplicitlyEnabled_stillReachesNeitherSink() {
        givenSkill(SkillVisibility.PRIVATE, false);
        givenSubscribers(ORDINARY_MEMBER, OWNER);
        lenient().when(preferenceRepository.findByUserIdAndCategoryAndChannel(
                        ORDINARY_MEMBER, NotificationCategory.PUBLISH, NotificationChannel.IN_APP))
                .thenReturn(Optional.of(new NotificationPreference(
                        ORDINARY_MEMBER, NotificationCategory.PUBLISH, NotificationChannel.IN_APP, true)));

        publish();

        assertNoSinkReached(ORDINARY_MEMBER);
        assertBothSinksReachedOnce(OWNER, PUBLISH_EVENT_TYPE);
    }

    // ------------------------------------------------------------------
    // Recording repository: the durable sink, observed rather than mocked so the
    // dispatcher's own read-back of the persisted id stays on the real code path.
    // ------------------------------------------------------------------

    private static final class RecordingNotificationRepository implements NotificationRepository {

        private final List<Notification> saved = new ArrayList<>();
        private long nextId = 1L;

        @Override
        public Notification save(Notification notification) {
            ReflectionTestUtils.setField(notification, "id", nextId++);
            saved.add(notification);
            return notification;
        }

        @Override
        public Optional<Notification> findById(Long id) {
            return saved.stream().filter(n -> id != null && id.equals(n.getId())).findFirst();
        }

        @Override
        public Page<Notification> findByRecipientId(String recipientId, Pageable pageable) {
            throw new UnsupportedOperationException("not exercised by the subscriber fanout path");
        }

        @Override
        public Page<Notification> findByRecipientIdAndCategory(String recipientId, NotificationCategory category,
                                                               Pageable pageable) {
            throw new UnsupportedOperationException("not exercised by the subscriber fanout path");
        }

        @Override
        public long countByRecipientIdAndStatus(String recipientId, NotificationStatus status) {
            throw new UnsupportedOperationException("not exercised by the subscriber fanout path");
        }

        @Override
        public int markAllReadByRecipientId(String recipientId, Instant readAt) {
            throw new UnsupportedOperationException("not exercised by the subscriber fanout path");
        }

        @Override
        public int deleteByIdAndRecipientIdAndStatus(Long id, String recipientId, NotificationStatus status) {
            throw new UnsupportedOperationException("not exercised by the subscriber fanout path");
        }

        @Override
        public int deleteByStatusAndCreatedAtBefore(NotificationStatus status, Instant before) {
            throw new UnsupportedOperationException("not exercised by the subscriber fanout path");
        }
    }
}
