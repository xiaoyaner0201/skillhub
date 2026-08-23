package com.iflytek.skillhub.domain.social;

import com.iflytek.skillhub.domain.shared.exception.DomainNotFoundException;
import com.iflytek.skillhub.domain.shared.exception.DomainForbiddenException;
import com.iflytek.skillhub.domain.namespace.Namespace;
import com.iflytek.skillhub.domain.namespace.NamespaceMember;
import com.iflytek.skillhub.domain.namespace.NamespaceMemberRepository;
import com.iflytek.skillhub.domain.namespace.NamespaceRepository;
import com.iflytek.skillhub.domain.skill.Skill;
import com.iflytek.skillhub.domain.skill.SkillRepository;
import com.iflytek.skillhub.domain.skill.VisibilityChecker;
import com.iflytek.skillhub.domain.social.event.SkillSubscribedEvent;
import com.iflytek.skillhub.domain.social.event.SkillUnsubscribedEvent;
import com.iflytek.skillhub.domain.user.UserAccount;
import com.iflytek.skillhub.domain.user.UserAccountRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class SkillSubscriptionService {
    private final SkillSubscriptionRepository subscriptionRepository;
    private final SkillRepository skillRepository;
    private final UserAccountRepository userAccountRepository;
    private final NamespaceRepository namespaceRepository;
    private final NamespaceMemberRepository namespaceMemberRepository;
    private final VisibilityChecker visibilityChecker;
    private final ApplicationEventPublisher eventPublisher;

    public SkillSubscriptionService(SkillSubscriptionRepository subscriptionRepository,
                                    SkillRepository skillRepository,
                                    UserAccountRepository userAccountRepository,
                                    NamespaceRepository namespaceRepository,
                                    NamespaceMemberRepository namespaceMemberRepository,
                                    VisibilityChecker visibilityChecker,
                                    ApplicationEventPublisher eventPublisher) {
        this.subscriptionRepository = subscriptionRepository;
        this.skillRepository = skillRepository;
        this.userAccountRepository = userAccountRepository;
        this.namespaceRepository = namespaceRepository;
        this.namespaceMemberRepository = namespaceMemberRepository;
        this.visibilityChecker = visibilityChecker;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void subscribe(Long skillId, String userId, Set<String> platformRoles) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new DomainNotFoundException("skill.not_found", skillId));
        UserAccount account = userAccountRepository.findById(userId).orElse(null);
        Namespace namespace = namespaceRepository.findById(skill.getNamespaceId()).orElse(null);
        NamespaceMember membership = namespaceMemberRepository
                .findByNamespaceIdAndUserId(skill.getNamespaceId(), userId)
                .orElse(null);
        if (!visibilityChecker.canAccess(
                skill,
                account,
                namespace,
                membership == null ? null : membership.getRole(),
                platformRoles)) {
            throw new DomainForbiddenException("error.skill.subscription.noPermission");
        }
        if (subscriptionRepository.findBySkillIdAndUserId(skillId, userId).isPresent()) {
            return; // idempotent
        }
        subscriptionRepository.save(new SkillSubscription(skillId, userId));
        skillRepository.incrementSubscriptionCount(skillId);
        eventPublisher.publishEvent(new SkillSubscribedEvent(skillId, userId));
    }

    @Transactional
    public void unsubscribe(Long skillId, String userId) {
        ensureSkillExists(skillId);
        subscriptionRepository.findBySkillIdAndUserId(skillId, userId).ifPresent(subscription -> {
            subscriptionRepository.delete(subscription);
            skillRepository.decrementSubscriptionCount(skillId);
            eventPublisher.publishEvent(new SkillUnsubscribedEvent(skillId, userId));
        });
    }

    public boolean isSubscribed(Long skillId, String userId) {
        ensureSkillExists(skillId);
        return subscriptionRepository.findBySkillIdAndUserId(skillId, userId).isPresent();
    }

    /**
     * Subscribers of a skill in stable subscription order.
     *
     * <p>The persistence query carries no ordering of its own, so the fan-out recipient order would
     * otherwise depend on the storage engine. Ordering by the generated subscription id makes the
     * downstream notification order reproducible without changing the recipient set.
     */
    public List<String> findSubscribersBySkillId(Long skillId) {
        List<SkillSubscription> subscriptions =
                new ArrayList<>(subscriptionRepository.findAllBySkillId(skillId));
        subscriptions.sort(Comparator.comparing(SkillSubscription::getId,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return subscriptions.stream()
                .map(SkillSubscription::getUserId)
                .distinct()
                .toList();
    }

    private void ensureSkillExists(Long skillId) {
        if (skillRepository.findById(skillId).isEmpty()) {
            throw new DomainNotFoundException("skill.not_found", skillId);
        }
    }
}
