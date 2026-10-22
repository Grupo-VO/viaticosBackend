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
@Table(name = "ubicacion_vehiculo")
public class UbicacionVehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idUbicacion;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_viaje")
    private Viaje viaje;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_vehiculo")
    private Vehiculo vehiculo;
    @Column(nullable = false)
    private Double latitud;
    @Column(nullable = false)
    private Double longitud;
    @Column(nullable = false)
    private Double velocidadKmh;
    @Column(nullable = false)
    private LocalDateTime fecha;

}
