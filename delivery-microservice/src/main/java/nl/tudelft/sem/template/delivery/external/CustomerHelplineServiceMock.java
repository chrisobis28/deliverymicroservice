package nl.tudelft.sem.template.delivery.external;

import nl.tudelft.sem.template.model.Delivery;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;


@Service
public class CustomerHelplineServiceMock implements CustomerHelplineService {

    private final Log logger = LogFactory.getLog(CustomerHelplineServiceMock.class);

    @Override
    public void sendRequest(Delivery delivery, String message) {
        logger.debug("Send request to helpline for delivery " + delivery.getDeliveryID());
    }
}
