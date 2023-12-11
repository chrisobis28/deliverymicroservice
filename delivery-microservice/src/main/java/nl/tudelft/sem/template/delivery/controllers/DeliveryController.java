package nl.tudelft.sem.template.delivery.controllers;

import nl.tudelft.sem.template.api.DeliveriesApi;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class DeliveryController implements DeliveriesApi {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    // TODO: Authenticate user id
    @Override
    public ResponseEntity<String> deliveriesDeliveryIdStatusGet(@PathVariable UUID deliveryId, @RequestHeader String userId) {
        DeliveryStatus status = deliveryService.getDeliveryStatus(deliveryId);
        return ResponseEntity.ok(status.toString());
    }
    public ResponseEntity<Void> insert(@RequestBody Delivery delivery) {
        try {
            deliveryService.insert(delivery);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    @Override
    public ResponseEntity<List<Double>> deliveriesDeliveryIdDeliveryAddressGet(@PathVariable UUID deliveryId, @RequestHeader String userId) {
        List<Double> deliveryAddress = deliveryService.getDeliveryAddress(deliveryId);
        return ResponseEntity.ok(deliveryAddress);
    }
    // TODO: Authenticate user id
    @Override
    public ResponseEntity<List<Double>>  deliveriesDeliveryIdPickupLocationGet(@PathVariable UUID deliveryId, @RequestHeader String userId) {
        List<Double> pickupAddress = deliveryService.getPickupLocation(deliveryId);
        return ResponseEntity.ok(pickupAddress);
    }
    // TODO: Authenticate user id
    @Override
    public ResponseEntity<Delivery> deliveriesDeliveryIdStatusPut(@PathVariable UUID deliveryId, @RequestHeader String userId, @RequestBody String statusString) {
        DeliveryStatus status = DeliveryStatus.fromValue(statusString);
        deliveryService.updateDeliveryStatus(deliveryId, status);
        Delivery updatedDelivery = deliveryService.getDelivery(deliveryId);
        return ResponseEntity.ok(updatedDelivery);
    }
}
