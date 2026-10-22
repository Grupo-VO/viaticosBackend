package com.grupovo.viaticos.domain;

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
@Table(name = "ruta")
public class Ruta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idRuta;
    @Column(nullable = false, length = 100)
    private String nombre;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_origen")
    private Destino origen;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_destino")
    private Destino destino;
    @Column(nullable = false)
    private Boolean usaCasetas;
    @Column(nullable = false)
    private Double duracionMinutos;
    @Column(nullable = false)
    private Double distanciaKm;
    @Column(nullable = false)
    private Double costoComida;
    @Column(nullable = false)
    private Double costoHospedaje;
    @Column(columnDefinition = "LONGTEXT")
    private String polylineCasetas;
    @Column(columnDefinition = "LONGTEXT")
    private String polylineLibre;
    @Column(columnDefinition = "LONGTEXT")
    private String polylineCasetasIda;
    @Column(columnDefinition = "LONGTEXT")
    private String polylineCasetasRegreso;
    @Column(columnDefinition = "LONGTEXT")
    private String polylineLibreIda;
    @Column(columnDefinition = "LONGTEXT")
    private String polylineLibreRegreso;
    @Column(nullable = false)
    private Boolean activo;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;
    @OneToMany(mappedBy = "ruta", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Caseta> casetas = new ArrayList<>();

}
