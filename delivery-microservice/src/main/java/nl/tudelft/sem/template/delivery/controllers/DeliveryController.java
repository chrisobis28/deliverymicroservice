package nl.tudelft.sem.template.delivery.controllers;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.util.ArrayList;
import nl.tudelft.sem.template.api.DeliveriesApi;
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
        if (isNullOrEmpty(userId)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID is invalid.");
        UsersAuthenticationService.AccountType user = usersCommunication.getUserAccountType(userId);
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        boolean check = usersCommunication.checkUserAccessToDelivery(userId, delivery);
        String customer_id = delivery.getCustomerID();
        String c_id = delivery.getCourierID();
        String r_id = delivery.getRestaurantID();
        if (isNullOrEmpty(c_id) || isNullOrEmpty(customer_id) || isNullOrEmpty(r_id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unexpected event could not be found.");
        if (!check) {
            if (user.equals(UsersAuthenticationService.AccountType.INVALID))
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User lacks valid authentication credentials.");
            else throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
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
        if (isNullOrEmpty(userId)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID is invalid.");
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        boolean check = usersCommunication.checkUserAccessToDelivery(userId, delivery);
        UsersAuthenticationService.AccountType userType = usersCommunication.getUserAccountType(userId);
        String r_id = delivery.getRestaurantID();
        String c_id = delivery.getCourierID();
        if (isNullOrEmpty(r_id) || isNullOrEmpty(c_id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant ID was not found");
        if (!check && userType.equals(UsersAuthenticationService.AccountType.INVALID))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User lacks valid authentication credentials.");
        if (!check /*|| userType.equals(UsersAuthenticationService.AccountType.CLIENT)*/) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
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
        if (accountType.equals(UsersAuthenticationService.AccountType.ADMIN)) {
            return ResponseEntity.ok(delivery.getPickupTime());
        }
        if(accountType.equals(UsersAuthenticationService.AccountType.COURIER))
        {
            if(!delivery.getCourierID().equals(userId))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Courier does not correspond to the order.");
            return ResponseEntity.ok().body(delivery.getPickupTime());
        }
        if(accountType.equals(UsersAuthenticationService.AccountType.VENDOR))
        {
            if(!delivery.getRestaurantID().equals(userId))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vendor does not correspond to the order.");
            return ResponseEntity.ok(delivery.getPickupTime());
        }
        if(accountType.equals(UsersAuthenticationService.AccountType.CLIENT))
        {
            if(!delivery.getCustomerID().equals(userId))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Client does not correspond to the order.");
            return ResponseEntity.ok(delivery.getPickupTime());
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account could not be verified.");
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
        if (accountType.equals(UsersAuthenticationService.AccountType.ADMIN) ) {
            deliveryService.updatePickupTime(deliveryId,pickupTime);
            return ResponseEntity.ok(delivery);
        }
        if(accountType.equals(UsersAuthenticationService.AccountType.COURIER))
        {
            if(!delivery.getCourierID().equals(userId))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Courier does not correspond to the order.");
            deliveryService.updatePickupTime(deliveryId, pickupTime);
            return ResponseEntity.ok(delivery);
        }
        if(accountType.equals(UsersAuthenticationService.AccountType.VENDOR))
        {
            if(!delivery.getRestaurantID().equals(userId))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vendor does not correspond to the order.");
            deliveryService.updatePickupTime(deliveryId, pickupTime);
            return ResponseEntity.ok(delivery);
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account could not be verified.");
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
        OffsetDateTime now = OffsetDateTime.now();

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
        delivery.setOrderTime(now);
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Delivery is invalid.");
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
        if (isNullOrEmpty(userId) || deliveryId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID or Delivery ID is invalid.");
        Delivery d = deliveryService.getDelivery(deliveryId);
        UsersAuthenticationService.AccountType user = usersCommunication.getUserAccountType(userId);
        if (isNullOrEmpty(d.getCourierID()) || isNullOrEmpty(d.getCustomerID()) || isInvalidLatLong(d.getCurrentLocation())) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Current location could not be found.");
        switch (user) {
            case ADMIN: return ResponseEntity.ok(d.getCurrentLocation());
            case CLIENT, COURIER, VENDOR: {
                if (usersCommunication.checkUserAccessToDelivery(userId, d)) return ResponseEntity.ok(d.getCurrentLocation());
                else throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
            }
            default: throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User lacks valid authentication credentials.");
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
        if (isNullOrEmpty(userId)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID is invalid.");
        Delivery d = deliveryService.getDelivery(deliveryId);
        UsersAuthenticationService.AccountType user = usersCommunication.getUserAccountType(userId);
        if (isNullOrEmpty(d.getCustomerID()) || isNullOrEmpty(d.getCourierID()) || isNullOrEmpty(d.getRestaurantID())) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer ID could not be found");
        switch (user) {
            case ADMIN: return ResponseEntity.ok(d.getCustomerID());
            case CLIENT, COURIER, VENDOR: {
                if (usersCommunication.checkUserAccessToDelivery(userId, d)) return ResponseEntity.ok(d.getCustomerID());
                else throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
            }
            default: throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User lacks valid authentication credentials.");
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
        if (isNullOrEmpty(userId)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID is invalid.");
        Delivery d = deliveryService.getDelivery(deliveryId);
        UsersAuthenticationService.AccountType user = usersCommunication.getUserAccountType(userId);
        if (isNullOrEmpty(d.getRestaurantID()) || isNullOrEmpty(d.getCustomerID()) || isNullOrEmpty(d.getCourierID()) || d.getDeliveredTime() == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivered time could not be found.");
        switch (user) {
            case ADMIN: return ResponseEntity.ok(d.getDeliveredTime());
            case CLIENT, COURIER, VENDOR: {
                if (usersCommunication.checkUserAccessToDelivery(userId, d)) return ResponseEntity.ok(d.getDeliveredTime());
                else throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
            }
            default: throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User lacks valid authentication credentials.");
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery was not found");

        switch (accountType) {
            case ADMIN: return ResponseEntity.ok(deliveryService.getDeliveryAddress(deliveryId));
            case VENDOR, CLIENT, COURIER: {
                if (usersCommunication.checkUserAccessToDelivery(userId, delivery)) return ResponseEntity.ok(deliveryService.getDeliveryAddress(deliveryId));
                else throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
            }
            default: throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User lacks valid authentication credentials.");
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

        // Check user access based on account type and association with the delivery
        if (usersCommunication.checkUserAccessToDelivery(userId, delivery)) {
            List<Double> pickupAddress = deliveryService.getPickupLocation(deliveryId);
            return ResponseEntity.ok(pickupAddress);
        } else {
            if (type.equals(UsersAuthenticationService.AccountType.INVALID)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User lacks valid authentication credentials.");
            // User does not have access
            else throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID is invalid.");
        } else {
            Delivery delivery = deliveryService.getDelivery(deliveryId);
            //Call user endpoint that verifies the role of user path:"/account/type"
            UsersAuthenticationService.AccountType type = usersCommunication.getUserAccountType(userId);
            String email = delivery.getCustomerID();
            boolean isCustomer = userId.equals(email) && type.equals(UsersAuthenticationService.AccountType.CLIENT);
            if (!isCustomer && !type.equals(UsersAuthenticationService.AccountType.ADMIN)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID is invalid.");
        } else {
            Delivery delivery = deliveryService.getDelivery(deliveryId);
            //Call user endpoint that verifies the role of user path:"/account/type"
            UsersAuthenticationService.AccountType type = usersCommunication.getUserAccountType(userId);
            String email = delivery.getCustomerID();//orderMockRepo.getUserEmail(deliveryId);
            boolean isCustomer = userId.equals(email) && type.equals(UsersAuthenticationService.AccountType.CLIENT);
            if (!isCustomer && !type.equals(UsersAuthenticationService.AccountType.ADMIN)) {
                if (type.equals(UsersAuthenticationService.AccountType.INVALID))
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User lacks valid authentication credentials.");
                else throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
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
        if (isNullOrEmpty(userId)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID is invalid.");
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        if (isNullOrEmpty(delivery.getRestaurantID()) || isNullOrEmpty(delivery.getCustomerID())) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant rating could not be found.");
        UsersAuthenticationService.AccountType type = usersCommunication.getUserAccountType(userId);

        switch (type) {
            case COURIER: throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
            case CLIENT, VENDOR: {
                if (usersCommunication.checkUserAccessToDelivery(userId, delivery)) return ResponseEntity.ok(delivery.getRatingRestaurant());
                else throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
            }
            case ADMIN: return ResponseEntity.ok(delivery.getRatingRestaurant());
            default: throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User lacks valid authentication credentials.");
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
        if (isNullOrEmpty(userId)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID is invalid.");
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        if (isNullOrEmpty(delivery.getCourierID()) || isNullOrEmpty(delivery.getCustomerID())) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Courier rating could not be found");
        UsersAuthenticationService.AccountType type = usersCommunication.getUserAccountType(userId);

        switch (type) {
            case VENDOR: throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
            case CLIENT, COURIER: {
                if (usersCommunication.checkUserAccessToDelivery(userId, delivery)) return ResponseEntity.ok(delivery.getRatingCourier());
                else throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
            }
            case ADMIN: return ResponseEntity.ok(delivery.getRatingCourier());
            default: throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User could not be authorized.");
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
        if (isNullOrEmpty(delivery.getCourierID())) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No courier assigned to order");
        boolean check = usersCommunication.checkUserAccessToDelivery(userId, delivery);

        switch (userType) {
            case CLIENT: throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
            case ADMIN: return ResponseEntity.ok(delivery.getCourierID());
            case VENDOR, COURIER: {
                if (check) return ResponseEntity.ok(delivery.getCourierID());
                else throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
            }
            default: throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User lacks valid authentication credentials.");
        }
    }

    /**
     * Updates the courier ID of an order
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId     ID of the User for authorization (required)
     * @param courierId  ID of the courier for updating the Delivery (required)
     * @return a Delivery Object with the updates that took place
     */
    @Override
    public ResponseEntity<Delivery> deliveriesDeliveryIdCourierPut(@PathVariable UUID deliveryId,
                                                                   @RequestHeader String userId,
                                                                   @RequestBody String courierId) {
        UsersAuthenticationService.AccountType courier = usersCommunication.getUserAccountType(courierId);
        UsersAuthenticationService.AccountType userType = usersCommunication.getUserAccountType(userId);
        Delivery delivery = deliveryService.getDelivery(deliveryId);

        if (!courier.equals(UsersAuthenticationService.AccountType.COURIER))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The person you are trying to assign to the order is not a courier.");
        if (userType.equals(UsersAuthenticationService.AccountType.ADMIN)) {
            deliveryService.updateDeliveryCourier(deliveryId, courierId);
            return ResponseEntity.ok(delivery);
        }
        if (delivery.getCourierID() != null)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Delivery already has a courier assigned.");

        switch (userType) {
            case CLIENT -> throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
            case INVALID ->
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User lacks valid authentication credentials.");
            case COURIER -> {
                if (userId.equals(courierId)) {
                    deliveryService.updateDeliveryCourier(deliveryId, courierId);
                    availableDeliveryProxy.checkIfAvailable(delivery);
                    return ResponseEntity.ok(delivery);
                }
                // Courier is not allowed to assign other couriers to orders or assign themselves over someone
                else
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
            }
            case VENDOR -> {
                Restaurant restaurant = deliveryService.getRestaurant(delivery.getRestaurantID());
                // Not allowed to assign couriers to different vendors
                if (!restaurant.getRestaurantID().equals(userId))
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
                // Not allowed to assign couriers that are not in the list of own couriers (if restaurant uses it)
                if (restaurant.getCouriers() == null || !restaurant.getCouriers().contains(courierId))
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
                deliveryService.updateDeliveryCourier(deliveryId, courierId);
                return ResponseEntity.ok(delivery);
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
            case ADMIN -> {
                deliveryService.updateEstimatedPrepTime(deliveryId, body);
                return ResponseEntity.ok(deliveryService.getDelivery(deliveryId));
            }
            case CLIENT, COURIER ->
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
            case VENDOR -> {
                if (check) {
                    deliveryService.updateEstimatedPrepTime(deliveryId, body);
                    return ResponseEntity.ok(deliveryService.getDelivery(deliveryId));
                } else throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
            }
            default ->
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User lacks valid authentication credentials.");
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID is null or empty");
        }
        UsersAuthenticationService.AccountType userType = usersCommunication.getUserAccountType(userId);
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        boolean check = usersCommunication.checkUserAccessToDelivery(userId, delivery);

        switch (userType) {
            case ADMIN, CLIENT, COURIER, VENDOR -> {
                if (check) {
                    OffsetDateTime estimate = deliveryService.computeEstimatedDeliveryTime(deliveryId);
                    return ResponseEntity.ok(estimate);
                } else {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permission levels.");
                }
            }
            default -> {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized access. User cannot be authorized.");
            }
        }
    }

    /**
     * Return a specific delivery entity
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId User ID for authorization (required)
     * @return Response Entity containing Delivery
     */
    @Override
    public ResponseEntity<Delivery> deliveriesDeliveryIdGet(@PathVariable UUID deliveryId, @RequestHeader String userId)
    {
        if (isNullOrEmpty(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "BAD REQUEST");
        }
        System.out.println(userId);
        UsersAuthenticationService.AccountType userType = usersCommunication.getUserAccountType(userId);
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        // Vendors can see estimations for their orders only
        boolean allowedVendor = userType.equals(UsersAuthenticationService.AccountType.VENDOR) && userId.equals(delivery.getRestaurantID());
        // Couriers can see estimations for their deliveries only
        boolean allowedCourier = userType.equals(UsersAuthenticationService.AccountType.COURIER) && userId.equals(delivery.getCourierID());
        // Customers can see estimations for their orders only
        boolean allowedCustomer = userType.equals(UsersAuthenticationService.AccountType.CLIENT) && userId.equals(delivery.getCustomerID());
        if (userType.equals(UsersAuthenticationService.AccountType.ADMIN) ||
                allowedVendor ||
                allowedCourier ||
                allowedCustomer) {
            return ResponseEntity.ok(delivery);
        } else if (userType.equals(UsersAuthenticationService.AccountType.VENDOR) ||
                userType.equals(UsersAuthenticationService.AccountType.COURIER) ||
                userType.equals(UsersAuthenticationService.AccountType.CLIENT)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "THIS ACTION IS FORBIDDEN");
        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "YOU ARE NOT AUTHORIZED");
        }
    }

    /**
     * Returns the preparation time for a specific entity
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId User ID for authorization (required)
     * @return Response Entity containing preparation time
     */
    @Override
    public ResponseEntity<Integer> deliveriesDeliveryIdPrepGet(@PathVariable UUID deliveryId, @RequestHeader String userId)
    {
        if (isNullOrEmpty(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "BAD REQUEST");
        }
        System.out.println(userId);
        UsersAuthenticationService.AccountType userType = usersCommunication.getUserAccountType(userId);
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        // Vendors can see estimations for their orders only
        boolean allowedVendor = userType.equals(UsersAuthenticationService.AccountType.VENDOR) && userId.equals(delivery.getRestaurantID());
        // Couriers can see estimations for their deliveries only
        boolean allowedCourier = userType.equals(UsersAuthenticationService.AccountType.COURIER) && userId.equals(delivery.getCourierID());
        // Customers can see estimations for their orders only
        boolean allowedCustomer = userType.equals(UsersAuthenticationService.AccountType.CLIENT) && userId.equals(delivery.getCustomerID());
        if (userType.equals(UsersAuthenticationService.AccountType.ADMIN) ||
                allowedVendor ||
                allowedCourier ||
                allowedCustomer) {
            return ResponseEntity.ok(delivery.getEstimatedPrepTime());
        } else if (userType.equals(UsersAuthenticationService.AccountType.VENDOR) ||
                userType.equals(UsersAuthenticationService.AccountType.COURIER) ||
                userType.equals(UsersAuthenticationService.AccountType.CLIENT)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "THIS ACTION IS FORBIDDEN");

        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "YOU ARE NOT AUTHORIZED");
        }
    }

    /**
     * Updates the delivery address of an order
     *
     * @param deliveryId  ID of the Delivery entity (required)
     * @param userId      ID of the User for authorization (required)
     * @param requestBody new address to be used in the future (required)
     * @return a Delivery Object with the updates that took place
     */
    @PutMapping("/deliveries/{deliveryId}/delivery-address")
    @Override
    public ResponseEntity<Delivery> deliveriesDeliveryIdDeliveryAddressPut(@PathVariable UUID deliveryId, @RequestHeader String userId, @RequestBody List<Double> requestBody) {
        UsersAuthenticationService.AccountType userType = usersCommunication.getUserAccountType(userId);
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        if (requestBody == null || requestBody.size() != 2)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Delivery Address not set correctly.");
        switch (userType) {
            case CLIENT, ADMIN -> {
                if (userType.equals(UsersAuthenticationService.AccountType.CLIENT) && !delivery.getCustomerID().equals(userId)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customer does not correspond to the order.");
                }
                deliveryService.updateDeliveryAddress(deliveryId, new ArrayList<>(requestBody));
                return ResponseEntity.ok(deliveryService.getDelivery(deliveryId));
            }
            case COURIER, VENDOR -> throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only customers can update the delivery address.");
            default -> throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account could not be verified.");
        }
    }

}

