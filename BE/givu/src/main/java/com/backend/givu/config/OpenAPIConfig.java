package com.backend.givu.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
//public class OpenAPIConfig {
//
//    @Bean
//    public OpenAPI openAPI() {
//        return new OpenAPI()
//                .info(new Info()
//                        .title("GIVU API")
//                        .description("GIVU 서비스의 API 명세서입니다.")
//                        .version("v1.0.0"));
//    }
//}

public class OpenAPIConfig {
    @Bean
    public OpenAPI openAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("GIVU API")
                        .description("GIVU 서비스의 API 명세서입니다.")
                        .version("v1.0.0"))
                .servers(List.of(
                        new Server().url("https://j12d107.p.ssafy.io/api").description("EC2 Server"),
                        new Server().url("http://192.168.100.203:8080/api").description("Local Server 1"),
                        new Server().url("http://192.168.100.201:8080/api").description("Local Server 2"),
                        new Server().url("http://192.168.0.115:8080/api").description("문경집"),
//                        new Server().url("http://172.30.1.79:8080/api").description("투썸")
                        new Server().url("http://192.168.0.78:8080/api").description("에이바우트"),
                        new Server().url("http://192.168.1.14:8080/api").description("커피비치")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}