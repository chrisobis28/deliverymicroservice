package nl.tudelft.sem.template.delivery.controllers;

import io.swagger.v3.oas.annotations.Parameter;
import nl.tudelft.sem.template.api.DeliveriesApi;
import nl.tudelft.sem.template.delivery.AvailableDeliveryProxy;
import nl.tudelft.sem.template.delivery.domain.ErrorRepository;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.delivery.services.ErrorService;
import nl.tudelft.sem.template.delivery.services.RestaurantService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService.*;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.model.*;
import nl.tudelft.sem.template.model.Error;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;


@RestController
public class DeliveryController implements DeliveriesApi {

    private final transient DeliveryService deliveryService;
    private final transient ErrorService errorService;
    private final transient UsersAuthenticationService usersAuthenticationService;
    private final transient DeliveryStatusHandler deliveryStatusHandler;
    private final transient AvailableDeliveryProxy availableDeliveryProxy;

    /**
     * Constructor.
     *
     * @param deliveryService       the delivery service
     * @param usersAuthenticationService    mock for users authorization
     * @param deliveryStatusHandler Handles the status of Delivery entities
     */
    public DeliveryController(DeliveryService deliveryService,
                              ErrorService errorService,
                              UsersAuthenticationService usersAuthenticationService,
                              DeliveryStatusHandler deliveryStatusHandler,
                              AvailableDeliveryProxy availableDeliveryProxy) {
        this.deliveryService = deliveryService;
        this.errorService = errorService;
        this.usersAuthenticationService = usersAuthenticationService;
        this.deliveryStatusHandler = deliveryStatusHandler;
        this.availableDeliveryProxy = availableDeliveryProxy;
    }

    /**
     * Authenticates user for a delivery associated with the given deliveryId.
     * If user with the given userId doesn't exist UNAUTHORIZED is thrown.
     * If the user lacks necessary permissions FORBIDDEN is thrown.
     * Otherwise, the delivery is returned.
     *
     * @return Delivery object that given user is authenticated for
     * @throws InvalidUserException      if user with the given userId doesn't exist
     * @throws NotAuthenticatedException if the user lacks necessary permissions
     */
    public Delivery getDeliveryAndAuthenticateUser(UUID deliveryId, String userId) {

        UsersAuthenticationService.AccountType accountType = usersAuthenticationService.getUserAccountType(userId);
        if (AccountType.INVALID.equals(accountType)) {
            throw new InvalidUserException();
        }

        Delivery delivery = deliveryService.getDelivery(deliveryId);
        boolean userIsAuthenticated = usersAuthenticationService.checkUserAccessToDelivery(userId, delivery);
        if (!userIsAuthenticated) {
            throw new NotAuthenticatedException();
        }

        return delivery;
    }

    /**
     * Get an error associated with a delivery.
     * (don't check for null error bc error is initialized along with Delivery)
     *
     * @param deliveryId ID of the Delivery entity (required) id of Delivery entity
     * @param userId     User ID for authorization (required) user ID
     * @return ResponseEntity containing error (if user is authorized)
     */
    @Override
    public ResponseEntity<Error> deliveriesDeliveryIdUnexpectedEventGet(@PathVariable UUID deliveryId,
                                                                        @RequestHeader String userId) {
        Delivery delivery = getDeliveryAndAuthenticateUser(deliveryId, userId);
        return ResponseEntity.ok(delivery.getError());
    }

    /**
     * Get the id of the restaurant making the delivery.
     * - access granted only for vendor of restaurant making delivery, courier making delivery and admin
     *
     * @param deliveryId ID of the Delivery entity (required) - checking if Delivery entity exists
     * @param userId     User ID for authorization (required) - ID of user making the request
     * @return ResponseEntity containing restaurant id (if user is authorized)
     */
    @Override
    public ResponseEntity<String> deliveriesDeliveryIdRestaurantGet(UUID deliveryId, String userId) {

        Delivery delivery = getDeliveryAndAuthenticateUser(deliveryId, userId);
        return ResponseEntity.ok(delivery.getRestaurantID());
    }

    /**
     * The get method for getting the pickup time of a delivery.
     *
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId     User ID for authorization (required)
     * @return pickup time for a delivery
     */
    @Override
    public ResponseEntity<OffsetDateTime> deliveriesDeliveryIdPickupGet(@PathVariable UUID deliveryId,
                                                                        @RequestHeader String userId) {
        Delivery delivery = getDeliveryAndAuthenticateUser(deliveryId, userId);
        return ResponseEntity.ok(delivery.getPickupTime());
    }

    /**
     * The put method for updating the pickup time of a delivery.
     *
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId     User ID for authorization (required)
     * @param pickupTime Update pick up time of delivery (required)
     * @return updated pickup time of delivery
     */
    @Override
    public ResponseEntity<Delivery> deliveriesDeliveryIdPickupPut(@PathVariable UUID deliveryId,
                                                                  @RequestHeader String userId,
                                                                  @RequestBody OffsetDateTime pickupTime) {

        Delivery delivery = getDeliveryAndAuthenticateUser(deliveryId, userId);
        UsersAuthenticationService.AccountType accountType = usersAuthenticationService.getUserAccountType(userId);
        if (UsersAuthenticationService.AccountType.CLIENT.equals(accountType)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customers cannot update delivery pickup time");
        }

        deliveryService.updatePickupTime(deliveryId, pickupTime);
        return ResponseEntity.ok(delivery);
    }

    /**
     * Add Delivery endpoint.
     *
     * @param deliveriesPostRequest Request body for creating a new Delivery entity (optional)
     * @return Delivery entity that was just added to the database
     */
    @Override
    public ResponseEntity<Delivery> deliveriesPost(@Valid DeliveriesPostRequest deliveriesPostRequest) {
        if (deliveriesPostRequest == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body cannot be null");
        }

        String statusString = deliveriesPostRequest.getStatus().toUpperCase(Locale.ROOT);
        DeliveryStatus status = DeliveryStatus.fromValue(statusString);
        String orderId = deliveriesPostRequest.getOrderId();
        String customerId = deliveriesPostRequest.getCustomerId();
        String vendorId = deliveriesPostRequest.getVendorId();
        List<Double> address = deliveriesPostRequest.getDeliveryAddress();

        if (isNullOrEmpty(orderId) || isNullOrEmpty(customerId) || isNullOrEmpty(vendorId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Restaurant ID, customer ID or Delivery ID is invalid.");
        }

        if (address == null || address.size() != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Address is invalid.");
        }

        Restaurant r;
        try {
            r = deliveryService.getRestaurant(vendorId);
        } catch (RestaurantService.RestaurantNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "VENDOR NOT FOUND.");
        }

        if (deliveryService.computeHaversine(r.getLocation().get(0),
                r.getLocation().get(1), address.get(0), address.get(1)) > r.getDeliveryZone()) {
            status = DeliveryStatus.REJECTED;
        }

        UUID deliveryId = UUID.fromString(orderId);

        Error error = new Error()
                .errorId(deliveryId)
                .type(ErrorType.NONE);

        Delivery delivery = new Delivery()
                .deliveryID(deliveryId)
                .customerID(customerId)
                .restaurantID(vendorId)
                .deliveryAddress(address)
                .status(status)
                .orderTime(OffsetDateTime.now())
                .error(error)
                .currentLocation(r.getLocation());


        if(delivery.getStatus().equals(DeliveryStatus.DELIVERED)) {
            delivery.setDeliveredTime(OffsetDateTime.now());
        }

        deliveryService.insert(delivery);

        availableDeliveryProxy.insertDelivery(delivery);

        return ResponseEntity.ok(delivery);
    }


    @Override
    public ResponseEntity<String> deliveriesDeliveryIdStatusGet(@PathVariable UUID deliveryId,
                                                                @RequestHeader String userId) {
        Delivery delivery = getDeliveryAndAuthenticateUser(deliveryId, userId);
        return ResponseEntity.ok(delivery.getStatus().getValue());
    }

    @Override
    public ResponseEntity<Delivery> deliveriesDeliveryIdStatusPut(UUID deliveryId, String userId, String status) {

        getDeliveryAndAuthenticateUser(deliveryId, userId);
        Delivery delivery = deliveryStatusHandler.updateDeliveryStatus(deliveryId, userId, status);
        availableDeliveryProxy.insertDelivery(delivery);
        return ResponseEntity.ok(delivery);
    }

    /**
     * Get current location (only customer and admin).
     *
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId     User ID for authorization (required)
     * @return list of doubles showing address (empty for now, will make adapter design pattern)
     */
    @Override
    public ResponseEntity<List<Double>> deliveriesDeliveryIdCurrentLocationGet(UUID deliveryId, String userId) {

        Delivery delivery = getDeliveryAndAuthenticateUser(deliveryId, userId);
        return ResponseEntity.ok(delivery.getCurrentLocation());
    }


    /**
     * Get customer that ordered the Delivery.
     *
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId     User ID for authorization (required)
     * @return customer id if the user is authorized to see it
     */
    @Override
    public ResponseEntity<String> deliveriesDeliveryIdCustomerGet(UUID deliveryId, String userId) {

        Delivery delivery = getDeliveryAndAuthenticateUser(deliveryId, userId);
        return ResponseEntity.ok(delivery.getCustomerID());
    }

    /**
     * Gets delivery time of Delivery entity.
     *
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId     User ID for authorization (required)
     * @return gets delivery time (if user is authorized to see it)
     */
    @Override
    public ResponseEntity<OffsetDateTime> deliveriesDeliveryIdDeliveredTimeGet(UUID deliveryId, String userId) {

        Delivery delivery = getDeliveryAndAuthenticateUser(deliveryId, userId);
        return ResponseEntity.ok(delivery.getDeliveredTime());
    }

    /**
     * Returns the delivery address.
     *
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId     User ID for authorization (required)
     * @return delivery address
     */
    @Override
    public ResponseEntity<List<Double>> deliveriesDeliveryIdDeliveryAddressGet(@PathVariable UUID deliveryId,
                                                                               @RequestHeader String userId) {
        Delivery delivery = getDeliveryAndAuthenticateUser(deliveryId, userId);
        return ResponseEntity.ok(delivery.getDeliveryAddress());
    }

    /**
     * Returns the pickup location.
     *
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId     User ID for authorization (required)
     * @return the pickup location
     */
    @Override
    public ResponseEntity<List<Double>> deliveriesDeliveryIdPickupLocationGet(@PathVariable UUID deliveryId,
                                                                              @RequestHeader String userId) {
        getDeliveryAndAuthenticateUser(deliveryId, userId);
        List<Double> pickupAddress = deliveryService.getPickupLocation(deliveryId);
        return ResponseEntity.ok(pickupAddress);
    }

    /**
     * Checks if a string is null or empty.
     *
     * @param str string to check
     * @return boolean value indicating whether string is empty or not
     */
    public boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty() || str.isBlank();
    }

    /**
     * Allows the customer to update a courier's rating.
     *
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId     User ID for authorization (required)
     * @param rating       Update rating of delivery (required)
     * @return Response entity containing the updated Delivery object
     */
    @Override
    public ResponseEntity<Delivery> deliveriesDeliveryIdRatingCourierPut(@PathVariable UUID deliveryId,
                                                                         @RequestHeader @NotNull String userId,
                                                                         @RequestBody @Valid Integer rating) {

        Delivery delivery = getDeliveryAndAuthenticateUser(deliveryId, userId);
        AccountType accountType = usersAuthenticationService.getUserAccountType(userId);
        if (!List.of(AccountType.ADMIN, AccountType.CLIENT).contains(accountType)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only customer can give a rating to courier");
        }

        deliveryService.updateCourierRating(deliveryId, rating);
        return ResponseEntity.ok(delivery);
    }

    @Override
    public ResponseEntity<Delivery> deliveriesDeliveryIdRatingRestaurantPut(@PathVariable UUID deliveryId,
                                                                            @RequestHeader @NotNull String userId,
                                                                            @RequestBody @Valid Integer rating) {

        Delivery delivery = getDeliveryAndAuthenticateUser(deliveryId, userId);
        AccountType accountType = usersAuthenticationService.getUserAccountType(userId);
        if (!List.of(AccountType.ADMIN, AccountType.CLIENT).contains(accountType)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only customer can give a rating to a restaurant");
        }

        deliveryService.updateRestaurantRating(deliveryId, rating);
        return ResponseEntity.ok(delivery);
    }

    @Override
    public ResponseEntity<Integer> deliveriesDeliveryIdRatingRestaurantGet(@PathVariable UUID deliveryId,
                                                                           @RequestHeader @NotNull String userId) {
        Delivery delivery = getDeliveryAndAuthenticateUser(deliveryId, userId);
        if (usersAuthenticationService.getUserAccountType(userId).equals(AccountType.COURIER)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Couriers are not allowed to see restaurant rating");
        }
        return ResponseEntity.ok(delivery.getRatingRestaurant());
    }

    @Override
    public ResponseEntity<Integer> deliveriesDeliveryIdRatingCourierGet(@Parameter @PathVariable UUID deliveryId,
                                                                        @Parameter @RequestHeader @NotNull String userId) {
        Delivery delivery = getDeliveryAndAuthenticateUser(deliveryId, userId);
        if (usersAuthenticationService.getUserAccountType(userId).equals(AccountType.VENDOR)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vendors are not allowed to see couriers rating");
        }
        return ResponseEntity.ok(delivery.getRatingRestaurant());
    }

    /**
     * Gets all accepted orders in the system, meaning those that do not yet have a courier assigned.
     *
     * @param userId ID of the User for authorization (required)
     * @return a List of Delivery Objects
     */
    @Override
    public ResponseEntity<List<Delivery>> deliveriesAllAcceptedGet(@RequestHeader String userId) {
        UsersAuthenticationService.AccountType accountType = usersAuthenticationService.getUserAccountType(userId);

        return switch (accountType) {
            case ADMIN, COURIER -> ResponseEntity.ok(deliveryService.getAcceptedDeliveries());
            case VENDOR, CLIENT -> throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "User lacks necessary permissions.");
            default -> throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "User lacks valid authentication credentials.");
        };
    }

    /**
     * Gets the courier ID of an order.
     *
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId     ID of the User for authorization (required)
     * @return a String with the ID of the courier
     */
    @Override
    public ResponseEntity<String> deliveriesDeliveryIdCourierGet(@PathVariable UUID deliveryId,
                                                                 @RequestHeader String userId) {
        Delivery delivery = getDeliveryAndAuthenticateUser(deliveryId, userId);
        return ResponseEntity.ok(delivery.getCourierID());
    }

    /**
     * Updates the courier ID of an order.
     *
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId     ID of the User for authorization (required)
     * @param courierId  ID of the courier for updating the Delivery (required)
     * @return a Delivery Object with the updates that took place
     */
    @Override
    public ResponseEntity<Delivery> deliveriesDeliveryIdCourierPut(@PathVariable UUID deliveryId,
                                                                   @RequestHeader String userId,
                                                                   @RequestBody String courierId) {
        AccountType courier = usersAuthenticationService.getUserAccountType(courierId);
        AccountType userType = usersAuthenticationService.getUserAccountType(userId);
        Delivery delivery = deliveryService.getDelivery(deliveryId);

        if (!courier.equals(AccountType.COURIER)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "The person you are trying to assign to the order is not a courier.");
        }
        if (userType.equals(AccountType.ADMIN)) {
            deliveryService.updateDeliveryCourier(deliveryId, courierId);
            return ResponseEntity.ok(delivery);
        }
        if (delivery.getCourierID() != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Delivery already has a courier assigned.");
        }
        switch (userType) {
            case CLIENT ->
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
            case INVALID ->
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User lacks valid authentication credentials.");
            case COURIER -> {
                if (userId.equals(courierId)) {
                    deliveryService.updateDeliveryCourier(deliveryId, courierId);
                    availableDeliveryProxy.insertDelivery(delivery);
                    return ResponseEntity.ok(delivery);
                } else {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
                }
            }
            case VENDOR -> {
                Restaurant restaurant = deliveryService.getRestaurant(delivery.getRestaurantID());
                // Not allowed to assign couriers to different vendors
                if (!restaurant.getRestaurantID().equals(userId)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
                }
                // Not allowed to assign couriers that are not in the list of own couriers (if restaurant uses it)
                if (restaurant.getCouriers() == null || !restaurant.getCouriers().contains(courierId)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
                }
                deliveryService.updateDeliveryCourier(deliveryId, courierId);
                return ResponseEntity.ok(delivery);
            }
        }
        return ResponseEntity.ok(delivery);
    }

    /**
     * Updates the estimated preparation time of an order.
     *
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId     User ID for authorization (required)
     * @param prepTime   Update prep time of delivery (required)
     * @return a delivery object with the updates that took place
     */
    @Override
    public ResponseEntity<Delivery> deliveriesDeliveryIdPrepPut(@PathVariable UUID deliveryId,
                                                                @RequestHeader String userId,
                                                                @RequestBody Integer prepTime) {

        Delivery delivery = getDeliveryAndAuthenticateUser(deliveryId, userId);
        AccountType accountType = usersAuthenticationService.getUserAccountType(userId);
        if (!List.of(AccountType.ADMIN, AccountType.VENDOR).contains(accountType)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only restaurant owner can update estimated prep time");
        }

        deliveryService.updateEstimatedPrepTime(deliveryId, prepTime);
        return ResponseEntity.ok(delivery);
    }

    /**
     * Computes an estimation for when an order will be delivered.
     *
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId     User ID for authorization (required)
     * @return an estimated delivery time
     */
    @Override
    public ResponseEntity<OffsetDateTime> deliveriesDeliveryIdEstimatedDeliveryTimeGet(@PathVariable UUID deliveryId,
                                                                                       @RequestHeader String userId) {
        getDeliveryAndAuthenticateUser(deliveryId, userId);
        OffsetDateTime estimate = deliveryService.computeEstimatedDeliveryTime(deliveryId);
        return ResponseEntity.ok(estimate);
    }

    /**
     * Return a specific delivery entity.
     *
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId     User ID for authorization (required)
     * @return delivery
     */
    @Override
    public ResponseEntity<Delivery> deliveriesDeliveryIdGet(@PathVariable UUID deliveryId, @RequestHeader String userId) {
        Delivery delivery = getDeliveryAndAuthenticateUser(deliveryId, userId);
        return ResponseEntity.ok(delivery);
    }

    /**
     * Returns the preparation time for a specific entity.
     *
     * @param deliveryId ID of the Delivery entity (required)
     * @param userId     User ID for authorization (required)
     * @return the estimated preparation time
     */
    @Override
    public ResponseEntity<Integer> deliveriesDeliveryIdPrepGet(@PathVariable UUID deliveryId, @RequestHeader String userId) {
        Delivery delivery = getDeliveryAndAuthenticateUser(deliveryId, userId);
        return ResponseEntity.ok(delivery.getEstimatedPrepTime());
    }

    /**
     * Updates the delivery address of an order.
     *
     * @param deliveryId  ID of the Delivery entity (required)
     * @param userId      ID of the User for authorization (required)
     * @param address new address to be used in the future (required)
     * @return a Delivery Object with the updates that took place
     */
    @Override
    public ResponseEntity<Delivery> deliveriesDeliveryIdDeliveryAddressPut(@PathVariable UUID deliveryId,
                                                                           @RequestHeader String userId,
                                                                           @RequestBody List<Double> address) {

        Delivery delivery = getDeliveryAndAuthenticateUser(deliveryId, userId);
        AccountType accountType = usersAuthenticationService.getUserAccountType(userId);
        if (!List.of(AccountType.ADMIN, AccountType.CLIENT).contains(accountType)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only restaurant owner can update estimated preparation time");
        }
        if (address.size() != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Delivery Address not set correctly");
        }

        deliveryService.updateDeliveryAddress(deliveryId, address);
        return ResponseEntity.ok(delivery);
    }


    static public class InvalidUserException extends ResponseStatusException {
        public InvalidUserException() {
            super(HttpStatus.UNAUTHORIZED, "User is not known to the system");
        }
    }

    static public class NotAuthenticatedException extends ResponseStatusException {
        public NotAuthenticatedException() {
            super(HttpStatus.FORBIDDEN, "User could not be authorized");
        }
    }
}