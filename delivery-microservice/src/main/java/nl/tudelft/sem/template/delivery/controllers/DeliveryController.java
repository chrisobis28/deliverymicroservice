package nl.tudelft.sem.template.delivery.controllers;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import nl.tudelft.sem.template.api.DeliveriesApi;
//import nl.tudelft.sem.template.delivery.communication.UsersCommunication;
import nl.tudelft.sem.template.delivery.AvailableDeliveryProxy;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.delivery.services.RestaurantService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.model.Error;
import nl.tudelft.sem.template.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class DeliveryController implements DeliveriesApi {

    private final DeliveryService deliveryService;

    private final UsersAuthenticationService usersCommunication;
    private final DeliveryStatusHandler deliveryStatusHandler;

    private final AvailableDeliveryProxy availableDeliveryProxy;

    /**
     * Constructor
     *
     * @param deliveryService       the delivery service
     * @param usersCommunication    mock for users authorization
     * @param deliveryStatusHandler Handles the status of Delivery entities
     */
    public DeliveryController(DeliveryService deliveryService, UsersAuthenticationService usersCommunication, DeliveryStatusHandler deliveryStatusHandler) {
        this.deliveryService = deliveryService;
        this.usersCommunication = usersCommunication;
        this.deliveryStatusHandler = deliveryStatusHandler;
        this.availableDeliveryProxy = new AvailableDeliveryProxy(deliveryService);
    }

    /**
     * Get an error associated with a delivery
     * (don't check for null error bc error is initialized along with Delivery)
     * @param deliveryId ID of the Delivery entity (required) id of Delivery entity
     * @param userId User ID for authorization (required) user ID
     * @return ResponseEntity containing error (if user is authorized)
     */
    @Override
    public ResponseEntity<Error> deliveriesDeliveryIdUnexpectedEventGet(@PathVariable("deliveryId") UUID deliveryId, @RequestHeader String userId) {
        if (isNullOrEmpty(userId)) return ResponseEntity.badRequest().build();
        UsersAuthenticationService.AccountType user = usersCommunication.getUserAccountType(userId);
        System.out.println(user);
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        boolean check = usersCommunication.checkUserAccessToDelivery(userId, delivery);
        System.out.println(check);
        String customer_id = delivery.getCustomerID();
        String c_id = delivery.getCourierID();
        String r_id = delivery.getRestaurantID();
        if (isNullOrEmpty(c_id) || isNullOrEmpty(customer_id) || isNullOrEmpty(r_id))
            return ResponseEntity.notFound().build();
        if (!check) {
            if (user.equals(UsersAuthenticationService.AccountType.INVALID)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            else return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        else return ResponseEntity.ok(delivery.getError());
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
        boolean check = usersCommunication.checkUserAccessToDelivery(userId, delivery);
        UsersAuthenticationService.AccountType userType = usersCommunication.getUserAccountType(userId);
        String r_id = delivery.getRestaurantID();
        String c_id = delivery.getCourierID();
        if (isNullOrEmpty(r_id) || isNullOrEmpty(c_id)) return ResponseEntity.notFound().build();
        if (!check && userType.equals(UsersAuthenticationService.AccountType.INVALID)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!check /*|| userType.equals(UsersAuthenticationService.AccountType.CLIENT)*/) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        else return ResponseEntity.ok(r_id);
    }

    /**
     * The get method for getting the pickup time of a delivery
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId User ID for authorization (required)
     * @return pickup time for a delivery
     */
    @Override
    public ResponseEntity<OffsetDateTime> deliveriesDeliveryIdPickupGet(@PathVariable UUID deliveryId, @RequestHeader String userId) {
        UsersAuthenticationService.AccountType accountType = usersCommunication.getUserAccountType(userId);
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        if(delivery == null)
            return  ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        if (accountType.equals(UsersAuthenticationService.AccountType.ADMIN)) {
            return ResponseEntity.ok(delivery.getPickupTime());
        }
        if(accountType.equals(UsersAuthenticationService.AccountType.COURIER))
        {
            if(!delivery.getCourierID().equals(userId))
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            return ResponseEntity.ok(delivery.getPickupTime());
        }
        if(accountType.equals(UsersAuthenticationService.AccountType.VENDOR))
        {
            if(!delivery.getRestaurantID().equals(userId))
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            return ResponseEntity.ok(delivery.getPickupTime());
        }
        if(accountType.equals(UsersAuthenticationService.AccountType.CLIENT))
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
     * @return updated pickup time of delivery
     */
    @Override
    public ResponseEntity<Delivery> deliveriesDeliveryIdPickupPut(@PathVariable UUID deliveryId, @RequestHeader String userId,@RequestBody OffsetDateTime pickupTime) {
        UsersAuthenticationService.AccountType accountType = usersCommunication.getUserAccountType(userId);
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        if(delivery == null)
            return  ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        if (accountType.equals(UsersAuthenticationService.AccountType.ADMIN) ) {
            deliveryService.updatePickupTime(deliveryId,pickupTime);
            return ResponseEntity.ok(delivery);
        }
        if(accountType.equals(UsersAuthenticationService.AccountType.COURIER))
        {
            if(!delivery.getCourierID().equals(userId))
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            deliveryService.updatePickupTime(deliveryId, pickupTime);
            return ResponseEntity.ok(delivery);
        }
        if(accountType.equals(UsersAuthenticationService.AccountType.VENDOR))
        {
            if(!delivery.getRestaurantID().equals(userId))
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            deliveryService.updatePickupTime(deliveryId, pickupTime);
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "BAD REQUEST");
        }

        Delivery delivery = new Delivery();
        String status = deliveriesPostRequest.getStatus().toUpperCase();
        String orderId = deliveriesPostRequest.getOrderId();
        String customerId = deliveriesPostRequest.getCustomerId();
        String vendorId = deliveriesPostRequest.getVendorId();
        List<Double> addr = deliveriesPostRequest.getDeliveryAddress();

        delivery.setStatus(DeliveryStatus.fromValue(status));
        if (isNullOrEmpty(orderId) || isNullOrEmpty(customerId) || isNullOrEmpty(vendorId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "BAD REQUEST");
        }

        if (addr == null || addr.size() != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "BAD REQUEST");
        }

        Restaurant r;
        try{
            r = deliveryService.getRestaurant(vendorId);
        }
        catch(RestaurantService.RestaurantNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "VENDOR NOT FOUND.");
        }

        if(deliveryService.computeHaversine(r.getLocation().get(0), r.getLocation().get(1), addr.get(0), addr.get(1)) > r.getDeliveryZone()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CUSTOMER OUTSIDE THE VENDOR DELIVERY ZONE.");
        }
        UUID deliveryId = UUID.fromString(orderId);
        delivery.deliveryID(deliveryId);
        delivery.setCustomerID(customerId);
        delivery.setRestaurantID(vendorId);
        delivery.setDeliveryAddress(addr);
        delivery.setError(null);
        delivery = deliveryService.insert(delivery);
        return ResponseEntity.ok(delivery);
    }


    @Override
    public ResponseEntity<String> deliveriesDeliveryIdStatusGet(@PathVariable UUID deliveryId, @RequestHeader String userId) {
        return deliveryStatusHandler.getDeliveryStatus(deliveryId, userId);
    }

    @Override
    public ResponseEntity<Delivery> deliveriesDeliveryIdStatusPut(UUID deliveryId, String userId, String status) {
        ResponseEntity<Delivery> delivery = deliveryStatusHandler.updateDeliveryStatus(deliveryId, userId, status);
        if (delivery.getBody() != null) availableDeliveryProxy.checkIfAvailable(delivery.getBody());
        return delivery;
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
     * Get current location (only customer and admin)
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId User ID for authorization (required)
     * @return list of doubles showing address (empty for now, will make adapter design pattern)
     */
    @Override
    public ResponseEntity<List<Double>> deliveriesDeliveryIdCurrentLocationGet(UUID deliveryId, String userId) {
        if (isNullOrEmpty(userId) || deliveryId == null) return ResponseEntity.badRequest().build();
        Delivery d = deliveryService.getDelivery(deliveryId);
        UsersAuthenticationService.AccountType user = usersCommunication.getUserAccountType(userId);
        if (isNullOrEmpty(d.getCourierID()) || isNullOrEmpty(d.getCustomerID()) || isInvalidLatLong(d.getCurrentLocation())) return ResponseEntity.notFound().build();
        switch (user) {
            case ADMIN: return ResponseEntity.ok(d.getCurrentLocation());
            case CLIENT, COURIER, VENDOR: {
                if (usersCommunication.checkUserAccessToDelivery(userId, d)) return ResponseEntity.ok(d.getCurrentLocation());
                else return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            default: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Method to check if coordinates are valid
     * @param coords coordinates to check
     * @return boolean value indicating whether coordinates are valid
     */
    public boolean isInvalidLatLong(List<Double> coords) {
        return coords == null || coords.size() != 2;
    }

    /**
     * Get customer that ordered the Delivery
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId User ID for authorization (required)
     * @return customer id if the user is authorized to see it
     */
    @Override
    public ResponseEntity<String> deliveriesDeliveryIdCustomerGet(UUID deliveryId, String userId) {
        if (isNullOrEmpty(userId)) return ResponseEntity.badRequest().build();
        Delivery d = deliveryService.getDelivery(deliveryId);
        UsersAuthenticationService.AccountType user = usersCommunication.getUserAccountType(userId);
        if (isNullOrEmpty(d.getCustomerID()) || isNullOrEmpty(d.getCourierID()) || isNullOrEmpty(d.getRestaurantID())) return ResponseEntity.notFound().build();
        switch (user) {
            case ADMIN: return ResponseEntity.ok(d.getCustomerID());
            case CLIENT, COURIER, VENDOR: {
                if (usersCommunication.checkUserAccessToDelivery(userId, d)) return ResponseEntity.ok(d.getCustomerID());
                else return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            default: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
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
        if (isNullOrEmpty(userId)) return ResponseEntity.badRequest().build();
        Delivery d = deliveryService.getDelivery(deliveryId);
        UsersAuthenticationService.AccountType user = usersCommunication.getUserAccountType(userId);
        if (isNullOrEmpty(d.getRestaurantID()) || isNullOrEmpty(d.getCustomerID()) || isNullOrEmpty(d.getCourierID()) || d.getDeliveredTime() == null) return ResponseEntity.notFound().build();
        switch (user) {
            case ADMIN: return ResponseEntity.ok(d.getDeliveredTime());
            case CLIENT, COURIER, VENDOR: {
                if (usersCommunication.checkUserAccessToDelivery(userId, d)) return ResponseEntity.ok(d.getDeliveredTime());
                else return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            default: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
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
        UsersAuthenticationService.AccountType accountType = usersCommunication.getUserAccountType(userId);
        Delivery delivery = deliveryService.getDelivery(deliveryId);

        if(delivery == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(List.of());

        switch (accountType) {
            case ADMIN: return ResponseEntity.ok(deliveryService.getDeliveryAddress(deliveryId));
            case VENDOR, CLIENT, COURIER: {
                if (usersCommunication.checkUserAccessToDelivery(userId, delivery)) return ResponseEntity.ok(deliveryService.getDeliveryAddress(deliveryId));
                else return ResponseEntity.status(HttpStatus.FORBIDDEN).body(List.of());
            }
            default: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(List.of());
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
        //try {
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        UsersAuthenticationService.AccountType type = usersCommunication.getUserAccountType(userId);

        if (delivery == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(List.of());

        // Check user access based on account type and association with the delivery
        if (usersCommunication.checkUserAccessToDelivery(userId, delivery)) {
            List<Double> pickupAddress = deliveryService.getPickupLocation(deliveryId);
            return ResponseEntity.ok(pickupAddress);
        } else {
            if (type.equals(UsersAuthenticationService.AccountType.INVALID)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(List.of());
            // User does not have access
            else return ResponseEntity.status(HttpStatus.FORBIDDEN).body(List.of());
        }
    }

    /**
     * Checks if a string is null or empty
     * @param str string to check
     * @return boolean value indicating whether string is empty or not
     */
    public boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty() || str.isBlank();
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
            UsersAuthenticationService.AccountType type = usersCommunication.getUserAccountType(userId);
            String email = delivery.getCustomerID();
            boolean isCustomer = userId.equals(email) && type.equals(UsersAuthenticationService.AccountType.CLIENT);
            if (!isCustomer && !type.equals(UsersAuthenticationService.AccountType.ADMIN)) {
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
            UsersAuthenticationService.AccountType type = usersCommunication.getUserAccountType(userId);
            String email = delivery.getCustomerID();//orderMockRepo.getUserEmail(deliveryId);
            boolean isCustomer = userId.equals(email) && type.equals(UsersAuthenticationService.AccountType.CLIENT);
            if (!isCustomer && !type.equals(UsersAuthenticationService.AccountType.ADMIN)) {
                if (type.equals(UsersAuthenticationService.AccountType.INVALID)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                else return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
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
    public ResponseEntity<Integer> deliveriesDeliveryIdRatingRestaurantGet(@Parameter(name = "deliveryId", description = "ID of the Delivery entity", required = true, in = ParameterIn.PATH) @PathVariable("deliveryId") UUID deliveryId, @Parameter(name = "userId", description = "User ID for authorization", required = true, in = ParameterIn.HEADER) @RequestHeader @NotNull String userId) {
        //Only people that can see rating is the customer who left the rating, the vendor and the admin
        if (isNullOrEmpty(userId)) return ResponseEntity.badRequest().build();
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        if (isNullOrEmpty(delivery.getRestaurantID()) || isNullOrEmpty(delivery.getCustomerID())) return ResponseEntity.notFound().build();
        UsersAuthenticationService.AccountType type = usersCommunication.getUserAccountType(userId);

        switch (type) {
            case COURIER: return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            case CLIENT, VENDOR: {
                if (usersCommunication.checkUserAccessToDelivery(userId, delivery)) return ResponseEntity.ok(delivery.getRatingRestaurant());
                else return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            case ADMIN: return ResponseEntity.ok(delivery.getRatingRestaurant());
            default: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Override
    @RequestMapping(
        method = {RequestMethod.GET},
        value = {"/{deliveryId}/rating-courier"},
        produces = {"application/json"}
    )
    public ResponseEntity<Integer> deliveriesDeliveryIdRatingCourierGet(@Parameter(name = "deliveryId", description = "ID of the Delivery entity", required = true, in = ParameterIn.PATH) @PathVariable("deliveryId") UUID deliveryId, @Parameter(name = "userId", description = "User ID for authorization", required = true, in = ParameterIn.HEADER) @RequestHeader @NotNull String userId) {
        //Only people that can see rating is the customer who left the rating, the courier and the admin
        if (isNullOrEmpty(userId)) return ResponseEntity.badRequest().build();
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        if (isNullOrEmpty(delivery.getCourierID()) || isNullOrEmpty(delivery.getCustomerID())) return ResponseEntity.notFound().build();
        UsersAuthenticationService.AccountType type = usersCommunication.getUserAccountType(userId);

        switch (type) {
            case VENDOR: return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            case CLIENT, COURIER: {
                if (usersCommunication.checkUserAccessToDelivery(userId, delivery)) return ResponseEntity.ok(delivery.getRatingCourier());
                else return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            case ADMIN: return ResponseEntity.ok(delivery.getRatingCourier());
            default: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
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
        UsersAuthenticationService.AccountType accountType = usersCommunication.getUserAccountType(userId);

        return switch(accountType) {
            case ADMIN, COURIER -> ResponseEntity.ok(deliveryService.getAcceptedDeliveries());
            case VENDOR, CLIENT -> ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            default -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        };
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
        UsersAuthenticationService.AccountType userType = usersCommunication.getUserAccountType(userId);
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        if (isNullOrEmpty(delivery.getCourierID())) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No courier assigned to order.");
        boolean check = usersCommunication.checkUserAccessToDelivery(userId, delivery);

        switch (userType) {
            case CLIENT: return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User lacks necessary permissions.");
            case ADMIN: return ResponseEntity.ok(delivery.getCourierID());
            case VENDOR, COURIER: {
                if (check) return ResponseEntity.ok(delivery.getCourierID());
                else return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User lacks necessary permissions.");
            }
            default: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User lacks valid authentication credentials.");
        }
    }

    public UUID getNextDelivery() {
        return availableDeliveryProxy.getAvailableDeliveries().poll();
    }

    /**
     * Updates the courier ID of an order
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId     ID of the User for authorization (required)
     * @param courierId  ID of the courier for updating the Delivery (required)
     * @return a Delivery Object with the updates that took place
     */
    @PostMapping("/deliveries/{deliveryId}/courier")
    @Override
    public ResponseEntity<Delivery> deliveriesDeliveryIdCourierPut(@PathVariable UUID deliveryId,
                                                                   @RequestHeader String userId,
                                                                   @RequestBody String courierId) {
        UsersAuthenticationService.AccountType courier = usersCommunication.getUserAccountType(courierId);
        UsersAuthenticationService.AccountType userType = usersCommunication.getUserAccountType(userId);
        Delivery delivery = deliveryService.getDelivery(deliveryId);

        if (!courier.equals(UsersAuthenticationService.AccountType.COURIER))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        if (delivery.getCourierID() != null)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);

        switch (userType) {
            case CLIENT:
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            case INVALID:
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            case ADMIN: {
                deliveryService.updateDeliveryCourier(deliveryId, courierId);
                break;
            }
            case COURIER: {
                if (userId.equals(courierId)){
                    deliveryService.updateDeliveryCourier(deliveryId, courierId);
                    availableDeliveryProxy.checkIfAvailable(delivery);
                    break;
                }
                // Courier is not allowed to assign other couriers to orders or assign themselves over someone
                else
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }
            case VENDOR: {
                Restaurant restaurant = deliveryService.getRestaurant(delivery.getRestaurantID());
                // Not allowed to assign couriers to different vendors
                if (!restaurant.getRestaurantID().equals(userId))
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
                // Not allowed to assign couriers that are not in the list of own couriers (if restaurant uses it)
                if (restaurant.getCouriers() == null || !restaurant.getCouriers().contains(courierId))
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
                deliveryService.updateDeliveryCourier(deliveryId, courierId);
                break;
            }
        }
        return ResponseEntity.ok(delivery);
    }

    /**
     * Updates the estimated preparation time of an order
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId     User ID for authorization (required)
     * @param body       Update prep time of delivery (required)
     * @return a delivery object with the updates that took place
     */
    @PutMapping("/deliveries/{deliveryId}/prep")
    @Override
    public ResponseEntity<Delivery> deliveriesDeliveryIdPrepPut(@PathVariable UUID deliveryId, @RequestHeader String userId, @RequestBody Integer body) {
        UsersAuthenticationService.AccountType userType = usersCommunication.getUserAccountType(userId);
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        boolean check = usersCommunication.checkUserAccessToDelivery(userId, delivery);

        switch (userType) {
            case ADMIN: {
                deliveryService.updateEstimatedPrepTime(deliveryId, body);
                return ResponseEntity.ok(deliveryService.getDelivery(deliveryId));
            }
            case CLIENT, COURIER: return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            case VENDOR: {
                if (check) {
                    deliveryService.updateEstimatedPrepTime(deliveryId, body);
                    return ResponseEntity.ok(deliveryService.getDelivery(deliveryId));
                }
                else return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            default: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Computes an estimation for when an order will be delivered
     *
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId     User ID for authorization (required)
     * @return an estimated delivery time
     */
    @Override
    public ResponseEntity<OffsetDateTime> deliveriesDeliveryIdEstimatedDeliveryTimeGet(@PathVariable UUID deliveryId, @RequestHeader String userId) {
        if (isNullOrEmpty(userId)) {
            return ResponseEntity.badRequest().build();
        }
        UsersAuthenticationService.AccountType userType = usersCommunication.getUserAccountType(userId);
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        boolean check = usersCommunication.checkUserAccessToDelivery(userId, delivery);

        switch (userType) {
            case ADMIN: {
                OffsetDateTime now = OffsetDateTime.now();
                OffsetDateTime estimate = deliveryService.computeEstimatedDeliveryTime(deliveryId, now);
                return ResponseEntity.ok(estimate);
            }
            case CLIENT, COURIER, VENDOR: {
                if (check) {
                    OffsetDateTime now = OffsetDateTime.now();
                    OffsetDateTime estimate = deliveryService.computeEstimatedDeliveryTime(deliveryId, now);
                    return ResponseEntity.ok(estimate);
                }
                else return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            default: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
//        // Vendors can see estimations for their orders only
//        boolean allowedVendor = userType.equals(UsersAuthenticationService.AccountType.VENDOR) && userId.equals(delivery.getRestaurantID());
//        // Couriers can see estimations for their deliveries only
//        boolean allowedCourier = userType.equals(UsersAuthenticationService.AccountType.COURIER) && userId.equals(delivery.getCourierID());
//        // Customers can see estimations for their orders only
//        boolean allowedCustomer = userType.equals(UsersAuthenticationService.AccountType.CLIENT) && userId.equals(delivery.getCustomerID());
//        if (userType.equals(UsersAuthenticationService.AccountType.ADMIN) ||
//                allowedVendor ||
//                allowedCourier ||
//                allowedCustomer) {
//            OffsetDateTime now = OffsetDateTime.now();
//            OffsetDateTime estimate = deliveryService.computeEstimatedDeliveryTime(deliveryId, now);
//            return ResponseEntity.ok(estimate);
//        } else if (userType.equals(UsersAuthenticationService.AccountType.VENDOR) ||
//                userType.equals(UsersAuthenticationService.AccountType.COURIER) ||
//                userType.equals(UsersAuthenticationService.AccountType.CLIENT)) {
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
//        } else {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
//        }
    }
}
