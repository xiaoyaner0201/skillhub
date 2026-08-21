package com.iflytek.skillhub.listener;

import com.iflytek.skillhub.auth.entity.Role;
import com.iflytek.skillhub.auth.entity.UserRoleBinding;
import com.iflytek.skillhub.auth.repository.UserRoleBindingRepository;
import com.iflytek.skillhub.domain.namespace.Namespace;
import com.iflytek.skillhub.domain.namespace.NamespaceMember;
import com.iflytek.skillhub.domain.namespace.NamespaceMemberRepository;
import com.iflytek.skillhub.domain.namespace.NamespaceRole;
import com.iflytek.skillhub.domain.skill.Skill;
import com.iflytek.skillhub.domain.skill.SkillVisibility;
import com.iflytek.skillhub.domain.skill.VisibilityChecker;
import com.iflytek.skillhub.domain.user.UserAccount;
import com.iflytek.skillhub.domain.user.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriberAccessResolverTest {

    @Mock private UserAccountRepository userAccountRepository;
    @Mock private NamespaceMemberRepository namespaceMemberRepository;
    @Mock private UserRoleBindingRepository userRoleBindingRepository;
    @Mock private VisibilityChecker visibilityChecker;

    private SubscriberAccessResolver resolver;
    private Skill skill;
    private Namespace namespace;

    @BeforeEach
    void setUp() {
        resolver = new SubscriberAccessResolver(
                userAccountRepository,
                namespaceMemberRepository,
                userRoleBindingRepository,
                visibilityChecker);
        skill = new Skill(5L, "test-skill", "owner", SkillVisibility.PUBLIC);
        skill.setLatestVersionId(10L);
        namespace = new Namespace("demo", "Demo", "owner");
        ReflectionTestUtils.setField(namespace, "id", 5L);
    }

    @Test
    void readableRecipients_arePreservedElementForElementInCandidateOrder() {
        UserAccount alpha = account("alpha");
        UserAccount beta = account("beta");
        NamespaceMember alphaMember = new NamespaceMember(5L, "alpha", NamespaceRole.MEMBER);
        NamespaceMember betaMember = new NamespaceMember(5L, "beta", NamespaceRole.ADMIN);
        when(userAccountRepository.findByIdIn(List.of("alpha", "beta", "denied")))
                .thenReturn(List.of(alpha, beta, account("denied")));
        when(namespaceMemberRepository.findByNamespaceIdAndUserIdIn(
                5L, List.of("alpha", "beta", "denied")))
                .thenReturn(List.of(alphaMember, betaMember));
        when(userRoleBindingRepository.findByUserIdIn(List.of("alpha", "beta", "denied")))
                .thenReturn(List.of());
        when(visibilityChecker.canAccess(skill, alpha, namespace, NamespaceRole.MEMBER, Set.of()))
                .thenReturn(true);
        when(visibilityChecker.canAccess(skill, beta, namespace, NamespaceRole.ADMIN, Set.of()))
                .thenReturn(true);

        List<String> filtered = resolver.resolveReadableSubscribers(
                skill, namespace, List.of("alpha", "beta", "denied", "alpha"));

        assertThat(filtered).containsExactlyElementsOf(List.of("alpha", "beta"));
        verify(userAccountRepository).findByIdIn(List.of("alpha", "beta", "denied"));
        verify(namespaceMemberRepository).findByNamespaceIdAndUserIdIn(
                5L, List.of("alpha", "beta", "denied"));
        verify(userRoleBindingRepository).findByUserIdIn(List.of("alpha", "beta", "denied"));
    }

    @Test
    void roleCodesAreFactsPassedToCheckerWithoutResolverDecision() {
        UserAccount user = account("super");
        NamespaceMember membership = new NamespaceMember(5L, "super", NamespaceRole.MEMBER);
        Role role = new Role();
        ReflectionTestUtils.setField(role, "code", "SUPER_ADMIN");
        when(userAccountRepository.findByIdIn(List.of("super"))).thenReturn(List.of(user));
        when(namespaceMemberRepository.findByNamespaceIdAndUserIdIn(5L, List.of("super")))
                .thenReturn(List.of(membership));
        when(userRoleBindingRepository.findByUserIdIn(List.of("super")))
                .thenReturn(List.of(new UserRoleBinding("super", role)));
        when(visibilityChecker.canAccess(
                skill, user, namespace, NamespaceRole.MEMBER, Set.of("SUPER_ADMIN")))
                .thenReturn(true);

        assertThat(resolver.resolveReadableSubscribers(skill, namespace, List.of("super")))
                .containsExactly("super");
    }

    @Test
    void batchFailure_happensBeforeEveryPublishedSink() {
        when(userAccountRepository.findByIdIn(anyList())).thenThrow(new IllegalStateException("batch failed"));

        assertThatThrownBy(() -> resolver.resolveReadableSubscribers(skill, namespace, List.of("user")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("batch failed");
        verify(namespaceMemberRepository, never()).findByNamespaceIdAndUserIdIn(any(), any());
        verify(userRoleBindingRepository, never()).findByUserIdIn(any());
    }

    @Test
    void batchFailure_happensBeforeEveryYankedSink() {
        when(userAccountRepository.findByIdIn(anyList())).thenThrow(new IllegalStateException("batch failed"));

        assertThatThrownBy(() -> resolver.resolveReadableSubscribers(skill, namespace, List.of("user")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("batch failed");
        verify(visibilityChecker, never()).canAccess(any(), any(), any(), any(), any());
    }

    private UserAccount account(String id) {
        return new UserAccount(id, id, id + "@example.com", null);
    }
}
