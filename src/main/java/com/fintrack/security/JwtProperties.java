package com.fintrack.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fintrack.jwt")
public record JwtProperties(String secret, long expirationMs) {
}
