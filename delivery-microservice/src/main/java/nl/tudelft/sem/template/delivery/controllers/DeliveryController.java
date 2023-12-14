package nl.tudelft.sem.template.delivery.controllers;

import java.util.Collections;
import java.util.List;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import nl.tudelft.sem.template.api.DeliveriesApi;
import nl.tudelft.sem.template.delivery.communication.UsersCommunication;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.UUID;

@RestController
public class DeliveryController implements DeliveriesApi {

    private final DeliveryService deliveryService;

    private final UsersCommunication usersCommunication;

    /**
     * Constructor
     * @param deliveryService the delivery service
     */
    @Autowired
    public DeliveryController(DeliveryService deliveryService, UsersCommunication usersCommunication) {
        this.deliveryService = deliveryService;
        this.usersCommunication = usersCommunication;
    }


    @Override
    public ResponseEntity<String> deliveriesDeliveryIdStatusGet(@PathVariable UUID deliveryId, @RequestHeader String userId) {
        String accountType = usersCommunication.getAccountType(userId);
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        if (accountType.equals("admin") ) {
            return ResponseEntity.ok(delivery.getStatus().toString());
        }
        if(accountType.equals("courier"))
        {
            if(!delivery.getCourierID().equals(userId))
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("FORBIDDEN");
            return ResponseEntity.ok(delivery.getStatus().toString());
        }
        if(accountType.equals("vendor"))
        {
            if(!delivery.getRestaurantID().equals(userId))
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("FORBIDDEN");
            return ResponseEntity.ok(delivery.getStatus().toString());
        }
        if(accountType.equals("customer"))
        {
            if(!delivery.getCustomerID().equals(userId))
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("FORBIDDEN");
            return ResponseEntity.ok(delivery.getStatus().toString());
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("UNAUTHORIZED");
    }

    /**
     * inserts an element into the repo
     * @param delivery delivery being inserted
     * @return the entity
     */
    public ResponseEntity<Void> insert(@RequestBody Delivery delivery) {
        try {
            deliveryService.insert(delivery);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * returns the delivery address
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId User ID for authorization (required)
     * @return delivery address
     */
    @Override
    public ResponseEntity<List<Double>> deliveriesDeliveryIdDeliveryAddressGet(@PathVariable UUID deliveryId, @RequestHeader String userId) {
        try {
            String accountType = usersCommunication.getAccountType(userId);

            // Retrieve delivery information
            Delivery delivery = deliveryService.getDelivery(deliveryId);

            // Check user access based on account type and association with the delivery
            if (accountType.equals("admin") || (accountType.equals("courier") && delivery.getCourierID().equals(userId))
                    || (accountType.equals("vendor") && delivery.getRestaurantID().equals(userId))
                    || (accountType.equals("customer") && delivery.getCustomerID().equals(userId))) {
                List<Double> deliveryAddress = deliveryService.getDeliveryAddress(deliveryId);
                return ResponseEntity.ok(deliveryAddress);
            } else {
                // User does not have access
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(List.of());
            }
        } catch (DeliveryService.DeliveryNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(List.of());
        }
    }

    /**
     * Returns the pickup location
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId User ID for authorization (required)
     * @return the pickup location
     */
    @Override
    public ResponseEntity<List<Double>> deliveriesDeliveryIdPickupLocationGet(@PathVariable UUID deliveryId, @RequestHeader String userId) {
        try {
            String accountType = usersCommunication.getAccountType(userId);
            Delivery delivery = deliveryService.getDelivery(deliveryId);
            // Check user access based on account type and association with the delivery
            if (accountType.equals("admin") || (accountType.equals("courier") && delivery.getCourierID().equals(userId))
                    || (accountType.equals("vendor") && delivery.getRestaurantID().equals(userId))) {
                List<Double> pickupAddress = deliveryService.getPickupLocation(deliveryId);
                return ResponseEntity.ok(pickupAddress);
            } else if (accountType.equals("customer") && delivery.getCustomerID().equals(userId)) {
                // Customer has access
                List<Double> pickupAddress = deliveryService.getPickupLocation(deliveryId);
                return ResponseEntity.ok(pickupAddress);
            } else {
                // User does not have access
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(List.of());
            }
        } catch (DeliveryService.DeliveryNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(List.of());
        }
    }
    @Override
    public ResponseEntity<Delivery> deliveriesDeliveryIdStatusPut(@PathVariable UUID deliveryId, @RequestHeader String userId, @RequestBody String statusString) {

        String accountType = usersCommunication.getAccountType(userId);
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        DeliveryStatus newStatus = DeliveryStatus.fromValue(statusString);

        if (accountType.equals("admin") ) {
            deliveryService.updateDeliveryStatus(deliveryId, newStatus);
            Delivery updatedDelivery = deliveryService.getDelivery(deliveryId);
            return ResponseEntity.ok(updatedDelivery);
        }
        if(accountType.equals("courier"))
        {
            //You do not have access
            if(!delivery.getCourierID().equals(userId))
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            deliveryService.updateDeliveryStatus(deliveryId, newStatus);
            Delivery updatedDelivery = deliveryService.getDelivery(deliveryId);
            return ResponseEntity.ok(updatedDelivery);
        }
        if(accountType.equals("vendor"))
        {
            //You do not have access
            if(!delivery.getRestaurantID().equals(userId))
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            deliveryService.updateDeliveryStatus(deliveryId, newStatus);
            Delivery updatedDelivery = deliveryService.getDelivery(deliveryId);
            return ResponseEntity.ok(updatedDelivery);
        }
        if(accountType.equals("customer"))
        {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        //We do not know who you are
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
    }

    /**
     * Checks if a string is null or empty
     * @param str string to check
     * @return boolean value indicating whether string is empty or not
     */
    public boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty() || str.equals(" ");
    }

    /**
     * Allows the customer to update a courier's rating
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId User ID for authorization (required)
     * @param body Update rating of delivery (required)
     * @return Response entity containing the updated Delivery object
     */
    @Override
    @RequestMapping(
        method = {RequestMethod.PUT},
        value = {"/{deliveryId}/rating-courier"},
        produces = {"application/json"},
        consumes = {"application/json"}
    )
    public ResponseEntity<Delivery> deliveriesDeliveryIdRatingCourierPut(@PathVariable("deliveryId") UUID deliveryId, @RequestHeader @NotNull String userId, @RequestBody @Valid Integer body) {
        if (isNullOrEmpty(userId)) {
            return ResponseEntity.badRequest().build();
        } else {
            Delivery delivery = deliveryService.getDelivery(deliveryId);
            //Call user endpoint that verifies the role of user path:"/account/type"
            String type = usersCommunication.getAccountType(userId);
            String email = delivery.getCustomerID();
            boolean isCustomer = userId.equals(email) && type.equals("customer");
            if (!isCustomer && !type.equals("admin")) {
                return ResponseEntity.status(403).build();
            } else {
                deliveryService.updateCourierRating(deliveryId, body);
                delivery = deliveryService.getDelivery(deliveryId);
                return ResponseEntity.ok(delivery);
            }
        }
    }

    @Override
    @RequestMapping(
        method = {RequestMethod.PUT},
        value = {"/{deliveryId}/rating-restaurant"},
        produces = {"application/json"},
        consumes = {"application/json"}
    )
    public ResponseEntity<Delivery> deliveriesDeliveryIdRatingRestaurantPut(/*@Parameter(name = "deliveryId",description = "ID of the Delivery entity",required = true,in = ParameterIn.PATH) */@PathVariable("deliveryId") UUID deliveryId, /*@Parameter(name = "userId",description = "User ID for authorization",required = true,in = ParameterIn.HEADER)*/ @RequestHeader @NotNull String userId, /*@Parameter(name = "body",description = "Update rating of restaurant for delivery",required = true) */@RequestBody @Valid Integer body) {
        if (isNullOrEmpty(userId)) {
            return ResponseEntity.badRequest().build();
        } else {
            Delivery delivery = deliveryService.getDelivery(deliveryId);
            //Call user endpoint that verifies the role of user path:"/account/type"
            String type = usersCommunication.getAccountType(userId);
            String email = delivery.getCustomerID();//orderMockRepo.getUserEmail(deliveryId);
            boolean isCustomer = userId.equals(email) && type.equals("customer");
            if (!isCustomer && !type.equals("admin")) {
                return ResponseEntity.status(403).build();
            } else {
                deliveryService.updateRestaurantRating(deliveryId, body);
                delivery = deliveryService.getDelivery(deliveryId);
                return ResponseEntity.ok(delivery);
            }
        }
    }

    @Override
    @RequestMapping(
        method = {RequestMethod.GET},
        value = {"/{deliveryId}/rating-restaurant"},
        produces = {"application/json"}
    )
    public ResponseEntity<Integer> deliveriesDeliveryIdRatingRestaurantGet(@Parameter(name = "deliveryId",description = "ID of the Delivery entity",required = true,in = ParameterIn.PATH) @PathVariable("deliveryId") UUID deliveryId, @Parameter(name = "userId",description = "User ID for authorization",required = true,in = ParameterIn.HEADER) @RequestHeader @NotNull String userId) {
        //Only people that can see rating is the customer who left the rating, the vendor and the admin
        if (isNullOrEmpty(userId)) {
            return ResponseEntity.badRequest().build();
        } else {
            Delivery delivery = deliveryService.getDelivery(deliveryId);
            String type = usersCommunication.getAccountType(userId);
            String restaurantEmail = delivery.getRestaurantID();
            String customerEmail = delivery.getCustomerID();
            boolean isVendor = type.equals("vendor") && restaurantEmail.equals(userId);
            boolean isCustomer = type.equals("customer") && customerEmail.equals(userId);
            if (!type.equals("admin") && !isVendor && !isCustomer) {
                return ResponseEntity.status(403).build();
            } else {
                return ResponseEntity.ok(delivery.getRatingRestaurant());
            }
        }
    }

    @Override
    @RequestMapping(
        method = {RequestMethod.GET},
        value = {"/{deliveryId}/rating-courier"},
        produces = {"application/json"}
    )
    public ResponseEntity<Integer> deliveriesDeliveryIdRatingCourierGet(@Parameter(name = "deliveryId",description = "ID of the Delivery entity",required = true,in = ParameterIn.PATH) @PathVariable("deliveryId") UUID deliveryId, @Parameter(name = "userId",description = "User ID for authorization",required = true,in = ParameterIn.HEADER) @RequestHeader @NotNull String userId) {
        //Only people that can see rating is the customer who left the rating, the courier and the admin
        if (isNullOrEmpty(userId)) {
            return ResponseEntity.badRequest().build();
        } else {
            Delivery delivery = deliveryService.getDelivery(deliveryId);
            String type = usersCommunication.getAccountType(userId);
            String courierEmail = delivery.getCourierID();
            String customerEmail = delivery.getCustomerID();
            boolean isCourier = type.equals("courier") && courierEmail.equals(userId);
            boolean isCustomer = type.equals("customer") && customerEmail.equals(userId);
            if (!type.equals("admin") && !isCourier && !isCustomer) {
                return ResponseEntity.status(403).build();
            } else {
                return ResponseEntity.ok(delivery.getRatingCourier());
            }
        }
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
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
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
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No courier assigned to order.");
                if (delivery.getCourierID().equals(userId))
                    return ResponseEntity.ok(delivery.getCourierID());
            }
            case "customer": {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User lacks necessary permissions.");
            }
        }

        // Account type is "non-existent"
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User lacks valid authentication credentials.");
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
            case "non-existent":
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            case "admin":
                deliveryService.updateDeliveryCourier(deliveryId, courierId);
            case "courier": {
                if (userId.equals(courierId) && deliveryService.getDelivery(deliveryId).getCourierID() == null) {
                    deliveryService.updateDeliveryCourier(deliveryId, courierId);
                } else {
                    // Courier is not allowed to assign other couriers to orders or assign themselves over someone
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
                }
            }
            case "vendor": {
                // TODO : Once we have the vendor repository and DAO -> vendor can only assign couriers that are in their list
            }
        }

        Delivery delivery = deliveryService.getDelivery(deliveryId);
        return ResponseEntity.ok(delivery);
    }

    /**
     * Updates the estimated preparation time of an order
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId User ID for authorization (required)
     * @param body Update prep time of delivery (required)
     * @return a delivery object with the updates that took place
     */
    @PutMapping("/deliveries/{deliveryId}/prep")
    @Override
    public ResponseEntity<Delivery> deliveriesDeliveryIdPrepPut(@PathVariable UUID deliveryId, @RequestHeader String userId, @RequestBody Integer body) {
        String userType = usersCommunication.getAccountType(userId);
        if(userType.equals("admin") ||
                userType.equals("vendor")) {
            deliveryService.updateEstimatedPrepTime(deliveryId, body);
            Delivery delivery = deliveryService.getDelivery(deliveryId);
            return ResponseEntity.ok(delivery);
        } else if (userType.equals("client") ||
                userType.equals("courier")) {
            // User lacks necessary permission levels.
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        } else {
            // User lacks valid authentication credentials.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }
}
