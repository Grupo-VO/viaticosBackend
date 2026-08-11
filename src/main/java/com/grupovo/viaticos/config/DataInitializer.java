package com.grupovo.viaticos.config;

import com.grupovo.viaticos.domain.ConfiguracionSistema;
import com.grupovo.viaticos.domain.Persona;
import com.grupovo.viaticos.domain.en.Role;
import com.grupovo.viaticos.repository.ConfiguracionSistemaRepository;
import com.grupovo.viaticos.repository.PersonaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DataInitializer implements ApplicationRunner {

    private final PersonaRepository personaRepository;
    private final ConfiguracionSistemaRepository configuracionRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.seed-admin.enabled:true}")
    private boolean enabled;

    @Value("${app.security.seed-admin.username:admin}")
    private String username;

    @Value("${app.security.seed-admin.password:Admin123}")
    private String password;

    public DataInitializer(PersonaRepository personaRepository,
                           ConfiguracionSistemaRepository configuracionRepository,
                           PasswordEncoder passwordEncoder) {
        this.personaRepository = personaRepository;
        this.configuracionRepository = configuracionRepository;
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
        sembrarConfiguraciones();
    }

    private void sembrarConfiguraciones() {
        Map<String, List<String>> valores = Map.of(
                "PRECIO_COMBUSTIBLE", List.of("24.0", "Costo del litro de gasolina (pesos)"),
                "UMBRAL_GASOLINA", List.of("20", "Porcentaje del tanque que dispara la alerta de gasolina baja")
        );
        valores.forEach((clave, datos) -> {
            if (configuracionRepository.findByClave(clave).isEmpty()) {
                configuracionRepository.save(ConfiguracionSistema.builder()
                        .clave(clave)
                        .valor(datos.get(0))
                        .descripcion(datos.get(1))
                        .build());
            }
        });
    }
}
