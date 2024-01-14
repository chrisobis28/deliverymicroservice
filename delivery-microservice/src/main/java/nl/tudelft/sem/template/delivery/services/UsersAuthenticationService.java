package nl.tudelft.sem.template.delivery.services;

import nl.tudelft.sem.template.delivery.communication.UsersCommunication;
import nl.tudelft.sem.template.model.Delivery;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class UsersAuthenticationService {

    private final UsersCommunication usersCommunication;

    public UsersAuthenticationService(UsersCommunication usersCommunication) {
        this.usersCommunication = usersCommunication;
    }

    public AccountType getUserAccountType(String userId) {
        String accountTypeString = usersCommunication.getAccountType(userId);
        return AccountType.fromValue(accountTypeString);
    }

    /**
     * @param userId   ID of the user
     * @param delivery Delivery object we want to check permission for
     * @return whether the user has permission to access this delivery object
     */
    public boolean checkUserAccessToDelivery(String userId, Delivery delivery) {
        if (userId == null) return false;
        AccountType accountType = getUserAccountType(userId);

        return switch (accountType) {
            case INVALID -> false;
            case CLIENT -> userId.equals(delivery.getCustomerID());
            case COURIER -> userId.equals(delivery.getCourierID());
            case VENDOR -> userId.equals(delivery.getRestaurantID());
            case ADMIN -> true;
        };
    }

    public enum AccountType {
        INVALID("INVALID"),
        CLIENT("CLIENT"),
        COURIER("COURIER"),
        VENDOR("VENDOR"),
        ADMIN("ADMIN");

        private final String value;

        AccountType(String value) {
            this.value = value;
        }

        public static AccountType fromValue(String value) {
            return Arrays.stream(AccountType.values())
                    .filter(x -> x.value.equals(value))
                    .findAny()
                    .orElse(INVALID);
        }
    }
}
