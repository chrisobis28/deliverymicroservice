package nl.tudelft.sem.template.delivery.services;

import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.RestaurantRepository;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.Restaurant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import javax.transaction.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
@EntityScan("nl.tudelft.sem.template.*")
@ExtendWith(MockitoExtension.class)
@Transactional
@DataJpaTest
class CouriersServiceTest {

  @Autowired
  private RestaurantRepository rr;
  @Autowired
  private DeliveryRepository dr;

  private CouriersService cs;

  UUID id = UUID.randomUUID();
  UUID id2 = UUID.randomUUID();

  UUID id3 = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    cs = new CouriersService(dr, rr);
  }

  @Test
  void test_courier_does_belong() {
    Restaurant r = new Restaurant();
    r.setRestaurantID("bla");
    r.setCouriers(List.of("courier@testmail.com"));
    r.setLocation(List.of(12.2,13.3));
    rr.save(r);
    assertTrue(cs.courierBelongsToRestaurant("courier@testmail.com"));
  }

  @Test
  void test_courier_does_not_belong() {
    Restaurant r = new Restaurant();
    r.setRestaurantID("bla");
    r.setLocation(List.of(12.2,13.3));
    rr.save(r);
    assertFalse(cs.courierBelongsToRestaurant("courier@testmail.com"));
  }

  @Test
  void test_deliveries_of_courier() {
    Delivery d = new Delivery();
    d.setDeliveryID(id);
    d.setCourierID("courier@testmail.com");

    Delivery d2 = new Delivery();
    d2.setDeliveryID(id2);
    d2.setCourierID("other_courier@testmail.com");

    Delivery d3 = new Delivery();
    d3.setDeliveryID(id3);
    d3.setCourierID("courier@testmail.com");
    dr.save(d);
    dr.save(d2);
    dr.save(d3);

    assertEquals(List.of(id, id3), cs.getDeliveriesForACourier("courier@testmail.com"));
    assertEquals(List.of(id2), cs.getDeliveriesForACourier("other_courier@testmail.com"));
    assertEquals(List.of(), cs.getDeliveriesForACourier("other_other_courier@testmail.com"));
  }
}