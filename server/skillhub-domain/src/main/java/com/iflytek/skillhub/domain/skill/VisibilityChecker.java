package com.iflytek.skillhub.domain.skill;

import com.iflytek.skillhub.domain.namespace.NamespaceRole;
import com.iflytek.skillhub.domain.namespace.Namespace;
import com.iflytek.skillhub.domain.namespace.NamespaceStatus;
import com.iflytek.skillhub.domain.user.UserAccount;

import java.util.Map;
import java.util.Set;

/**
 * Evaluates whether a caller may read a skill based on publication state, visibility, ownership,
 * and namespace roles.
 */
public class VisibilityChecker {

    /**
     * Why a skill is being accessed. The purpose never relaxes a confidentiality guard; it only
     * selects which publication-state rule applies, and every exception lives inside this class.
     */
    public enum AccessPurpose {

        /** Ordinary metadata read: an unpublished skill stays owner-only. */
        METADATA_READ,

        /**
         * Delivery of a yank revocation notice. A yank that clears {@code latestVersionId} must not
         * silence the notice about that same yank, so this purpose — and only this purpose — skips
         * the self-invalidating publication-state guard. Hidden, visibility, membership, account
         * status and namespace status rules are all still enforced.
         */
        YANK_REVOCATION_NOTICE
    }

    public boolean canAccess(Skill skill, String currentUserId, Map<Long, NamespaceRole> userNamespaceRoles) {
        return canAccess(skill, currentUserId, userNamespaceRoles, Set.of());
    }

    public boolean canAccess(Skill skill, String currentUserId, Map<Long, NamespaceRole> userNamespaceRoles, Set<String> platformRoles) {
        return canAccess(skill, currentUserId, userNamespaceRoles, platformRoles, AccessPurpose.METADATA_READ);
    }

    public boolean canAccess(Skill skill,
                             UserAccount account,
                             Namespace namespace,
                             NamespaceRole namespaceRole,
                             Set<String> platformRoles) {
        return canAccess(skill, account, namespace, namespaceRole, platformRoles, AccessPurpose.METADATA_READ);
    }

    public boolean canAccess(Skill skill,
                             UserAccount account,
                             Namespace namespace,
                             NamespaceRole namespaceRole,
                             Set<String> platformRoles,
                             AccessPurpose purpose) {
        if (account == null || !account.isActive()) {
            return false;
        }
        if (isSuperAdmin(platformRoles)) {
            return true;
        }
        if (namespace != null
                && namespace.getStatus() == NamespaceStatus.ARCHIVED
                && namespaceRole == null) {
            return false;
        }
        Map<Long, NamespaceRole> roles = namespaceRole == null
                ? Map.of()
                : Map.of(skill.getNamespaceId(), namespaceRole);
        return canAccess(skill, account.getId(), roles, platformRoles, purpose);
    }

    private boolean canAccess(Skill skill,
                              String currentUserId,
                              Map<Long, NamespaceRole> userNamespaceRoles,
                              Set<String> platformRoles,
                              AccessPurpose purpose) {
        Map<Long, NamespaceRole> roles = userNamespaceRoles != null ? userNamespaceRoles : Map.of();
        if (isSuperAdmin(platformRoles)) {
            return true;
        }
        if (skill.isHidden()) {
            return isOwner(skill, currentUserId) || isAdminOrAbove(roles.get(skill.getNamespaceId()));
        }
        if (skill.getLatestVersionId() == null && purpose != AccessPurpose.YANK_REVOCATION_NOTICE) {
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
