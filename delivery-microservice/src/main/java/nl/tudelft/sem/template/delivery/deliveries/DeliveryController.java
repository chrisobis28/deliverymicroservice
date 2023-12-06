package nl.tudelft.sem.template.delivery.deliveries;

import nl.tudelft.sem.template.api.DeliveriesApi;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController("/deliveries")
public class DeliveryController implements DeliveriesApi {

    private final DeliveryDao deliveryDao;

    public DeliveryController(DeliveryDao deliveryDao) {
        this.deliveryDao = deliveryDao;
    }

    @Override
    @GetMapping("/{deliveryId}/status")
    public ResponseEntity<String> deliveriesDeliveryIdStatusGet(@PathVariable UUID deliveryId, @RequestParam String userId) {
        DeliveryStatus status = deliveryDao.getDeliveryStatus(deliveryId);
        return ResponseEntity.ok(status.toString());
    }

    @Override
    @PutMapping("/{deliveryId}/status")
    public ResponseEntity<Delivery> deliveriesDeliveryIdStatusPut(@PathVariable UUID deliveryId, @RequestParam String userId, @RequestBody String statusString) {
        DeliveryStatus status = DeliveryStatus.fromValue(statusString);
        deliveryDao.updateDeliveryStatus(deliveryId, status);
        Delivery updatedDelivery = deliveryDao.getDelivery(deliveryId);
        return ResponseEntity.ok(updatedDelivery);
    }
}
