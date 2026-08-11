package com.grupovo.viaticos.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

@Audited
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "viaje_caseta")
public class ViajeCaseta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idViajeCaseta;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_viaje")
    private Viaje viaje;
    @Column(nullable = false, length = 100)
    private String nombre;
    @Column(nullable = false)
    private Double costo;
    @Column(nullable = false)
    private Integer orden;
    private Double latitud;
    private Double longitud;

}
