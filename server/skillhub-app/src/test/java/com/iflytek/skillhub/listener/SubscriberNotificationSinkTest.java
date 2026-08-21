package com.iflytek.skillhub.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.skillhub.domain.event.SkillPublishedEvent;
import com.iflytek.skillhub.domain.event.SkillVersionYankedEvent;
import com.iflytek.skillhub.domain.namespace.Namespace;
import com.iflytek.skillhub.domain.namespace.NamespaceMember;
import com.iflytek.skillhub.domain.namespace.NamespaceMemberRepository;
import com.iflytek.skillhub.domain.namespace.NamespaceRepository;
import com.iflytek.skillhub.domain.namespace.NamespaceRole;
import com.iflytek.skillhub.domain.skill.Skill;
import com.iflytek.skillhub.domain.skill.SkillRepository;
import com.iflytek.skillhub.domain.skill.SkillVersionRepository;
import com.iflytek.skillhub.domain.skill.SkillVisibility;
import com.iflytek.skillhub.domain.social.SkillSubscriptionService;
import com.iflytek.skillhub.domain.user.UserAccount;
import com.iflytek.skillhub.domain.user.UserAccountRepository;
import com.iflytek.skillhub.domain.user.UserStatus;
import com.iflytek.skillhub.auth.repository.UserRoleBindingRepository;
import com.iflytek.skillhub.notification.domain.Notification;
import com.iflytek.skillhub.notification.domain.NotificationCategory;
import com.iflytek.skillhub.notification.service.NotificationDispatcher;
import com.iflytek.skillhub.notification.service.NotificationPreferenceService;
import com.iflytek.skillhub.notification.service.NotificationService;
import com.iflytek.skillhub.notification.sse.SseEmitterManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    @Mock private NamespaceMemberRepository namespaceMemberRepository;
    @Mock private UserRoleBindingRepository userRoleBindingRepository;
    @Mock private RecipientResolver recipientResolver;
    @Mock private SkillSubscriptionService subscriptionService;
    @Mock private NotificationService notificationService;
    @Mock private NotificationPreferenceService preferenceService;
    @Mock private SseEmitterManager sseEmitterManager;

    private NotificationEventListener listener;
    private final AtomicReference<List<String>> readableRecipients = new AtomicReference<>(List.of(CONTROL));

    @BeforeEach
    void setUp() {
        NotificationDispatcher dispatcher = new NotificationDispatcher(
                notificationService, preferenceService, sseEmitterManager);
        SubscriberAccessResolver subscriberAccessResolver = new SubscriberAccessResolver(
                userAccountRepository,
                namespaceMemberRepository,
                userRoleBindingRepository,
                new com.iflytek.skillhub.domain.skill.VisibilityChecker());
        listener = new NotificationEventListener(
                skillRepository,
                skillVersionRepository,
                namespaceRepository,
                recipientResolver,
                subscriberAccessResolver,
                dispatcher,
                subscriptionService,
                new ObjectMapper());
    }

    @Test
    void publish_privateOrdinaryMember_hasZeroRowAndSse() {
        publish(SkillVisibility.PRIVATE, false, true);

        assertDeniedAndControlSink("SUBSCRIPTION_NEW_VERSION", "Skill updated: Test Skill");
    }

    @Test
    void publish_hiddenOrdinaryMember_hasZeroRowAndSse() {
        publish(SkillVisibility.PUBLIC, true, true);

        assertDeniedAndControlSink("SUBSCRIPTION_NEW_VERSION", "Skill updated: Test Skill");
    }

    @Test
    void publish_namespaceOnlyRemovedMember_hasZeroRowAndSse() {
        publish(SkillVisibility.NAMESPACE_ONLY, false, true);

        assertDeniedAndControlSink("SUBSCRIPTION_NEW_VERSION", "Skill updated: Test Skill");
    }

    @Test
    void publish_disabledUser_hasZeroRowAndSse() {
        publish(SkillVisibility.PUBLIC, false, true);

        assertDeniedAndControlSink("SUBSCRIPTION_NEW_VERSION", "Skill updated: Test Skill");
    }

    @Test
    void yank_privateOrdinaryMember_hasZeroRowAndSse() {
        yank(SkillVisibility.PRIVATE, false, true);

        assertDeniedAndControlSink("SUBSCRIPTION_VERSION_YANKED", "Skill version yanked: Test Skill");
    }

    @Test
    void yank_hiddenOrdinaryMember_hasZeroRowAndSse() {
        yank(SkillVisibility.PUBLIC, true, true);

        assertDeniedAndControlSink("SUBSCRIPTION_VERSION_YANKED", "Skill version yanked: Test Skill");
    }

    @Test
    void yank_namespaceOnlyRemovedMember_hasZeroRowAndSse() {
        yank(SkillVisibility.NAMESPACE_ONLY, false, true);

        assertDeniedAndControlSink("SUBSCRIPTION_VERSION_YANKED", "Skill version yanked: Test Skill");
    }

    @Test
    void yank_disabledUser_hasZeroRowAndSse() {
        yank(SkillVisibility.PUBLIC, false, true);

        assertDeniedAndControlSink("SUBSCRIPTION_VERSION_YANKED", "Skill version yanked: Test Skill");
    }

    @Test
    void yank_lastPublishedVersion_nonOwnerGetsNoSink() {
        yank(SkillVisibility.PUBLIC, false, false);

        assertDeniedAndControlSink("SUBSCRIPTION_VERSION_YANKED", "Skill version yanked: Test Skill");
    }

    @Test
    void publish_namespaceOnlyCurrentMember_receivesExactPayload() {
        publish(SkillVisibility.NAMESPACE_ONLY, false, true);

        assertDeniedAndControlSink("SUBSCRIPTION_NEW_VERSION", "Skill updated: Test Skill");
    }

    @Test
    void yank_namespaceOnlyCurrentMember_receivesExactPayload() {
        yank(SkillVisibility.NAMESPACE_ONLY, false, true);

        assertDeniedAndControlSink("SUBSCRIPTION_VERSION_YANKED", "Skill version yanked: Test Skill");
    }

    @Test
    void publish_readableRecipients_remainElementForElementAfterFiltering() {
        readableRecipients.set(List.of(DENIED, CONTROL));
        publish(SkillVisibility.PUBLIC, false, true);

        verify(notificationService).create(eq(DENIED), eq(NotificationCategory.PUBLISH),
                eq("SUBSCRIPTION_NEW_VERSION"), anyString(), anyString(), eq("SKILL"), eq(SKILL_ID));
        verify(notificationService).create(eq(CONTROL), eq(NotificationCategory.PUBLISH),
                eq("SUBSCRIPTION_NEW_VERSION"), anyString(), anyString(), eq("SKILL"), eq(SKILL_ID));
        verify(sseEmitterManager).push(eq(DENIED), any());
        verify(sseEmitterManager).push(eq(CONTROL), any());
    }

    private void publish(SkillVisibility visibility, boolean hidden, boolean published) {
        arrange(visibility, hidden, published);
        listener.onSkillPublishedForSubscribers(
                new SkillPublishedEvent(SKILL_ID, VERSION_ID, "publisher-user"));
    }

    private void yank(SkillVisibility visibility, boolean hidden, boolean published) {
        arrange(visibility, hidden, published);
        listener.onSkillVersionYankedForSubscribers(
                new SkillVersionYankedEvent(SKILL_ID, VERSION_ID, "actor-user"));
    }

    private void arrange(SkillVisibility visibility, boolean hidden, boolean published) {
        Skill skill = new Skill(5L, "test-skill", CONTROL, visibility);
        ReflectionTestUtils.setField(skill, "id", SKILL_ID);
        skill.setDisplayName("Test Skill");
        skill.setHidden(hidden);
        skill.setLatestVersionId(published ? VERSION_ID : null);
        when(skillRepository.findById(SKILL_ID)).thenReturn(Optional.of(skill));
        when(subscriptionService.findSubscribersBySkillId(SKILL_ID))
                .thenReturn(List.of(DENIED, CONTROL));
        when(preferenceService.isEnabled(anyString(), eq(NotificationCategory.PUBLISH), any()))
                .thenReturn(true);
        when(notificationService.create(anyString(), eq(NotificationCategory.PUBLISH), anyString(),
                anyString(), anyString(), eq("SKILL"), eq(SKILL_ID)))
                .thenAnswer(invocation -> notification(
                        invocation.getArgument(0),
                        invocation.getArgument(2),
                        invocation.getArgument(3),
                        invocation.getArgument(4)));

        Namespace namespace = new Namespace("demo", "Demo", CONTROL);
        ReflectionTestUtils.setField(namespace, "id", 5L);
        when(namespaceRepository.findById(5L)).thenReturn(Optional.of(namespace));

        boolean deniedShouldRead = readableRecipients.get().contains(DENIED);
        UserAccount deniedAccount = account(DENIED);
        if (!deniedShouldRead && visibility == SkillVisibility.PUBLIC && !hidden && published) {
            deniedAccount.setStatus(UserStatus.DISABLED);
        }
        when(userAccountRepository.findByIdIn(List.of(DENIED, CONTROL)))
                .thenReturn(List.of(deniedAccount, account(CONTROL)));

        NamespaceMember controlMembership = new NamespaceMember(5L, CONTROL, NamespaceRole.MEMBER);
        NamespaceRole deniedRole = visibility == SkillVisibility.PRIVATE || hidden
                ? NamespaceRole.MEMBER
                : NamespaceRole.ADMIN;
        List<NamespaceMember> memberships = visibility == SkillVisibility.NAMESPACE_ONLY && !deniedShouldRead
                ? List.of(controlMembership)
                : List.of(new NamespaceMember(5L, DENIED, deniedRole), controlMembership);
        when(namespaceMemberRepository.findByNamespaceIdAndUserIdIn(
                5L, List.of(DENIED, CONTROL))).thenReturn(memberships);
        when(userRoleBindingRepository.findByUserIdIn(List.of(DENIED, CONTROL))).thenReturn(List.of());
    }

    private Notification notification(String recipientId, String eventType, String title, String bodyJson) {
        Notification notification = new Notification(
                recipientId,
                NotificationCategory.PUBLISH,
                eventType,
                title,
                bodyJson,
                "SKILL",
                SKILL_ID,
                Instant.parse("2026-08-21T14:00:00Z"));
        ReflectionTestUtils.setField(notification, "id", recipientId.equals(CONTROL) ? 2L : 1L);
        return notification;
    }

    private UserAccount account(String userId) {
        return new UserAccount(userId, userId, userId + "@example.com", null);
    }

    private void assertDeniedAndControlSink(String eventType, String title) {
        verify(notificationService, never()).create(
                eq(DENIED), eq(NotificationCategory.PUBLISH), eq(eventType),
                eq(title), anyString(), eq("SKILL"), eq(SKILL_ID));
        verify(sseEmitterManager, never()).push(eq(DENIED), any());

        verify(notificationService).create(
                eq(CONTROL), eq(NotificationCategory.PUBLISH), eq(eventType),
                eq(title), anyString(), eq("SKILL"), eq(SKILL_ID));
        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(sseEmitterManager).push(eq(CONTROL), payload.capture());
        assertThat(payload.getValue()).containsEntry("id", 2L)
                .containsEntry("category", "PUBLISH")
                .containsEntry("eventType", eventType)
                .containsEntry("title", title)
                .containsEntry("entityType", "SKILL")
                .containsEntry("entityId", SKILL_ID)
                .containsEntry("createdAt", "2026-08-21T14:00:00Z");
    }
}
