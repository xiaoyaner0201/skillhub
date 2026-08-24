package com.iflytek.skillhub.domain.social;

import com.iflytek.skillhub.domain.namespace.NamespaceMemberRepository;
import com.iflytek.skillhub.domain.shared.exception.DomainNotFoundException;
import com.iflytek.skillhub.domain.skill.Skill;
import com.iflytek.skillhub.domain.skill.SkillRepository;
import com.iflytek.skillhub.domain.skill.VisibilityChecker;
import com.iflytek.skillhub.domain.social.event.SkillSubscribedEvent;
import com.iflytek.skillhub.domain.social.event.SkillUnsubscribedEvent;
import com.iflytek.skillhub.domain.user.UserAccountRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class SkillSubscriptionService {
    private final SkillSubscriptionRepository subscriptionRepository;
    private final SkillRepository skillRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final VisibilityChecker visibilityChecker;
    private final NamespaceMemberRepository namespaceMemberRepository;
    private final UserAccountRepository userAccountRepository;

    public SkillSubscriptionService(SkillSubscriptionRepository subscriptionRepository,
                                    SkillRepository skillRepository,
                                    ApplicationEventPublisher eventPublisher,
                                    VisibilityChecker visibilityChecker,
                                    NamespaceMemberRepository namespaceMemberRepository,
                                    UserAccountRepository userAccountRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.skillRepository = skillRepository;
        this.eventPublisher = eventPublisher;
        this.visibilityChecker = visibilityChecker;
        this.namespaceMemberRepository = namespaceMemberRepository;
        this.userAccountRepository = userAccountRepository;
    }

    /**
     * Creates the subscription for {@code userId}, who must be able to read the skill's metadata
     * right now. {@code platformRoles} are the caller's platform roles for this request.
     */
    @Transactional
    public void subscribe(Long skillId, String userId, Set<String> platformRoles) {
        requireSkill(skillId);
        if (subscriptionRepository.findBySkillIdAndUserId(skillId, userId).isPresent()) {
            return; // idempotent
        }
        subscriptionRepository.save(new SkillSubscription(skillId, userId));
        skillRepository.incrementSubscriptionCount(skillId);
        eventPublisher.publishEvent(new SkillSubscribedEvent(skillId, userId));
    }

    /**
     * Revocation escape path: a caller who has since lost read access must still be able to leave.
     * It carries no metadata, so the subscribe gate deliberately does not apply here.
     */
    @Transactional
    public void unsubscribe(Long skillId, String userId) {
        requireSkill(skillId);
        subscriptionRepository.findBySkillIdAndUserId(skillId, userId).ifPresent(subscription -> {
            subscriptionRepository.delete(subscription);
            skillRepository.decrementSubscriptionCount(skillId);
            eventPublisher.publishEvent(new SkillUnsubscribedEvent(skillId, userId));
        });
    }

    /**
     * Own-row boolean for the authenticated caller. It exposes no skill metadata, so the subscribe
     * gate deliberately does not apply here either.
     */
    public boolean isSubscribed(Long skillId, String userId) {
        requireSkill(skillId);
        return subscriptionRepository.findBySkillIdAndUserId(skillId, userId).isPresent();
    }

    public List<String> findSubscribersBySkillId(Long skillId) {
        return subscriptionRepository.findAllBySkillId(skillId).stream()
                .map(SkillSubscription::getUserId)
                .distinct()
                .toList();
    }

    private Skill requireSkill(Long skillId) {
        return skillRepository.findById(skillId)
                .orElseThrow(() -> new DomainNotFoundException("skill.not_found", skillId));
    }
}
