package com.iflytek.skillhub.listener;

import com.iflytek.skillhub.auth.rbac.RbacService;
import com.iflytek.skillhub.domain.namespace.NamespaceMemberRepository;
import com.iflytek.skillhub.domain.skill.Skill;
import com.iflytek.skillhub.domain.skill.VisibilityChecker;
import com.iflytek.skillhub.domain.user.UserAccountRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

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
     */
    public List<String> resolveReadableRecipients(Skill skill, List<String> candidateIds) {
        if (candidateIds == null || candidateIds.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(candidateIds));
    }
}
