package nl.tudelft.sem.template.delivery.deliveries;

import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@Configuration
public class DeliveryRepositoryMockConfig {

    DeliveryRepository mock = Mockito.mock(DeliveryRepository.class);

    public DeliveryRepositoryMockConfig() {
        Delivery sampleDelivery = new Delivery();
        sampleDelivery.setDeliveryID(UUID.randomUUID());
        sampleDelivery.setStatus(DeliveryStatus.ON_TRANSIT);

        when(mock.findById(any())).thenReturn(Optional.of(sampleDelivery));
        when(mock.save(any())).thenReturn(sampleDelivery);
    }

    @Bean
    public DeliveryRepository getDeliveryRepositoryMock() {
        return mock;
    }
}
