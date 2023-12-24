package nl.tudelft.sem.template.delivery.controllers;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import nl.tudelft.sem.template.api.DeliveriesApi;
import nl.tudelft.sem.template.delivery.communication.UsersCommunication;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.model.*;
import nl.tudelft.sem.template.model.Error;
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

    /**
     * Get an error associated with a delivery
     * (don't check for null error bc error is initialized along with Delivery)
     * @param deliveryId ID of the Delivery entity (required) id of Delivery entity
     * @param userId User ID for authorization (required) user ID
     * @return ResponseEntity containing error (if user is authorized)
     */
    @Override
    public ResponseEntity<Error> deliveriesDeliveryIdUnexpectedEventGet(@PathVariable("deliveryId") UUID deliveryId, @RequestHeader(value = "userId") String userId) {
        if (isNullOrEmpty(userId)) return ResponseEntity.badRequest().build();
        String user = usersCommunication.getAccountType(userId);
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        String customer_id = delivery.getCustomerID();
        String c_id = delivery.getCourierID();
        String r_id = delivery.getRestaurantID();
        if (isNullOrEmpty(c_id) || isNullOrEmpty(customer_id) || isNullOrEmpty(r_id)) return ResponseEntity.notFound().build();
        boolean isVendor = userId.equals(r_id) && user.equals("vendor");
        boolean isCustomer = userId.equals(customer_id) && user.equals("customer");
        boolean isCourier = userId.equals(c_id) && user.equals("courier");
        if (!user.equals("admin") && !isCourier && !isVendor && !isCustomer) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } else {
            return ResponseEntity.ok(delivery.getError());
        }
    }

    /**
     * Get the id of the restaurant making the delivery
     * - access granted only for vendor of restaurant making delivery, courier making delivery and admin
     * @param deliveryId ID of the Delivery entity (required) - checking if Delivery entity exists
     * @param userId User ID for authorization (required) - ID of user making the request
     * @return ResponseEntity containing restaurant id (if user is authorized)
     */
    @Override
    public ResponseEntity<String> deliveriesDeliveryIdRestaurantGet(UUID deliveryId, String userId) {
        if (isNullOrEmpty(userId)) return ResponseEntity.badRequest().build();
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        String userType = usersCommunication.getAccountType(userId);
        String r_id = delivery.getRestaurantID();
        String c_id = delivery.getCourierID();
        if (isNullOrEmpty(r_id) || isNullOrEmpty(c_id)) return ResponseEntity.notFound().build();
        boolean isVendor = userType.equals("vendor") && userId.equals(r_id);
        boolean isCourier = userType.equals("courier") && userId.equals(c_id);
        if (!userType.equals("admin") && !isVendor && !isCourier) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } else {
            return ResponseEntity.ok(r_id);
        }
    }

    /**
     * The get method for getting the pickup time of a delivery
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId User ID for authorization (required)
     * @return
     */
    @Override
    public ResponseEntity<OffsetDateTime> deliveriesDeliveryIdPickupGet(@PathVariable UUID deliveryId, @RequestHeader String userId) {
        String accountType = usersCommunication.getAccountType(userId);
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        if(delivery == null)
            return  ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        if (accountType.equals("admin") ) {
            return ResponseEntity.ok(delivery.getPickupTime());
        }
        if(accountType.equals("courier"))
        {
            if(!delivery.getCourierID().equals(userId))
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            return ResponseEntity.ok(delivery.getPickupTime());
        }
        if(accountType.equals("vendor"))
        {
            if(!delivery.getRestaurantID().equals(userId))
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            return ResponseEntity.ok(delivery.getPickupTime());
        }
        if(accountType.equals("customer"))
        {
            if(!delivery.getCustomerID().equals(userId))
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            return ResponseEntity.ok(delivery.getPickupTime());
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
    }

    /**
     * The put method for updating the pickup time of a delivery
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId User ID for authorization (required)
     * @param pickupTime Update pick up time of delivery (required)
     * @return
     */
    @Override
    public ResponseEntity<Delivery> deliveriesDeliveryIdPickupPut(@PathVariable UUID deliveryId, @RequestHeader String userId,@RequestBody OffsetDateTime pickupTime) {
        String accountType = usersCommunication.getAccountType(userId);
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        if(delivery == null)
            return  ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        if (accountType.equals("admin") ) {
            deliveryService.updatePickupTime(deliveryId,pickupTime);
            return ResponseEntity.ok(delivery);
        }
        if(accountType.equals("courier"))
        {
            if(!delivery.getCourierID().equals(userId))
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            deliveryService.updatePickupTime(deliveryId,pickupTime);
            return ResponseEntity.ok(delivery);
        }
        if(accountType.equals("vendor"))
        {
            if(!delivery.getRestaurantID().equals(userId))
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            deliveryService.updatePickupTime(deliveryId,pickupTime);
            return ResponseEntity.ok(delivery);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
    }

    /**
     * Add Delivery endpoint
     * @param deliveriesPostRequest Request body for creating a new Delivery entity (optional)
     * @return Delivery entity that was just added to the database
     */
    @Override
    public ResponseEntity<Delivery> deliveriesPost(@Valid DeliveriesPostRequest deliveriesPostRequest) {
        if (deliveriesPostRequest == null) {
            return ResponseEntity.badRequest().build();
        }
        Delivery delivery = new Delivery();
        String orderId = deliveriesPostRequest.getOrderId();
        String customerId = deliveriesPostRequest.getCustomerId();
        String vendorId = deliveriesPostRequest.getVendorId();
        List<Double> addr = deliveriesPostRequest.getDeliveryAddress();
        if (isNullOrEmpty(orderId) || isNullOrEmpty(customerId) || isNullOrEmpty(vendorId)) {
            return ResponseEntity.badRequest().build();
        }
        if (addr == null || addr.size() != 2) {
            return ResponseEntity.badRequest().build();
        }
        UUID deliveryId = UUID.fromString(orderId);
        delivery.deliveryID(deliveryId);
        delivery.setCustomerID(customerId);
        delivery.setRestaurantID(vendorId);
        String status = deliveriesPostRequest.getStatus().toUpperCase();
        switch (status) {
            case "PENDING":
                delivery.setStatus(DeliveryStatus.PENDING);
                break;
            case "ACCEPTED":
                delivery.setStatus(DeliveryStatus.ACCEPTED);
                break;
            case "REJECTED":
                delivery.setStatus(DeliveryStatus.REJECTED);
                break;
            case "PREPARING":
                delivery.setStatus(DeliveryStatus.PREPARING);
                break;
            case "GIVEN_TO_COURIER":
                delivery.setStatus(DeliveryStatus.GIVEN_TO_COURIER);
                break;
            case "ON_TRANSIT":
                delivery.setStatus(DeliveryStatus.ON_TRANSIT);
                break;
            case "DELIVERED":
                delivery.setStatus(DeliveryStatus.DELIVERED);
                break;
            default:
                return ResponseEntity.badRequest().build();
        }
        delivery.setDeliveryAddress(addr);
        Error e = new Error().errorId(UUID.randomUUID());
        e.setType(ErrorType.NONE);
        delivery.setError(e);
        delivery = deliveryService.insert(delivery);
        return ResponseEntity.ok(delivery);
    }

    @Override
    public ResponseEntity<String> deliveriesDeliveryIdStatusGet(@PathVariable UUID deliveryId, @RequestHeader String userId) {
        String accountType = usersCommunication.getAccountType(userId);
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        if(delivery == null)
            return  ResponseEntity.status(HttpStatus.NOT_FOUND).body("NOT_FOUND");
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
     * inserts an element into the repo (internal method)
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
     * Gets delivery time of Delivery entity
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId User ID for authorization (required)
     * @return gets delivery time (if user is authorized to see it)
     */
    @Override
    public ResponseEntity<OffsetDateTime> deliveriesDeliveryIdDeliveredTimeGet(UUID deliveryId, String userId) {
        try {
            if (isNullOrEmpty(userId)) return ResponseEntity.badRequest().build();
            String user = usersCommunication.getAccountType(userId);
            Delivery delivery = deliveryService.getDelivery(deliveryId);
            String cust_id = delivery.getCustomerID();
            String c_id = delivery.getCourierID();
            String r_id = delivery.getRestaurantID();
            if (isNullOrEmpty(c_id) || isNullOrEmpty(cust_id) || isNullOrEmpty(r_id) || delivery.getDeliveredTime() == null)
                return ResponseEntity.notFound().build();
            boolean isVendor = userId.equals(r_id) && user.equals("vendor");
            boolean isCustomer = userId.equals(cust_id) && user.equals("customer");
            boolean isCourier = userId.equals(c_id) && user.equals("courier");
            if (!user.equals("admin") && !isCourier && !isVendor && !isCustomer) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            } else {
                return ResponseEntity.ok(delivery.getDeliveredTime());
            }
        } catch (DeliveryService.DeliveryNotFoundException e) {
            return ResponseEntity.notFound().build();
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
            Delivery delivery = deliveryService.getDelivery(deliveryId);

            if(delivery == null)
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(List.of());

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

            if(delivery == null)
                return  ResponseEntity.status(HttpStatus.NOT_FOUND).body(List.of());

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

            if (isNullOrEmpty(restaurantEmail) || isNullOrEmpty(customerEmail)) return ResponseEntity.notFound().build();
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
            if (isNullOrEmpty(courierEmail) || isNullOrEmpty(customerEmail)) return ResponseEntity.notFound().build();
            boolean isCourier = type.equals("courier") && courierEmail.equals(userId);
            boolean isCustomer = type.equals("customer") && customerEmail.equals(userId);
            if (!type.equals("admin") && !isCourier && !isCustomer) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            } else {
                return ResponseEntity.ok(delivery.getRatingCourier());
            }
        }
    }

    /**
     * Gets all accepted orders in the system, meaning those that do not yet have a courier assigned
     * @param userId ID of the User for authorization (required)
     * @return a List of Delivery Objects
     */
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

    /**
     * Gets the courier ID of an order
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId ID of the User for authorization (required)
     * @return a String with the ID of the courier
     */
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
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User lacks necessary permissions.");
            }
            case "courier": {
                if(delivery.getCourierID() == null)
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No courier assigned to order.");
                if (delivery.getCourierID().equals(userId))
                    return ResponseEntity.ok(delivery.getCourierID());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User lacks necessary permissions.");
            }
            case "customer": {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User lacks necessary permissions.");
            }
        }

        // Account type is "non-existent"
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User lacks valid authentication credentials.");
    }

    /**
     * Updates the courier ID of an order
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId ID of the User for authorization (required)
     * @param courierId ID of the courier for updating the Delivery (required)
     * @return a Delivery Object with the updates that took place
     */
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
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            case "admin": {
                deliveryService.updateDeliveryCourier(deliveryId, courierId);
                break;
            }
            case "courier": {
                if (userId.equals(courierId) && deliveryService.getDelivery(deliveryId).getCourierID() == null){
                    deliveryService.updateDeliveryCourier(deliveryId, courierId);
                    break;
                }
                // Courier is not allowed to assign other couriers to orders or assign themselves over someone
                else
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }
            case "vendor": {
                Delivery delivery = deliveryService.getDelivery(deliveryId);
                Restaurant restaurant = deliveryService.getRestaurant(delivery.getRestaurantID());
                // Not allowed to assign couriers to different vendors
                if(!restaurant.getRestaurantID().equals(userId))
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
                // Not allowed to assign another courier
                if(delivery.getCourierID() != null)
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
                // Not allowed to assign couriers that are not in the list of own couriers (if restaurant uses it)
                if(restaurant.getCouriers() != null && !restaurant.getCouriers().contains(courierId))
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
                deliveryService.updateDeliveryCourier(deliveryId, courierId);
                break;
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
