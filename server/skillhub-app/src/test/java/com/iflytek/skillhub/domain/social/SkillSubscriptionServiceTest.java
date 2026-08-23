package com.iflytek.skillhub.domain.social;

import com.iflytek.skillhub.domain.namespace.Namespace;
import com.iflytek.skillhub.domain.namespace.NamespaceMember;
import com.iflytek.skillhub.domain.namespace.NamespaceMemberRepository;
import com.iflytek.skillhub.domain.namespace.NamespaceRepository;
import com.iflytek.skillhub.domain.namespace.NamespaceRole;
import com.iflytek.skillhub.domain.shared.exception.DomainForbiddenException;
import com.iflytek.skillhub.domain.skill.Skill;
import com.iflytek.skillhub.domain.skill.SkillRepository;
import com.iflytek.skillhub.domain.skill.SkillVisibility;
import com.iflytek.skillhub.domain.skill.VisibilityChecker;
import com.iflytek.skillhub.domain.social.event.SkillSubscribedEvent;
import com.iflytek.skillhub.domain.social.event.SkillUnsubscribedEvent;
import com.iflytek.skillhub.domain.user.UserAccount;
import com.iflytek.skillhub.domain.user.UserAccountRepository;
import com.iflytek.skillhub.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillSubscriptionServiceTest {

    @Mock private SkillSubscriptionRepository subscriptionRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private NamespaceRepository namespaceRepository;
    @Mock private NamespaceMemberRepository namespaceMemberRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Spy private VisibilityChecker visibilityChecker = new VisibilityChecker();

    @InjectMocks private SkillSubscriptionService service;

    @BeforeEach
    void setUp() {
        lenient().when(userAccountRepository.findById(anyString()))
                .thenAnswer(invocation -> Optional.of(activeUser(invocation.getArgument(0))));
        lenient().when(namespaceRepository.findById(5L)).thenReturn(Optional.of(activeNamespace()));
        lenient().when(namespaceMemberRepository.findByNamespaceIdAndUserId(eq(5L), anyString()))
                .thenAnswer(invocation -> Optional.of(new NamespaceMember(
                        5L, invocation.getArgument(1), NamespaceRole.MEMBER)));
    }

    @Test
    void subscribe_createsSubscriptionAndPublishesEvent() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(skill(SkillVisibility.PUBLIC, false)));
        when(subscriptionRepository.findBySkillIdAndUserId(1L, "user-1")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        subscribe(1L, "user-1", Set.of());

        verify(subscriptionRepository).save(any(SkillSubscription.class));
        verify(skillRepository).incrementSubscriptionCount(1L);
        ArgumentCaptor<SkillSubscribedEvent> captor = ArgumentCaptor.forClass(SkillSubscribedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().skillId()).isEqualTo(1L);
        assertThat(captor.getValue().userId()).isEqualTo("user-1");
    }

    @Test
    void subscribe_idempotent_doesNotDuplicate() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(skill(SkillVisibility.PUBLIC, false)));
        when(subscriptionRepository.findBySkillIdAndUserId(1L, "user-1"))
                .thenReturn(Optional.of(mock(SkillSubscription.class)));

        subscribe(1L, "user-1", Set.of());

        verify(subscriptionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void unsubscribe_deletesSubscriptionAndPublishesEvent() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(mock(Skill.class)));
        SkillSubscription existing = mock(SkillSubscription.class);
        when(subscriptionRepository.findBySkillIdAndUserId(1L, "user-1")).thenReturn(Optional.of(existing));

        service.unsubscribe(1L, "user-1");

        verify(subscriptionRepository).delete(existing);
        verify(skillRepository).decrementSubscriptionCount(1L);
        ArgumentCaptor<SkillUnsubscribedEvent> captor = ArgumentCaptor.forClass(SkillUnsubscribedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().skillId()).isEqualTo(1L);
    }

    @Test
    void unsubscribe_noOp_whenNotSubscribed() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(mock(Skill.class)));
        when(subscriptionRepository.findBySkillIdAndUserId(1L, "user-1")).thenReturn(Optional.empty());

        service.unsubscribe(1L, "user-1");

        verify(subscriptionRepository, never()).delete(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void isSubscribed_returnsTrue_whenSubscriptionExists() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(mock(Skill.class)));
        when(subscriptionRepository.findBySkillIdAndUserId(1L, "user-1"))
                .thenReturn(Optional.of(mock(SkillSubscription.class)));

        assertThat(service.isSubscribed(1L, "user-1")).isTrue();
    }

    @Test
    void isSubscribed_returnsFalse_whenNoSubscription() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(mock(Skill.class)));
        when(subscriptionRepository.findBySkillIdAndUserId(1L, "user-1")).thenReturn(Optional.empty());

        assertThat(service.isSubscribed(1L, "user-1")).isFalse();
    }

    @Test
    void subscribe_privateOrdinaryMember_deniesBeforeMutation() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(skill(SkillVisibility.PRIVATE, false)));

        assertForbiddenWithoutMutation("user-1");
    }

    @Test
    void subscribe_hiddenOrdinaryMember_deniesBeforeMutation() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(skill(SkillVisibility.PUBLIC, true)));

        assertForbiddenWithoutMutation("user-1");
    }

    @Test
    void subscribe_namespaceOnlyRemovedMember_deniesBeforeMutation() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(skill(SkillVisibility.NAMESPACE_ONLY, false)));
        when(namespaceMemberRepository.findByNamespaceIdAndUserId(5L, "user-1"))
                .thenReturn(Optional.empty());

        assertForbiddenWithoutMutation("user-1");
    }

    @Test
    void subscribe_disabledUser_deniesEvenWithOtherwiseReadableSkill() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(skill(SkillVisibility.PUBLIC, false)));
        UserAccount disabled = activeUser("user-1");
        disabled.setStatus(UserStatus.DISABLED);
        when(userAccountRepository.findById("user-1")).thenReturn(Optional.of(disabled));

        assertForbiddenWithoutMutation("user-1");
    }

    @Test
    void subscribe_namespaceOnlyCurrentMember_allows() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(skill(SkillVisibility.NAMESPACE_ONLY, false)));
        when(subscriptionRepository.findBySkillIdAndUserId(1L, "user-1")).thenReturn(Optional.empty());

        subscribe(1L, "user-1", Set.of());

        verify(subscriptionRepository).save(any(SkillSubscription.class));
        verify(skillRepository).incrementSubscriptionCount(1L);
        verify(eventPublisher).publishEvent(new SkillSubscribedEvent(1L, "user-1"));
    }

    @Test
    void unsubscribe_afterMetadataRevocation_stillDeletesOwnRow() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(mock(Skill.class)));
        SkillSubscription existing = mock(SkillSubscription.class);
        when(subscriptionRepository.findBySkillIdAndUserId(1L, "user-1")).thenReturn(Optional.of(existing));

        service.unsubscribe(1L, "user-1");

        verify(subscriptionRepository).delete(existing);
        verify(skillRepository).decrementSubscriptionCount(1L);
    }

    @Test
    void isSubscribed_staleOwnRow_remainsTrueWithoutMetadata() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(mock(Skill.class)));
        when(subscriptionRepository.findBySkillIdAndUserId(1L, "user-1"))
                .thenReturn(Optional.of(mock(SkillSubscription.class)));

        assertThat(service.isSubscribed(1L, "user-1")).isTrue();
        verifyNoInteractions(userAccountRepository, namespaceRepository, namespaceMemberRepository);
    }

    private void assertForbiddenWithoutMutation(String userId) {
        assertThatThrownBy(() -> subscribe(1L, userId, Set.of()))
                .isInstanceOf(DomainForbiddenException.class)
                .extracting("messageCode")
                .isEqualTo("error.skill.subscription.noPermission");
        verify(subscriptionRepository, never()).save(any());
        verify(skillRepository, never()).incrementSubscriptionCount(anyLong());
        verify(eventPublisher, never()).publishEvent(any());
    }

    /**
     * Every probe goes through the authenticated platform-role entry point the controller calls.
     * The empty-role overload no longer exists, so no test can hide where the roles came from.
     */
    private void subscribe(Long skillId, String userId, Set<String> platformRoles) {
        service.subscribe(skillId, userId, platformRoles);
    }

    @Test
    void subscribe_exposesOnlyThePlatformRoleAwareEntryPoint() {
        assertThat(Arrays.stream(SkillSubscriptionService.class.getMethods())
                .filter(method -> method.getName().equals("subscribe"))
                .map(Method::getParameterCount)
                .toList())
                .as("the empty-role two-argument subscribe must not survive")
                .containsExactly(3);
    }

    private Skill skill(SkillVisibility visibility, boolean hidden) {
        Skill skill = new Skill(5L, "test-skill", "owner-1", visibility);
        skill.setLatestVersionId(10L);
        skill.setHidden(hidden);
        return skill;
    }

    private UserAccount activeUser(String userId) {
        return new UserAccount(userId, userId, userId + "@example.com", null);
    }

    private Namespace activeNamespace() {
        Namespace namespace = new Namespace("demo", "Demo", "owner-1");
        ReflectionTestUtils.setField(namespace, "id", 5L);
        return namespace;
    }
}
