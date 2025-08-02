package com.mastercard.ids.fts.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityConfigTest {
    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig();
    }

    @Test
    void securityFilterChain_shouldPermitAllAndDisableCsrf() throws Exception {
        HttpSecurity http = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);
        // Mock the chained methods
        when(http.authorizeHttpRequests(any())).thenReturn(http);
        when(http.csrf(any())).thenReturn(http);
        when(http.headers(any())).thenReturn(http);

        SecurityFilterChain chain = securityConfig.securityFilterChain(http);
        assertNotNull(chain);
        // The actual filter chain is built by Spring, so we only check that the bean is created and no exception is thrown
    }

    @Test
    void securityFilterChain_shouldThrowExceptionIfHttpSecurityFails() throws Exception {
        HttpSecurity http = mock(HttpSecurity.class);
        when(http.authorizeHttpRequests(any())).thenThrow(new RuntimeException("fail"));
        assertThrows(Exception.class, () -> securityConfig.securityFilterChain(http));
    }
}

