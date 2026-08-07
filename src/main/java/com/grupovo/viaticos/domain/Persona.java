package com.grupovo.viaticos.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.envers.Audited;
import com.grupovo.viaticos.domain.en.Role;

import java.time.LocalDateTime;

@Audited
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "persona")
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPersona;
    @Column(nullable = false, length = 100)
    private String nombre;
    @Column(nullable = false, length = 100)
    private String apellidoPaterno;
    @Column(nullable = false, length = 100)
    private String apellidoMaterno;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    @Column(nullable = false)
    private Boolean activa;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    @Column(nullable = false, length = 100)
    private String password;

}
