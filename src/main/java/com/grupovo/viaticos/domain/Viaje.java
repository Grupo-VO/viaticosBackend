package com.grupovo.viaticos.domain;

import com.grupovo.viaticos.domain.en.TipoTrayecto;
import com.grupovo.viaticos.domain.en.ViajeStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Audited
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "viaje")
public class Viaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idViaje;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ruta")
    private Ruta ruta;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_vehiculo")
    private Vehiculo vehiculo;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_chofer")
    private Persona chofer;
    @Column(nullable = false)
    private Integer numeroPasajeros;
    @Column(nullable = false)
    private LocalDateTime fechaSalida;
    @Column(nullable = false)
    private LocalDateTime fechaRegreso;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoTrayecto tipoTrayecto;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ViajeStatus status = ViajeStatus.PROXIMO;
    @Column(nullable = false)
    @Builder.Default
    private Boolean excesoCapacidad = false;
    @Column(columnDefinition = "LONGTEXT")
    private String polylineCasetas;
    @Column(columnDefinition = "LONGTEXT")
    private String polylineLibre;
    private Double distanciaKm;
    private Double duracionMinutos;
    private Double costoComida;
    private Double costoHospedaje;
    private LocalDateTime fechaInicio;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;
    @OneToMany(mappedBy = "viaje", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ViajeDestino> viajeDestinos = new ArrayList<>();
    @OneToMany(mappedBy = "viaje", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ViajeCaseta> viajeCasetas = new ArrayList<>();

}
