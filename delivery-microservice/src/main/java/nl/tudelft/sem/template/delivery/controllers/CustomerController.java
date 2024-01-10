package nl.tudelft.sem.template.delivery.controllers;

import nl.tudelft.sem.template.api.CustomersApi;
import nl.tudelft.sem.template.delivery.services.CustomersService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService.AccountType;
import nl.tudelft.sem.template.model.Delivery;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CustomerController implements CustomersApi {

    private final CustomersService customersService;
    private final UsersAuthenticationService usersAuth;

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

        List<UUID> deliveryUUIDs = customersService.getDeliveriesForCustomer(customerId)
                .stream()
                .map(Delivery::getDeliveryID)
                .collect(Collectors.toList());
        return ResponseEntity.ok(deliveryUUIDs);
    }
}
