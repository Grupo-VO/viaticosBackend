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
@Table(name = "caseta")
public class Caseta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idCaseta;
    @Column(nullable = false, length = 100)
    private String nombre;
    @Column(nullable = false)
    private Double costo;
    @Column(nullable = false)
    private Integer orden;
    private Double latitud;
    private Double longitud;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ruta")
    private Ruta ruta;

}
