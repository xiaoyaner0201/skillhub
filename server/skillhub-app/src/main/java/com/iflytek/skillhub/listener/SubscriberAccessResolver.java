package com.iflytek.skillhub.listener;

import com.iflytek.skillhub.auth.rbac.RbacService;
import com.iflytek.skillhub.domain.namespace.Namespace;
import com.iflytek.skillhub.domain.namespace.NamespaceMember;
import com.iflytek.skillhub.domain.namespace.NamespaceMemberRepository;
import com.iflytek.skillhub.domain.skill.Skill;
import com.iflytek.skillhub.domain.skill.VisibilityChecker;
import com.iflytek.skillhub.domain.skill.VisibilityChecker.AccessPurpose;
import com.iflytek.skillhub.domain.user.UserAccount;
import com.iflytek.skillhub.domain.user.UserAccountRepository;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Batches the authority facts a fan-out needs and hands each candidate to {@link VisibilityChecker}.
 *
 * <p>This class holds no authorization rule of its own: it loads facts, forwards the caller's
 * {@link AccessPurpose} verbatim and preserves candidate order. Any loader failure propagates before
 * a single recipient reaches a sink.
 */
@Component
public class SubscriberAccessResolver {

    private final UserAccountRepository userAccountRepository;
    private final NamespaceMemberRepository namespaceMemberRepository;
    private final RbacService rbacService;
    private final VisibilityChecker visibilityChecker;

    public SubscriberAccessResolver(UserAccountRepository userAccountRepository,
                                    NamespaceMemberRepository namespaceMemberRepository,
                                    RbacService rbacService,
                                    VisibilityChecker visibilityChecker) {
        this.userAccountRepository = userAccountRepository;
        this.namespaceMemberRepository = namespaceMemberRepository;
        this.rbacService = rbacService;
        this.visibilityChecker = visibilityChecker;
    }

    public List<String> resolveReadableSubscribers(Skill skill,
                                                   Namespace namespace,
                                                   List<String> candidateUserIds,
                                                   AccessPurpose purpose) {
        List<String> candidates = new LinkedHashSet<>(candidateUserIds).stream().toList();
        Map<String, UserAccount> accounts = userAccountRepository.findByIdIn(candidates).stream()
                .collect(Collectors.toMap(UserAccount::getId, account -> account));
        // Only the skill's own namespace is loaded: the checker consumes exactly one namespace role
        // for a given skill, so any other membership would be an unused fact.
        Map<String, NamespaceMember> memberships = namespaceMemberRepository
                .findByNamespaceIdAndUserIdIn(skill.getNamespaceId(), candidates).stream()
                .collect(Collectors.toMap(NamespaceMember::getUserId, member -> member));
        Map<String, Set<String>> platformRoles = rbacService.getUserRoleCodesByUserIds(candidates);
        Map<String, Boolean> readable = new LinkedHashMap<>();
        for (String candidate : candidates) {
            NamespaceMember membership = memberships.get(candidate);
            readable.put(candidate, visibilityChecker.canAccess(
                    skill,
                    accounts.get(candidate),
                    namespace,
                    membership == null ? null : membership.getRole(),
                    platformRoles.getOrDefault(candidate, Set.of()),
                    purpose));
        }
        return candidates.stream().filter(candidate -> readable.get(candidate)).toList();
    }
}
