package com.grupovo.viaticos.domain;

import com.grupovo.viaticos.domain.en.ReporteIncidenteTipo;
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
@Table(name = "reporte_incidente")
public class ReporteIncidente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idReporte;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_viaje")
    private Viaje viaje;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReporteIncidenteTipo tipo;
    @Column(nullable = false, length = 1000)
    private String descripcion;
    @Column(nullable = false)
    private LocalDateTime fecha;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_creado_por")
    private Persona creadoPor;
    @Column(length = 500)
    private String evidenciaRuta;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

}
