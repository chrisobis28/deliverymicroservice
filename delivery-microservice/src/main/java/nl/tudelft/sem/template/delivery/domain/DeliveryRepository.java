package nl.tudelft.sem.template.delivery.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    List<Delivery> findAllByrestaurantID(String restaurantId);

    List<Delivery> findAllByCustomerID(String customerId);

    List<Delivery> findAllByCourierID(String courierId);

    @Query("SELECT e FROM Delivery e WHERE :courierId = e.courierID AND "
        + "e.status = :status AND e.deliveredTime < :end AND e.deliveredTime > :start")
    List<Delivery> findAllByCourierIDAndStatus(String courierId,
                                               DeliveryStatus status,
                                               @Param("start") OffsetDateTime startTime,
                                               @Param("end") OffsetDateTime endTime);

    @Query("SELECT e FROM Delivery e WHERE :vendorId = e.restaurantID AND e.status = :status")
    List<Delivery> findAllByRestaurantIDAndStatus(String vendorId, @Param("status") DeliveryStatus status);
}
