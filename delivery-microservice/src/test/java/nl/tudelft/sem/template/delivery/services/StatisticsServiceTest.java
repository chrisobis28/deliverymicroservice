package nl.tudelft.sem.template.delivery.services;

import javax.transaction.Transactional;
import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.model.Delivery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
@EntityScan("nl.tudelft.sem.template.*")
@ExtendWith(MockitoExtension.class)
@Transactional
@DataJpaTest
class StatisticsServiceTest {
    @Autowired
    public DeliveryRepository deliveryRepositoryMock;
    private StatisticsService statisticsService;


    @BeforeEach
    void setUp() {
        statisticsService = new StatisticsService(deliveryRepositoryMock);
    }

    @Test
    void getOrderRating() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setRatingRestaurant(5);

        Delivery test = statisticsService.insert(delivery);
        assertEquals(test.getRatingRestaurant(), deliveryRepositoryMock.findById(deliveryId).get().getRatingRestaurant());
        assertThat(statisticsService.getOrderRating(deliveryId)).isEqualTo(5);
    }
}