package com.grupovo.viaticos.domain;

import com.grupovo.viaticos.domain.en.VehiculoStatus;
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
@Table(name = "vehiculo")
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idVehiculo;
    @Column(nullable = false, length = 50)
    private String marca;
    @Column(nullable = false, length = 50)
    private String modelo;
    @Column(nullable = false, length = 20)
    private String placa;
    @Column(nullable = false)
    private Double kilometraje;
    @Column(nullable = false)
    private Double rendimiento;
    @Column(nullable = false)
    private Integer limitePasajeros;
    @Column(nullable = false)
    private Double capacidadTanqueLitros;
    @Column(nullable = false)
    private Double nivelCombustible;
    @Column(nullable = false)
    private Boolean activo;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VehiculoStatus status;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

}
