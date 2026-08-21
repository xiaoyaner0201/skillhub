package com.iflytek.skillhub.exception;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionMessageBundleTest {

    private static final String CODE = "error.skill.subscription.noPermission";

    @Test
    void subscriptionDenialMessageResolvesInEnglish() {
        assertThat(resolve(Locale.ENGLISH))
                .isEqualTo("You do not have access to this skill")
                .isNotEqualTo(CODE);
    }

    @Test
    void subscriptionDenialMessageResolvesInSimplifiedChinese() {
        assertThat(resolve(Locale.SIMPLIFIED_CHINESE))
                .isEqualTo("你没有权限访问该技能")
                .isNotEqualTo(CODE);
    }

    private String resolve(Locale locale) {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource.getMessage(CODE, null, CODE, locale);
    }
}
