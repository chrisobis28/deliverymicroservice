package nl.tudelft.sem.template.delivery.deliveries;

import nl.tudelft.sem.template.api.DeliveriesApi;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class DeliveryController implements DeliveriesApi {

    private final DeliveryDao deliveryDao;

    public DeliveryController(DeliveryDao deliveryDao) {
        this.deliveryDao = deliveryDao;
    }

    // TODO: Authenticate user id
    @Override
    public ResponseEntity<String> deliveriesDeliveryIdStatusGet(@PathVariable UUID deliveryId, @RequestHeader String userId) {
        DeliveryStatus status = deliveryDao.getDeliveryStatus(deliveryId);
        return ResponseEntity.ok(status.toString());
    }

    // TODO: Authenticate user id
    @Override
    public ResponseEntity<Delivery> deliveriesDeliveryIdStatusPut(@PathVariable UUID deliveryId, @RequestHeader String userId, @RequestBody String statusString) {
        DeliveryStatus status = DeliveryStatus.fromValue(statusString);
        deliveryDao.updateDeliveryStatus(deliveryId, status);
        Delivery updatedDelivery = deliveryDao.getDelivery(deliveryId);
        return ResponseEntity.ok(updatedDelivery);
    }
}
