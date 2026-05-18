package pl.goeuropa.tc_helper.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://**", "https://**")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*", "Authorization")
                .allowCredentials(true);
    }
}
