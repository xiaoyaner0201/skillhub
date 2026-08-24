package com.iflytek.skillhub.domain.skill;

import com.iflytek.skillhub.domain.namespace.NamespaceRole;

import java.util.Map;
import java.util.Set;

/**
 * Evaluates whether a caller may read a skill based on publication state, visibility, ownership,
 * and namespace roles.
 */
public class VisibilityChecker {

    /**
     * Current authority facts for one caller: who they are, whether their account is still active,
     * their current namespace roles and their current platform roles.
     *
     * <p>Callers assemble this from live sources at decision time. Nothing derived from a retained
     * relationship row (a subscription, a star, a past membership) belongs here.
     */
    public record AccessFacts(String userId,
                              boolean accountActive,
                              Map<Long, NamespaceRole> namespaceRoles,
                              Set<String> platformRoles) {
    }

    public boolean canAccess(Skill skill, String currentUserId, Map<Long, NamespaceRole> userNamespaceRoles) {
        return canAccess(skill, currentUserId, userNamespaceRoles, Set.of());
    }

    /**
     * Full-context overload. Account status is evaluated fail-closed ahead of every role rule, so a
     * SUPER_ADMIN or namespace-owner binding cannot readmit an inactive account. Once the account is
     * known active the decision is the same one the legacy overloads make - this adds a gate in
     * front of the policy, it does not fork it.
     */
    public boolean canAccess(Skill skill, AccessFacts facts) {
        if (facts == null || !facts.accountActive()) {
            return false;
        }
        return canAccess(skill, facts.userId(), facts.namespaceRoles(), facts.platformRoles());
    }

    public boolean canAccess(Skill skill, String currentUserId, Map<Long, NamespaceRole> userNamespaceRoles, Set<String> platformRoles) {
        Map<Long, NamespaceRole> roles = userNamespaceRoles != null ? userNamespaceRoles : Map.of();
        if (isSuperAdmin(platformRoles)) {
            return true;
        }
        if (skill.isHidden()) {
            return isOwner(skill, currentUserId) || isAdminOrAbove(roles.get(skill.getNamespaceId()));
        }
        if (skill.getLatestVersionId() == null) {
            return isOwner(skill, currentUserId);
        }
        return switch (skill.getVisibility()) {
            case PUBLIC -> true;
            case NAMESPACE_ONLY -> roles.containsKey(skill.getNamespaceId());
            case PRIVATE -> isOwner(skill, currentUserId) || isAdminOrAbove(roles.get(skill.getNamespaceId()));
        };
    }

    private boolean isOwner(Skill skill, String currentUserId) {
        return currentUserId != null && skill.getOwnerId().equals(currentUserId);
    }

    private boolean isAdminOrAbove(NamespaceRole role) {
        return role == NamespaceRole.ADMIN || role == NamespaceRole.OWNER;
    }

    private boolean isSuperAdmin(Set<String> platformRoles) {
        return platformRoles != null && platformRoles.contains("SUPER_ADMIN");
    }
}
