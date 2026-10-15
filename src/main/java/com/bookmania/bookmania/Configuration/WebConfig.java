package com.bookmania.bookmania.Configuration;

import com.bookmania.bookmania.Security.DemoReadOnlyInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final DemoReadOnlyInterceptor demoReadOnlyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(demoReadOnlyInterceptor).addPathPatterns("/api/**");
    }
}
