package nl.tudelft.sem.template.delivery.deliveries;

import nl.tudelft.sem.template.model.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository {

    Optional<Delivery> findById(UUID uuid);

    <S extends Delivery> S save(S delivery);
}
