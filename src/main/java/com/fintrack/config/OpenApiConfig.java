package com.fintrack.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * A seguranca "bearerAuth" e aplicada globalmente (todo endpoint exige
 * token por padrao no Swagger UI); os endpoints publicos de
 * /api/auth/** usam @SecurityRequirements() (vazio) para sobrescrever
 * isso e nao pedir Authorize. Ver docs/CONVENCOES.md.
 */
@OpenAPIDefinition(
        info = @Info(
                title = "FinTrack API",
                description = "API RESTful de controle financeiro pessoal: autenticação, "
                        + "categorias (globais e customizadas), transações, saldo e relatórios.",
                version = "1.0.0"
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "Token obtido em POST /api/auth/login ou /api/auth/register. "
                + "Cole apenas o token, sem o prefixo 'Bearer '."
)
@Configuration
public class OpenApiConfig {
}
