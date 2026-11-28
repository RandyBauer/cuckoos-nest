package io.github.randybauer.cuckoosnest.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * Languages: English (default) and Spanish. {@code ?lang=es} switches, and a cookie remembers the
 * choice. All page text comes from {@code messages*.properties}.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    public static final List<Locale> SUPPORTED = List.of(Locale.ENGLISH, Locale.forLanguageTag("es"));

    @Bean
    LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("nest-lang");
        resolver.setDefaultLocale(Locale.ENGLISH);
        resolver.setCookieMaxAge(Duration.ofDays(365));
        return resolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                    throws ServletException {
                String lang = request.getParameter(getParamName());
                // Only switch to languages we have text for; ignore anything else.
                if (lang != null
                        && SUPPORTED.stream().noneMatch(l -> l.getLanguage().equals(lang))) {
                    return true;
                }
                return super.preHandle(request, response, handler);
            }
        };
        interceptor.setParamName("lang");
        registry.addInterceptor(interceptor);
    }
}
