package com.iflytek.skillhub.exception;

import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Probe subscribe-message-en-zh.
 *
 * <p>The subscription denial raised by the subscribe path must carry a code that the production
 * bundle actually localizes. {@code ApiResponseFactory} resolves codes with the code itself as the
 * default message, so a missing key is not an error at runtime - it silently leaks the raw code to
 * the caller. This probe pins that failure mode at the bundle boundary.
 */
class SubscriptionMessageBundleTest {

    private static final String SUBSCRIPTION_DENIED_CODE = "error.skill.subscription.noPermission";

    /** Already present on the base tree; guards the bundle wiring itself against false positives. */
    private static final String EXISTING_DENIAL_CODE = "error.skill.lifecycle.noPermission";

    private MessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        source.setUseCodeAsDefaultMessage(false);
        return source;
    }

    /** Mirrors ApiResponseFactory: the code is the default message, so misses degrade silently. */
    private String resolve(MessageSource source, String code, Locale locale) {
        return source.getMessage(code, null, code, locale);
    }

    @Test
    void subscriptionDeniedMessage_resolvesEnglishAndChinese() {
        MessageSource source = messageSource();

        String controlEnglish = resolve(source, EXISTING_DENIAL_CODE, Locale.ENGLISH);
        String controlChinese = resolve(source, EXISTING_DENIAL_CODE, Locale.SIMPLIFIED_CHINESE);
        assertThat(controlEnglish)
                .as("control: an existing denial code must localize in English")
                .isNotEqualTo(EXISTING_DENIAL_CODE);
        assertThat(controlChinese)
                .as("control: an existing denial code must localize in Chinese")
                .isNotEqualTo(EXISTING_DENIAL_CODE);
        assertThat(controlChinese)
                .as("control: the two locales must not collapse to the same copy")
                .isNotEqualTo(controlEnglish);

        String english = resolve(source, SUBSCRIPTION_DENIED_CODE, Locale.ENGLISH);
        String chinese = resolve(source, SUBSCRIPTION_DENIED_CODE, Locale.SIMPLIFIED_CHINESE);

        assertThat(english)
                .as("English subscription denial copy must not fall back to the raw code")
                .isNotEqualTo(SUBSCRIPTION_DENIED_CODE)
                .isNotBlank();
        assertThat(chinese)
                .as("Chinese subscription denial copy must not fall back to the raw code")
                .isNotEqualTo(SUBSCRIPTION_DENIED_CODE)
                .isNotBlank();
        assertThat(chinese)
                .as("English and Chinese subscription denial copy must be distinct localizations")
                .isNotEqualTo(english);
    }
}
