package nl.tudelft.sem.template.delivery.controllers;

import java.util.Collections;
import java.util.List;
import nl.tudelft.sem.template.api.DeliveriesApi;
import nl.tudelft.sem.template.delivery.communication.UsersCommunication;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class DeliveryController implements DeliveriesApi {

    private final DeliveryService deliveryService;

    private final UsersCommunication usersCommunication;

    public DeliveryController(DeliveryService deliveryService, UsersCommunication usersCommunication) {
        this.deliveryService = deliveryService;
        this.usersCommunication = usersCommunication;
    }

    // TODO: Authenticate user id
    @Override
    public ResponseEntity<String> deliveriesDeliveryIdStatusGet(@PathVariable UUID deliveryId, @RequestHeader String userId) {
        DeliveryStatus status = deliveryService.getDeliveryStatus(deliveryId);
        return ResponseEntity.ok(status.toString());
    }

    // TODO: Authenticate user id
    @Override
    public ResponseEntity<Delivery> deliveriesDeliveryIdStatusPut(@PathVariable UUID deliveryId, @RequestHeader String userId, @RequestBody String statusString) {
        DeliveryStatus status = DeliveryStatus.fromValue(statusString);
        deliveryService.updateDeliveryStatus(deliveryId, status);
        Delivery updatedDelivery = deliveryService.getDelivery(deliveryId);
        return ResponseEntity.ok(updatedDelivery);
    }

    // TODO: CHRIS
    @GetMapping("/deliveries/all/accepted")
    @Override
    public ResponseEntity<List<Delivery>> deliveriesAllAcceptedGet(@RequestHeader String userId) {
        String accountType = usersCommunication.getAccountType(userId);
        if (accountType.equals("admin") || accountType.equals("courier"))
            return ResponseEntity.ok(deliveryService.getAcceptedDeliveries());
        if (accountType.equals("vendor") || accountType.equals("customer"))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.emptyList());

        // Account type is "in-existent"
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.emptyList());
    }

    // TODO: CHRIS
    @GetMapping("/deliveries/{deliveryId}/courier")
    @Override
    public ResponseEntity<String> deliveriesDeliveryIdCourierGet(@PathVariable UUID deliveryId,
                                                                 @RequestHeader String userId) {
        String userType = usersCommunication.getAccountType(userId);
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        switch(userType){
            case "admin": return ResponseEntity.ok(delivery.getCourierID());
            case "vendor": {
                if (delivery.getRestaurantID().equals(userId))
                    return ResponseEntity.ok(delivery.getCourierID());
            }
            case "courier": {
                if(delivery.getCourierID() == null)
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No courier assigned to order");
                if (delivery.getCourierID().equals(userId))
                    return ResponseEntity.ok(delivery.getCourierID());
            }
            case "customer": {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("FORBIDDEN");
            }
        }

        // Account type is "in-existent"
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("SERVER_ERROR");
    }

    // TODO: CHRIS
    @PostMapping("/deliveries/{deliveryId}/courier")
    @Override
    public ResponseEntity<Delivery> deliveriesDeliveryIdCourierPut(@PathVariable UUID deliveryId,
                                                                   @RequestHeader String userId,
                                                                   @RequestBody String courierId) {
        String courier = usersCommunication.getAccountType(courierId);
        String userType = usersCommunication.getAccountType(userId);
        if (!courier.equals("courier"))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        switch (userType) {
            case "customer":
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            case "in-existent":
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            case "admin":
                deliveryService.updateDeliveryCourier(deliveryId, courierId);
            case "courier": {
                if (userId.equals(courierId) && deliveryService.getDelivery(deliveryId).getCourierID() == null) {
                    deliveryService.updateDeliveryCourier(deliveryId, courierId);
                } else {
                    // Courier is not allowed to assign other couriers to orders or assign themselves over someone
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
                }
            }
            case "vendor": {
                // TODO : Once we have the vendor repository and DAO -> vendor can only assign couriers that are in their list
            }
        }

        Delivery delivery = deliveryService.getDelivery(deliveryId);
        return ResponseEntity.ok(delivery);

    }

}
