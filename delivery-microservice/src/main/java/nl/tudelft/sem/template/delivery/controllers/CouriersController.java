package nl.tudelft.sem.template.delivery.controllers;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import nl.tudelft.sem.template.api.CouriersApi;
import nl.tudelft.sem.template.delivery.AvailableDeliveryProxy;
import nl.tudelft.sem.template.delivery.services.CouriersService;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.delivery.services.UpdateService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.model.Delivery;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static nl.tudelft.sem.template.delivery.services.UsersAuthenticationService.AccountType.COURIER;
import static nl.tudelft.sem.template.delivery.services.UsersAuthenticationService.AccountType.INVALID;

@RestController
public class CouriersController implements CouriersApi {

    private final transient UsersAuthenticationService usersCommunication;
    private final transient DeliveryService deliveryService;
    private final transient CouriersService couriersService;
    private final transient AvailableDeliveryProxy availableDeliveryProxy;
    private final transient UpdateService updateService;

    /**
     * Constructor.
     *
     * @param deliveryService        the delivery service
     * @param usersCommunication     mock for users authorization
     * @param couriersService        the service for assigning orders to couriers
     * @param availableDeliveryProxy the proxy that creates a cache for deliveries without courier
     * @param updateService          service of all updates
     */
    public CouriersController(DeliveryService deliveryService,
                              UsersAuthenticationService usersCommunication,
                              CouriersService couriersService,
                              AvailableDeliveryProxy availableDeliveryProxy,
                              UpdateService updateService) {
        this.deliveryService = deliveryService;
        this.couriersService = couriersService;
        this.usersCommunication = usersCommunication;
        this.availableDeliveryProxy = availableDeliveryProxy;
        this.updateService = updateService;
    }

    /**
     * Retrieves a list of all deliveries assigned to a courier.
     *
     * @param courierId the id of the courier (required)
     * @param userId    the id of the user for authorization (required)
     * @return the list of delivery ids with the corresponding response
     */

    public ResponseEntity<List<UUID>> couriersCourierIdOrdersGet(@PathVariable("courierId") String courierId,
                                                                 @RequestHeader String userId) {
        if (courierId == null || userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Courier ID cannot be NULL.");
        }
        UsersAuthenticationService.AccountType userType = usersCommunication.getUserAccountType(userId);
        UsersAuthenticationService.AccountType courierType = usersCommunication.getUserAccountType(courierId);
        if (userType.equals(INVALID)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account could not be verified.");
        }
        if (!courierType.equals(COURIER)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The id is not recognised as courier.");
        }
        List<UUID> list = couriersService.getDeliveriesForCourier(courierId);
        return ResponseEntity.ok(list);
    }

    /**
     * Assign the order next in the queue to the courier.
     *
     * @param courierId The id of a courier we want to assign the next order to (required)
     * @return Response Entity containing the Delivery that the courier was assigned to
     */
    @Override
    public ResponseEntity<Delivery> couriersCourierIdNextOrderPut(@Parameter(name = "courierId",
            required = true, in = ParameterIn.PATH) @PathVariable String courierId) {
        UsersAuthenticationService.AccountType account = usersCommunication.getUserAccountType(courierId);
        if (!Objects.equals(account, COURIER)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "There is no such courier");
        }
        if (couriersService.courierBelongsToRestaurant(courierId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This courier works for a specific restaurant");
        }
        UUID id = availableDeliveryProxy.getAvailableDeliveryId();
        updateService.updateDeliveryCourier(id, courierId);
        Delivery delivery = deliveryService.getDelivery(id);

        return ResponseEntity.ok(delivery);
    }

    /**
     * Retrieves all the ratings of a courier.
     *
     * @param courierId ID of the Courier entity (required)
     * @param userId    ID of the user for authorization (required)
     * @return a List of Integers corresponding to the ratings of the courier
     */
    @Override
    public ResponseEntity<List<Integer>> couriersCourierIdRatingsGet(@PathVariable String courierId,
                                                                     @RequestHeader String userId) {
        if (courierId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Courier ID cannot be NULL.");
        }
        UsersAuthenticationService.AccountType userType = usersCommunication.getUserAccountType(userId);
        UsersAuthenticationService.AccountType courierType = usersCommunication.getUserAccountType(courierId);
        if (userType.equals(INVALID)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account could not be verified.");
        }
        if (!courierType.equals(COURIER)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The id is not recognised as courier.");
        }
        List<Delivery> deliveries = couriersService.getDeliveriesForCourierRatings(courierId);
        List<Integer> ratings = deliveries.stream().map(Delivery::getRatingCourier).collect(Collectors.toList());
        return ResponseEntity.ok(ratings);
    }
}

