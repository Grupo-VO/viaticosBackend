package com.grupovo.viaticos.repository;

import com.grupovo.viaticos.domain.UbicacionVehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UbicacionVehiculoRepository extends JpaRepository<UbicacionVehiculo, Integer> {

    List<UbicacionVehiculo> findAllByViajeIdViajeOrderByFechaAsc(Integer idViaje);

    List<UbicacionVehiculo> findAllByViajeIdViajeOrderByFechaDesc(Integer idViaje);

    Optional<UbicacionVehiculo> findFirstByViajeIdViajeOrderByFechaDesc(Integer idViaje);

    List<UbicacionVehiculo> findFirstByVehiculoIdVehiculoOrderByFechaDesc(Integer idVehiculo);
}
