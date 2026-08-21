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

    private UserAccount account(String userId) {
        return new UserAccount(userId, userId, userId + "@example.com", null);
    }

    private Namespace archivedNamespace() {
        Namespace namespace = new Namespace("archived", "Archived", OWNER_ID);
        namespace.setStatus(NamespaceStatus.ARCHIVED);
        return namespace;
    }
}
