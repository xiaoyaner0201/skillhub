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
import com.iflytek.skillhub.domain.user.UserAccount;
import com.iflytek.skillhub.domain.user.UserAccountRepository;
import com.iflytek.skillhub.domain.user.UserStatus;
import com.iflytek.skillhub.notification.domain.Notification;
import com.iflytek.skillhub.notification.domain.NotificationCategory;
import com.iflytek.skillhub.notification.service.NotificationDispatcher;
import com.iflytek.skillhub.notification.service.NotificationPreferenceService;
import com.iflytek.skillhub.notification.service.NotificationService;
import com.iflytek.skillhub.notification.sse.SseEmitterManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RED probes against the real subscriber event entry and final notification sinks. The extra
 * account fixture is deliberately authoritative input that the base listener currently ignores.
 */
@ExtendWith(MockitoExtension.class)
class SubscriberNotificationSinkTest {

    private static final Long SKILL_ID = 42L;
    private static final Long VERSION_ID = 84L;
    private static final String DENIED = "denied-user";
    private static final String CONTROL = "control-user";

    @Mock private SkillRepository skillRepository;
    @Mock private SkillVersionRepository skillVersionRepository;
    @Mock private NamespaceRepository namespaceRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private RecipientResolver recipientResolver;
    @Mock private SkillSubscriptionService subscriptionService;
    @Mock private NotificationService notificationService;
    @Mock private NotificationPreferenceService preferenceService;
    @Mock private SseEmitterManager sseEmitterManager;

    private NotificationEventListener listener;

    @BeforeEach
    void setUp() {
        NotificationDispatcher dispatcher = new NotificationDispatcher(
                notificationService, preferenceService, sseEmitterManager);
        listener = new NotificationEventListener(
                skillRepository,
                skillVersionRepository,
                namespaceRepository,
                recipientResolver,
                dispatcher,
                subscriptionService,
                new ObjectMapper());
    }

    @Test
    void publish_privateOrdinaryMember_hasZeroRowAndSse() {
        publish(SkillVisibility.PRIVATE, false, true, activeAccount());
        assertDeniedHasNoSink("SUBSCRIPTION_NEW_VERSION", "Skill updated: Test Skill");
    }

    @Test
    void publish_hiddenOrdinaryMember_hasZeroRowAndSse() {
        publish(SkillVisibility.PUBLIC, true, true, activeAccount());
        assertDeniedHasNoSink("SUBSCRIPTION_NEW_VERSION", "Skill updated: Test Skill");
    }

    @Test
    void publish_namespaceOnlyRemovedMember_hasZeroRowAndSse() {
        publish(SkillVisibility.NAMESPACE_ONLY, false, true, activeAccount());
        assertDeniedHasNoSink("SUBSCRIPTION_NEW_VERSION", "Skill updated: Test Skill");
    }

    @Test
    void publish_disabledUser_hasZeroRowAndSse() {
        publish(SkillVisibility.PUBLIC, false, true, disabledAccount());
        assertDeniedHasNoSink("SUBSCRIPTION_NEW_VERSION", "Skill updated: Test Skill");
    }

    @Test
    void yank_privateOrdinaryMember_hasZeroRowAndSse() {
        yank(SkillVisibility.PRIVATE, false, true, activeAccount());
        assertDeniedHasNoSink("SUBSCRIPTION_VERSION_YANKED", "Skill version yanked: Test Skill");
    }

    @Test
    void yank_hiddenOrdinaryMember_hasZeroRowAndSse() {
        yank(SkillVisibility.PUBLIC, true, true, activeAccount());
        assertDeniedHasNoSink("SUBSCRIPTION_VERSION_YANKED", "Skill version yanked: Test Skill");
    }

    @Test
    void yank_namespaceOnlyRemovedMember_hasZeroRowAndSse() {
        yank(SkillVisibility.NAMESPACE_ONLY, false, true, activeAccount());
        assertDeniedHasNoSink("SUBSCRIPTION_VERSION_YANKED", "Skill version yanked: Test Skill");
    }

    @Test
    void yank_disabledUser_hasZeroRowAndSse() {
        yank(SkillVisibility.PUBLIC, false, true, disabledAccount());
        assertDeniedHasNoSink("SUBSCRIPTION_VERSION_YANKED", "Skill version yanked: Test Skill");
    }

    @Test
    void yank_lastPublishedVersion_nonOwnerGetsNoSink() {
        yank(SkillVisibility.PUBLIC, false, false, activeAccount());
        assertDeniedHasNoSink("SUBSCRIPTION_VERSION_YANKED", "Skill version yanked: Test Skill");
    }

    private void publish(SkillVisibility visibility, boolean hidden, boolean published,
                         UserAccount account) {
        arrange(visibility, hidden, published, account);
        listener.onSkillPublishedForSubscribers(
                new SkillPublishedEvent(SKILL_ID, VERSION_ID, "publisher-user"));
    }

    private void yank(SkillVisibility visibility, boolean hidden, boolean published,
                      UserAccount account) {
        arrange(visibility, hidden, published, account);
        listener.onSkillVersionYankedForSubscribers(
                new SkillVersionYankedEvent(SKILL_ID, VERSION_ID, "actor-user"));
    }

    private void arrange(SkillVisibility visibility, boolean hidden, boolean published,
                         UserAccount deniedAccount) {
        Skill skill = new Skill(5L, "test-skill", CONTROL, visibility);
        ReflectionTestUtils.setField(skill, "id", SKILL_ID);
        skill.setDisplayName("Test Skill");
        skill.setHidden(hidden);
        skill.setLatestVersionId(published ? VERSION_ID : null);
        when(skillRepository.findById(SKILL_ID)).thenReturn(Optional.of(skill));
        when(subscriptionService.findSubscribersBySkillId(SKILL_ID))
                .thenReturn(List.of(DENIED, CONTROL));

        Namespace namespace = new Namespace("demo", "Demo", CONTROL);
        ReflectionTestUtils.setField(namespace, "id", 5L);
        when(namespaceRepository.findById(5L)).thenReturn(Optional.of(namespace));

        // This is the event-time account fact the base listener fails to consult.
        lenient().when(userAccountRepository.findById(DENIED)).thenReturn(Optional.of(deniedAccount));
        when(preferenceService.isEnabled(anyString(), eq(NotificationCategory.PUBLISH), any()))
                .thenReturn(true);
        when(notificationService.create(anyString(), eq(NotificationCategory.PUBLISH), anyString(),
                anyString(), anyString(), eq("SKILL"), eq(SKILL_ID)))
                .thenAnswer(invocation -> notification(
                        invocation.getArgument(0),
                        invocation.getArgument(2),
                        invocation.getArgument(3),
                        invocation.getArgument(4)));
    }

    private Notification notification(String recipientId, String eventType, String title,
                                      String bodyJson) {
        Notification notification = new Notification(
                recipientId,
                NotificationCategory.PUBLISH,
                eventType,
                title,
                bodyJson,
                "SKILL",
                SKILL_ID,
                Instant.parse("2026-08-22T15:00:00Z"));
        ReflectionTestUtils.setField(notification, "id", recipientId.equals(CONTROL) ? 2L : 1L);
        return notification;
    }

    private UserAccount activeAccount() {
        return new UserAccount(DENIED, DENIED, DENIED + "@example.com", null);
    }

    private UserAccount disabledAccount() {
        UserAccount account = activeAccount();
        account.setStatus(UserStatus.DISABLED);
        return account;
    }

    private void assertDeniedHasNoSink(String eventType, String title) {
        verify(notificationService, never()).create(
                eq(DENIED), eq(NotificationCategory.PUBLISH), eq(eventType),
                eq(title), anyString(), eq("SKILL"), eq(SKILL_ID));
        verify(sseEmitterManager, never()).push(eq(DENIED), any());

        verify(notificationService).create(
                eq(CONTROL), eq(NotificationCategory.PUBLISH), eq(eventType),
                eq(title), anyString(), eq("SKILL"), eq(SKILL_ID));
        verify(sseEmitterManager).push(eq(CONTROL), any());
    }
}
