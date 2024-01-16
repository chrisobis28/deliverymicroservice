package nl.tudelft.sem.template.delivery.controllers;

import java.util.UUID;
import nl.tudelft.sem.template.api.ErrorsApi;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.delivery.services.ErrorService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.Error;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


@RestController
public class ErrorController implements ErrorsApi {

    private final transient ErrorService errorService;
    private final transient DeliveryService deliveryService;
    private final transient UsersAuthenticationService usersCommunication;

    /**
     * Constructor.
     *
     * @param errorService       the error service
     * @param deliveryService    the delivery service
     * @param usersCommunication mock for users authorization
     */
    public ErrorController(ErrorService errorService, DeliveryService deliveryService,
                           UsersAuthenticationService usersCommunication) {
        this.errorService = errorService;
        this.deliveryService = deliveryService;
        this.usersCommunication = usersCommunication;
    }

    /**
     * Gets the error of a given delivery.
     *
     * @param userId     User ID for authorization (required)
     * @param deliveryId ID of the Delivery entity (required)
     * @return the error
     */
    @Override
    public ResponseEntity<Error> errorsDeliveryIdGet(@RequestHeader String userId, @PathVariable UUID deliveryId) {
        if (isNullOrEmpty(userId)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID is invalid.");

        UsersAuthenticationService.AccountType userType = usersCommunication.getUserAccountType(userId);
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        boolean check = usersCommunication.checkUserAccessToDelivery(userId, delivery);

        switch (userType) {
            case ADMIN: break;
            // Customers can see errors only in orders they made
            // Couriers can see errors only in orders they deliver
            // Vendors can see errors only in their orders
            case CLIENT, COURIER, VENDOR: {
                if (check) break;
                else throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
            }
            default: throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User lacks valid authentication credentials.");
        }
        Error error = errorService.getError(deliveryId);
        return ResponseEntity.ok(error);
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
    @Override
    public ResponseEntity<Error> errorsDeliveryIdPut(@RequestHeader String userId, @PathVariable UUID deliveryId, @RequestBody Error error) {
        if (isNullOrEmpty(userId)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID is invalid.");

        UsersAuthenticationService.AccountType userType = usersCommunication.getUserAccountType(userId);
        //Error deliveryError = errorService.getError(deliveryId);
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        boolean check = usersCommunication.checkUserAccessToDelivery(userId, delivery);

        switch (userType) {
            case ADMIN: break;
            // Vendors can edit errors only in their orders
            // Couriers can edit errors only in orders they deliver
            case VENDOR, COURIER: {
                if (check) break;
                else throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
            }
            case CLIENT: throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
            default: throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User lacks valid authentication credentials.");
        }
        Error deliveryError = errorService.updateError(deliveryId, error);
        return ResponseEntity.ok(deliveryError);
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
     * inserts an element into the repo.
     *
     * @param error error being inserted
     * @return empty response entity
     */
    public ResponseEntity<Void> insert(@RequestBody Error error) {
        try {
            errorService.insert(error);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "BAD REQUEST");
        }
    }
}
