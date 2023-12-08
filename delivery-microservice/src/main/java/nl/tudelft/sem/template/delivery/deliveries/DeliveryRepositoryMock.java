package nl.tudelft.sem.template.delivery.deliveries;

import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class DeliveryRepositoryMock implements DeliveryRepository {

    private final Logger logger = LoggerFactory.getLogger(DeliveryRepositoryMock.class);
    private final Delivery sampleDelivery;

    public DeliveryRepositoryMock() {
        sampleDelivery = new Delivery();
        sampleDelivery.setDeliveryID(UUID.randomUUID());
        sampleDelivery.setStatus(DeliveryStatus.ON_TRANSIT);
    }

    @Override
    public Optional<Delivery> findById(UUID deliveryId) {
        logger.debug("This method is mocked");
        return Optional.of(sampleDelivery);
    }


    @Override
    public <S extends Delivery> S save(S delivery) {
        logger.debug("This method is mocked");
        return null;
    }
}
