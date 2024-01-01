package nl.tudelft.sem.template.delivery.controllers;

import nl.tudelft.sem.template.api.ErrorsApi;
//import nl.tudelft.sem.template.delivery.communication.UsersCommunication;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.delivery.services.ErrorService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.model.Delivery;
//import nl.tudelft.sem.template.model.DeliveryStatus;
import nl.tudelft.sem.template.model.Error;
//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequestMapping("/errors")
@RestController
public class ErrorController implements ErrorsApi {

    private final ErrorService errorService;
    private final DeliveryService deliveryService;
    private final UsersAuthenticationService usersCommunication;

    /**
     * Constructor
     *
     * @param errorService       the error service
     * @param deliveryService    the delivery service
     * @param usersCommunication mock for users authorization
     */
    public ErrorController(ErrorService errorService, DeliveryService deliveryService, UsersAuthenticationService usersCommunication) {
        this.errorService = errorService;
        this.deliveryService = deliveryService;
        this.usersCommunication = usersCommunication;
    }

    /**
     * Gets the error of a given delivery
     *
     * @param userId     User ID for authorization (required)
     * @param deliveryId ID of the Delivery entity (required)
     * @return the error
     */
    @GetMapping("/{deliveryId}/")
    @Override
    public ResponseEntity<Error> errorsDeliveryIdGet(@RequestHeader String userId, @PathVariable UUID deliveryId) {
        if (isNullOrEmpty(userId)) {
            return ResponseEntity.badRequest().build();
        }
        UsersAuthenticationService.AccountType userType = usersCommunication.getUserAccountType(userId);
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        // Vendors can see errors only in their orders
        boolean allowedVendor = userType.equals(UsersAuthenticationService.AccountType.VENDOR) && userId.equals(delivery.getRestaurantID());
        // Couriers can see errors only in orders they deliver
        boolean allowedCourier = userType.equals(UsersAuthenticationService.AccountType.COURIER) && userId.equals(delivery.getCourierID());
        // Customers can see errors only in orders they made
        boolean allowedCustomer = userType.equals(UsersAuthenticationService.AccountType.CLIENT) && userId.equals(delivery.getCustomerID());
        if (userType.equals(UsersAuthenticationService.AccountType.ADMIN) ||
                allowedVendor ||
                allowedCourier ||
                allowedCustomer) {
            Error error = errorService.getError(deliveryId);
            return ResponseEntity.ok(error);
        } else if (userType.equals(UsersAuthenticationService.AccountType.VENDOR) ||
                userType.equals(UsersAuthenticationService.AccountType.COURIER) ||
                userType.equals(UsersAuthenticationService.AccountType.CLIENT)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    /**
     * Updates the error of a Delivery item.
     * Each Delivery has exactly one corresponding error.
     * In the case of no unexpected events occurring, the error code is set to NONE.
     *
     * @param userId     User ID for authorization (required)
     * @param deliveryId ID of the Delivery entity (required)
     * @param error      Update unexpected event of delivery (required)
     * @return the updated Error
     */
    @PutMapping("/{deliveryId}/")
    @Override
    public ResponseEntity<Error> errorsDeliveryIdPut(@RequestHeader String userId, @PathVariable UUID deliveryId, @RequestBody Error error) {
        if (isNullOrEmpty(userId)) {
            return ResponseEntity.badRequest().build();
        }
        UsersAuthenticationService.AccountType userType = usersCommunication.getUserAccountType(userId);
        Error deliveryError = errorService.getError(deliveryId);
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        // Vendors can edit errors only in their orders
        boolean allowedVendor = userType.equals(UsersAuthenticationService.AccountType.VENDOR) && userId.equals(delivery.getRestaurantID());
        // Couriers can edit errors only in orders they deliver
        boolean allowedCourier = userType.equals(UsersAuthenticationService.AccountType.COURIER) && userId.equals(delivery.getCourierID());
        if (userType.equals(UsersAuthenticationService.AccountType.ADMIN) ||
                allowedVendor ||
                allowedCourier) {
            deliveryError = errorService.updateError(deliveryId, error);
            return ResponseEntity.ok(deliveryError);
        } else if (userType.equals(UsersAuthenticationService.AccountType.VENDOR) ||
                userType.equals(UsersAuthenticationService.AccountType.COURIER) ||
                userType.equals(UsersAuthenticationService.AccountType.CLIENT)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    /**
     * Checks if a string is null or empty
     *
     * @param str string to check
     * @return boolean value indicating whether string is empty or not
     */
    public boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty() || str.isBlank();
    }

    /**
     * inserts an element into the repo
     *
     * @param error error being inserted
     * @return empty response entity
     */
    public ResponseEntity<Void> insert(@RequestBody Error error) {
        try {
            errorService.insert(error);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
