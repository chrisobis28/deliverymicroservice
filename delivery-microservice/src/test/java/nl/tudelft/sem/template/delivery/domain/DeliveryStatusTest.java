package nl.tudelft.sem.template.delivery.domain;


import nl.tudelft.sem.template.delivery.services.DeliveryService;
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
    public DeliveryService deliveryDao;

    @Test
    void Returns_delivery_status_when_getDeliveryStatus_called() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setStatus(DeliveryStatus.ACCEPTED);

        when(deliveryRepositoryMock.findById(deliveryId)).thenReturn(Optional.of(delivery));
        assertThat(deliveryDao.getDeliveryStatus(deliveryId)).isEqualTo(DeliveryStatus.ACCEPTED);
    }

    @Test
    void Throws_deliveryNotFound_when_deliveryId_is_invalid() {
        UUID invalidDeliveryId = UUID.randomUUID();
        when(deliveryRepositoryMock.findById(any(UUID.class))).thenReturn(Optional.empty());
        assertThatExceptionOfType(DeliveryService.DeliveryNotFoundException.class)
                .isThrownBy(() -> deliveryDao.getDeliveryStatus(invalidDeliveryId));
    }

    @Test
    void Updates_status_when_updateDeliveryStatus_called() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);

        when(deliveryRepositoryMock.findById(deliveryId)).thenReturn(Optional.of(delivery));
        deliveryDao.updateDeliveryStatus(deliveryId, DeliveryStatus.DELIVERED);
        verify(deliveryRepositoryMock, times(1)).save(argThat(x ->
                x.getDeliveryID().equals(deliveryId) && x.getStatus().equals(DeliveryStatus.DELIVERED)));
    }
}