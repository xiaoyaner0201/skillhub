package com.iflytek.skillhub.domain.skill;

import com.iflytek.skillhub.domain.namespace.NamespaceRole;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stable differential matrix for the two pre-existing VisibilityChecker overloads. The emitted
 * lines are compared byte-for-byte between RED_BASE_SHA and GREEN_SHA.
 */
class VisibilityCheckerLegacyCompatibilityTest {

    private static final long NAMESPACE_ID = 7L;
    private static final String OWNER = "owner";
    private static final String USER = "user";

    private final VisibilityChecker checker = new VisibilityChecker();

    @Test
    void legacyOverloads_emitStableObservableMatrix() {
        Skill publicSkill = skill("public", SkillVisibility.PUBLIC, false, true);
        Skill namespaceOnly = skill("namespace-only", SkillVisibility.NAMESPACE_ONLY, false, true);
        Skill privateSkill = skill("private", SkillVisibility.PRIVATE, false, true);
        Skill hiddenPublic = skill("hidden", SkillVisibility.PUBLIC, true, true);
        Skill latestNull = skill("latest-null", SkillVisibility.PUBLIC, false, false);

        List<LegacyCase> cases = List.of(
                new LegacyCase("anonymous-public", true,
                        checker.canAccess(publicSkill, null, Map.of())),
                new LegacyCase("ordinary-public", true,
                        checker.canAccess(publicSkill, USER, Map.of())),
                new LegacyCase("anonymous-namespace-only", false,
                        checker.canAccess(namespaceOnly, null, Map.of())),
                new LegacyCase("nonmember-namespace-only", false,
                        checker.canAccess(namespaceOnly, USER, Map.of())),
                new LegacyCase("member-namespace-only", true,
                        checker.canAccess(namespaceOnly, USER,
                                Map.of(NAMESPACE_ID, NamespaceRole.MEMBER))),
                new LegacyCase("owner-private", true,
                        checker.canAccess(privateSkill, OWNER, Map.of())),
                new LegacyCase("member-private", false,
                        checker.canAccess(privateSkill, USER,
                                Map.of(NAMESPACE_ID, NamespaceRole.MEMBER))),
                new LegacyCase("admin-private", true,
                        checker.canAccess(privateSkill, USER,
                                Map.of(NAMESPACE_ID, NamespaceRole.ADMIN))),
                new LegacyCase("ordinary-hidden", false,
                        checker.canAccess(hiddenPublic, USER, Map.of())),
                new LegacyCase("owner-hidden", true,
                        checker.canAccess(hiddenPublic, OWNER, Map.of())),
                new LegacyCase("admin-hidden", true,
                        checker.canAccess(hiddenPublic, USER,
                                Map.of(NAMESPACE_ID, NamespaceRole.ADMIN))),
                new LegacyCase("ordinary-latest-null", false,
                        checker.canAccess(latestNull, USER, Map.of())),
                new LegacyCase("owner-latest-null", true,
                        checker.canAccess(latestNull, OWNER, Map.of())),
                new LegacyCase("super-admin-private", true,
                        checker.canAccess(privateSkill, USER, Map.of(), Set.of("SUPER_ADMIN"))),
                new LegacyCase("non-super-platform-role-private", false,
                        checker.canAccess(privateSkill, USER, Map.of(), Set.of("AUDITOR"))));

        for (LegacyCase legacyCase : cases) {
            assertThat(legacyCase.actual())
                    .as("legacy case %s", legacyCase.id())
                    .isEqualTo(legacyCase.expected());
            System.out.printf("HD30_LEGACY_MATRIX %s=%s%n",
                    legacyCase.id(), legacyCase.actual());
        }
    }

    private Skill skill(String slug, SkillVisibility visibility, boolean hidden, boolean published) {
        Skill skill = new Skill(NAMESPACE_ID, slug, OWNER, visibility);
        skill.setHidden(hidden);
        skill.setLatestVersionId(published ? 99L : null);
        return skill;
    }

    private record LegacyCase(String id, boolean expected, boolean actual) {
    }
}
