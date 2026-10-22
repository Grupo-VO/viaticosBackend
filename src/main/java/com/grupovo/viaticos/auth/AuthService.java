package com.grupovo.viaticos.auth;

import com.grupovo.viaticos.domain.Persona;
import com.grupovo.viaticos.domain.Token;
import com.grupovo.viaticos.exception.InvalidEntryException;
import com.grupovo.viaticos.repository.PersonaRepository;
import com.grupovo.viaticos.repository.TokenRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final PersonaRepository personaRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(PersonaRepository personaRepository,
                       TokenRepository tokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.personaRepository = personaRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthSession register(RegisterRequest request) {
        if (personaRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new InvalidEntryException("El usuario ya existe");
        }

        Persona persona = Persona.builder()
                .nombre(request.nombre())
                .apellidoPaterno(request.apellidoPaterno())
                .apellidoMaterno(request.apellidoMaterno())
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .activa(true)
                .build();

        persona = personaRepository.save(persona);

        Token token = jwtService.generateToken(persona);
        AuthSession session = prepararToken(token);

        return session;
    }

    @Transactional
    public AuthSession login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(), request.password()
                )
        );

        Persona persona = personaRepository.findByUsernameIgnoreCase(request.username())
                .orElseThrow(() -> new InvalidEntryException("Credenciales inválidas"));

        revokeAllTokens(persona);

        Token token = jwtService.generateToken(persona);
        AuthSession session = prepararToken(token);

        return session;
    }

    @Transactional
    public AuthSession refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidEntryException("Token de refresco inválido");
        }

        final String username;
        try {
            username = jwtService.extractUsername(refreshToken);
        } catch (Exception ex) {
            throw new InvalidEntryException("Token de refresco inválido");
        }

        Persona persona = personaRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new InvalidEntryException("Usuario no encontrado"));

        var storedToken = tokenRepository.findByRefreshTokenHash(jwtService.hashRefreshToken(refreshToken)).orElse(null);
        if (storedToken == null || storedToken.getIsRevoked() || storedToken.getIsExpired()) {
            throw new InvalidEntryException("Token de refresco inválido o expirado");
        }

        if (jwtService.isTokenExpired(refreshToken)) {
            storedToken.setIsExpired(true);
            tokenRepository.save(storedToken);
            throw new InvalidEntryException("Token de refresco expirado");
        }

        revokeAllTokens(persona);

        Token newToken = jwtService.generateToken(persona);
        AuthSession session = prepararToken(newToken);

        return session;
    }

    @Transactional
    public void logout(String accessToken, String refreshToken) {
        var storedToken = refreshToken == null ? null
                : tokenRepository.findByRefreshTokenHash(jwtService.hashRefreshToken(refreshToken)).orElse(null);
        if (storedToken == null && accessToken != null) {
            storedToken = tokenRepository.findByToken(accessToken).orElse(null);
        }
        if (storedToken != null) {
            storedToken.setIsRevoked(true);
            storedToken.setIsExpired(true);
            tokenRepository.save(storedToken);
        }
    }

    private void revokeAllTokens(Persona persona) {
        var tokens = tokenRepository.findAllValidTokensByPersona(persona.getIdPersona());
        tokens.forEach(t -> {
            t.setIsExpired(true);
            t.setIsRevoked(true);
        });
        tokenRepository.saveAll(tokens);
    }

    private AuthSession prepararToken(Token token) {
        String refreshToken = token.getRefreshToken();
        token.setRefreshTokenHash(jwtService.hashRefreshToken(refreshToken));
        token.setRefreshToken(null);
        return new AuthSession(tokenRepository.save(token), refreshToken);
    }

    public AuthResponse sesion(String username) {
        Persona persona = personaRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new InvalidEntryException("Usuario no encontrado"));
        return new AuthResponse(
                persona.getUsername(),
                persona.getNombre() + " " + persona.getApellidoPaterno(),
                persona.getRole().name()
        );
    }

}
