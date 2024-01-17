package nl.tudelft.sem.template.delivery.controllers;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.tudelft.sem.template.delivery.GPS;
import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.ErrorRepository;
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
import org.springframework.web.server.ResponseStatusException;

@EntityScan("nl.tudelft.sem.template.*")
@ExtendWith(MockitoExtension.class)
@DataJpaTest
class CouriersControllerTest {
    @Autowired
    private DeliveryRepository dr;

    @Autowired
    private RestaurantRepository rr;


    @Autowired
    private ErrorRepository er;

    private CouriersService cs;

    private DeliveryService ds;

    private CouriersController sut;

    @Mock
    private UsersAuthenticationService usersAuth;

    @BeforeEach
    void setUp() {
        ds = new DeliveryService(dr, new GPS(), rr, er);
        cs = new CouriersService(dr, rr);
        sut = new CouriersController(ds, usersAuth, cs);
    }

    @Test
    void couriersCourierIdNextOrderPut_NotCourier() {
        List<UUID> deliveryUUIDs = Stream.generate(UUID::randomUUID).limit(3).collect(Collectors.toList());
        List<Delivery> deliveries = deliveryUUIDs
                .stream()
                .map(x -> new Delivery().deliveryID(x).customerID("not_a_courier@testmail.com"))
                .collect(Collectors.toList());
        dr.saveAll(deliveries);
        when(usersAuth.getUserAccountType("not_a_courier@testmail.com"))
                .thenReturn(UsersAuthenticationService.AccountType.CLIENT);

        assertThatThrownBy(() -> sut.couriersCourierIdNextOrderPut("not_a_courier@testmail.com"))
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> sut.couriersCourierIdNextOrderPut("not_a_courier@testmail.com"))
                .message()
                .isEqualTo("400 BAD_REQUEST \"There is no such courier\"");
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
        Restaurant r = new Restaurant();
        r.setRestaurantID("vendor@testmail.com");
        rr.save(r);
        deliveries.forEach(x -> sut.testMethod().insertDelivery(x));
        dr.saveAll(deliveries);

        when(usersAuth.getUserAccountType("courier@testmail.com"))
                .thenReturn(UsersAuthenticationService.AccountType.COURIER);

        ResponseEntity<Delivery> res = sut.couriersCourierIdNextOrderPut("courier@testmail.com");
        assertEquals(HttpStatus.OK, res.getStatusCode());

        res = sut.couriersCourierIdNextOrderPut("courier@testmail.com");
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
        when(usersAuth.getUserAccountType("courier@testmail.com"))
                .thenReturn(UsersAuthenticationService.AccountType.COURIER);

        assertThatThrownBy(() -> sut.couriersCourierIdNextOrderPut("courier@testmail.com"))
                .extracting("status")
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThatThrownBy(() -> sut.couriersCourierIdNextOrderPut("courier@testmail.com"))
                .message()
                .isEqualTo("404 NOT_FOUND \"There are no available deliveries at the moment.\"");
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
        Restaurant r = new Restaurant();
        r.setCouriers(List.of("courier@testmail.com"));
        r.setRestaurantID("vendor@testmail.com");
        rr.save(r);

        deliveries.forEach(x -> sut.testMethod().insertDelivery(x));
        dr.saveAll(deliveries);

        when(usersAuth.getUserAccountType("courier@testmail.com"))
                .thenReturn(UsersAuthenticationService.AccountType.COURIER);

        assertThatThrownBy(() -> sut.couriersCourierIdNextOrderPut("courier@testmail.com"))
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> sut.couriersCourierIdNextOrderPut("courier@testmail.com"))
                .message()
                .isEqualTo("400 BAD_REQUEST \"This courier works for a specific restaurant\"");
    }

    @Test
    void couriersCourierIdRatingsGetNull() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.couriersCourierIdRatingsGet(null, "user@testmail.com"));
        assertEquals(exception.getStatus(), HttpStatus.BAD_REQUEST);
        assertEquals(exception.getReason(), "Courier ID cannot be NULL.");
    }

    @Test
    void couriersCourierIdRatingsGetInvalid() {
        String userId = "invalid@testmail.com";
        String courierId = "courier@testmail.com";
        when(usersAuth.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.INVALID);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.couriersCourierIdRatingsGet(courierId, userId));
        assertEquals(exception.getStatus(), HttpStatus.UNAUTHORIZED);
        assertEquals(exception.getReason(), "Account could not be verified.");
    }

    @Test
    void couriersCourierIdRatingsGetNoCourier() {
        String userId = "invalid@testmail.com";
        String vendorId = "vendor@testmail.com";
        when(usersAuth.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.ADMIN);
        when(usersAuth.getUserAccountType(vendorId)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.couriersCourierIdRatingsGet(vendorId, userId));
        assertEquals(exception.getStatus(), HttpStatus.NOT_FOUND);
        assertEquals(exception.getReason(), "The id is not recognised as courier.");
    }

    @Test
    void couriersCourierIdRatingsGetOK() {
        String userId = "invalid@testmail.com";
        String courierId = "courier@testmail.com";
        String otherCourierId = "other_courier@testmail.com";
        when(usersAuth.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.ADMIN);
        when(usersAuth.getUserAccountType(courierId)).thenReturn(UsersAuthenticationService.AccountType.COURIER);

        Delivery d1 = new Delivery();
        UUID orderId = UUID.randomUUID();
        d1.setStatus(DeliveryStatus.DELIVERED);
        d1.setCustomerID("user@testmail.com");
        d1.setRestaurantID("hi_im_a_vendor@testmail.com");
        d1.setCourierID(courierId);
        d1.setDeliveryID(orderId);
        d1.setRatingCourier(3);
        d1.setDeliveryAddress(List.of(50.4, 32.6));

        Delivery d2 = new Delivery();
        orderId = UUID.randomUUID();
        d2.setStatus(DeliveryStatus.DELIVERED);
        d2.setCustomerID("user@testmail.com");
        d2.setRestaurantID("hi_im_a_vendor@testmail.com");
        d2.setCourierID(courierId);
        d2.setDeliveryID(orderId);
        d2.setRatingCourier(4);
        d2.setDeliveryAddress(List.of(50.4, 32.6));

        Delivery d3 = new Delivery();
        orderId = UUID.randomUUID();
        d3.setStatus(DeliveryStatus.DELIVERED);
        d3.setCustomerID("user@testmail.com");
        d3.setRestaurantID("hi_im_a_vendor@testmail.com");
        d3.setCourierID(otherCourierId);
        d3.setDeliveryID(orderId);
        d3.setRatingCourier(2);
        d3.setDeliveryAddress(List.of(50.4, 32.6));

        dr.save(d1);
        dr.save(d2);
        dr.save(d3);

        ResponseEntity<List<Integer>> result = sut.couriersCourierIdRatingsGet(courierId, userId);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(2, result.getBody().size());
        assertTrue(result.getBody().containsAll(List.of(3, 4)));
    }

    @Test
    void couriersCourierIdOrdersGetNull() {
        String userId = "user@testmail.com";
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.couriersCourierIdOrdersGet(null, userId));
        assertEquals(exception.getStatus(), HttpStatus.BAD_REQUEST);
        assertEquals(exception.getReason(), "Courier ID cannot be NULL.");
    }

    @Test
    void couriersCourierIdOrdersGetInvalid() {
        String userId = "invalid@testmail.com";
        String courierId = "courier@testmail.com";
        when(usersAuth.getUserAccountType(userId))
                .thenReturn(UsersAuthenticationService.AccountType.INVALID);
        when(usersAuth.getUserAccountType(courierId))
                .thenReturn(UsersAuthenticationService.AccountType.COURIER);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.couriersCourierIdOrdersGet(courierId, userId));
        assertEquals(exception.getStatus(), HttpStatus.UNAUTHORIZED);
        assertEquals(exception.getReason(), "Account could not be verified.");
    }

    @Test
    void couriersCourierIdOrdersGetNoCourier() {
        String userId = "invalid@testmail.com";
        String vendorId = "vendor@testmail.com";
        when(usersAuth.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.ADMIN);
        when(usersAuth.getUserAccountType(vendorId)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.couriersCourierIdOrdersGet(vendorId, userId));
        assertEquals(exception.getStatus(), HttpStatus.NOT_FOUND);
        assertEquals(exception.getReason(), "The id is not recognised as courier.");
    }

    @Test
    void couriersCourierIdOrdersGetBadRequest() {
        String userId = "invalid@testmail.com";
        String vendorId = "vendor@testmail.com";

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.couriersCourierIdOrdersGet(null, userId));
        assertEquals(exception.getStatus(), HttpStatus.BAD_REQUEST);
    }
    @Test
    void couriersCourierIdOrdersGetBadRequest2() {
        String userId = "invalid@testmail.com";
        String vendorId = "vendor@testmail.com";

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.couriersCourierIdOrdersGet(userId, null));
        assertEquals(exception.getStatus(), HttpStatus.BAD_REQUEST);

    }

    @Test
    void couriersCourierIdOrdersGetOK() {
        String userId = "invalid@testmail.com";
        String courierId = "courier@testmail.com";
        String otherCourierId = "other_courier@testmail.com";
        when(usersAuth.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.ADMIN);
        when(usersAuth.getUserAccountType(courierId)).thenReturn(UsersAuthenticationService.AccountType.COURIER);

        Delivery d1 = new Delivery();
        UUID orderId1 = UUID.randomUUID();
        d1.setStatus(DeliveryStatus.DELIVERED);
        d1.setCustomerID("user@testmail.com");
        d1.setRestaurantID("hi_im_a_vendor@testmail.com");
        d1.setCourierID(courierId);
        d1.setDeliveryID(orderId1);
        d1.setDeliveryAddress(List.of(50.4, 32.6));

        Delivery d2 = new Delivery();
        UUID orderId2 = UUID.randomUUID();
        d2.setStatus(DeliveryStatus.DELIVERED);
        d2.setCustomerID("user@testmail.com");
        d2.setRestaurantID("hi_im_a_vendor@testmail.com");
        d2.setCourierID(courierId);
        d2.setDeliveryID(orderId2);
        d2.setDeliveryAddress(List.of(50.4, 32.6));

        Delivery d3 = new Delivery();
        UUID orderId3 = UUID.randomUUID();
        d3.setStatus(DeliveryStatus.DELIVERED);
        d3.setCustomerID("user@testmail.com");
        d3.setRestaurantID("hi_im_a_vendor@testmail.com");
        d3.setCourierID(otherCourierId);
        d3.setDeliveryID(orderId3);
        d3.setDeliveryAddress(List.of(50.4, 32.6));

        dr.save(d1);
        dr.save(d2);
        dr.save(d3);

        ResponseEntity<List<UUID>> result = sut.couriersCourierIdOrdersGet(courierId, userId);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, Objects.requireNonNull(result.getBody()).size());
        assertTrue(result.getBody().containsAll(List.of(orderId1, orderId2)));
    }
}
