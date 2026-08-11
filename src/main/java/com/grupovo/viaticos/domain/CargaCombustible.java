package com.grupovo.viaticos.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;

@Audited
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "carga_combustible")
public class CargaCombustible {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idCarga;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_vehiculo")
    private Vehiculo vehiculo;
    @Column(nullable = false)
    private LocalDateTime fecha;
    @Column(nullable = false)
    private Double litros;
    @Column(nullable = false)
    private Double costoTotal;
    @Column(nullable = false)
    private Double odometro;
    @Column(length = 500)
    private String comprobanteRuta;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

}
