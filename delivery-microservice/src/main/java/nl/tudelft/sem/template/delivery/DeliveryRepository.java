package nl.tudelft.sem.template.delivery;

import nl.tudelft.sem.template.model.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
}
