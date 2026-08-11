package com.grupovo.viaticos.domain;

import com.grupovo.viaticos.domain.en.AlertaTipo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;

@Audited
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "alerta")
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idAlerta;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_viaje")
    private Viaje viaje;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_vehiculo")
    private Vehiculo vehiculo;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertaTipo tipo;
    @Column(nullable = false, length = 255)
    private String mensaje;
    @Column(nullable = false)
    private LocalDateTime fecha;
    @Column(nullable = false)
    private Boolean activa;

}
