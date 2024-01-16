package nl.tudelft.sem.template.delivery.external;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationsServiceMock implements NotificationService {

    private final Log logger = LogFactory.getLog(NotificationsServiceMock.class);

    @Override
    public void sendNotification(String userId, String notification) {
        logger.debug("Notify user " + userId + " with message '" + notification + "'");
    }
}
