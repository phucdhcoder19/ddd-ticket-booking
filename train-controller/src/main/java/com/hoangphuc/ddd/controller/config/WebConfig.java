package com.hoangphuc.ddd.controller.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.method.HandlerTypePredicate;

/**
 * Cấu hình tầng HTTP. Nằm ở train-controller chứ không phải train-infrastructure,
 * theo đúng phép thử: "đổi MySQL sang MongoDB thì file này có phải sửa không?"
 * — Không. Nhưng đổi từ HTTP sang Kafka thì file này vô nghĩa. Vậy nó thuộc
 * tầng giao tiếp, không thuộc tầng lưu trữ.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Origin của trang web. Vite nhảy cổng khi 5173 bận (5174, 5180, 5181...)
     * nên dùng pattern thay vì liệt kê từng cổng.
     */
    @Value("${app.web.allowed-origin-pattern:http://localhost:[*]}")
    private String allowedOriginPattern;

    /**
     * Thêm tiền tố /api cho MỌI @RestController.
     *
     * Không dùng server.servlet.context-path vì nó đẩy cả /actuator thành
     * /api/actuator — Prometheus đang cấu hình scrape /actuator/prometheus
     * sẽ hỏng. Cách này chỉ chạm vào controller của mình, actuator giữ nguyên.
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api", HandlerTypePredicate.forAnnotation(RestController.class));
    }

    /**
     * CORS — trình duyệt chặn mọi request sang khác origin nếu server không
     * tự khai báo là "tôi cho phép". Trang web chạy ở cổng 5173+, API ở 9999,
     * khác cổng là khác origin.
     *
     * Chỉ mở cho /api/**; /actuator không cần và không nên mở.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOriginPattern)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);   // cache preflight 1 giờ, đỡ 1 request OPTIONS mỗi lần gọi
    }
}
