package com.grupovo.viaticos.domain;

import com.grupovo.viaticos.domain.en.MantenimientoEstado;
import com.grupovo.viaticos.domain.en.MantenimientoTipo;
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
@Table(name = "mantenimiento")
public class Mantenimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idMantenimiento;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_vehiculo")
    private Vehiculo vehiculo;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MantenimientoTipo tipo;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MantenimientoEstado estado = MantenimientoEstado.EN_CURSO;
    @Column(nullable = false)
    private LocalDateTime fecha;
    @Column(nullable = false, length = 500)
    private String descripcion;
    @Column(nullable = false)
    private Double costo;
    @Column(nullable = false)
    private Double odometro;
    @Column(length = 500)
    private String evidenciaRuta;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

}
