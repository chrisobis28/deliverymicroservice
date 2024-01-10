package nl.tudelft.sem.template.delivery.domain;

import java.util.List;
import nl.tudelft.sem.template.model.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {
  List<Delivery> findAllByrestaurantID(String restaurantID);

  List<Delivery> findAllByCustomerID(String customerID);
  List<Delivery> findAllByCourierID(String courierID);
}
