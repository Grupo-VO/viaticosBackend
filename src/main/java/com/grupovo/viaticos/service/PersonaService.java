package com.grupovo.viaticos.service;

import com.grupovo.viaticos.domain.Persona;
import com.grupovo.viaticos.domain.Token;
import com.grupovo.viaticos.domain.en.Role;
import com.grupovo.viaticos.dto.PersonaRequest;
import com.grupovo.viaticos.dto.PersonaResponse;
import com.grupovo.viaticos.exception.InvalidEntryException;
import com.grupovo.viaticos.exception.NotFoundException;
import com.grupovo.viaticos.repository.PersonaRepository;
import com.grupovo.viaticos.repository.TokenRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PersonaService {

    private final PersonaRepository personaRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    public PersonaService(PersonaRepository personaRepository,
                          TokenRepository tokenRepository,
                          PasswordEncoder passwordEncoder) {
        this.personaRepository = personaRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<PersonaResponse> listar(Role role) {
        List<Persona> personas = role == null
                ? personaRepository.findAllByOrderByNombreAsc()
                : personaRepository.findAllByRole(role);
        return personas.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PersonaResponse obtener(Integer id) {
        return toResponse(getPersona(id));
    }

    @Transactional
    public PersonaResponse crear(PersonaRequest request) {
        if (personaRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new InvalidEntryException("El usuario ya existe");
        }
        Persona persona = Persona.builder()
                .nombre(request.nombre().trim())
                .apellidoPaterno(request.apellidoPaterno().trim())
                .apellidoMaterno(request.apellidoMaterno().trim())
                .username(request.username().trim())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .activa(request.activa() == null || request.activa())
                .build();
        return toResponse(personaRepository.save(persona));
    }

    @Transactional
    public PersonaResponse actualizar(Integer id, PersonaRequest request) {
        Persona persona = getPersona(id);
        if (personaRepository.existsByUsernameIgnoreCaseAndIdPersonaNot(request.username(), id)) {
            throw new InvalidEntryException("El usuario ya existe");
        }
        persona.setNombre(request.nombre().trim());
        persona.setApellidoPaterno(request.apellidoPaterno().trim());
        persona.setApellidoMaterno(request.apellidoMaterno().trim());
        persona.setUsername(request.username().trim());
        persona.setRole(request.role());
        if (request.password() != null && !request.password().isBlank()) {
            persona.setPassword(passwordEncoder.encode(request.password()));
        }
        if (request.activa() != null) {
            persona.setActiva(request.activa());
            if (!request.activa()) {
                revokeAllTokens(persona);
            }
        }
        return toResponse(personaRepository.save(persona));
    }

    @Transactional
    public PersonaResponse cambiarEstado(Integer id, Boolean activa) {
        Persona persona = getPersona(id);
        persona.setActiva(activa);
        if (!activa) {
            revokeAllTokens(persona);
        }
        return toResponse(personaRepository.save(persona));
    }

    @Transactional
    public void eliminar(Integer id) {
        Persona persona = getPersona(id);
        revokeAllTokens(persona);
        persona.setActiva(false);
        personaRepository.save(persona);
    }

    private void revokeAllTokens(Persona persona) {
        List<Token> tokens = tokenRepository.findAllValidTokensByPersona(persona.getIdPersona());
        tokens.forEach(t -> {
            t.setIsExpired(true);
            t.setIsRevoked(true);
        });
        tokenRepository.saveAll(tokens);
    }

    public Persona getPersona(Integer id) {
        return personaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Persona no encontrada con id " + id));
    }

    private PersonaResponse toResponse(Persona p) {
        return new PersonaResponse(
                p.getIdPersona(),
                p.getNombre(),
                p.getApellidoPaterno(),
                p.getApellidoMaterno(),
                p.getUsername(),
                p.getRole(),
                p.getActiva(),
                p.getFechaCreacion()
        );
    }
}
