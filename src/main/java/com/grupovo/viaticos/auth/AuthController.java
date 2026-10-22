package com.grupovo.viaticos.auth;

import com.grupovo.viaticos.domain.Persona;
import com.grupovo.viaticos.domain.Token;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final boolean secureCookies;
    private final String sameSite;

    public AuthController(AuthService authService,
                          @Value("${app.security.cookies.secure:false}") boolean secureCookies,
                          @Value("${app.security.cookies.same-site:Lax}") String sameSite) {
        this.authService = authService;
        this.secureCookies = secureCookies;
        this.sameSite = sameSite;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletResponse response) {
        return authenticatedResponse(authService.login(request), response);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletResponse response) {
        return authenticatedResponse(authService.register(request), response);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(
            @CookieValue(value = "VO_REFRESH", required = false) String refreshToken,
            HttpServletResponse response) {
        return authenticatedResponse(authService.refreshToken(refreshToken), response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = "VO_ACCESS", required = false) String accessToken,
            @CookieValue(value = "VO_REFRESH", required = false) String refreshToken,
            HttpServletResponse response) {
        authService.logout(accessToken, refreshToken);
        deleteCookies(response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(authService.sesion(userDetails.getUsername()));
    }

    @GetMapping("/csrf")
    public ResponseEntity<String> csrf(CsrfToken token) {
        return ResponseEntity.ok("{\"token\":\"" + token.getToken() + "\"}");
    }

    private ResponseEntity<AuthResponse> authenticatedResponse(AuthSession session, HttpServletResponse response) {
        Token token = session.token();
        addCookie(response, "VO_ACCESS", token.getToken(), "/api", 900);
        addCookie(response, "VO_REFRESH", session.refreshToken(), "/api/v1/auth", 604800);
        Persona persona = token.getPersona();
        return ResponseEntity.ok(new AuthResponse(
                persona.getUsername(),
                persona.getNombre() + " " + persona.getApellidoPaterno(),
                persona.getRole().name()));
    }

    private void addCookie(HttpServletResponse response, String name, String value, String path, long maxAge) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(name, value)
                .httpOnly(true).secure(secureCookies).sameSite(sameSite).path(path).maxAge(maxAge).build().toString());
    }

    private void deleteCookies(HttpServletResponse response) {
        addCookie(response, "VO_ACCESS", "", "/api", 0);
        addCookie(response, "VO_REFRESH", "", "/api/v1/auth", 0);
    }
}
