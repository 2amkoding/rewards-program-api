package com.portalsplatform.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String API_KEY_SCHEME_NAME = "ApiKey";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Rewards Program API")
                .version("1.0.0")
                .description("REST API for customer rewards program management\n\n" +
                           "**Authentication:** All endpoints except health and documentation require an API key.\n\n" +
                           "**Default API Key (Development):** `dev-demo-key-12345`\n\n" +
                           "**Rate Limiting:** 100 requests per minute per API key")
                .contact(new Contact()
                    .name("Portal Platform Team")
                    .email("team@portalplatform.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("http://www.apache.org/licenses/LICENSE-2.0.html")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Local Development"),
                new Server().url("https://api.portalplatform.com").description("Production")
            ))
            .components(new Components()
                .addSecuritySchemes(API_KEY_SCHEME_NAME, new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("X-API-Key")
                    .description("API key required for authentication. Use: dev-demo-key-12345")
                )
            )
            .addSecurityItem(new SecurityRequirement().addList(API_KEY_SCHEME_NAME));
    }
}
