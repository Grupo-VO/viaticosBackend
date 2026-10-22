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
@Table(name = "destino")
public class Destino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idDestino;
    @Column(nullable = false, length = 70)
    private String nombre;
    @Column(nullable = false, length = 200)
    private String direccion;
    @Column(nullable = false)
    private Double latitud;
    @Column(nullable = false)
    private Double longitud;
    @Column(nullable = false)
    private Boolean activo;

}
