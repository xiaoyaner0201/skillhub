package com.iflytek.skillhub.domain.skill;

import com.iflytek.skillhub.domain.namespace.NamespaceRole;
import com.iflytek.skillhub.domain.namespace.Namespace;
import com.iflytek.skillhub.domain.namespace.NamespaceStatus;
import com.iflytek.skillhub.domain.user.UserAccount;
import com.iflytek.skillhub.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class VisibilityCheckerTest {

    private VisibilityChecker checker;
    private Skill publicSkill;
    private Skill namespaceOnlySkill;
    private Skill privateSkill;
    private Skill unpublishedPublicSkill;
    private Skill hiddenPublicSkill;

    private static final Long NAMESPACE_ID = 1L;
    private static final String OWNER_ID = "user-100";
    private static final String OTHER_USER_ID = "user-200";
    private static final String ADMIN_USER_ID = "user-300";
    private static final String NAMESPACE_OWNER_ID = "user-400";

    @BeforeEach
    void setUp() {
        checker = new VisibilityChecker();

        publicSkill = new Skill(NAMESPACE_ID, "public-skill", OWNER_ID, SkillVisibility.PUBLIC);
        publicSkill.setLatestVersionId(10L);
        namespaceOnlySkill = new Skill(NAMESPACE_ID, "namespace-skill", OWNER_ID, SkillVisibility.NAMESPACE_ONLY);
        namespaceOnlySkill.setLatestVersionId(11L);
        privateSkill = new Skill(NAMESPACE_ID, "private-skill", OWNER_ID, SkillVisibility.PRIVATE);
        privateSkill.setLatestVersionId(12L);
        unpublishedPublicSkill = new Skill(NAMESPACE_ID, "draft-public-skill", OWNER_ID, SkillVisibility.PUBLIC);
        hiddenPublicSkill = new Skill(NAMESPACE_ID, "hidden-public-skill", OWNER_ID, SkillVisibility.PUBLIC);
        hiddenPublicSkill.setLatestVersionId(13L);
        hiddenPublicSkill.setHidden(true);
    }

    @Test
    void testPublicSkillAccessibleByAnonymous() {
        boolean canAccess = checker.canAccess(publicSkill, null, Map.of());
        assertTrue(canAccess);
    }

    @Test
    void testPublicSkillAccessibleByAnyUser() {
        boolean canAccess = checker.canAccess(publicSkill, OTHER_USER_ID, Map.of());
        assertTrue(canAccess);
    }

    @Test
    void testNamespaceOnlySkillNotAccessibleByAnonymous() {
        boolean canAccess = checker.canAccess(namespaceOnlySkill, null, Map.of());
        assertFalse(canAccess);
    }

    @Test
    void testNamespaceOnlySkillNotAccessibleByNonMember() {
        boolean canAccess = checker.canAccess(namespaceOnlySkill, OTHER_USER_ID, Map.of());
        assertFalse(canAccess);
    }

    @Test
    void testNamespaceOnlySkillAccessibleByMember() {
        Map<Long, NamespaceRole> roles = Map.of(NAMESPACE_ID, NamespaceRole.MEMBER);
        boolean canAccess = checker.canAccess(namespaceOnlySkill, OTHER_USER_ID, roles);
        assertTrue(canAccess);
    }

    @Test
    void testPrivateSkillNotAccessibleByAnonymous() {
        boolean canAccess = checker.canAccess(privateSkill, null, Map.of());
        assertFalse(canAccess);
    }

    @Test
    void testPrivateSkillAccessibleByOwner() {
        boolean canAccess = checker.canAccess(privateSkill, OWNER_ID, Map.of());
        assertTrue(canAccess);
    }

    @Test
    void testPrivateSkillAccessibleByAdmin() {
        Map<Long, NamespaceRole> roles = Map.of(NAMESPACE_ID, NamespaceRole.ADMIN);
        boolean canAccess = checker.canAccess(privateSkill, ADMIN_USER_ID, roles);
        assertTrue(canAccess);
    }

    @Test
    void testPrivateSkillAccessibleByNamespaceOwner() {
        Map<Long, NamespaceRole> roles = Map.of(NAMESPACE_ID, NamespaceRole.OWNER);
        boolean canAccess = checker.canAccess(privateSkill, NAMESPACE_OWNER_ID, roles);
        assertTrue(canAccess);
    }

    @Test
    void testPrivateSkillNotAccessibleByRegularMember() {
        Map<Long, NamespaceRole> roles = Map.of(NAMESPACE_ID, NamespaceRole.MEMBER);
        boolean canAccess = checker.canAccess(privateSkill, OTHER_USER_ID, roles);
        assertFalse(canAccess);
    }

    @Test
    void testPrivateSkillNotAccessibleByNonMember() {
        boolean canAccess = checker.canAccess(privateSkill, OTHER_USER_ID, Map.of());
        assertFalse(canAccess);
    }

    @Test
    void testUnpublishedSkillNotAccessibleByAnonymousEvenWhenPublic() {
        boolean canAccess = checker.canAccess(unpublishedPublicSkill, null, Map.of());
        assertFalse(canAccess);
    }

    @Test
    void testUnpublishedSkillNotAccessibleByOtherUserEvenWhenPublic() {
        boolean canAccess = checker.canAccess(unpublishedPublicSkill, OTHER_USER_ID, Map.of());
        assertFalse(canAccess);
    }

    @Test
    void testUnpublishedSkillNotAccessibleByAdmin() {
        Map<Long, NamespaceRole> roles = Map.of(NAMESPACE_ID, NamespaceRole.ADMIN);
        boolean canAccess = checker.canAccess(unpublishedPublicSkill, ADMIN_USER_ID, roles);
        assertFalse(canAccess);
    }

    @Test
    void testUnpublishedSkillAccessibleByOwner() {
        boolean canAccess = checker.canAccess(unpublishedPublicSkill, OWNER_ID, Map.of());
        assertTrue(canAccess);
    }

    @Test
    void testHiddenSkillNotAccessibleByAnonymous() {
        boolean canAccess = checker.canAccess(hiddenPublicSkill, null, Map.of());
        assertFalse(canAccess);
    }

    @Test
    void testHiddenSkillNotAccessibleByOtherUser() {
        boolean canAccess = checker.canAccess(hiddenPublicSkill, OTHER_USER_ID, Map.of());
        assertFalse(canAccess);
    }

    @Test
    void testHiddenSkillAccessibleByOwner() {
        boolean canAccess = checker.canAccess(hiddenPublicSkill, OWNER_ID, Map.of());
        assertTrue(canAccess);
    }

    @Test
    void testHiddenSkillAccessibleByNamespaceAdmin() {
        Map<Long, NamespaceRole> roles = Map.of(NAMESPACE_ID, NamespaceRole.ADMIN);
        boolean canAccess = checker.canAccess(hiddenPublicSkill, ADMIN_USER_ID, roles);
        assertTrue(canAccess);
    }

    @Test
    void testSuperAdminCanAccessPrivateSkill() {
        boolean canAccess = checker.canAccess(privateSkill, OTHER_USER_ID, Map.of(), Set.of("SUPER_ADMIN"));
        assertTrue(canAccess);
    }

    @Test
    void testSuperAdminCanAccessHiddenSkill() {
        boolean canAccess = checker.canAccess(hiddenPublicSkill, OTHER_USER_ID, Map.of(), Set.of("SUPER_ADMIN"));
        assertTrue(canAccess);
    }

    @Test
    void testSuperAdminCanAccessUnpublishedSkill() {
        boolean canAccess = checker.canAccess(unpublishedPublicSkill, OTHER_USER_ID, Map.of(), Set.of("SUPER_ADMIN"));
        assertTrue(canAccess);
    }

    @Test
    void testNonSuperAdminPlatformRolesDoNotGrantAccess() {
        boolean canAccess = checker.canAccess(privateSkill, OTHER_USER_ID, Map.of(), Set.of("REVIEWER"));
        assertFalse(canAccess);
    }

    @Test
    void testEmptyPlatformRolesDoNotGrantAccess() {
        boolean canAccess = checker.canAccess(privateSkill, OTHER_USER_ID, Map.of(), Set.of());
        assertFalse(canAccess);
    }

    @Test
    void fullContext_missingAccount_deniesBeforeSuperAdmin() {
        assertFalse(checker.canAccess(hiddenPublicSkill, null, archivedNamespace(), null,
                Set.of("SUPER_ADMIN")));
    }

    @Test
    void fullContext_disabledAccount_deniesBeforeSuperAdmin() {
        UserAccount disabled = account(OTHER_USER_ID);
        disabled.setStatus(UserStatus.DISABLED);

        assertFalse(checker.canAccess(hiddenPublicSkill, disabled, archivedNamespace(), null,
                Set.of("SUPER_ADMIN")));
    }

    @Test
    void fullContext_activeSuperAdmin_allowsArchivedHiddenPrivate() {
        Skill hiddenPrivate = new Skill(NAMESPACE_ID, "hidden-private", OWNER_ID, SkillVisibility.PRIVATE);
        hiddenPrivate.setHidden(true);

        assertTrue(checker.canAccess(hiddenPrivate, account(OTHER_USER_ID), archivedNamespace(), null,
                Set.of("SUPER_ADMIN")));
    }

    @Test
    void fullContext_archivedNonMember_deniesOtherwisePublic() {
        assertFalse(checker.canAccess(publicSkill, account(OTHER_USER_ID), archivedNamespace(), null,
                Set.of()));
    }

    @Test
    void fullContext_archivedCurrentMember_usesExistingVisibilityRules() {
        assertTrue(checker.canAccess(namespaceOnlySkill, account(OTHER_USER_ID), archivedNamespace(),
                NamespaceRole.MEMBER, Set.of()));
        assertFalse(checker.canAccess(privateSkill, account(OTHER_USER_ID), archivedNamespace(),
                NamespaceRole.MEMBER, Set.of()));
    }

    // --- YANK_REVOCATION_NOTICE_REACHABILITY (plan-r2 R-P1-01) ---
    // The revocation notice must survive the latest-null state that this very yank created,
    // and must not relax any other guard. Purpose is looked up reflectively so the probe
    // compiles against the pre-fix checker and fails on the missing behavior, not on javac.

    @Test
    void yankRevocationPurpose_latestNullPublicNonOwner_isReachable() {
        assertTrue(canAccessForPurpose(unpublishedPublicSkill, account(OTHER_USER_ID), liveNamespace(),
                null, Set.of("USER"), "YANK_REVOCATION_NOTICE"));
    }

    @Test
    void yankRevocationPurpose_latestNullPrivate_deniesOrdinaryMemberAndAllowsNamespaceAdmin() {
        Skill privateDraft = new Skill(NAMESPACE_ID, "private-draft", OWNER_ID, SkillVisibility.PRIVATE);

        assertFalse(canAccessForPurpose(privateDraft, account(OTHER_USER_ID), liveNamespace(),
                NamespaceRole.MEMBER, Set.of("USER"), "YANK_REVOCATION_NOTICE"));
        assertTrue(canAccessForPurpose(privateDraft, account(ADMIN_USER_ID), liveNamespace(),
                NamespaceRole.ADMIN, Set.of("USER"), "YANK_REVOCATION_NOTICE"));
    }

    @Test
    void yankRevocationPurpose_latestNullHidden_deniesOrdinaryMemberAndAllowsOwner() {
        Skill hiddenDraft = new Skill(NAMESPACE_ID, "hidden-draft", OWNER_ID, SkillVisibility.PUBLIC);
        hiddenDraft.setHidden(true);

        assertFalse(canAccessForPurpose(hiddenDraft, account(OTHER_USER_ID), liveNamespace(),
                NamespaceRole.MEMBER, Set.of("USER"), "YANK_REVOCATION_NOTICE"));
        assertTrue(canAccessForPurpose(hiddenDraft, account(OWNER_ID), liveNamespace(),
                null, Set.of("USER"), "YANK_REVOCATION_NOTICE"));
    }

    @Test
    void yankRevocationPurpose_latestNull_stillDeniesRemovedMemberAndDisabledAccount() {
        Skill namespaceOnlyDraft =
                new Skill(NAMESPACE_ID, "ns-draft", OWNER_ID, SkillVisibility.NAMESPACE_ONLY);
        UserAccount disabled = account(OTHER_USER_ID);
        disabled.setStatus(UserStatus.DISABLED);

        assertFalse(canAccessForPurpose(namespaceOnlyDraft, account(OTHER_USER_ID), liveNamespace(),
                null, Set.of("USER"), "YANK_REVOCATION_NOTICE"));
        assertFalse(canAccessForPurpose(unpublishedPublicSkill, disabled, liveNamespace(),
                null, Set.of("USER"), "YANK_REVOCATION_NOTICE"));
        assertFalse(canAccessForPurpose(unpublishedPublicSkill, account(OTHER_USER_ID),
                archivedNamespace(), null, Set.of("USER"), "YANK_REVOCATION_NOTICE"));
    }

    @Test
    void metadataReadPurpose_latestNull_remainsOwnerOnly() {
        assertFalse(canAccessForPurpose(unpublishedPublicSkill, account(OTHER_USER_ID), liveNamespace(),
                NamespaceRole.ADMIN, Set.of("USER"), "METADATA_READ"));
        assertTrue(canAccessForPurpose(unpublishedPublicSkill, account(OWNER_ID), liveNamespace(),
                null, Set.of("USER"), "METADATA_READ"));
        assertFalse(checker.canAccess(unpublishedPublicSkill, account(OTHER_USER_ID), liveNamespace(),
                NamespaceRole.ADMIN, Set.of("USER")));
    }

    private boolean canAccessForPurpose(Skill skill, UserAccount account, Namespace namespace,
                                        NamespaceRole namespaceRole, Set<String> platformRoles,
                                        String purposeName) {
        java.lang.reflect.Method method = java.util.Arrays.stream(VisibilityChecker.class.getMethods())
                .filter(candidate -> candidate.getName().equals("canAccess"))
                .filter(candidate -> candidate.getParameterCount() == 6)
                .filter(candidate -> candidate.getParameterTypes()[5].isEnum())
                .findFirst()
                .orElse(null);
        assertNotNull(method,
                "VisibilityChecker must expose a full-context overload carrying an explicit access purpose");
        Object purpose = java.util.Arrays.stream(method.getParameterTypes()[5].getEnumConstants())
                .filter(constant -> ((Enum<?>) constant).name().equals(purposeName))
                .findFirst()
                .orElse(null);
        assertNotNull(purpose, "VisibilityChecker access purpose " + purposeName + " is missing");
        try {
            return (Boolean) method.invoke(checker, skill, account, namespace, namespaceRole,
                    platformRoles, purpose);
        } catch (java.lang.reflect.InvocationTargetException exception) {
            throw new AssertionError("VisibilityChecker purpose overload failed", exception.getCause());
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("VisibilityChecker purpose overload was not accessible", exception);
        }
    }

    private UserAccount account(String userId) {
        return new UserAccount(userId, userId, userId + "@example.com", null);
    }

    private Namespace liveNamespace() {
        return new Namespace("live", "Live", OWNER_ID);
    }

    private Namespace archivedNamespace() {
        Namespace namespace = new Namespace("archived", "Archived", OWNER_ID);
        namespace.setStatus(NamespaceStatus.ARCHIVED);
        return namespace;
    }
}
