package nl.tudelft.sem.template.delivery.controllers;

import nl.tudelft.sem.template.api.ErrorsApi;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.delivery.services.ErrorService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.delivery.services.errors.DeliveryErrorAction;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;


@RestController
public class ErrorController implements ErrorsApi {

    private final ErrorService errorService;
    private final DeliveryService deliveryService;
    private final UsersAuthenticationService usersAuthentication;
    private final DeliveryErrorAction errorHandlingChain;

    /**
     * Constructor.
     *
     * @param errorService       the error service
     * @param deliveryService    the delivery service
     * @param usersAuthentication users authentication service
     * @param errorHandlingChain chain of responsibility for error handling
     */
    public ErrorController(ErrorService errorService, DeliveryService deliveryService, UsersAuthenticationService usersAuthentication, DeliveryErrorAction errorHandlingChain) {
        this.errorService = errorService;
        this.deliveryService = deliveryService;
        this.usersAuthentication = usersAuthentication;
        this.errorHandlingChain = errorHandlingChain;
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
        Delivery delivery = deliveryService.getDelivery(deliveryId);

        boolean isUserAllowed = usersAuthentication.checkUserAccessToDelivery(userId, delivery);
        if (!isUserAllowed) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated to view error for this delivery");
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
        Delivery delivery = deliveryService.getDelivery(deliveryId);

        boolean isUserAllowed = usersAuthentication.checkUserAccessToDelivery(userId, delivery);
        if (!isUserAllowed) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated to update error for this delivery");
        }

        Error deliveryError = errorService.updateError(deliveryId, error);
        errorHandlingChain.handle(delivery);

        return ResponseEntity.ok(deliveryError);
    }
}
