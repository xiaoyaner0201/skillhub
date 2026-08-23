package com.iflytek.skillhub.auth.rbac;

import com.iflytek.skillhub.auth.entity.Permission;
import com.iflytek.skillhub.auth.entity.RolePermission;
import com.iflytek.skillhub.auth.entity.UserRoleBinding;
import com.iflytek.skillhub.auth.repository.UserRoleBindingRepository;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        return normalizeRoleCodes(roleBindingRepo.findByUserId(userId));
    }

    /**
     * Single source for batch platform-role derivation.
     *
     * <p>Input is de-duplicated in encounter order; an empty input never touches the repository and
     * a non-empty input issues exactly one {@code findByUserIdIn}. Every requested user is present
     * in the result — a user without an explicit binding normalizes to the default user role — and
     * the map iterates in the de-duplicated input order.
     */
    public Map<String, Set<String>> getUserRoleCodesByUserIds(Collection<String> userIds) {
        List<String> distinctIds = userIds == null
            ? List.of()
            : userIds.stream().filter(Objects::nonNull).distinct().toList();
        Map<String, Set<String>> roleCodesByUserId = new LinkedHashMap<>();
        if (distinctIds.isEmpty()) {
            return roleCodesByUserId;
        }

        Map<String, List<UserRoleBinding>> bindingsByUserId = new LinkedHashMap<>();
        for (UserRoleBinding binding : roleBindingRepo.findByUserIdIn(distinctIds)) {
            bindingsByUserId.computeIfAbsent(binding.getUserId(), key -> new ArrayList<>()).add(binding);
        }
        for (String userId : distinctIds) {
            roleCodesByUserId.put(userId,
                normalizeRoleCodes(bindingsByUserId.getOrDefault(userId, List.of())));
        }
        return roleCodesByUserId;
    }

    private Set<String> normalizeRoleCodes(Collection<UserRoleBinding> bindings) {
        return PlatformRoleDefaults.withDefaultUserRole(bindings.stream()
            .map(rb -> rb.getRole().getCode())
            .collect(Collectors.toSet()));
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
