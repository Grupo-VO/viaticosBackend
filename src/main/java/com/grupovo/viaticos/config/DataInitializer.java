package com.grupovo.viaticos.config;

import com.grupovo.viaticos.domain.Persona;
import com.grupovo.viaticos.domain.en.Role;
import com.grupovo.viaticos.repository.PersonaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

    private final PersonaRepository personaRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.seed-admin.enabled:true}")
    private boolean enabled;

    @Value("${app.security.seed-admin.username:admin}")
    private String username;

    @Value("${app.security.seed-admin.password:Admin123}")
    private String password;

    public DataInitializer(PersonaRepository personaRepository, PasswordEncoder passwordEncoder) {
        this.personaRepository = personaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        if (personaRepository.count() == 0) {
            Persona admin = Persona.builder()
                    .nombre("Administrador")
                    .apellidoPaterno("Sistema")
                    .apellidoMaterno("Viaticos")
                    .username(username)
                    .password(passwordEncoder.encode(password))
                    .role(Role.ADMINISTRADOR)
                    .activa(true)
                    .build();
            personaRepository.save(admin);
        }
    }
}
