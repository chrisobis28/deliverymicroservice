package nl.tudelft.sem.template.delivery.controllers;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import nl.tudelft.sem.template.api.CouriersApi;
import nl.tudelft.sem.template.delivery.AvailableDeliveryProxy;
import nl.tudelft.sem.template.delivery.services.CouriersService;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.model.Delivery;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;

import static nl.tudelft.sem.template.delivery.services.UsersAuthenticationService.AccountType.COURIER;

@RestController
public class CouriersController implements CouriersApi {

    private final UsersAuthenticationService usersCommunication;
    private final DeliveryService deliveryService;

    private final CouriersService couriersService;

    private final AvailableDeliveryProxy availableDeliveryProxy;

    /**
     * Constructor
     *
     * @param deliveryService    the delivery service
     * @param usersCommunication mock for users authorization
     */
    public CouriersController(DeliveryService deliveryService, UsersAuthenticationService usersCommunication, CouriersService couriersService) {
        this.deliveryService = deliveryService;
        this.couriersService = couriersService;
        this.usersCommunication = usersCommunication;
        this.availableDeliveryProxy = new AvailableDeliveryProxy(deliveryService);
    }

    /**
     * Retrieves a list of all deliveries assigned to a courier
     * @param courierId the id of the courier
     * @return the list of delivery ids with the corresponding response
     */

    public ResponseEntity<List<UUID>> couriersCourierIdOrdersGet(@PathVariable("courierId") String courierId) {
        if (courierId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Courier ID cannot be NULL");
        }
        UsersAuthenticationService.AccountType account = usersCommunication.getUserAccountType(courierId);
        if (!Objects.equals(account, COURIER)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no such courier");
        }
        List<UUID> list = couriersService.getDeliveriesForACourier(courierId);
        return ResponseEntity.ok(list);

    }

    /**
     * Assign the order next in the queue to the courier
     * @param courierId The id of a courier we want to assign the next order to (required)
     * @return Response Entity containing the Delivery that the courier was assigned to
     */
    @Override
    public ResponseEntity<Delivery> couriersCourierIdNextOrderPut(@Parameter(name = "courierId",required = true,in = ParameterIn.PATH) @PathVariable String courierId) {
        UsersAuthenticationService.AccountType account = usersCommunication.getUserAccountType(courierId);
        if (!Objects.equals(account, COURIER)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "There is no such courier");
        if (couriersService.courierBelongsToRestaurant(courierId)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This courier works for a specific restaurant");

        Queue<UUID> deliveries = availableDeliveryProxy.updateQueue();
        if (deliveries.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "There are no available deliveries at the moment");

        UUID id = deliveries.poll();
        deliveryService.updateDeliveryCourier(id, courierId);
        Delivery delivery = deliveryService.getDelivery(id);
        availableDeliveryProxy.checkIfAvailable(delivery);

        return ResponseEntity.ok(delivery);
    }
}

