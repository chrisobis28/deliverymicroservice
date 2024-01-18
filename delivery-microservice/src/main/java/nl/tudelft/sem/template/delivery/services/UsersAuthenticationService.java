package nl.tudelft.sem.template.delivery.services;

import java.util.Arrays;

import nl.tudelft.sem.template.delivery.communication.UsersCommunication;
import nl.tudelft.sem.template.model.Delivery;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.tuple.Pair;


@Service
public class UsersAuthenticationService {

    private final transient UsersCommunication usersCommunication;

    public UsersAuthenticationService(UsersCommunication usersCommunication) {
        this.usersCommunication = usersCommunication;
    }

    public AccountType getUserAccountType(String userId) {
        String accountTypeString = usersCommunication.getAccountType(userId);
        return AccountType.fromValue(accountTypeString);
    }


    /**
     * The method checks if a user should have permission to view delivery.
     *
     * @param userId   ID of the user
     * @param delivery Delivery object we want to check permission for
     * @return whether the user has permission to access this delivery object
     */
    public boolean checkUserAccessToDelivery(String userId, Delivery delivery) {
        if (userId == null) {
            return false;
        }
        AccountType accountType = getUserAccountType(userId);

        return switch (accountType) {
            case INVALID -> false;
            case CLIENT -> userId.equals(delivery.getCustomerID());
            case COURIER -> userId.equals(delivery.getCourierID());
            case VENDOR -> userId.equals(delivery.getRestaurantID());
            case ADMIN -> true;
        };
    }

    /**
     * The method checks if a user should have permission to update/view an entity.
     *
     * @param userId        ID of the user
     * @param entityId  Restaurant object we want to check permission for
     * @param fieldName     Name of field being updated
     * @return              whether the user has permission to access this restaurant object
     */
    public Pair<HttpStatus, String> checkUserAccessToRestaurant(String userId, String entityId, String fieldName) {
        if (isNullOrEmpty(userId) || isNullOrEmpty(entityId)) {
            return Pair.of(HttpStatus.BAD_REQUEST, "User ID or Restaurant ID is invalid.");
        }
        AccountType accountType = getUserAccountType(userId);

        switch (accountType) {
            case ADMIN:
                return Pair.of(HttpStatus.OK, "OK");
            case CLIENT, COURIER:
                return courierAndClientRestaurantAccess(fieldName);
            case VENDOR: {
                if (userId.equals(entityId)) {
                    return Pair.of(HttpStatus.OK, "OK");
                } else {
                    return Pair.of(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
                }
            }
            default:
                return Pair.of(HttpStatus.UNAUTHORIZED, "User lacks valid authentication credentials.");
        }
    }

    /**
     * Checks how to respond for couriers and clients depending on the field being accessed.
     *
     * @param fieldName the field being accessed
     * @return whether the user has permission to access this restaurant object
     */
    public Pair<HttpStatus, String> courierAndClientRestaurantAccess(String fieldName) {
        switch (fieldName) {
            case "Couriers", "DPH", "New Order": {
                return Pair.of(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
            }
            case "Delivery Zone": {
                return Pair.of(HttpStatus.FORBIDDEN,
                    "Only vendors and admins can change the delivery zone of a restaurant.");
            }
            case "Location": {
                return Pair.of(HttpStatus.FORBIDDEN,
                    "Only vendors and admins can change the restaurant's address");
            }
            default: return Pair.of(HttpStatus.OK, "OK");
        }
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

    public enum AccountType {
        INVALID("invalid"),
        CLIENT("customer"),
        COURIER("courier"),
        VENDOR("vendor"),
        ADMIN("admin");

        private final String value;

        /**
         * Constructor that sets account type of user.
         *
         * @param value account type
         */
        AccountType(String value) {
            this.value = value;
        }

        /**
         * Recognizes account type. If unknown returns invalid.
         *
         * @param value string representation of type
         * @return account type or invalid is unknown
         */
        public static AccountType fromValue(String value) {
            return Arrays.stream(AccountType.values())
                    .filter(x -> x.value.equals(value))
                    .findAny()
                    .orElse(INVALID);
        }
    }
}
