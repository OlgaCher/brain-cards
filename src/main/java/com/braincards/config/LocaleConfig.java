package com.braincards.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

@Configuration
public class LocaleConfig implements WebMvcConfigurer {

    // springdoc-openapi registers its own MessageSource-typed bean for its own purposes. Spring's
    // AbstractApplicationContext only ever picks up the bean literally NAMED "messageSource" as the
    // app's message source, and Boot's own auto-configured one backs off once ANY MessageSource bean
    // exists - so without this, the two collide and the app silently falls back to an empty
    // DelegatingMessageSource (every #{...} lookup fails, for every locale). Defining our own bean
    // under that exact name/config guarantees it wins.
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }

    @Bean
    public LocaleResolver localeResolver() {
        // Java's Locale always lowercases the language subtag internally (new Locale("UA").toString() == "ua"),
        // so the bundle files are messages_ua.properties / messages_en.properties even though the
        // user-facing "?lang=UA" link and the stored Parent.locale value stay uppercase "UA".
        SessionLocaleResolver resolver = new SessionLocaleResolver();
        resolver.setDefaultLocale(new Locale("UA"));
        return resolver;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
