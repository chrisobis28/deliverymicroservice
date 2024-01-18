package nl.tudelft.sem.template.delivery.services;

import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@EntityScan("nl.tudelft.sem.template.*")
@ExtendWith(MockitoExtension.class)
@Transactional
@DataJpaTest
class UpdateServiceTest {

    @Autowired
    public DeliveryRepository dr;

    public UpdateService updateDao;

    @BeforeEach
    void setUp() {
        updateDao = new UpdateService(dr);
    }

    @Test
    void updatesStatusWhenUpdateDeliveryStatusCalled() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        dr.save(delivery);

        updateDao.updateDeliveryStatus(deliveryId, DeliveryStatus.DELIVERED);
        assertNotNull(dr.findById(deliveryId).get());
        assertEquals(DeliveryStatus.DELIVERED, dr.findById(deliveryId).get().getStatus());
    }

    @Test
    void updatesCourier() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        dr.save(delivery);

        updateDao.updateDeliveryCourier(deliveryId, "test_courier");
        assertNotNull(dr.findById(deliveryId).get());
        assertEquals("test_courier", dr.findById(deliveryId).get().getCourierID());
    }

    @Test
    void updatesCourierRating() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        dr.save(delivery);

        updateDao.updateCourierRating(deliveryId, 4);
        assertNotNull(dr.findById(deliveryId).get());
        assertEquals(4, dr.findById(deliveryId).get().getRatingCourier());
    }

    @Test
    void updatesRestaurantRating() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        dr.save(delivery);

        updateDao.updateRestaurantRating(deliveryId, 4);
        assertNotNull(dr.findById(deliveryId).get());
        assertEquals(4, dr.findById(deliveryId).get().getRatingRestaurant());
    }

    @Test
    void updatesDeliveryAddress() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        dr.save(delivery);

        updateDao.updateDeliveryAddress(deliveryId, List.of(4.5, 5.0));
        assertNotNull(dr.findById(deliveryId).get());
        assertEquals(List.of(4.5, 5.0), dr.findById(deliveryId).get().getDeliveryAddress());
    }

    @Test
    void updateStatusException() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        assertThrows(DeliveryService.DeliveryNotFoundException.class,
            () -> updateDao.updateDeliveryStatus(deliveryId, DeliveryStatus.ACCEPTED));
    }

    @Test
    void updateAddressException() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        assertThrows(DeliveryService.DeliveryNotFoundException.class,
            () -> updateDao.updateDeliveryAddress(deliveryId, List.of(4.5, 5.0)));
    }

    @Test
    void updateCourierException() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        assertThrows(DeliveryService.DeliveryNotFoundException.class,
            () -> updateDao.updateDeliveryCourier(deliveryId, "courier"));
    }

    @Test
    void updateCourierRatingException() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        dr.save(delivery);

        assertThrows(DeliveryService.DeliveryNotFoundException.class,
            () -> updateDao.updateCourierRating(UUID.randomUUID(), 4));
    }

    @Test
    void updateRestaurantRatingException() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        dr.save(delivery);

        assertThrows(DeliveryService.DeliveryNotFoundException.class,
            () -> updateDao.updateRestaurantRating(UUID.randomUUID(), 4));
    }
}