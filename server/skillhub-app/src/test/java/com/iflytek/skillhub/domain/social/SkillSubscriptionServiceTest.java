package com.iflytek.skillhub.domain.social;

import com.iflytek.skillhub.domain.shared.exception.DomainForbiddenException;
import com.iflytek.skillhub.domain.skill.Skill;
import com.iflytek.skillhub.domain.skill.SkillRepository;
import com.iflytek.skillhub.domain.skill.SkillVisibility;
import com.iflytek.skillhub.domain.social.event.SkillSubscribedEvent;
import com.iflytek.skillhub.domain.social.event.SkillUnsubscribedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillSubscriptionServiceTest {

    @Mock private SkillSubscriptionRepository subscriptionRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private SkillSubscriptionService service;

    @BeforeEach
    void setUp() {
        service = new SkillSubscriptionService(subscriptionRepository, skillRepository, eventPublisher);
    }

    @Test
    void subscribe_createsSubscriptionAndPublishesEvent() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(mock(Skill.class)));
        when(subscriptionRepository.findBySkillIdAndUserId(1L, "user-1")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.subscribe(1L, "user-1");

        verify(subscriptionRepository).save(any(SkillSubscription.class));
        verify(skillRepository).incrementSubscriptionCount(1L);
        ArgumentCaptor<SkillSubscribedEvent> captor = ArgumentCaptor.forClass(SkillSubscribedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().skillId()).isEqualTo(1L);
        assertThat(captor.getValue().userId()).isEqualTo("user-1");
    }

    @Test
    void subscribe_idempotent_doesNotDuplicate() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(mock(Skill.class)));
        when(subscriptionRepository.findBySkillIdAndUserId(1L, "user-1"))
                .thenReturn(Optional.of(mock(SkillSubscription.class)));

        service.subscribe(1L, "user-1");

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

    private Skill publishedSkill(Long id, String ownerId, SkillVisibility visibility, boolean hidden) {
        Skill skill = new Skill(5L, "test-skill", ownerId, visibility);
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

    private void assertDeniedWithNoSideEffect(Long skillId, String userId) {
        assertThatThrownBy(() -> service.subscribe(skillId, userId))
                .isInstanceOf(DomainForbiddenException.class)
                .extracting(thrown -> ((DomainForbiddenException) thrown).messageCode())
                .isEqualTo(SUBSCRIPTION_DENIED_CODE);

        verify(subscriptionRepository, never()).save(any());
        verify(skillRepository, never()).incrementSubscriptionCount(skillId);
        verifyNoInteractions(eventPublisher);
    }

    private void assertSubscribeMutatesOnce(Long skillId, String userId) {
        service.subscribe(skillId, userId);

        verify(subscriptionRepository).save(any(SkillSubscription.class));
        verify(skillRepository).incrementSubscriptionCount(skillId);
        verify(eventPublisher).publishEvent(any(SkillSubscribedEvent.class));
    }

    /** Probe subscribe-private-denied. */
    @Test
    void subscribe_privateOrdinaryMember_deniesBeforeMutation() {
        Skill privateSkill = publishedSkill(7L, "owner-1", SkillVisibility.PRIVATE, false);
        when(skillRepository.findById(7L)).thenReturn(Optional.of(privateSkill));

        assertDeniedWithNoSideEffect(7L, "member-1");

        // control: PRIVATE stays readable for the owner/manager side, so that call still mutates once
        assertSubscribeMutatesOnce(7L, "owner-1");
    }

    /** Probe subscribe-hidden-denied. */
    @Test
    void subscribe_hiddenOrdinaryMember_deniesBeforeMutation() {
        Skill hiddenSkill = publishedSkill(8L, "owner-1", SkillVisibility.PUBLIC, true);
        when(skillRepository.findById(8L)).thenReturn(Optional.of(hiddenSkill));

        assertDeniedWithNoSideEffect(8L, "member-1");

        // control: hidden keeps owner/manager access
        assertSubscribeMutatesOnce(8L, "owner-1");
    }

    /**
     * Probe subscribe-removed-denied.
     *
     * <p>The former member carries no current namespace membership fact, so a NAMESPACE_ONLY skill
     * must not be subscribable. The control here is the owner rather than a current MEMBER: the base
     * service takes no membership collaborator, so "current MEMBER" is not expressible from this
     * entry point yet (reported as a Stage 1 plan gap).
     */
    @Test
    void subscribe_namespaceOnlyRemovedMember_deniesBeforeMutation() {
        Skill namespaceOnlySkill = publishedSkill(9L, "owner-1", SkillVisibility.NAMESPACE_ONLY, false);
        when(skillRepository.findById(9L)).thenReturn(Optional.of(namespaceOnlySkill));

        assertDeniedWithNoSideEffect(9L, "former-member-1");

        assertSubscribeMutatesOnce(9L, "owner-1");
    }
}
