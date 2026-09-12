package com.example.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Configures Springdoc OpenAPI / Swagger UI.
 *
 * <p>
 * Registers a global Bearer token security scheme so that every endpoint
 * has an "Authorize" button in the Swagger UI for easy manual testing.
 */
@Configuration
public class SwaggerConfig {

        @Bean
        public OpenAPI openAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("PhysLive API")
                                                .description("Backend API cho nền tảng mô phỏng vật lý tương tác PhysLive. ")
                                                .version("v1.0")
                                                .contact(new Contact()
                                                                .name("PhysLive Team")
                                                                .email("")))
                                .components(new Components()
                                                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                                                .type(SecurityScheme.Type.HTTP)
                                                                .scheme("bearer")
                                                                .bearerFormat("JWT")
                                                                .description("Nhập JWT token lấy từ endpoint /api/auth/login")))
                                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
        }
}
