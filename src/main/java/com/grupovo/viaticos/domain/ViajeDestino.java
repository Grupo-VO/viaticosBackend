package com.grupovo.viaticos.domain;

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
@Table(name = "viaje_destino")
public class ViajeDestino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idViajeDestino;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_viaje")
    private Viaje viaje;
    @Column(nullable = false, length = 70)
    private String nombre;
    @Column(nullable = false, length = 200)
    private String direccion;
    @Column(nullable = false)
    private Double latitud;
    @Column(nullable = false)
    private Double longitud;
    @Column(nullable = false)
    private Integer orden;
    @Column(nullable = false)
    @Builder.Default
    private Boolean visitado = false;
    private LocalDateTime fechaLlegada;

}
