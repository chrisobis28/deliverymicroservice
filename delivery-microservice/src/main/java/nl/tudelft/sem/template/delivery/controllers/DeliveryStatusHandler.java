package nl.tudelft.sem.template.delivery.controllers;

import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService.AccountType;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DeliveryStatusHandler {

    private final DeliveryService deliveryService;
    private final UsersAuthenticationService usersAuthenticationService;


    public DeliveryStatusHandler(DeliveryService deliveryService, UsersAuthenticationService usersAuthenticationService) {
        this.deliveryService = deliveryService;
        this.usersAuthenticationService = usersAuthenticationService;
    }

    public ResponseEntity<String> getDeliveryStatus(UUID deliveryId, String userId) {
        AccountType accountType = usersAuthenticationService.getUserAccountType(userId);
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        authorizeUser(userId, accountType, delivery);

        return ResponseEntity.ok(delivery.getStatus().toString());
    }


    public ResponseEntity<Delivery> updateDeliveryStatus(UUID deliveryId, String userId, String status) {
        AccountType accountType = usersAuthenticationService.getUserAccountType(userId);
        Delivery delivery = deliveryService.getDelivery(deliveryId);
        authorizeUser(userId, accountType, delivery);

        DeliveryStatus oldStatus = delivery.getStatus();
        DeliveryStatus newStatus = StatusValidity.validate(status);
        if (!StatusValidity.isStatusUpdateLegal(accountType, oldStatus, newStatus)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        deliveryService.updateDeliveryStatus(deliveryId, newStatus);
        return ResponseEntity.ok(delivery);
    }

    private void authorizeUser(String userId, AccountType accountType, Delivery delivery) {
        if (accountType == AccountType.INVALID) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (!usersAuthenticationService.checkUserAccessToDelivery(userId, delivery)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private static final class StatusValidity {
        private static final Map<AccountType, List<DeliveryStatus>> possibleUpdates = Map.of(
                AccountType.INVALID, List.of(),
                AccountType.CLIENT, List.of(),
                AccountType.VENDOR, List.of(DeliveryStatus.ACCEPTED, DeliveryStatus.REJECTED, DeliveryStatus.PREPARING),
                AccountType.COURIER, List.of(DeliveryStatus.ON_TRANSIT, DeliveryStatus.DELIVERED)
        );

        private static final Map<DeliveryStatus, DeliveryStatus> chronologicalPredecessor = Map.of(
                //DeliveryStatus.PENDING, null,
                DeliveryStatus.REJECTED, DeliveryStatus.PENDING,
                DeliveryStatus.ACCEPTED, DeliveryStatus.PENDING,
                DeliveryStatus.PREPARING, DeliveryStatus.ACCEPTED,
                DeliveryStatus.GIVEN_TO_COURIER, DeliveryStatus.PREPARING,
                DeliveryStatus.ON_TRANSIT, DeliveryStatus.GIVEN_TO_COURIER,
                DeliveryStatus.DELIVERED, DeliveryStatus.ON_TRANSIT
        );

        public static DeliveryStatus validate(String status) {
            try {
                return DeliveryStatus.fromValue(status);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Delivery status type");
            }
        }

        /**
         * Checks if the update of status from old to new value is legal.
         * <ul>The update is legal if:
         * <li>User is authorized to update with specified value</li>
         * <li>New value is a chronological successor of the old value</li>
         * </ul>
         */
        public static boolean isStatusUpdateLegal(AccountType accountType, DeliveryStatus oldStatus, DeliveryStatus newStatus) {
            return accountType.equals(AccountType.ADMIN)
                    || (possibleUpdates.get(accountType).contains(newStatus)
                    && chronologicalPredecessor.get(newStatus).equals(oldStatus));
        }
    }

}
