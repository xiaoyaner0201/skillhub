package com.iflytek.skillhub.auth.rbac;

import com.iflytek.skillhub.auth.entity.Role;
import com.iflytek.skillhub.auth.entity.UserRoleBinding;
import com.iflytek.skillhub.auth.repository.UserRoleBindingRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RbacServiceTest {

    @Mock private UserRoleBindingRepository roleBindingRepository;
    @Mock private EntityManager entityManager;

    @Test
    void getUserRoleCodesByUserIds_returnsEveryUserNormalizedInInputOrderWithOneBatch()
            throws Exception {
        RbacService service = new RbacService(roleBindingRepository, entityManager);
        Role userRole = role("USER");
        Role skillAdminRole = role("SKILL_ADMIN");
        List<String> distinctUsers = List.of("beta", "alpha", "unbound");
        when(roleBindingRepository.findByUserIdIn(distinctUsers)).thenReturn(List.of(
                new UserRoleBinding("alpha", skillAdminRole),
                new UserRoleBinding("beta", userRole)));

        Method batchMethod = Arrays.stream(RbacService.class.getMethods())
                .filter(method -> method.getName().equals("getUserRoleCodesByUserIds"))
                .filter(method -> method.getParameterCount() == 1)
                .filter(method -> Collection.class.isAssignableFrom(method.getParameterTypes()[0]))
                .findFirst()
                .orElse(null);

        assertThat(batchMethod)
                .as("RbacService must expose the single-source batch role lookup")
                .isNotNull();
        if (batchMethod == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Set<String>> result = (Map<String, Set<String>>) batchMethod.invoke(
                service, List.of("beta", "alpha", "beta", "unbound"));

        assertThat(result.keySet()).containsExactly("beta", "alpha", "unbound");
        assertThat(result.get("beta")).containsExactly("USER");
        assertThat(result.get("alpha")).containsExactly("SKILL_ADMIN");
        assertThat(result.get("unbound")).containsExactly("USER");
        verify(roleBindingRepository).findByUserIdIn(distinctUsers);
    }

    private Role role(String code) {
        Role role = new Role();
        ReflectionTestUtils.setField(role, "code", code);
        return role;
    }
}
