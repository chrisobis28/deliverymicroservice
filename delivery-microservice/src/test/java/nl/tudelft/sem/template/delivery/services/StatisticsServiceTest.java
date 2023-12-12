package nl.tudelft.sem.template.delivery.services;

import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.model.Delivery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {
    @Mock
    public DeliveryRepository deliveryRepositoryMock;

    @InjectMocks
    public StatisticsService statisticsService;

    @Test
    void getOrderRating() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setRatingRestaurant(5);

        when(deliveryRepositoryMock.findById(deliveryId)).thenReturn(Optional.of(delivery));
        assertThat(statisticsService.getOrderRating(deliveryId)).isEqualTo(5);
    }
}