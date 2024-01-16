package nl.tudelft.sem.template.delivery.domain;

import java.util.List;
import java.util.UUID;
import nl.tudelft.sem.template.model.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    List<Delivery> findAllByrestaurantID(String restaurantId);

    List<Delivery> findAllByCustomerID(String customerId);

    List<Delivery> findAllByCourierID(String courierId);
}
