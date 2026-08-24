package com.iflytek.skillhub.listener;

import com.iflytek.skillhub.auth.rbac.RbacService;
import com.iflytek.skillhub.domain.namespace.NamespaceMember;
import com.iflytek.skillhub.domain.namespace.NamespaceMemberRepository;
import com.iflytek.skillhub.domain.namespace.NamespaceRole;
import com.iflytek.skillhub.domain.skill.Skill;
import com.iflytek.skillhub.domain.skill.VisibilityChecker;
import com.iflytek.skillhub.domain.user.UserAccount;
import com.iflytek.skillhub.domain.user.UserAccountRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves which of a set of candidate recipients may currently read a skill.
 *
 * <p>Fact-only seam: it loads current account, membership and platform-role facts in one batch per
 * source and hands them to {@link VisibilityChecker}, which stays the single visibility decision
 * source. It never consults the subscription row itself - a retained row is history, not authority.
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

    /**
     * Returns the candidates that may currently read {@code skill}, in input order and deduplicated.
     *
     * <p>Exactly one batch read per authority source happens before the decision loop, and the loop
     * itself performs no repository call. A source that fails throws out of this method, so no
     * partial recipient set can reach a downstream sink.
     */
    public List<String> resolveReadableRecipients(Skill skill, List<String> candidateIds) {
        if (skill == null || candidateIds == null || candidateIds.isEmpty()) {
            return List.of();
        }
        List<String> candidates = new ArrayList<>(new LinkedHashSet<>(candidateIds));

        Set<String> activeUserIds = loadActiveUserIds(candidates);
        Map<String, NamespaceRole> namespaceRoleByUser = loadNamespaceRoles(skill.getNamespaceId(), candidates);
        Map<String, Set<String>> platformRolesByUser = rbacService.getUserRoleCodesIn(candidates);

        List<String> readable = new ArrayList<>();
        for (String candidateId : candidates) {
            NamespaceRole namespaceRole = namespaceRoleByUser.get(candidateId);
            VisibilityChecker.AccessFacts facts = new VisibilityChecker.AccessFacts(
                    candidateId,
                    activeUserIds.contains(candidateId),
                    namespaceRole == null ? Map.of() : Map.of(skill.getNamespaceId(), namespaceRole),
                    platformRolesByUser.getOrDefault(candidateId, Set.of()));
            if (visibilityChecker.canAccess(skill, facts)) {
                readable.add(candidateId);
            }
        }
        return List.copyOf(readable);
    }

    /** Absent account row is treated exactly like an inactive one: fail closed. */
    private Set<String> loadActiveUserIds(List<String> candidates) {
        return userAccountRepository.findByIdIn(candidates).stream()
                .filter(UserAccount::isActive)
                .map(UserAccount::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<String, NamespaceRole> loadNamespaceRoles(Long namespaceId, List<String> candidates) {
        Map<String, NamespaceRole> byCandidate = new LinkedHashMap<>();
        for (NamespaceMember member : namespaceMemberRepository
                .findByNamespaceIdAndUserIdIn(namespaceId, candidates)) {
            byCandidate.put(member.getUserId(), member.getRole());
        }
        return byCandidate;
    }
}
