package nl.tudelft.sem.template.delivery.deliveries;

import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DeliveryStatusTest {

    @Mock
    public DeliveryRepository deliveryRepositoryMock;

    @InjectMocks
    public DeliveryDao deliveryDao;

    @Test
    void Returns_delivery_status_when_findById_called() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setStatus(DeliveryStatus.ACCEPTED);

        when(deliveryRepositoryMock.findById(deliveryId)).thenReturn(Optional.of(delivery));
        assertThat(deliveryDao.getDeliveryStatus(deliveryId)).isEqualTo(Optional.of(DeliveryStatus.ACCEPTED));
    }

    @Test
    void Returns_empty_when_deliveryId_not_found() {
        UUID incorrectDeliveryId = UUID.randomUUID();
        when(deliveryRepositoryMock.findById(any(UUID.class))).thenReturn(Optional.empty());
        assertThat(deliveryDao.getDeliveryStatus(incorrectDeliveryId)).isEqualTo(Optional.empty());

    }
}