package com.iflytek.skillhub.auth.rbac;

import com.iflytek.skillhub.auth.entity.Permission;
import com.iflytek.skillhub.auth.entity.RolePermission;
import com.iflytek.skillhub.auth.entity.UserRoleBinding;
import com.iflytek.skillhub.auth.repository.UserRoleBindingRepository;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves platform roles and permissions for a user from persisted RBAC
 * bindings.
 *
 * <p>This service intentionally keeps a small amount of direct JPA querying in
 * the auth module. Permission expansion is module-local authorization
 * infrastructure rather than an application read model, so it remains a
 * documented exception instead of being pushed into app-layer query
 * repositories.
 */
@Service
public class RbacService {

    private final UserRoleBindingRepository roleBindingRepo;
    private final EntityManager entityManager;

    public RbacService(UserRoleBindingRepository roleBindingRepo, EntityManager entityManager) {
        this.roleBindingRepo = roleBindingRepo;
        this.entityManager = entityManager;
    }

    public Set<String> getUserRoleCodes(String userId) {
        return PlatformRoleDefaults.withDefaultUserRole(roleBindingRepo.findByUserId(userId).stream()
            .map(rb -> rb.getRole().getCode())
            .collect(Collectors.toSet()));
    }

    /**
     * Batch counterpart of {@link #getUserRoleCodes(String)}: one binding read for the whole set,
     * normalized through the same default-role rule so a caller cannot end up with a parallel
     * role model. Every requested id is present in the result, unbound users included.
     */
    public Map<String, Set<String>> getUserRoleCodesIn(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Set<String> distinctIds = new LinkedHashSet<>(userIds);
        Map<String, Set<String>> bound = roleBindingRepo.findByUserIdIn(distinctIds).stream()
            .collect(Collectors.groupingBy(UserRoleBinding::getUserId,
                Collectors.mapping(rb -> rb.getRole().getCode(), Collectors.toSet())));
        Map<String, Set<String>> resolved = new LinkedHashMap<>();
        for (String userId : distinctIds) {
            resolved.put(userId, PlatformRoleDefaults.withDefaultUserRole(bound.get(userId)));
        }
        return Map.copyOf(resolved);
    }

    public Set<String> getUserPermissions(String userId) {
        List<UserRoleBinding> bindings = roleBindingRepo.findByUserId(userId);
        Set<Long> roleIds = bindings.stream()
            .map(rb -> rb.getRole().getId())
            .collect(Collectors.toSet());

        if (roleIds.isEmpty()) return Set.of();

        // Check if user has SUPER_ADMIN role - grant all permissions
        boolean isSuperAdmin = bindings.stream()
            .anyMatch(rb -> "SUPER_ADMIN".equals(rb.getRole().getCode()));
        if (isSuperAdmin) {
            return entityManager.createQuery("SELECT p.code FROM Permission p", String.class)
                .getResultList().stream().collect(Collectors.toSet());
        }

        return entityManager.createQuery(
                "SELECT p.code FROM RolePermission rp JOIN Permission p ON rp.permissionId = p.id WHERE rp.roleId IN :roleIds", String.class)
            .setParameter("roleIds", roleIds)
            .getResultList().stream().collect(Collectors.toSet());
    }

    public boolean hasPermission(String userId, String permissionCode) {
        return getUserPermissions(userId).contains(permissionCode);
    }

    public boolean hasRole(String userId, String roleCode) {
        return getUserRoleCodes(userId).contains(roleCode);
    }
}
