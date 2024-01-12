package nl.tudelft.sem.template.delivery.controllers;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import nl.tudelft.sem.template.api.CouriersApi;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static nl.tudelft.sem.template.delivery.services.UsersAuthenticationService.AccountType.COURIER;

@RestController
public class CouriersController implements CouriersApi {

    private final UsersAuthenticationService usersCommunication;
    private final DeliveryService deliveryService;

    /**
     * Constructor
     *
     * @param deliveryService    the delivery service
     * @param usersCommunication mock for users authorization
     */
    public CouriersController(DeliveryService deliveryService, UsersAuthenticationService usersCommunication) {
        this.deliveryService = deliveryService;
        this.usersCommunication = usersCommunication;

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
        List<UUID> list = deliveryService.getDeliveriesForACourier(courierId);
        return ResponseEntity.ok(list);

    }
}

