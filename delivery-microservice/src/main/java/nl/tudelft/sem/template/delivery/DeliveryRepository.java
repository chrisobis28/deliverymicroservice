package nl.tudelft.sem.template.delivery;

import nl.tudelft.sem.template.model.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;
//@Repository
public interface DeliveryRepository extends CrudRepository<Delivery, UUID> {
}