package com.iflytek.skillhub.listener;

import com.iflytek.skillhub.auth.entity.UserRoleBinding;
import com.iflytek.skillhub.auth.repository.UserRoleBindingRepository;
import com.iflytek.skillhub.domain.namespace.Namespace;
import com.iflytek.skillhub.domain.namespace.NamespaceMember;
import com.iflytek.skillhub.domain.namespace.NamespaceMemberRepository;
import com.iflytek.skillhub.domain.skill.Skill;
import com.iflytek.skillhub.domain.skill.VisibilityChecker;
import com.iflytek.skillhub.domain.user.UserAccount;
import com.iflytek.skillhub.domain.user.UserAccountRepository;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SubscriberAccessResolver {

    private final UserAccountRepository userAccountRepository;
    private final NamespaceMemberRepository namespaceMemberRepository;
    private final UserRoleBindingRepository userRoleBindingRepository;
    private final VisibilityChecker visibilityChecker;

    public SubscriberAccessResolver(UserAccountRepository userAccountRepository,
                                    NamespaceMemberRepository namespaceMemberRepository,
                                    UserRoleBindingRepository userRoleBindingRepository,
                                    VisibilityChecker visibilityChecker) {
        this.userAccountRepository = userAccountRepository;
        this.namespaceMemberRepository = namespaceMemberRepository;
        this.userRoleBindingRepository = userRoleBindingRepository;
        this.visibilityChecker = visibilityChecker;
    }

    public List<String> resolveReadableSubscribers(Skill skill,
                                                   Namespace namespace,
                                                   List<String> candidateUserIds) {
        List<String> candidates = new LinkedHashSet<>(candidateUserIds).stream().toList();
        Map<String, UserAccount> accounts = userAccountRepository.findByIdIn(candidates).stream()
                .collect(Collectors.toMap(UserAccount::getId, account -> account));
        Map<String, NamespaceMember> memberships = namespaceMemberRepository
                .findByNamespaceIdAndUserIdIn(skill.getNamespaceId(), candidates).stream()
                .collect(Collectors.toMap(NamespaceMember::getUserId, member -> member));
        Map<String, Set<String>> platformRoles = new HashMap<>();
        for (UserRoleBinding binding : userRoleBindingRepository.findByUserIdIn(candidates)) {
            platformRoles.computeIfAbsent(binding.getUserId(), ignored -> new LinkedHashSet<>())
                    .add(binding.getRole().getCode());
        }
        Map<String, Boolean> readable = new LinkedHashMap<>();
        for (String candidate : candidates) {
            NamespaceMember membership = memberships.get(candidate);
            readable.put(candidate, visibilityChecker.canAccess(
                    skill,
                    accounts.get(candidate),
                    namespace,
                    membership == null ? null : membership.getRole(),
                    platformRoles.getOrDefault(candidate, Set.of())));
        }
        return candidates.stream().filter(candidate -> readable.get(candidate)).toList();
    }
}
