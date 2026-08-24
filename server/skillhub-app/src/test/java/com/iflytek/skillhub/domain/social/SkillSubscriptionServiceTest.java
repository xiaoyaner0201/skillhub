package com.iflytek.skillhub.domain.social;

import com.iflytek.skillhub.domain.namespace.NamespaceMember;
import com.iflytek.skillhub.domain.namespace.NamespaceMemberRepository;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillSubscriptionServiceTest {

    private static final Long NAMESPACE_ID = 5L;
    private static final Set<String> ORDINARY_ROLES = Set.of("USER");

    @Mock private SkillSubscriptionRepository subscriptionRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private NamespaceMemberRepository namespaceMemberRepository;
    @Mock private UserAccountRepository userAccountRepository;

    private SkillSubscriptionService service;

    @BeforeEach
    void setUp() {
        service = new SkillSubscriptionService(subscriptionRepository, skillRepository, eventPublisher,
                new VisibilityChecker(), namespaceMemberRepository, userAccountRepository);
    }

    // ------------------------------------------------------------------
    // Fixtures
    //
    // The real VisibilityChecker is used rather than a mock, so every assertion below binds to the
    // single visibility decision source instead of to a stubbed answer.
    // ------------------------------------------------------------------

    private Skill publishedSkill(Long id, String ownerId, SkillVisibility visibility, boolean hidden) {
        Skill skill = new Skill(NAMESPACE_ID, "test-skill", ownerId, visibility);
        skill.setDisplayName("Test Skill");
        skill.setLatestVersionId(90L);
        skill.setHidden(hidden);
        setId(skill, id);
        return skill;
    }

    private void setId(Skill skill, Long id) {
        try {
            java.lang.reflect.Field field = Skill.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(skill, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void givenSkill(Long id, Skill skill) {
        lenient().when(skillRepository.findById(id)).thenReturn(Optional.of(skill));
    }

    /** An account row that exists and is ACTIVE. Unstubbed ids resolve empty, i.e. fail closed. */
    private void givenActiveAccount(String... userIds) {
        for (String userId : userIds) {
            givenAccount(userId, UserStatus.ACTIVE);
        }
    }

    private void givenAccount(String userId, UserStatus status) {
        UserAccount account = new UserAccount(userId, userId, userId + "@example.com", null);
        account.setStatus(status);
        lenient().when(userAccountRepository.findById(userId)).thenReturn(Optional.of(account));
    }

    /** A current membership row. Unstubbed ids resolve empty, i.e. "no current role". */
    private void givenMembership(String userId, NamespaceRole role) {
        lenient().when(namespaceMemberRepository.findByNamespaceIdAndUserId(NAMESPACE_ID, userId))
                .thenReturn(Optional.of(new NamespaceMember(NAMESPACE_ID, userId, role)));
    }

    private void givenNotYetSubscribed(Long skillId, String userId) {
        lenient().when(subscriptionRepository.findBySkillIdAndUserId(skillId, userId))
                .thenReturn(Optional.empty());
        lenient().when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void subscribe_createsSubscriptionAndPublishesEvent() {
        givenSkill(1L, publishedSkill(1L, "owner-1", SkillVisibility.PUBLIC, false));
        givenActiveAccount("user-1");
        givenNotYetSubscribed(1L, "user-1");

        service.subscribe(1L, "user-1", ORDINARY_ROLES);

        verify(subscriptionRepository).save(any(SkillSubscription.class));
        verify(skillRepository).incrementSubscriptionCount(1L);
        ArgumentCaptor<SkillSubscribedEvent> captor = ArgumentCaptor.forClass(SkillSubscribedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().skillId()).isEqualTo(1L);
        assertThat(captor.getValue().userId()).isEqualTo("user-1");
    }

    @Test
    void subscribe_idempotent_doesNotDuplicate() {
        givenSkill(1L, publishedSkill(1L, "owner-1", SkillVisibility.PUBLIC, false));
        givenActiveAccount("user-1");
        when(subscriptionRepository.findBySkillIdAndUserId(1L, "user-1"))
                .thenReturn(Optional.of(mock(SkillSubscription.class)));

        service.subscribe(1L, "user-1", ORDINARY_ROLES);

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

    // ------------------------------------------------------------------
    // HD-32 path subscription-create: metadata-read authorization probes.
    //
    // Each probe drives the real subscribe entry point and asserts the three
    // side-effect sinks the plan names: the subscription row, the skill
    // subscription counter, and the SkillSubscribedEvent. A denial has to land
    // before all three, not after.
    // ------------------------------------------------------------------

    private static final String SUBSCRIPTION_DENIED_CODE = "error.skill.subscription.noPermission";

    private void assertDeniedWithNoSideEffect(Long skillId, String userId) {
        assertThatThrownBy(() -> service.subscribe(skillId, userId, ORDINARY_ROLES))
                .isInstanceOf(DomainForbiddenException.class)
                .extracting(thrown -> ((DomainForbiddenException) thrown).messageCode())
                .isEqualTo(SUBSCRIPTION_DENIED_CODE);

        verify(subscriptionRepository, never()).save(any());
        verify(skillRepository, never()).incrementSubscriptionCount(skillId);
        verifyNoInteractions(eventPublisher);
    }

    private void assertSubscribeMutatesOnce(Long skillId, String userId) {
        service.subscribe(skillId, userId, ORDINARY_ROLES);

        verify(subscriptionRepository).save(any(SkillSubscription.class));
        verify(skillRepository).incrementSubscriptionCount(skillId);
        verify(eventPublisher).publishEvent(any(SkillSubscribedEvent.class));
    }

    /** Probe subscribe-private-denied. Control is the namespace manager, per the frozen sink text. */
    @Test
    void subscribe_privateOrdinaryMember_deniesBeforeMutation() {
        givenSkill(7L, publishedSkill(7L, "owner-1", SkillVisibility.PRIVATE, false));
        givenActiveAccount("member-1", "manager-1");
        givenMembership("member-1", NamespaceRole.MEMBER);
        givenMembership("manager-1", NamespaceRole.ADMIN);
        givenNotYetSubscribed(7L, "manager-1");

        assertDeniedWithNoSideEffect(7L, "member-1");

        // control: PRIVATE stays readable for the manager side, so that call still mutates once
        assertSubscribeMutatesOnce(7L, "manager-1");
    }

    /** Probe subscribe-hidden-denied. */
    @Test
    void subscribe_hiddenOrdinaryMember_deniesBeforeMutation() {
        givenSkill(8L, publishedSkill(8L, "owner-1", SkillVisibility.PUBLIC, true));
        givenActiveAccount("member-1", "owner-1");
        givenMembership("member-1", NamespaceRole.MEMBER);
        givenNotYetSubscribed(8L, "owner-1");

        assertDeniedWithNoSideEffect(8L, "member-1");

        // control: hidden keeps owner/manager access
        assertSubscribeMutatesOnce(8L, "owner-1");
    }

    /**
     * Probe subscribe-removed-denied.
     *
     * <p>The former member carries no current namespace membership fact, so a NAMESPACE_ONLY skill
     * must not be subscribable. The control is a current MEMBER in the same namespace - now
     * expressible, because the entry point takes a membership collaborator as of this stage.
     */
    @Test
    void subscribe_namespaceOnlyRemovedMember_deniesBeforeMutation() {
        givenSkill(9L, publishedSkill(9L, "owner-1", SkillVisibility.NAMESPACE_ONLY, false));
        givenActiveAccount("former-member-1", "member-1");
        givenMembership("member-1", NamespaceRole.MEMBER);
        givenNotYetSubscribed(9L, "member-1");

        assertDeniedWithNoSideEffect(9L, "former-member-1");

        assertSubscribeMutatesOnce(9L, "member-1");
    }

    /**
     * Probe subscribe-disabled-denied.
     *
     * <p>The skill is PUBLIC and published, so visibility alone would admit anyone. What must deny
     * here is the account fact: a non-ACTIVE account reaches none of the three sinks.
     */
    @Test
    void subscribe_disabledUser_deniesEvenWithOtherwiseReadableSkill() {
        givenSkill(10L, publishedSkill(10L, "owner-1", SkillVisibility.PUBLIC, false));
        givenAccount("disabled-user-1", UserStatus.DISABLED);
        givenActiveAccount("active-user-1");
        givenNotYetSubscribed(10L, "active-user-1");

        assertDeniedWithNoSideEffect(10L, "disabled-user-1");

        // control: the same PUBLIC skill is subscribable by an active account
        assertSubscribeMutatesOnce(10L, "active-user-1");
    }
}
