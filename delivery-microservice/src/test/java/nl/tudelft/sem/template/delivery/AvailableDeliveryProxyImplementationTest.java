package nl.tudelft.sem.template.delivery;

import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.RestaurantRepository;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import nl.tudelft.sem.template.model.Restaurant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.HttpStatus;

import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@EntityScan("nl.tudelft.sem.template.*")
@Transactional
@DataJpaTest

class AvailableDeliveryProxyImplementationTest {

    private AvailableDeliveryProxyImplementation sut;

    @Autowired
    private DeliveryRepository dr;

    @Autowired
    private RestaurantRepository rr;

    @BeforeEach
    void setUp() {
        sut = new AvailableDeliveryProxyImplementation(new DeliveryService(dr, new GPS(), rr));
    }

    List<Delivery> getMockData() {
        // Mock data
        Restaurant r = new Restaurant();
        r.setRestaurantID("vendor@testmail.com");
        r.setLocation(List.of(0.5, 0.1));
        r.setDeliveryZone(10.0);

        Restaurant r2 = new Restaurant();
        r2.setRestaurantID("vendor2@testmail.com");
        r2.setLocation(List.of(0.5, 0.1));
        r2.setDeliveryZone(10.0);
        r2.setCouriers(List.of("courier1", "courier2"));

        Delivery d1 = new Delivery();
        UUID deliveryId = UUID.randomUUID();
        d1.setDeliveryID(deliveryId);
        d1.setRestaurantID("vendor@testmail.com");
        d1.setCourierID("hi_im_assigned");
        d1.setStatus(DeliveryStatus.DELIVERED);

        Delivery d2 = new Delivery();
        UUID deliveryId2 = UUID.randomUUID();
        d2.setDeliveryID(deliveryId2);
        d2.setRestaurantID("vendor@testmail.com");
        d2.setStatus(DeliveryStatus.ACCEPTED);

        Delivery d3 = new Delivery();
        UUID deliveryId3 = UUID.randomUUID();
        d3.setDeliveryID(deliveryId3);
        d3.setRestaurantID("vendor@testmail.com");
        d3.setStatus(DeliveryStatus.PREPARING);

        Delivery d4 = new Delivery();
        UUID deliveryId4 = UUID.randomUUID();
        d4.setDeliveryID(deliveryId4);
        d4.setRestaurantID("vendor2@testmail.com");
        d4.setStatus(DeliveryStatus.PENDING);

        Delivery d5 = new Delivery();
        UUID deliveryId5 = UUID.randomUUID();
        d5.setDeliveryID(deliveryId5);
        d5.setRestaurantID("vendor@testmail.com");
        d5.setStatus(DeliveryStatus.ACCEPTED);

        rr.save(r);
        rr.save(r2);
        dr.save(d1);
        dr.save(d2);
        dr.save(d3);
        dr.save(d4);
        dr.save(d5);

        return List.of(d1, d2, d3, d4, d5);
    }

    @Test
    void isNullOrEmpty() {
        assertTrue(sut.isNullOrEmpty(""));
        assertTrue(sut.isNullOrEmpty(null));
        assertTrue(sut.isNullOrEmpty("      "));
    }

    @Test
    void testCheckStatus() {
        List<Delivery> testData = getMockData();

        assertFalse(sut.checkStatus(testData.get(0)));
        assertTrue(sut.checkStatus(testData.get(1)));
        assertTrue(sut.checkStatus(testData.get(2)));
        assertFalse(sut.checkStatus(testData.get(3)));
    }

    @Test
    void testInsertData() {
        List<Delivery> testData = getMockData();

        sut.insertDelivery(testData.get(0));
        assertThatThrownBy(() -> sut.getAvailableDeliveryId())
            .extracting("status")
            .isEqualTo(HttpStatus.NOT_FOUND);
        assertThatThrownBy(() -> sut.getAvailableDeliveryId())
            .message()
            .isEqualTo("404 NOT_FOUND \"There are no available deliveries at the moment.\"");
        sut.insertDelivery(testData.get(1));
        sut.insertDelivery(testData.get(2));
        assertEquals(testData.get(1).getDeliveryID(), sut.getAvailableDeliveryId());
        assertEquals(testData.get(2).getDeliveryID(), sut.getAvailableDeliveryId());
        sut.insertDelivery(testData.get(3));
        assertThatThrownBy(() -> sut.getAvailableDeliveryId())
            .extracting("status")
            .isEqualTo(HttpStatus.NOT_FOUND);
        assertThatThrownBy(() -> sut.getAvailableDeliveryId())
            .message()
            .isEqualTo("404 NOT_FOUND \"There are no available deliveries at the moment.\"");
    }

    @Test
    void testCheckIfAvailableMultipleLoops() {
        List<Delivery> test = getMockData();

        Delivery d1 = test.get(1);
        Delivery d2 = test.get(2);
        Delivery d3 = test.get(4);

        sut.insertDelivery(d1);
        sut.insertDelivery(d2);
        sut.insertDelivery(d3);

        d1.setCourierID("oh_no_im_assigned_now");
        d2.setCourierID("oh_no_me_too");
        dr.save(d1);
        dr.save(d2);

        assertEquals(d3.getDeliveryID(), sut.getAvailableDeliveryId());
    }
}