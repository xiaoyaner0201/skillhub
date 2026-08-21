package com.iflytek.skillhub.exception;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionMessageBundleTest {

    @Test
    void subscriptionPermissionMessage_resolvesEnglishAndChinese() {
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("messages");
        messages.setDefaultEncoding(StandardCharsets.UTF_8.name());

        String english = messages.getMessage(
                "error.skill.subscription.noPermission", null,
                "error.skill.subscription.noPermission", Locale.ENGLISH);
        String chinese = messages.getMessage(
                "error.skill.subscription.noPermission", null,
                "error.skill.subscription.noPermission", Locale.SIMPLIFIED_CHINESE);

        assertThat(english)
                .isNotEqualTo("error.skill.subscription.noPermission")
                .isEqualTo("You do not have permission to subscribe to this skill");
        assertThat(chinese)
                .isNotEqualTo("error.skill.subscription.noPermission")
                .isEqualTo("你没有权限订阅该技能");
    }
}
