package nl.tudelft.sem.template.delivery.controllers;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import nl.tudelft.sem.template.api.CustomersApi;
import nl.tudelft.sem.template.delivery.services.CustomersService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService.AccountType;
import nl.tudelft.sem.template.model.Delivery;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class CustomerController implements CustomersApi {

    private final transient CustomersService customersService;
    private final transient UsersAuthenticationService usersAuth;

    /**
     * Constructor.
     *
     * @param customersService customer service
     * @param usersAuth user authentication
     */
    public CustomerController(CustomersService customersService, UsersAuthenticationService usersAuth) {
        this.customersService = customersService;
        this.usersAuth = usersAuth;
    }

    @Override
    public ResponseEntity<List<UUID>> customersCustomerIdOrdersGet(String customerId) {

        AccountType accountType = usersAuth.getUserAccountType(customerId);
        if (accountType != AccountType.CLIENT) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer with specified ID not found");
        }

        List<UUID> deliveryIds = customersService.getDeliveriesForCustomer(customerId)
                .stream()
                .map(Delivery::getDeliveryID)
                .collect(Collectors.toList());
        return ResponseEntity.ok(deliveryIds);
    }
}
