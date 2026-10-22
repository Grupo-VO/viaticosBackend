package com.grupovo.viaticos.auth;

import com.grupovo.viaticos.domain.Persona;
import com.grupovo.viaticos.repository.PersonaRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final PersonaRepository personaRepository;

    public UserDetailsServiceImpl(PersonaRepository personaRepository) {
        this.personaRepository = personaRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Persona persona = personaRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        return new User(
                persona.getUsername(),
                persona.getPassword(),
                persona.getActiva(),
                true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + persona.getRole().name()))
        );
    }

}
