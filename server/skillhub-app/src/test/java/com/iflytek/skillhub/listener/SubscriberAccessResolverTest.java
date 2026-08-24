package com.iflytek.skillhub.listener;

import com.iflytek.skillhub.auth.rbac.RbacService;
import com.iflytek.skillhub.domain.namespace.NamespaceMember;
import com.iflytek.skillhub.domain.namespace.NamespaceMemberRepository;
import com.iflytek.skillhub.domain.namespace.NamespaceRole;
import com.iflytek.skillhub.domain.skill.Skill;
import com.iflytek.skillhub.domain.skill.VisibilityChecker;
import com.iflytek.skillhub.domain.skill.SkillVisibility;
import com.iflytek.skillhub.domain.user.UserAccount;
import com.iflytek.skillhub.domain.user.UserAccountRepository;
import com.iflytek.skillhub.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HD-32 path current-authority-resolution: authority-fact probes.
 *
 * <p>The subject is {@link SubscriberAccessResolver}, the shared seam that turns a candidate
 * recipient set into the set that may currently read a skill. The two probes here pin the two
 * facts that a retained subscription row cannot substitute for: current namespace membership and
 * current account status. The real {@link VisibilityChecker} is used rather than a mock, so the
 * assertions bind to the single visibility decision source instead of to a stubbed answer.
 */
@ExtendWith(MockitoExtension.class)
class SubscriberAccessResolverTest {

    private static final Long NAMESPACE_ID = 5L;
    private static final Long SKILL_ID = 42L;
    private static final Long VERSION_ID = 90L;

    private static final String OWNER = "owner-1";
    private static final String CURRENT_MEMBER = "member-1";
    private static final String FORMER_MEMBER = "former-member-1";
    private static final String DISABLED_MEMBER = "disabled-member-1";

    @Mock private UserAccountRepository userAccountRepository;
    @Mock private NamespaceMemberRepository namespaceMemberRepository;
    @Mock private RbacService rbacService;

    private SubscriberAccessResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new SubscriberAccessResolver(userAccountRepository, namespaceMemberRepository,
                rbacService, new VisibilityChecker());
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private Skill skill(SkillVisibility visibility) {
        Skill skill = new Skill(NAMESPACE_ID, "test-skill", OWNER, visibility);
        skill.setDisplayName("Test Skill");
        skill.setLatestVersionId(VERSION_ID);
        ReflectionTestUtils.setField(skill, "id", SKILL_ID);
        return skill;
    }

    private UserAccount account(String id, UserStatus status) {
        UserAccount user = new UserAccount(id, id, id + "@example.com", null);
        user.setStatus(status);
        return user;
    }

    /** Every candidate has an account row; only those named in {@code activeIds} are ACTIVE. */
    private void givenAccounts(List<String> candidateIds, Set<String> activeIds) {
        List<UserAccount> accounts = new ArrayList<>();
        for (String id : candidateIds) {
            accounts.add(account(id, activeIds.contains(id) ? UserStatus.ACTIVE : UserStatus.DISABLED));
        }
        lenient().when(userAccountRepository.findByIdIn(any())).thenReturn(accounts);
    }

    /** Only the ids in {@code memberRoles} hold a current membership row in the namespace. */
    private void givenMemberships(Map<String, NamespaceRole> memberRoles) {
        List<NamespaceMember> members = memberRoles.entrySet().stream()
                .map(e -> new NamespaceMember(NAMESPACE_ID, e.getKey(), e.getValue()))
                .toList();
        lenient().when(namespaceMemberRepository.findByNamespaceIdAndUserIdIn(anyLong(), any())).thenReturn(members);
    }

    /** Default platform roles for every candidate, unless a specific id is overridden. */
    private void givenPlatformRoles(List<String> candidateIds, Map<String, Set<String>> overrides) {
        Map<String, Set<String>> roles = new LinkedHashMap<>();
        for (String id : candidateIds) {
            roles.put(id, overrides.getOrDefault(id, Set.of("USER")));
        }
        lenient().when(rbacService.getUserRoleCodesIn(any())).thenReturn(roles);
    }

    /**
     * Guards NO_N_PLUS_ONE: each authority source is read exactly once for the whole candidate set,
     * and no per-candidate lookup happens inside the decision loop.
     */
    private void assertOneBatchPerSourceAndNoLoopLookup() {
        verify(userAccountRepository, times(1)).findByIdIn(any());
        verify(namespaceMemberRepository, times(1)).findByNamespaceIdAndUserIdIn(anyLong(), any());
        verify(rbacService, times(1)).getUserRoleCodesIn(any());
        verify(userAccountRepository, never()).findById(anyString());
        verify(namespaceMemberRepository, never()).findByNamespaceIdAndUserId(anyLong(), anyString());
        verify(rbacService, never()).getUserRoleCodes(anyString());
    }

    // ------------------------------------------------------------------
    // Probes
    // ------------------------------------------------------------------

    /**
     * Probe authority-removed-member.
     *
     * <p>The former member still carries a retained subscription row - that is exactly why they are
     * a candidate - but holds no current membership row. Absent membership must reach the checker as
     * "no role" and deny; the current MEMBER in the same batch is the control that proves the
     * resolver is not simply returning nothing.
     */
    @Test
    void removedMember_isDeniedFromCurrentFacts() {
        List<String> candidates = List.of(FORMER_MEMBER, CURRENT_MEMBER);
        givenAccounts(candidates, Set.of(FORMER_MEMBER, CURRENT_MEMBER));
        givenMemberships(Map.of(CURRENT_MEMBER, NamespaceRole.MEMBER));
        givenPlatformRoles(candidates, Map.of());

        List<String> readable = resolver.resolveReadableRecipients(skill(SkillVisibility.NAMESPACE_ONLY), candidates);

        assertThat(readable)
                .as("a retained subscription row is history, not authority")
                .containsExactly(CURRENT_MEMBER);
        assertOneBatchPerSourceAndNoLoopLookup();
    }

    /**
     * Probe authority-disabled-user.
     *
     * <p>The disabled candidate holds bindings that would otherwise win outright - SUPER_ADMIN
     * bypasses every visibility gate, and a namespace OWNER row grants PRIVATE access. Neither may
     * override an inactive account: account status is evaluated fail-closed ahead of role strength.
     */
    @Test
    void disabledUser_isDeniedEvenWithBindings() {
        List<String> candidates = List.of(DISABLED_MEMBER, CURRENT_MEMBER);
        givenAccounts(candidates, Set.of(CURRENT_MEMBER));
        givenMemberships(Map.of(
                DISABLED_MEMBER, NamespaceRole.OWNER,
                CURRENT_MEMBER, NamespaceRole.ADMIN));
        givenPlatformRoles(candidates, Map.of(DISABLED_MEMBER, Set.of("USER", "SUPER_ADMIN")));

        List<String> readable = resolver.resolveReadableRecipients(skill(SkillVisibility.PRIVATE), candidates);

        assertThat(readable)
                .as("role bindings cannot override an inactive account")
                .containsExactly(CURRENT_MEMBER);
        assertOneBatchPerSourceAndNoLoopLookup();
    }
}
