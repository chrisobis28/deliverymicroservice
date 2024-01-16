package nl.tudelft.sem.template.delivery.services.errors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ErrorHandlingChainDefinition {

    /**
     * Creates a chain of responsibility consisting of DeliveryErrorActions.
     * The bean returned by this method can be used for handling Delivery Errors.
     *
     * @return the first element of the chain
     */
    @Primary
    @Bean
    public DeliveryErrorAction getDeliveryErrorHandlingChain(SendNotificationAction notificationAction,
                                                             ContactHelplineAction helplineAction) {

        // Create the error handling chain
        notificationAction.setNext(helplineAction);
        return notificationAction;
    }
}
