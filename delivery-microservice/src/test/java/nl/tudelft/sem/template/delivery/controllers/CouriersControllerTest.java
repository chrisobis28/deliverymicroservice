package nl.tudelft.sem.template.delivery.controllers;

import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.RestaurantRepository;
import nl.tudelft.sem.template.delivery.services.CouriersService;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import nl.tudelft.sem.template.model.Restaurant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@EntityScan("nl.tudelft.sem.template.*")
@ExtendWith(MockitoExtension.class)
@DataJpaTest
class CouriersControllerTest {
  @Autowired
  private DeliveryRepository dr;

  @Autowired
  private RestaurantRepository rr;

  private CouriersService cs;

  private DeliveryService ds;

  private CouriersController sut;

  @Mock
  private UsersAuthenticationService usersAuth;

  @BeforeEach
  void setUp() {
    ds = new DeliveryService(dr, rr);
    cs = new CouriersService(dr, rr);
    sut = new CouriersController(ds, usersAuth, cs);
  }

  @Test
  void couriersCourierIdOrdersGet() {
    List<UUID> deliveryUUIDs = Stream.generate(UUID::randomUUID).limit(3).collect(Collectors.toList());
    List<Delivery> deliveries = deliveryUUIDs
        .stream()
        .map(x -> new Delivery().deliveryID(x))
        .collect(Collectors.toList());
    deliveries.forEach(x -> x.setCourierID("courier@testmail.com"));
    dr.saveAll(deliveries);

    when(usersAuth.getUserAccountType("courier@testmail.com")).thenReturn(UsersAuthenticationService.AccountType.COURIER);

    ResponseEntity<List<UUID>> res = sut.couriersCourierIdOrdersGet("courier@testmail.com");
    assertEquals(HttpStatus.OK, res.getStatusCode());
    assertNotNull(res.getBody());
    assertEquals(3, res.getBody().size());
  }

  @Test
  void couriersCourierIdOrdersGet_OtherCourier() {
    List<UUID> deliveryUUIDs = Stream.generate(UUID::randomUUID).limit(3).collect(Collectors.toList());
    List<Delivery> deliveries = deliveryUUIDs
        .stream()
        .map(x -> new Delivery().deliveryID(x))
        .collect(Collectors.toList());
    deliveries.forEach(x -> x.setCourierID("othercourier@testmail.com"));
    dr.saveAll(deliveries);

    when(usersAuth.getUserAccountType("courier@testmail.com")).thenReturn(UsersAuthenticationService.AccountType.COURIER);

    ResponseEntity<List<UUID>> res = sut.couriersCourierIdOrdersGet("courier@testmail.com");
    assertEquals(HttpStatus.OK, res.getStatusCode());
    assertEquals(List.of(), res.getBody());
  }

  @Test
  void couriersCourierIdOrdersGet_NullId() {
    List<UUID> deliveryUUIDs = Stream.generate(UUID::randomUUID).limit(3).collect(Collectors.toList());
    List<Delivery> deliveries = deliveryUUIDs
        .stream()
        .map(x -> new Delivery().deliveryID(x))
        .collect(Collectors.toList());
    dr.saveAll(deliveries);

    assertThatThrownBy(() -> sut.couriersCourierIdOrdersGet(null))
        .extracting("status")
        .isEqualTo(HttpStatus.BAD_REQUEST);
    assertThatThrownBy(() -> sut.couriersCourierIdOrdersGet(null))
        .message()
        .isEqualTo("400 BAD_REQUEST \"Courier ID cannot be NULL\"");
  }

  @Test
  void couriersCourierIdOrdersGet_NotFound() {
    List<UUID> deliveryUUIDs = Stream.generate(UUID::randomUUID).limit(3).collect(Collectors.toList());
    List<Delivery> deliveries = deliveryUUIDs
        .stream()
        .map(x -> new Delivery().deliveryID(x))
        .collect(Collectors.toList());
    dr.saveAll(deliveries);

    when(usersAuth.getUserAccountType("notcourier@testmail.com")).thenReturn(UsersAuthenticationService.AccountType.CLIENT);

    assertThatThrownBy(() -> sut.couriersCourierIdOrdersGet("notcourier@testmail.com"))
        .extracting("status")
        .isEqualTo(HttpStatus.NOT_FOUND);
    assertThatThrownBy(() -> sut.couriersCourierIdOrdersGet("notcourier@testmail.com"))
        .message()
        .isEqualTo("404 NOT_FOUND \"There is no such courier\"");
  }

  @Test
  void couriersCourierIdNextOrderPut_NotCourier() {
    List<UUID> deliveryUUIDs = Stream.generate(UUID::randomUUID).limit(3).collect(Collectors.toList());
    List<Delivery> deliveries = deliveryUUIDs
        .stream()
        .map(x -> new Delivery().deliveryID(x).customerID("not_a_courier@testmail.com"))
        .collect(Collectors.toList());
    dr.saveAll(deliveries);
    when(usersAuth.getUserAccountType("not_a_courier@testmail.com")).thenReturn(UsersAuthenticationService.AccountType.CLIENT);

    ResponseEntity<Delivery> res = sut.couriersCourierIdNextOrderPut("not_a_courier@testmail.com");
    assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
  }

  @Test
  void couriersCourierIdNextOrderPut() {
    List<UUID> deliveryUUIDs = Stream.generate(UUID::randomUUID).limit(3).collect(Collectors.toList());
    List<Delivery> deliveries = deliveryUUIDs
        .stream()
        .map(x -> new Delivery().deliveryID(x))
        .collect(Collectors.toList());
    deliveries.forEach(y -> y.setStatus(DeliveryStatus.ACCEPTED));
    deliveries.forEach(x -> x.setRestaurantID("vendor@testmail.com"));
    dr.saveAll(deliveries);

    Restaurant r = new Restaurant();
    r.setRestaurantID("vendor@testmail.com");
    rr.save(r);
    when(usersAuth.getUserAccountType("courier@testmail.com")).thenReturn(UsersAuthenticationService.AccountType.COURIER);

    ResponseEntity<Delivery> res = sut.couriersCourierIdNextOrderPut("courier@testmail.com");
    assertEquals(HttpStatus.OK, res.getStatusCode());
  }

  @Test
  void couriersCourierIdNextOrderPut_NotFound() {
    List<UUID> deliveryUUIDs = Stream.generate(UUID::randomUUID).limit(3).collect(Collectors.toList());
    List<Delivery> deliveries = deliveryUUIDs
        .stream()
        .map(x -> new Delivery().deliveryID(x))
        .collect(Collectors.toList());
    deliveries.forEach(y -> y.setStatus(DeliveryStatus.DELIVERED));
    deliveries.forEach(x -> x.setRestaurantID("vendor@testmail.com"));
    dr.saveAll(deliveries);

    Restaurant r = new Restaurant();
    r.setRestaurantID("vendor@testmail.com");
    rr.save(r);
    when(usersAuth.getUserAccountType("courier@testmail.com")).thenReturn(UsersAuthenticationService.AccountType.COURIER);

    ResponseEntity<Delivery> res = sut.couriersCourierIdNextOrderPut("courier@testmail.com");
    assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
  }

  @Test
  void couriersCourierIdNextOrderPut_CourierBelongsToRestaurant() {
    List<UUID> deliveryUUIDs = Stream.generate(UUID::randomUUID).limit(3).collect(Collectors.toList());
    List<Delivery> deliveries = deliveryUUIDs
        .stream()
        .map(x -> new Delivery().deliveryID(x))
        .collect(Collectors.toList());
    deliveries.forEach(y -> y.setStatus(DeliveryStatus.ACCEPTED));
    deliveries.forEach(x -> x.setRestaurantID("vendor@testmail.com"));
    dr.saveAll(deliveries);

    Restaurant r = new Restaurant();
    r.setCouriers(List.of("courier@testmail.com"));
    r.setRestaurantID("vendor@testmail.com");
    rr.save(r);
    when(usersAuth.getUserAccountType("courier@testmail.com")).thenReturn(UsersAuthenticationService.AccountType.COURIER);

    ResponseEntity<Delivery> res = sut.couriersCourierIdNextOrderPut("courier@testmail.com");
    assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
  }
}