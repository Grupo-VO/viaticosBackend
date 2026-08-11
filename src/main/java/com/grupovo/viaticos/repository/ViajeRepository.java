package com.grupovo.viaticos.repository;

import com.grupovo.viaticos.domain.Viaje;
import com.grupovo.viaticos.domain.en.ViajeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface ViajeRepository extends JpaRepository<Viaje, Integer> {

    @Query("""
            SELECT v FROM Viaje v
            WHERE v.vehiculo.idVehiculo = :idVehiculo
              AND v.status IN :statuses
              AND v.fechaSalida <= :fin
              AND :inicio <= v.fechaRegreso
              AND (:idViaje IS NULL OR v.idViaje <> :idViaje)
            """)
    List<Viaje> findConflictingByVehiculo(@Param("idVehiculo") Integer idVehiculo,
                                          @Param("inicio") LocalDateTime inicio,
                                          @Param("fin") LocalDateTime fin,
                                          @Param("statuses") Collection<ViajeStatus> statuses,
                                          @Param("idViaje") Integer idViaje);

    @Query("""
            SELECT v FROM Viaje v
            WHERE v.chofer.idPersona = :idChofer
              AND v.status IN :statuses
              AND v.fechaSalida <= :fin
              AND :inicio <= v.fechaRegreso
              AND (:idViaje IS NULL OR v.idViaje <> :idViaje)
            """)
    List<Viaje> findConflictingByChofer(@Param("idChofer") Integer idChofer,
                                        @Param("inicio") LocalDateTime inicio,
                                        @Param("fin") LocalDateTime fin,
                                        @Param("statuses") Collection<ViajeStatus> statuses,
                                        @Param("idViaje") Integer idViaje);

    List<Viaje> findAllByOrderByFechaSalidaDesc();

    List<Viaje> findAllByFechaSalidaBetweenOrderByFechaSalidaDesc(LocalDateTime inicio, LocalDateTime fin);

    List<Viaje> findAllByChoferIdPersonaOrderByFechaSalidaDesc(Integer idChofer);

    List<Viaje> findByChoferIdPersonaAndStatusOrderByFechaSalidaAsc(Integer idChofer, ViajeStatus status);

    List<Viaje> findByVehiculoIdVehiculoAndStatusOrderByFechaSalidaAsc(Integer idVehiculo, ViajeStatus status);

    List<Viaje> findByStatusInOrderByFechaSalidaAsc(Collection<ViajeStatus> statuses);

    long countByVehiculoIdVehiculoAndStatus(Integer idVehiculo, ViajeStatus status);

    long countByVehiculoIdVehiculoAndStatusIn(Integer idVehiculo, Collection<ViajeStatus> statuses);
}
