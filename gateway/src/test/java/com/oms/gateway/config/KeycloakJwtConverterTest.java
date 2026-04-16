package com.oms.gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("Keycloak JWT Converter")
class KeycloakJwtConverterTest {

    private ReactiveJwtAuthenticationConverter converter;

    @BeforeEach
    void setUp() throws Exception {
        SecurityConfig securityConfig = new SecurityConfig();
        Method keycloakConverterMethod = SecurityConfig.class.getDeclaredMethod("keycloakConverter");
        keycloakConverterMethod.setAccessible(true);
        converter = (ReactiveJwtAuthenticationConverter) keycloakConverterMethod.invoke(securityConfig);
    }

    private Jwt buildJwt(Map<String, Object> claims) {
        var builder = Jwt.withTokenValue("mock-token")
                .header("alg", "RS256")
                .subject("test-user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));
        claims.forEach(builder::claim);
        return builder.build();
    }

    @Nested
    @DisplayName("when realm_access claim is null")
    class RealmAccessNull {

        @Test
        @DisplayName("returns authentication with empty authorities")
        void returnsEmptyAuthorities() {
            var jwt = buildJwt(Map.of());

            StepVerifier.create(converter.convert(jwt))
                    .assertNext(authentication -> assertThat(authentication.getAuthorities())
                            .filteredOn(a -> a.getAuthority().startsWith("ROLE_"))
                            .isEmpty())
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("when realm_access.roles is a non-empty list")
    class RealmAccessWithRoles {

        @Test
        @DisplayName("maps single role with ROLE_ prefix")
        void mapsSingleRole() {
            var jwt = buildJwt(Map.of(
                    "realm_access", Map.of("roles", List.of("CUSTOMER"))
            ));

            StepVerifier.create(converter.convert(jwt))
                    .assertNext(authentication -> assertThat(authentication.getAuthorities())
                            .extracting(GrantedAuthority::getAuthority)
                            .contains("ROLE_CUSTOMER"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("maps multiple roles correctly")
        void mapsMultipleRoles() {
            var jwt = buildJwt(Map.of(
                    "realm_access", Map.of("roles", List.of("CUSTOMER", "ADMIN", "MANAGER"))
            ));

            StepVerifier.create(converter.convert(jwt))
                    .assertNext(authentication -> assertThat(authentication.getAuthorities())
                            .extracting(GrantedAuthority::getAuthority)
                            .contains("ROLE_CUSTOMER", "ROLE_ADMIN", "ROLE_MANAGER"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("preserves role name casing")
        void preservesRoleNameCasing() {
            var jwt = buildJwt(Map.of(
                    "realm_access", Map.of("roles", List.of("SuperAdmin"))
            ));

            StepVerifier.create(converter.convert(jwt))
                    .assertNext(authentication -> assertThat(authentication.getAuthorities())
                            .extracting(GrantedAuthority::getAuthority)
                            .contains("ROLE_SuperAdmin"))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("when realm_access.roles is not a list")
    class RealmAccessRolesNotAList {

        @Test
        @DisplayName("returns authentication with no ROLE_ authorities when roles is a string")
        void returnsEmptyAuthoritiesWhenRolesIsString() {
            var jwt = buildJwt(Map.of(
                    "realm_access", Map.of("roles", "CUSTOMER")
            ));

            StepVerifier.create(converter.convert(jwt))
                    .assertNext(authentication -> assertThat(authentication.getAuthorities())
                            .filteredOn(a -> a.getAuthority().startsWith("ROLE_"))
                            .isEmpty())
                    .verifyComplete();
        }

        @Test
        @DisplayName("returns authentication with no ROLE_ authorities when roles is an integer")
        void returnsEmptyAuthoritiesWhenRolesIsInteger() {
            var jwt = buildJwt(Map.of(
                    "realm_access", Map.of("roles", 42)
            ));

            StepVerifier.create(converter.convert(jwt))
                    .assertNext(authentication -> assertThat(authentication.getAuthorities())
                            .filteredOn(a -> a.getAuthority().startsWith("ROLE_"))
                            .isEmpty())
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("when realm_access exists but has no roles key")
    class RealmAccessWithoutRolesKey {

        @Test
        @DisplayName("returns authentication with no ROLE_ authorities")
        void returnsEmptyAuthoritiesWhenNoRolesKey() {
            var jwt = buildJwt(Map.of(
                    "realm_access", Map.of("other_key", "value")
            ));

            StepVerifier.create(converter.convert(jwt))
                    .assertNext(authentication -> assertThat(authentication.getAuthorities())
                            .filteredOn(a -> a.getAuthority().startsWith("ROLE_"))
                            .isEmpty())
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("when realm_access.roles is an empty list")
    class RealmAccessEmptyRoles {

        @Test
        @DisplayName("returns authentication with no ROLE_ authorities")
        void returnsEmptyAuthoritiesWhenRolesListIsEmpty() {
            var jwt = buildJwt(Map.of(
                    "realm_access", Map.of("roles", List.of())
            ));

            StepVerifier.create(converter.convert(jwt))
                    .assertNext(authentication -> assertThat(authentication.getAuthorities())
                            .filteredOn(a -> a.getAuthority().startsWith("ROLE_"))
                            .isEmpty())
                    .verifyComplete();
        }
    }
}
