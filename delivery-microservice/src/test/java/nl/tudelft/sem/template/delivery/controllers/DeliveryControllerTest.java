package nl.tudelft.sem.template.delivery.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.transaction.Transactional;
import nl.tudelft.sem.template.delivery.GPS;
import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.RestaurantRepository;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.delivery.services.RestaurantService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.model.DeliveriesPostRequest;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import nl.tudelft.sem.template.model.Error;
import nl.tudelft.sem.template.model.ErrorType;
import nl.tudelft.sem.template.model.Restaurant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EntityScan("nl.tudelft.sem.template.*")
@Transactional
@DataJpaTest
@ExtendWith(MockitoExtension.class)

class DeliveryControllerTest {

    public UsersAuthenticationService usersCommunication;

    private DeliveryController sut;
    @Autowired
    private DeliveryRepository repo1;
    @Autowired
    private RestaurantRepository repo2;
    private RestaurantController restaurantController;

    String userId;
    UsersAuthenticationService.AccountType userType;
    UUID deliveryId;
    Integer prepTime;
    Delivery delivery;
    List<Double> coords;


    @BeforeEach
    void setUp() {
        coords = List.of(100.0, 100.0);
        userId = "user@example.org";
        deliveryId = UUID.randomUUID();
        prepTime = 25;
        delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setEstimatedPrepTime(prepTime);
        usersCommunication = mock(UsersAuthenticationService.class);

        restaurantController = new RestaurantController(new RestaurantService(repo2, repo1), usersCommunication);
        sut = new DeliveryController(new DeliveryService(repo1, new GPS(), repo2), usersCommunication, null);
    }

    @Test
    void deliveriesDeliveryIdPrepPut() {
        // Mock data
        userType = UsersAuthenticationService.AccountType.VENDOR;

        // Mock ratings and user type
        when(usersCommunication.getUserAccountType(userId)).thenReturn(userType);
        when(usersCommunication.checkUserAccessToDelivery(userId, delivery)).thenReturn(true);
        sut.insert(delivery);
        // Call the method
        ResponseEntity<Delivery> responseEntity = sut.deliveriesDeliveryIdPrepPut(deliveryId, userId, prepTime);

        // Verify the response
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        // Verify the returned delivery
        Delivery returned = responseEntity.getBody();
        assertEquals(returned, delivery);

        // Verify that we called the service methods and checked the user type
        verify(usersCommunication, times(1)).getUserAccountType(userId);
    }

    @Test
    void deliveriesDeliveryIdPrepPutForbidden() {
        // Mock data
        userType = UsersAuthenticationService.AccountType.COURIER;
        sut.insert(delivery);

        // Mock ratings and user type
        when(usersCommunication.getUserAccountType(userId)).thenReturn(userType);

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdPrepPut(deliveryId, userId, prepTime))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdPrepPut(deliveryId, userId, prepTime))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");

        verify(usersCommunication, times(2)).getUserAccountType(userId);
    }

    @Test
    void deliveriesDeliveryIdPrepPutUnauthorized() {
        // Mock data
        userType = UsersAuthenticationService.AccountType.INVALID;
        sut.insert(delivery);

        // Mock ratings and user type
        when(usersCommunication.getUserAccountType(userId)).thenReturn(userType);

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdPrepPut(deliveryId, userId, prepTime))
            .extracting("status")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdPrepPut(deliveryId, userId, prepTime))
            .message()
            .isEqualTo("401 UNAUTHORIZED \"User lacks valid authentication credentials.\"");

        verify(usersCommunication, times(2)).getUserAccountType(userId);
    }

    @Test
    void deliveriesDeliveryIdRatingCourierPutCustomer() {
        //Mock data
        UUID deliveryId = UUID.randomUUID();
        String userId = "hi_im_a_customer@testmail.com";
        String courierId = "hi_im_a_courier@testmail.com";
        String restaurantId = "hi_im_a_vendor@testmail.com";
        Delivery m = new Delivery();
        m.setDeliveryID(deliveryId);
        m.setCourierID(courierId);
        m.setRestaurantID(restaurantId);
        m.setCustomerID(userId);
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.CLIENT;

        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);
        sut.insert(m);
        ResponseEntity<Delivery> result = sut.deliveriesDeliveryIdRatingCourierPut(deliveryId, userId, 5);

        Delivery resultBody = result.getBody();
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assert resultBody != null;
        assertEquals(5, resultBody.getRatingCourier());

        verify(usersCommunication, times(1)).getUserAccountType(userId);
    }

    @Test
    void deliveriesDeliveryIdRatingCourierPutAdmin() {
        // Mock data
        UUID deliveryId = UUID.randomUUID();
        String customerId = "hi_im_a_customer@testmail.com";
        String userId = "hi_im_an_admin@testmail.com";
        String courierId = "hi_im_a_courier@testmail.com";
        String restaurantId = "hi_im_a_vendor@testmail.com";
        Delivery m = new Delivery();
        m.setDeliveryID(deliveryId);
        m.setCourierID(courierId);
        m.setRestaurantID(restaurantId);
        m.setCustomerID(customerId);
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.ADMIN;

        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);
        sut.insert(m);
        ResponseEntity<Delivery> result = sut.deliveriesDeliveryIdRatingCourierPut(deliveryId, userId, 0);

        Delivery resultBody = result.getBody();
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assert resultBody != null;
        assertEquals(0, resultBody.getRatingCourier());

        verify(usersCommunication, times(1)).getUserAccountType(userId);
    }

    @Test
    void deliveriesDeliveryIdRatingCourierPutCourier() {
        //Mock data
        UUID deliveryId = UUID.randomUUID();
        String customerId = "hi_im_a_customer@testmail.com";
        String courierId = "hi_im_a_courier@testmail.com";
        String restaurantId = "hi_im_a_vendor@testmail.com";
        Delivery m = new Delivery();
        m.setDeliveryID(deliveryId);
        m.setCourierID(courierId);
        m.setRestaurantID(restaurantId);
        m.setCustomerID(customerId);
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.COURIER;
        Integer rating = 5;

        when(usersCommunication.getUserAccountType(courierId)).thenReturn(type);
        sut.insert(m);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdRatingCourierPut(deliveryId, courierId, rating))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdRatingCourierPut(deliveryId, courierId, rating))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");

        verify(usersCommunication, times(2)).getUserAccountType(courierId);
    }

    @Test
    void deliveriesDeliveryIdRatingRestaurantPutCustomer() {
        //Mock data
        UUID deliveryId = UUID.randomUUID();
        String userId = "hi_im_a_customer@testmail.com";
        String courierId = "hi_im_a_courier@testmail.com";
        String restaurantId = "hi_im_a_vendor@testmail.com";
        Delivery m = new Delivery();
        m.setDeliveryID(deliveryId);
        m.setCourierID(courierId);
        m.setRestaurantID(restaurantId);
        m.setCustomerID(userId);
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.CLIENT;

        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);
        sut.insert(m);
        ResponseEntity<Delivery> result = sut.deliveriesDeliveryIdRatingRestaurantPut(deliveryId, userId, 5);

        Delivery resultBody = result.getBody();
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assert resultBody != null;
        assertEquals(5, resultBody.getRatingRestaurant());

        verify(usersCommunication, times(1)).getUserAccountType(userId);
    }

    @Test
    void deliveriesDeliveryIdRatingRestaurantPutDiffCustomer() {
        // Mock data
        UUID deliveryId = UUID.randomUUID();
        String customerId = "hi_im_a_customer@testmail.com";
        String userId = "hi_im_a_different_customer@testmail.com";
        String courierId = "hi_im_a_courier@testmail.com";
        String restaurantId = "hi_im_a_vendor@testmail.com";
        Delivery m = new Delivery();
        m.setDeliveryID(deliveryId);
        m.setCourierID(courierId);
        m.setRestaurantID(restaurantId);
        m.setCustomerID(customerId);
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.CLIENT;
        Integer rating = 5;

        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);
        sut.insert(m);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdRatingRestaurantPut(deliveryId, userId, rating))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdRatingRestaurantPut(deliveryId, userId, rating))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");

        verify(usersCommunication, times(2)).getUserAccountType(userId);
    }

    @Test
    void deliveriesDeliveryIdRatingRestaurantPutAdmin() {
        //Mock data
        UUID deliveryId = UUID.randomUUID();
        String customerId = "hi_im_a_customer@testmail.com";
        String userId = "hi_im_an_admin@testmail.com";
        String courierId = "hi_im_a_courier@testmail.com";
        String restaurantId = "hi_im_a_vendor@testmail.com";
        Delivery m = new Delivery();
        m.setDeliveryID(deliveryId);
        m.setCourierID(courierId);
        m.setRestaurantID(restaurantId);
        m.setCustomerID(customerId);
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.ADMIN;

        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);
        sut.insert(m);
        ResponseEntity<Delivery> result = sut.deliveriesDeliveryIdRatingRestaurantPut(deliveryId, userId, 0);

        Delivery resultBody = result.getBody();
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assert resultBody != null;
        assertEquals(0, resultBody.getRatingRestaurant());

        verify(usersCommunication, times(1)).getUserAccountType(userId);
    }

    @Test
    void deliveriesDeliveryIdRatingRestaurantPutVendor() {
        //Mock data
        UUID deliveryId = UUID.randomUUID();
        String customerId = "hi_im_a_customer@testmail.com";
        String userId = "hi_im_a_different_customer@testmail.com";
        String courierId = "hi_im_a_courier@testmail.com";
        String restaurantId = "hi_im_a_vendor@testmail.com";
        Delivery m = new Delivery();
        m.setDeliveryID(deliveryId);
        m.setCourierID(courierId);
        m.setRestaurantID(restaurantId);
        m.setCustomerID(customerId);
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.CLIENT;
        Integer rating = 5;

        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);
        sut.insert(m);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdRatingRestaurantPut(deliveryId, userId, rating))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdRatingRestaurantPut(deliveryId, userId, rating))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");

        verify(usersCommunication, times(2)).getUserAccountType(userId);
    }

    @Test
    void deliveriesDeliveryIdRatingRestaurantGetVendor() {
        //Mock data
        UUID deliveryId = UUID.randomUUID();
        String customerId = "hi_im_a_customer@testmail.com";
        String courierId = "hi_im_a_courier@testmail.com";
        String restaurantId = "hi_im_a_vendor@testmail.com";
        Delivery m = new Delivery();
        m.setDeliveryID(deliveryId);
        m.setCourierID(courierId);
        m.setRestaurantID(restaurantId);
        m.setCustomerID(customerId);
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.VENDOR;

        when(usersCommunication.getUserAccountType(restaurantId)).thenReturn(type);
        when(usersCommunication.checkUserAccessToDelivery(restaurantId, m)).thenReturn(true);
        sut.insert(m);
        ResponseEntity<Integer> result = sut.deliveriesDeliveryIdRatingRestaurantGet(deliveryId, restaurantId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNull(result.getBody());

        verify(usersCommunication, times(1)).getUserAccountType(restaurantId);
    }

    @Test
    void deliveriesDeliveryIdRatingRestaurantGetDiffVendor() {
        //Mock data
        UUID deliveryId = UUID.randomUUID();
        String customerId = "hi_im_a_customer@testmail.com";
        String courierId = "hi_im_a_courier@testmail.com";
        String vendorId = "hi_im_a_vendor@testmail.com";
        String restaurantId = "hi_im_a_rival_vendor@testmail.com";
        Delivery m = new Delivery();
        m.setDeliveryID(deliveryId);
        m.setCourierID(courierId);
        m.setRestaurantID(vendorId);
        m.setCustomerID(customerId);
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.VENDOR;

        when(usersCommunication.getUserAccountType(restaurantId)).thenReturn(type);
        sut.insert(m);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdRatingRestaurantGet(deliveryId, restaurantId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdRatingRestaurantGet(deliveryId, restaurantId))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");

        verify(usersCommunication, times(2)).getUserAccountType(restaurantId);
    }

    @Test
    void deliveriesDeliveryIdRatingRestaurantGetCustomer() {
        //Mock data
        UUID deliveryId = UUID.randomUUID();
        String customerId = "hi_im_a_customer@testmail.com";
        String courierId = "hi_im_a_courier@testmail.com";
        String vendorId = "hi_im_a_vendor@testmail.com";
        Delivery m = new Delivery();
        m.setDeliveryID(deliveryId);
        m.setCourierID(courierId);
        m.setRestaurantID(vendorId);
        m.setCustomerID(customerId);
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.CLIENT;

        when(usersCommunication.getUserAccountType(customerId)).thenReturn(type);
        when(usersCommunication.checkUserAccessToDelivery(customerId, m)).thenReturn(true);

        sut.insert(m);
        ResponseEntity<Integer> result = sut.deliveriesDeliveryIdRatingRestaurantGet(deliveryId, customerId);

        assertEquals(HttpStatus.OK, result.getStatusCode());

        verify(usersCommunication, times(1)).getUserAccountType(customerId);
    }

    @Test
    void deliveriesDeliveryIdRatingCourierGetCourier() {
        //Mock data
        UUID deliveryId = UUID.randomUUID();
        String customerId = "hi_im_a_customer@testmail.com";
        String courierId = "hi_im_a_courier@testmail.com";
        String vendorId = "hi_im_a_vendor@testmail.com";
        Delivery m = new Delivery();
        m.setDeliveryID(deliveryId);
        m.setCourierID(courierId);
        m.setRestaurantID(vendorId);
        m.setCustomerID(customerId);
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.COURIER;

        when(usersCommunication.getUserAccountType(courierId)).thenReturn(type);
        when(usersCommunication.checkUserAccessToDelivery(courierId, m)).thenReturn(true);

        sut.insert(m);
        ResponseEntity<Integer> result = sut.deliveriesDeliveryIdRatingCourierGet(deliveryId, courierId);

        assertEquals(HttpStatus.OK, result.getStatusCode());

        verify(usersCommunication, times(1)).getUserAccountType(courierId);
    }

    @Test
    void deliveriesDeliveryIdRatingCourierGetDiffCourier() {
        //Mock data
        UUID deliveryId = UUID.randomUUID();
        String customerId = "hi_im_a_customer@testmail.com";
        String courierId = "hi_im_a_courier@testmail.com";
        String diffCourierId = "hi_im_a_different_courier@testmail.com";
        String vendorId = "hi_im_a_vendor@testmail.com";
        Delivery m = new Delivery();
        m.setDeliveryID(deliveryId);
        m.setCourierID(courierId);
        m.setRestaurantID(vendorId);
        m.setCustomerID(customerId);
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.COURIER;

        when(usersCommunication.getUserAccountType(diffCourierId)).thenReturn(type);
        when(usersCommunication.checkUserAccessToDelivery(diffCourierId, m)).thenReturn(false);
        sut.insert(m);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdRatingCourierGet(deliveryId, diffCourierId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdRatingCourierGet(deliveryId, diffCourierId))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");

        verify(usersCommunication, times(2)).getUserAccountType(diffCourierId);
    }

    @Test
    void deliveriesDeliveryIdRatingCourierGetCustomer() {
        //Mock data
        UUID deliveryId = UUID.randomUUID();
        String customerId = "hi_im_a_customer@testmail.com";
        String courierId = "hi_im_a_courier@testmail.com";
        String vendorId = "hi_im_a_vendor@testmail.com";
        Delivery m = new Delivery();
        m.setDeliveryID(deliveryId);
        m.setCourierID(courierId);
        m.setRestaurantID(vendorId);
        m.setCustomerID(customerId);
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.CLIENT;

        when(usersCommunication.getUserAccountType(customerId)).thenReturn(type);
        when(usersCommunication.checkUserAccessToDelivery(customerId, m)).thenReturn(true);
        sut.insert(m);

        ResponseEntity<Integer> result = sut.deliveriesDeliveryIdRatingCourierGet(deliveryId, customerId);

        assertEquals(HttpStatus.OK, result.getStatusCode());

        verify(usersCommunication, times(1)).getUserAccountType(customerId);
    }

    @Test
    void deliveriesAllAcceptedGetAdmin() {
        UUID deliveryId1 = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        String courierId = "courier@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId1);
        m1.setCourierID(null);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        UUID deliveryId2 = UUID.randomUUID();
        Delivery m2 = new Delivery();
        m2.setDeliveryID(deliveryId2);
        m2.setCourierID(courierId);
        m2.setRestaurantID(vendorId);
        m2.setCustomerID(customerId);
        sut.insert(m2);

        String userId = "user@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.ADMIN;

        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        ResponseEntity<List<Delivery>> result = sut.deliveriesAllAcceptedGet(userId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assert (Objects.requireNonNull(result.getBody()).contains(m1));
        assert (!result.getBody().contains(m2));

        verify(usersCommunication, times(1)).getUserAccountType(userId);
    }

    @Test
    void deliveriesAllAcceptedGetCourier() {
        UUID deliveryId1 = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        String courierId = "courier@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId1);
        m1.setCourierID(null);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        UUID deliveryId2 = UUID.randomUUID();
        Delivery m2 = new Delivery();
        m2.setDeliveryID(deliveryId2);
        m2.setCourierID(courierId);
        m2.setRestaurantID(vendorId);
        m2.setCustomerID(customerId);
        sut.insert(m2);

        String userId = "user@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.COURIER;

        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        ResponseEntity<List<Delivery>> result = sut.deliveriesAllAcceptedGet(userId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assert (Objects.requireNonNull(result.getBody()).contains(m1));
        assert (!result.getBody().contains(m2));

        verify(usersCommunication, times(1)).getUserAccountType(userId);
    }

    @Test
    void deliveriesAllAcceptedGetCustomer() {
        UUID deliveryId1 = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId1);
        m1.setCourierID(null);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        String userId = "user@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.CLIENT;

        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> sut.deliveriesAllAcceptedGet(userId));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("User lacks necessary permissions.", exception.getReason());

        verify(usersCommunication, times(1)).getUserAccountType(userId);
    }

    @Test
    void deliveriesAllAcceptedGetVendor() {
        UUID deliveryId1 = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId1);
        m1.setCourierID(null);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        String userId = "user@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.VENDOR;

        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> sut.deliveriesAllAcceptedGet(userId));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("User lacks necessary permissions.", exception.getReason());

        verify(usersCommunication, times(1)).getUserAccountType(userId);
    }

    @Test
    void deliveriesAllAcceptedGetInExistent() {
        UUID deliveryId1 = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId1);
        m1.setCourierID(null);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        String userId = "user@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.INVALID;

        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> sut.deliveriesAllAcceptedGet(userId));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("User lacks valid authentication credentials.", exception.getReason());

        verify(usersCommunication, times(1)).getUserAccountType(userId);
    }

    @Test
    void deliveriesDeliveryIdCourierGetAdmin() {
        UUID deliveryId = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        String courierId = "courier@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId);
        m1.setCourierID(courierId);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        String userId = "user@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.ADMIN;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        ResponseEntity<String> res = sut.deliveriesDeliveryIdCourierGet(deliveryId, userId);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(res.getBody(), courierId);
        verify(usersCommunication, times(1)).getUserAccountType(userId);
    }

    @Test
    void deliveriesDeliveryIdCourierGetVendor() {
        UUID deliveryId = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        String courierId = "courier@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId);
        m1.setCourierID(courierId);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        String userId = "user@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.VENDOR;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);
        when(usersCommunication.getUserAccountType(vendorId)).thenReturn(type);
        when(usersCommunication.checkUserAccessToDelivery(vendorId, m1)).thenReturn(true);

        // Vendor checks the courier for their own order
        ResponseEntity<String> res1 = sut.deliveriesDeliveryIdCourierGet(deliveryId, vendorId);
        assertEquals(HttpStatus.OK, res1.getStatusCode());
        verify(usersCommunication, times(1)).getUserAccountType(vendorId);
        assertEquals(res1.getBody(), courierId);

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCourierGet(deliveryId, userId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCourierGet(deliveryId, userId))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");
        verify(usersCommunication, times(2)).getUserAccountType(userId);
    }

    @Test
    void deliveriesDeliveryIdCourierGetCustomer() {
        UUID deliveryId = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        String courierId = "courier@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId);
        m1.setCourierID(courierId);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        String userId = "user@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.CLIENT;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCourierGet(deliveryId, userId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCourierGet(deliveryId, userId))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");

        verify(usersCommunication, times(2)).getUserAccountType(userId);
    }

    @Test
    void deliveriesDeliveryIdCourierGetInExistent() {
        UUID deliveryId = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        String courierId = "courier@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId);
        m1.setCourierID(courierId);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        String userId = "user@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.INVALID;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCourierGet(deliveryId, userId))
            .extracting("status")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCourierGet(deliveryId, userId))
            .message()
            .isEqualTo("401 UNAUTHORIZED \"User lacks valid authentication credentials.\"");
        verify(usersCommunication, times(2)).getUserAccountType(userId);
    }

    @Test
    void deliveriesDeliveryIdCourierGetCourier() {
        String courierId = "courier@testmail.com";
        String otherCourierId = "othercourier@testmail.com";

        UUID deliveryId = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId);
        m1.setCourierID(null);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        UUID deliveryId2 = UUID.randomUUID();
        Delivery m2 = new Delivery();
        m2.setDeliveryID(deliveryId2);
        m2.setCourierID(courierId);
        m2.setRestaurantID(vendorId);
        m2.setCustomerID(customerId);
        sut.insert(m2);

        UUID deliveryId3 = UUID.randomUUID();
        Delivery m3 = new Delivery();
        m3.setDeliveryID(deliveryId3);
        m3.setCourierID(otherCourierId);
        m3.setRestaurantID(vendorId);
        m3.setCustomerID(customerId);
        sut.insert(m3);

        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.COURIER;
        when(usersCommunication.getUserAccountType(courierId)).thenReturn(type);
        when(usersCommunication.checkUserAccessToDelivery(courierId, m2)).thenReturn(true);

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCourierGet(deliveryId, courierId))
            .extracting("status")
            .isEqualTo(HttpStatus.NOT_FOUND);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCourierGet(deliveryId, courierId))
            .message()
            .isEqualTo("404 NOT_FOUND \"No courier assigned to order\"");
        verify(usersCommunication, times(2)).getUserAccountType(any());

        ResponseEntity<String> res2 = sut.deliveriesDeliveryIdCourierGet(deliveryId2, courierId);
        assertEquals(HttpStatus.OK, res2.getStatusCode());
        assertEquals(res2.getBody(), courierId);
        verify(usersCommunication, times(3)).getUserAccountType(courierId);

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCourierGet(deliveryId3, courierId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCourierGet(deliveryId3, courierId))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");
        verify(usersCommunication, times(5)).getUserAccountType(courierId);
    }

    @Test
    void deliveriesDeliveryIdCourierPutNonCourier() {
        UUID deliveryId = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        String courierId = "courier@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId);
        m1.setCourierID(courierId);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        String userId = "user@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.CLIENT;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);
        when(usersCommunication.getUserAccountType(courierId)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCourierPut(deliveryId, userId, courierId))
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCourierPut(deliveryId, userId, courierId))
            .message()
            .isEqualTo("400 BAD_REQUEST \"The person you are trying to assign to the order is not a courier.\"");
        verify(usersCommunication, times(4)).getUserAccountType(any());
    }

    @Test
    void deliveriesDeliveryIdCourierPutCustomer() {
        UUID deliveryId = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        String courierId = "courier@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId);
        m1.setCourierID(courierId);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        String userId = "user@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.CLIENT;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);
        when(usersCommunication.getUserAccountType(courierId)).thenReturn(UsersAuthenticationService.AccountType.COURIER);

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCourierPut(deliveryId, userId, courierId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCourierPut(deliveryId, userId, courierId))
            .message()
            .isEqualTo("403 FORBIDDEN \"Delivery already has a courier assigned.\"");
        verify(usersCommunication, times(4)).getUserAccountType(any());
    }

    @Test
    void deliveriesDeliveryIdCourierPutInExistent() {
        UUID deliveryId = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        String courierId = "courier@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId);
        m1.setCourierID(courierId);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        String userId = "user@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.INVALID;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);
        when(usersCommunication.getUserAccountType(courierId)).thenReturn(UsersAuthenticationService.AccountType.COURIER);

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCourierPut(deliveryId, userId, courierId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCourierPut(deliveryId, userId, courierId))
            .message()
            .isEqualTo("403 FORBIDDEN \"Delivery already has a courier assigned.\"");
        verify(usersCommunication, times(4)).getUserAccountType(any());
    }

    @Test
    void deliveriesDeliveryIdCourierPutAdmin() {
        UUID deliveryId = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        String courierId = "courier@testmail.com";
        String otherCourierId = "otherCourier@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId);
        m1.setCourierID(courierId);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        String userId = "user@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.ADMIN;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);
        when(usersCommunication.getUserAccountType(otherCourierId))
                .thenReturn(UsersAuthenticationService.AccountType.COURIER);

        ResponseEntity<Delivery> res = sut.deliveriesDeliveryIdCourierPut(deliveryId, userId, otherCourierId);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(Objects.requireNonNull(res.getBody()).getCourierID(), otherCourierId);
        verify(usersCommunication, times(2)).getUserAccountType(any());
    }

    @Test
    void deliveriesDeliveryIdCourierPutCourier() {
        UUID deliveryId = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        String otherCourierId = "otherCourier@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId);
        m1.setCourierID(null);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        UUID deliveryId2 = UUID.randomUUID();
        Delivery m2 = new Delivery();
        m2.setDeliveryID(deliveryId2);
        m2.setCourierID(otherCourierId);
        m2.setRestaurantID(vendorId);
        m2.setCustomerID(customerId);
        sut.insert(m2);

        String userId = "user@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.COURIER;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        ResponseEntity<Delivery> res = sut.deliveriesDeliveryIdCourierPut(deliveryId, userId, userId);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(Objects.requireNonNull(res.getBody()).getCourierID(), userId);
        verify(usersCommunication, times(2)).getUserAccountType(any());

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCourierPut(deliveryId2, userId, userId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCourierPut(deliveryId2, userId, userId))
            .message()
            .isEqualTo("403 FORBIDDEN \"Delivery already has a courier assigned.\"");
        verify(usersCommunication, times(6)).getUserAccountType(any());
    }

    @Test
    void deliveriesDeliveryIdCourierPutVendor() {
        UUID deliveryId = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        String courierId = "courier7@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId);
        m1.setCourierID(null);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        UUID deliveryId2 = UUID.randomUUID();
        Delivery m2 = new Delivery();
        m2.setDeliveryID(deliveryId2);
        m2.setCourierID(courierId);
        m2.setRestaurantID(vendorId);
        m2.setCustomerID(customerId);
        sut.insert(m2);

        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantID(vendorId);
        restaurant.setLocation(List.of(95.5, 56.5));
        String courierInList = "courier1@testmail.com";
        restaurant.setCouriers(List.of(courierInList));
        repo2.save(restaurant);

        String userId = "user@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.VENDOR;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);
        when(usersCommunication.getUserAccountType(vendorId))
            .thenReturn(type);
        when(usersCommunication.getUserAccountType(courierId))
            .thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersCommunication.getUserAccountType(courierInList))
            .thenReturn(UsersAuthenticationService.AccountType.COURIER);

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCourierPut(deliveryId, userId, courierId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCourierPut(deliveryId, userId, courierId))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCourierPut(deliveryId, vendorId, courierId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCourierPut(deliveryId, vendorId, courierId))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCourierPut(deliveryId2, vendorId, courierId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCourierPut(deliveryId2, vendorId, courierId))
            .message()
            .isEqualTo("403 FORBIDDEN \"Delivery already has a courier assigned.\"");

        ResponseEntity<Delivery> res4 = sut.deliveriesDeliveryIdCourierPut(deliveryId, vendorId, courierInList);
        assertEquals(HttpStatus.OK, res4.getStatusCode());
        assertEquals(Objects.requireNonNull(res4.getBody()).getCourierID(), courierInList);
    }


    @Test
    void deliveriesPostNull() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> sut.deliveriesPost(null));
        assertEquals(exception.getStatus(), HttpStatus.BAD_REQUEST);
        assertEquals("Delivery is invalid.", exception.getReason());
    }

    @Test
    void deliveriesPostInvalidStatus() {
        DeliveriesPostRequest dpr = new DeliveriesPostRequest();
        dpr.setStatus("invalid");
        dpr.setCustomerId("user@testmail.com");
        dpr.setVendorId("hi_im_a_vendor@testmail.com");
        dpr.setOrderId(UUID.randomUUID().toString());
        dpr.setDeliveryAddress(List.of(50.4, 32.6));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> sut.deliveriesPost(dpr));
        assertEquals(exception.getMessage(), "Unexpected value 'INVALID'");
    }

    @Test
    void deliveriesPostEmptyEmail() {
        DeliveriesPostRequest dpr = new DeliveriesPostRequest();
        dpr.setStatus("pending");
        dpr.setCustomerId("user@testmail.com");
        dpr.setOrderId(UUID.randomUUID().toString());
        dpr.setDeliveryAddress(List.of(50.4, 32.6));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> sut.deliveriesPost(dpr));
        assertEquals(exception.getStatus(), HttpStatus.BAD_REQUEST);
        assertEquals("Restaurant ID, customer ID or Delivery ID is invalid.", exception.getReason());
    }

    @Test
    void deliveriesPostEmptyAddress() {
        DeliveriesPostRequest dpr = new DeliveriesPostRequest();
        dpr.setStatus("pending");
        dpr.setVendorId("hi_im_a_vendor@testmail.com");
        dpr.setCustomerId("user@testmail.com");
        dpr.setOrderId(UUID.randomUUID().toString());
        dpr.setDeliveryAddress(new ArrayList<>());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> sut.deliveriesPost(dpr));
        assertEquals(exception.getStatus(), HttpStatus.BAD_REQUEST);
        assertEquals("Address is invalid.", exception.getReason());
    }

    @Test
    void deliveriesPostNullAddress() {
        DeliveriesPostRequest dpr = new DeliveriesPostRequest();
        dpr.setStatus("pending");
        dpr.setVendorId("hi_im_a_vendor@testmail.com");
        dpr.setCustomerId("user@testmail.com");
        dpr.setOrderId(UUID.randomUUID().toString());
        dpr.setDeliveryAddress(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> sut.deliveriesPost(dpr));
        assertEquals(exception.getStatus(), HttpStatus.BAD_REQUEST);
        assertEquals(exception.getReason(), "Address is invalid.");
    }

    @Test
    void deliveriesPostVendorNotFound() {
        DeliveriesPostRequest dpr = new DeliveriesPostRequest();
        dpr.setStatus("pending");
        dpr.setVendorId("hi_im_a_vendor@testmail.com");
        dpr.setCustomerId("user@testmail.com");
        dpr.setOrderId(UUID.randomUUID().toString());
        dpr.setDeliveryAddress(List.of(40.0, 30.0));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> sut.deliveriesPost(dpr));
        assertEquals(exception.getStatus(), HttpStatus.NOT_FOUND);
        assertEquals(exception.getReason(), "VENDOR NOT FOUND.");
    }

    @Test
    void deliveriesPostOutOfDeliveryZone() {
        DeliveriesPostRequest dpr = new DeliveriesPostRequest();
        String orderId = UUID.randomUUID().toString();
        dpr.setStatus("accepted");
        dpr.setCustomerId("user@testmail.com");
        dpr.setVendorId("hi_im_a_vendor@testmail.com");
        dpr.setOrderId(orderId);
        dpr.setDeliveryAddress(List.of(50.4, 32.6));

        Restaurant r = new Restaurant();
        r.setLocation(List.of(40.1, 32.4));
        r.setRestaurantID("hi_im_a_vendor@testmail.com");

        repo2.save(r);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> sut.deliveriesPost(dpr));
        assertEquals(exception.getStatus(), HttpStatus.BAD_REQUEST);
        assertEquals(exception.getReason(), "CUSTOMER OUTSIDE THE VENDOR DELIVERY ZONE.");
    }

    @Test
    void deliveriesPostPending() {
        DeliveriesPostRequest dpr = new DeliveriesPostRequest();
        String orderId = UUID.randomUUID().toString();
        dpr.setStatus("pending");
        dpr.setCustomerId("user@testmail.com");
        dpr.setVendorId("hi_im_a_vendor@testmail.com");
        dpr.setOrderId(orderId);
        dpr.setDeliveryAddress(List.of(50.4, 32.6));

        Restaurant r = new Restaurant();
        r.setLocation(List.of(50.3, 32.4));
        r.setRestaurantID("hi_im_a_vendor@testmail.com");

        repo2.save(r);
        ResponseEntity<Delivery> result = sut.deliveriesPost(dpr);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(Objects.requireNonNull(result.getBody()).getDeliveryID().toString(), orderId);
    }

    @Test
    void deliveriesPostAccepted() {
        DeliveriesPostRequest dpr = new DeliveriesPostRequest();
        String orderId = UUID.randomUUID().toString();
        dpr.setStatus("accepted");
        dpr.setCustomerId("user@testmail.com");
        dpr.setVendorId("hi_im_a_vendor@testmail.com");
        dpr.setOrderId(orderId);
        dpr.setDeliveryAddress(List.of(50.4, 32.6));

        Restaurant r = new Restaurant();
        r.setLocation(List.of(50.3, 32.4));
        r.setRestaurantID("hi_im_a_vendor@testmail.com");

        repo2.save(r);
        ResponseEntity<Delivery> result = sut.deliveriesPost(dpr);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(Objects.requireNonNull(result.getBody()).getDeliveryID().toString(), orderId);
    }

    @Test
    void deliveriesPostRejected() {
        DeliveriesPostRequest dpr = new DeliveriesPostRequest();
        String orderId = UUID.randomUUID().toString();
        dpr.setStatus("Rejected");
        dpr.setCustomerId("user@testmail.com");
        dpr.setVendorId("hi_im_a_vendor@testmail.com");
        dpr.setOrderId(orderId);
        dpr.setDeliveryAddress(List.of(50.4, 32.6));

        Restaurant r = new Restaurant();
        r.setLocation(List.of(50.3, 32.4));
        r.setRestaurantID("hi_im_a_vendor@testmail.com");

        repo2.save(r);
        ResponseEntity<Delivery> result = sut.deliveriesPost(dpr);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(Objects.requireNonNull(result.getBody()).getDeliveryID().toString(), orderId);
    }

    @Test
    void deliveriesPostPreparing() {
        DeliveriesPostRequest dpr = new DeliveriesPostRequest();
        String orderId = UUID.randomUUID().toString();
        dpr.setStatus("PREPARING");
        dpr.setCustomerId("user@testmail.com");
        dpr.setVendorId("hi_im_a_vendor@testmail.com");
        dpr.setOrderId(orderId);
        dpr.setDeliveryAddress(List.of(50.4, 32.6));

        Restaurant r = new Restaurant();
        r.setLocation(List.of(50.3, 32.4));
        r.setRestaurantID("hi_im_a_vendor@testmail.com");

        repo2.save(r);
        ResponseEntity<Delivery> result = sut.deliveriesPost(dpr);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(Objects.requireNonNull(result.getBody()).getDeliveryID().toString(), orderId);
    }

    @Test
    void deliveriesPostGivenToCourier() {
        DeliveriesPostRequest dpr = new DeliveriesPostRequest();
        String orderId = UUID.randomUUID().toString();
        dpr.setStatus("Given_to_Courier");
        dpr.setCustomerId("user@testmail.com");
        dpr.setVendorId("hi_im_a_vendor@testmail.com");
        dpr.setOrderId(orderId);
        dpr.setDeliveryAddress(List.of(50.4, 32.6));

        Restaurant r = new Restaurant();
        r.setLocation(List.of(50.3, 32.4));
        r.setRestaurantID("hi_im_a_vendor@testmail.com");

        repo2.save(r);
        ResponseEntity<Delivery> result = sut.deliveriesPost(dpr);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(Objects.requireNonNull(result.getBody()).getDeliveryID().toString(), orderId);
    }

    @Test
    void deliveriesPostOnTransit() {
        DeliveriesPostRequest dpr = new DeliveriesPostRequest();
        String orderId = UUID.randomUUID().toString();
        dpr.setStatus("ON_TRANSIT");
        dpr.setCustomerId("user@testmail.com");
        dpr.setVendorId("hi_im_a_vendor@testmail.com");
        dpr.setOrderId(orderId);
        dpr.setDeliveryAddress(List.of(50.4, 32.6));

        Restaurant r = new Restaurant();
        r.setLocation(List.of(50.3, 32.4));
        r.setRestaurantID("hi_im_a_vendor@testmail.com");

        repo2.save(r);
        ResponseEntity<Delivery> result = sut.deliveriesPost(dpr);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(Objects.requireNonNull(result.getBody()).getDeliveryID().toString(), orderId);
    }

    @Test
    void deliveriesPostDelivered() {
        DeliveriesPostRequest dpr = new DeliveriesPostRequest();
        String orderId = UUID.randomUUID().toString();
        dpr.setStatus("delivered");
        dpr.setCustomerId("user@testmail.com");
        dpr.setVendorId("hi_im_a_vendor@testmail.com");
        dpr.setOrderId(orderId);
        dpr.setDeliveryAddress(List.of(50.4, 32.6));

        Restaurant r = new Restaurant();
        r.setLocation(List.of(50.3, 32.4));
        r.setRestaurantID("hi_im_a_vendor@testmail.com");

        repo2.save(r);
        ResponseEntity<Delivery> result = sut.deliveriesPost(dpr);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(Objects.requireNonNull(result.getBody()).getDeliveryID().toString(), orderId);
    }

    @Test
    void getUnexpectedEventBadRequest() {
        UUID del_id = UUID.randomUUID();
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdUnexpectedEventGet(del_id, null))
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdUnexpectedEventGet(del_id, null))
            .message()
            .isEqualTo("400 BAD_REQUEST \"User ID is invalid.\"");
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdUnexpectedEventGet(del_id, ""))
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdUnexpectedEventGet(del_id, ""))
            .message()
            .isEqualTo("400 BAD_REQUEST \"User ID is invalid.\"");
    }

    @Test
    void getUnexpectedEventNotFound() {
        UUID deliveryId = UUID.randomUUID();
        String courierId = "hi_im_a_user@gmail.com";
        assertThrows(DeliveryService.DeliveryNotFoundException.class,
                () -> sut.deliveriesDeliveryIdDeliveredTimeGet(deliveryId, courierId));

        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setCourierID(courierId);

        sut.insert(delivery);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdUnexpectedEventGet(deliveryId, courierId))
            .extracting("status")
            .isEqualTo(HttpStatus.NOT_FOUND);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdUnexpectedEventGet(deliveryId, courierId))
            .message()
            .isEqualTo("404 NOT_FOUND \"Unexpected event could not be found.\"");
    }

    @Test
    void getUnexpectedEventUnauthorized() {
        UUID deliveryId = UUID.randomUUID();
        nl.tudelft.sem.template.model.Error e = new nl.tudelft.sem.template.model.Error();
        e.setType(ErrorType.NONE);
        String clientId = "ehi_im_a_user@gmail.com";
        String courierId = "ehi_im_a_courier@gmail.com";
        String vendorId = "ehi_im_a_user@gmail.com";
        String invalid = "ehi_im_an_impostor@gmail.com";
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setError(e);
        delivery.setCustomerID(clientId + "h");
        delivery.setCourierID(courierId + "h");
        delivery.setRestaurantID(vendorId + "h");

        sut.insert(delivery);

        when(usersCommunication.getUserAccountType(clientId)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        when(usersCommunication.getUserAccountType(courierId)).thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersCommunication.getUserAccountType(vendorId)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        when(usersCommunication.getUserAccountType(invalid)).thenReturn(UsersAuthenticationService.AccountType.INVALID);

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdUnexpectedEventGet(deliveryId, clientId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdUnexpectedEventGet(deliveryId, clientId))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdUnexpectedEventGet(deliveryId, courierId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdUnexpectedEventGet(deliveryId, courierId))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdUnexpectedEventGet(deliveryId, vendorId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdUnexpectedEventGet(deliveryId, vendorId))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdUnexpectedEventGet(deliveryId, invalid))
            .extracting("status")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdUnexpectedEventGet(deliveryId, invalid))
            .message()
            .isEqualTo("401 UNAUTHORIZED \"User lacks valid authentication credentials.\"");
    }

    @Test
    void getRestaurantBadRequest() {
        UUID deliveryId = UUID.randomUUID();
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdRestaurantGet(deliveryId, null))
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdRestaurantGet(deliveryId, null))
            .message()
            .isEqualTo("400 BAD_REQUEST \"User ID is invalid.\"");
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdRestaurantGet(deliveryId, ""))
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdRestaurantGet(deliveryId, ""))
            .message()
            .isEqualTo("400 BAD_REQUEST \"User ID is invalid.\"");
    }

    @Test
    void getRestaurantNotFound() {
        UUID deliveryId = UUID.randomUUID();
        String courierId = "hi_im_a_user@gmail.com";
        assertThrows(DeliveryService.DeliveryNotFoundException.class,
                () -> sut.deliveriesDeliveryIdDeliveredTimeGet(deliveryId, courierId));

        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setCourierID(courierId);

        sut.insert(delivery);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCurrentLocationGet(deliveryId, courierId))
            .extracting("status")
            .isEqualTo(HttpStatus.NOT_FOUND);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCurrentLocationGet(deliveryId, courierId))
            .message()
            .isEqualTo("404 NOT_FOUND \"Current location could not be found.\"");
    }

    @Test
    void getRestaurantUnauthorized() {
        UUID deliveryId = UUID.randomUUID();
        String clientId = "hi_im_a_user@gmail.com";
        String courierId = "hi_im_a_courier@gmail.com";
        String vendorId = "hi_im_a_user@gmail.com";
        String invalid = "hi_im_an_impostor@gmail.com";
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setCustomerID(clientId + "h");
        delivery.setCourierID(courierId + "h");
        delivery.setRestaurantID(vendorId + "h");

        sut.insert(delivery);

        when(usersCommunication.getUserAccountType(clientId)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        when(usersCommunication.getUserAccountType(courierId)).thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersCommunication.getUserAccountType(vendorId)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        when(usersCommunication.getUserAccountType(invalid)).thenReturn(UsersAuthenticationService.AccountType.INVALID);
        when(usersCommunication.checkUserAccessToDelivery(clientId, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(courierId, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(vendorId, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(invalid, delivery)).thenReturn(false);

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdRestaurantGet(deliveryId, clientId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdRestaurantGet(deliveryId, clientId))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdRestaurantGet(deliveryId, courierId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdRestaurantGet(deliveryId, courierId))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdRestaurantGet(deliveryId, vendorId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdRestaurantGet(deliveryId, vendorId))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdRestaurantGet(deliveryId, invalid))
            .extracting("status")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdRestaurantGet(deliveryId, invalid))
            .message()
            .isEqualTo("401 UNAUTHORIZED \"User lacks valid authentication credentials.\"");
    }

    @Test
    void getRestaurantAuthorized() {
        String admin = "hi_im_an_admin@gmail.com";
        UUID deliveryId = UUID.randomUUID();
        String clientId = "hi_im_a_user@gmail.com";
        String courierId = "hi_im_a_courier@gmail.com";
        String vendorId = "hi_im_a_vendor@gmail.com";
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setCustomerID(clientId);
        delivery.setCourierID(courierId);
        delivery.setRestaurantID(vendorId);

        sut.insert(delivery);

        when(usersCommunication.getUserAccountType(clientId)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        when(usersCommunication.getUserAccountType(courierId)).thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersCommunication.getUserAccountType(vendorId)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        when(usersCommunication.getUserAccountType(admin)).thenReturn(UsersAuthenticationService.AccountType.ADMIN);
        when(usersCommunication.checkUserAccessToDelivery(clientId, delivery)).thenReturn(true);
        when(usersCommunication.checkUserAccessToDelivery(courierId, delivery)).thenReturn(true);
        when(usersCommunication.checkUserAccessToDelivery(vendorId, delivery)).thenReturn(true);
        when(usersCommunication.checkUserAccessToDelivery(admin, delivery)).thenReturn(true);

        ResponseEntity<String> res = sut.deliveriesDeliveryIdRestaurantGet(deliveryId, courierId);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), vendorId);

        res = sut.deliveriesDeliveryIdRestaurantGet(deliveryId, vendorId);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), vendorId);

        res = sut.deliveriesDeliveryIdRestaurantGet(deliveryId, admin);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), vendorId);

        res = sut.deliveriesDeliveryIdRestaurantGet(deliveryId, clientId);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
    }

    @Test
    void getDeliveredTimeBadRequest() {
        UUID del_id = UUID.randomUUID();
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdDeliveredTimeGet(del_id, null))
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdDeliveredTimeGet(del_id, null))
            .message()
            .isEqualTo("400 BAD_REQUEST \"User ID is invalid.\"");
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdDeliveredTimeGet(del_id, ""))
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdDeliveredTimeGet(del_id, ""))
            .message()
            .isEqualTo("400 BAD_REQUEST \"User ID is invalid.\"");
    }

    @Test
    void getDeliveredTimeNotFound() {
        UUID deliveryId = UUID.randomUUID();
        String customerId = "hi_im_a_user@gmail.com";
        assertThrows(DeliveryService.DeliveryNotFoundException.class,
                () -> sut.deliveriesDeliveryIdDeliveredTimeGet(deliveryId, customerId));

        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setCustomerID(customerId);

        sut.insert(delivery);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdDeliveredTimeGet(deliveryId, customerId))
            .extracting("status")
            .isEqualTo(HttpStatus.NOT_FOUND);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdDeliveredTimeGet(deliveryId, customerId))
            .message()
            .isEqualTo("404 NOT_FOUND \"Delivered time could not be found.\"");
    }

    @Test
    void getDeliveredTimeUnauthorized() {
        UUID deliveryId = UUID.randomUUID();
        String clientId = "hi_im_a_user@gmail.com";
        String courierId = "hi_im_a_courier@gmail.com";
        String vendorId = "hi_im_a_user@gmail.com";
        String invalid = "hi_im_an_impostor@gmail.com";
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setCustomerID(clientId + "h");
        delivery.setCourierID(courierId + "h");
        delivery.setRestaurantID(vendorId + "h");

        sut.insert(delivery);

        when(usersCommunication.getUserAccountType(clientId)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        when(usersCommunication.getUserAccountType(courierId)).thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersCommunication.getUserAccountType(vendorId)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        when(usersCommunication.getUserAccountType(invalid)).thenReturn(UsersAuthenticationService.AccountType.INVALID);
        when(usersCommunication.checkUserAccessToDelivery(clientId, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(courierId, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(vendorId, delivery)).thenReturn(false);

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCustomerGet(deliveryId, clientId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCustomerGet(deliveryId, clientId))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCustomerGet(deliveryId, courierId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCustomerGet(deliveryId, courierId))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCustomerGet(deliveryId, vendorId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCustomerGet(deliveryId, vendorId))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCustomerGet(deliveryId, invalid))
            .extracting("status")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCustomerGet(deliveryId, invalid))
            .message()
            .isEqualTo("401 UNAUTHORIZED \"User lacks valid authentication credentials.\"");
    }

    @Test
    void getDeliveredTimeAuthorized() {
        String admin = "hi_im_an_admin@gmail.com";
        UUID deliveryId = UUID.randomUUID();
        String clientId = "hi_im_a_user@gmail.com";
        String courierId = "hi_im_a_courier@gmail.com";
        String vendorId = "hi_im_a_vendor@gmail.com";
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setCustomerID(clientId);
        delivery.setCourierID(courierId);
        delivery.setRestaurantID(vendorId);
        OffsetDateTime t = OffsetDateTime.of(2023, 12, 27, 12, 24, 10, 4, ZoneOffset.ofHours(0));
        delivery.setDeliveredTime(t);

        sut.insert(delivery);

        when(usersCommunication.getUserAccountType(clientId)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        when(usersCommunication.getUserAccountType(courierId)).thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersCommunication.getUserAccountType(vendorId)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        when(usersCommunication.getUserAccountType(admin)).thenReturn(UsersAuthenticationService.AccountType.ADMIN);
        when(usersCommunication.checkUserAccessToDelivery(clientId, delivery)).thenReturn(true);
        when(usersCommunication.checkUserAccessToDelivery(courierId, delivery)).thenReturn(true);
        when(usersCommunication.checkUserAccessToDelivery(vendorId, delivery)).thenReturn(true);

        ResponseEntity<OffsetDateTime> res = sut.deliveriesDeliveryIdDeliveredTimeGet(deliveryId, courierId);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), t);

        res = sut.deliveriesDeliveryIdDeliveredTimeGet(deliveryId, vendorId);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), t);

        res = sut.deliveriesDeliveryIdDeliveredTimeGet(deliveryId, admin);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), t);

        res = sut.deliveriesDeliveryIdDeliveredTimeGet(deliveryId, clientId);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
    }

    @Test
    void getCustomerBadRequest() {
        UUID del_id = UUID.randomUUID();
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCustomerGet(del_id, null))
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCustomerGet(del_id, null))
            .message()
            .isEqualTo("400 BAD_REQUEST \"User ID is invalid.\"");
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCustomerGet(del_id, ""))
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCustomerGet(del_id, ""))
            .message()
            .isEqualTo("400 BAD_REQUEST \"User ID is invalid.\"");
    }

    @Test
    void getCustomerDeliveryNotFound() {
        UUID deliveryId = UUID.randomUUID();
        String customerId = "hi_im_a_user@gmail.com";
        assertThrows(DeliveryService.DeliveryNotFoundException.class,
                () -> sut.deliveriesDeliveryIdDeliveredTimeGet(deliveryId, customerId));

        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setCustomerID(customerId);

        sut.insert(delivery);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCustomerGet(deliveryId, customerId))
            .extracting("status")
            .isEqualTo(HttpStatus.NOT_FOUND);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCustomerGet(deliveryId, customerId))
            .message()
            .isEqualTo("404 NOT_FOUND \"Customer ID could not be found\"");
    }

    @Test
    void getCustomerDeliveryUnauthorized() {
        UUID deliveryId = UUID.randomUUID();
        String clientId = "hi_im_a_user@gmail.com";
        String courierId = "hi_im_a_courier@gmail.com";
        String vendorId = "hi_im_a_user@gmail.com";
        String invalid = "hi_im_an_impostor@gmail.com";
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setCustomerID(clientId + "h");
        delivery.setCourierID(courierId + "h");
        delivery.setRestaurantID(vendorId + "h");

        sut.insert(delivery);

        when(usersCommunication.getUserAccountType(clientId)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        when(usersCommunication.getUserAccountType(courierId)).thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersCommunication.getUserAccountType(vendorId)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        when(usersCommunication.getUserAccountType(invalid)).thenReturn(UsersAuthenticationService.AccountType.INVALID);
        when(usersCommunication.checkUserAccessToDelivery(clientId, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(courierId, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(vendorId, delivery)).thenReturn(false);

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCustomerGet(deliveryId, clientId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCustomerGet(deliveryId, clientId))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCustomerGet(deliveryId, courierId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCustomerGet(deliveryId, courierId))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCustomerGet(deliveryId, vendorId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCustomerGet(deliveryId, vendorId))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCustomerGet(deliveryId, invalid))
            .extracting("status")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCustomerGet(deliveryId, invalid))
            .message()
            .isEqualTo("401 UNAUTHORIZED \"User lacks valid authentication credentials.\"");
    }

    @Test
    void getCustomerDeliveryAuthorized() {
        String admin = "hi_im_an_admin@gmail.com";
        UUID deliveryId = UUID.randomUUID();
        String clientId = "hi_im_a_user@gmail.com";
        String courierId = "hi_im_a_courier@gmail.com";
        String vendorId = "hi_im_a_vendor@gmail.com";
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setCustomerID(clientId);
        delivery.setCourierID(courierId);
        delivery.setRestaurantID(vendorId);

        sut.insert(delivery);

        when(usersCommunication.getUserAccountType(clientId)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        when(usersCommunication.getUserAccountType(courierId)).thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersCommunication.getUserAccountType(vendorId)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        when(usersCommunication.getUserAccountType(admin)).thenReturn(UsersAuthenticationService.AccountType.ADMIN);
        when(usersCommunication.checkUserAccessToDelivery(clientId, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(courierId, delivery)).thenReturn(true);
        when(usersCommunication.checkUserAccessToDelivery(vendorId, delivery)).thenReturn(true);

        ResponseEntity<String> res = sut.deliveriesDeliveryIdCustomerGet(deliveryId, courierId);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), clientId);

        res = sut.deliveriesDeliveryIdCustomerGet(deliveryId, vendorId);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), clientId);

        res = sut.deliveriesDeliveryIdCustomerGet(deliveryId, admin);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), clientId);

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCustomerGet(deliveryId, clientId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCustomerGet(deliveryId, clientId))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");
    }

    @Test
    void getCurrentLocationBadRequest() {
        UUID del_id = UUID.randomUUID();
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCurrentLocationGet(del_id, null))
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCurrentLocationGet(del_id, null))
            .message()
            .isEqualTo("400 BAD_REQUEST \"User ID or Delivery ID is invalid.\"");
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCurrentLocationGet(del_id, ""))
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCurrentLocationGet(del_id, ""))
            .message()
            .isEqualTo("400 BAD_REQUEST \"User ID or Delivery ID is invalid.\"");
    }

    @Test
    void getCurrentLocationDeliveryNotFound() {
        UUID deliveryId = UUID.randomUUID();
        String customerId = "hi_im_a_user@gmail.com";
        assertThrows(DeliveryService.DeliveryNotFoundException.class,
                () -> sut.deliveriesDeliveryIdCurrentLocationGet(deliveryId, customerId));

        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setCustomerID(customerId);

        sut.insert(delivery);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCurrentLocationGet(deliveryId, customerId))
            .extracting("status")
            .isEqualTo(HttpStatus.NOT_FOUND);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCurrentLocationGet(deliveryId, customerId))
            .message()
            .isEqualTo("404 NOT_FOUND \"Current location could not be found.\"");
    }

    @Test
    void getCurrentLocationUnauthorized() {
        UUID deliveryId = UUID.randomUUID();
        String clientId = "hi_im_a_user@gmail.com";
        String courierId = "hi_im_a_courier@gmail.com";
        String vendorId = "hi_im_a_user@gmail.com";
        String invalid = "hi_im_an_impostor@gmail.com";
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setCustomerID(clientId + "h");
        delivery.setCourierID(courierId + "h");
        delivery.setRestaurantID(vendorId + "h");
        delivery.setCurrentLocation(coords);

        sut.insert(delivery);

        when(usersCommunication.getUserAccountType(clientId)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        when(usersCommunication.getUserAccountType(courierId)).thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersCommunication.getUserAccountType(vendorId)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        when(usersCommunication.getUserAccountType(invalid)).thenReturn(UsersAuthenticationService.AccountType.INVALID);
        when(usersCommunication.checkUserAccessToDelivery(clientId, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(courierId, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(vendorId, delivery)).thenReturn(false);

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCurrentLocationGet(deliveryId, clientId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCurrentLocationGet(deliveryId, clientId))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCurrentLocationGet(deliveryId, courierId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCurrentLocationGet(deliveryId, courierId))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCurrentLocationGet(deliveryId, vendorId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCurrentLocationGet(deliveryId, vendorId))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCurrentLocationGet(deliveryId, invalid))
            .extracting("status")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCurrentLocationGet(deliveryId, invalid))
            .message()
            .isEqualTo("401 UNAUTHORIZED \"User lacks valid authentication credentials.\"");
    }

    @Test
    void getCurrentLocationAuthorized() {
        String admin = "hi_im_an_admin@gmail.com";
        UUID deliveryId = UUID.randomUUID();
        String clientId = "hi_im_a_user@gmail.com";
        String courierId = "hi_im_a_courier@gmail.com";
        String vendorId = "hi_im_a_vendor@gmail.com";
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setCustomerID(clientId);
        delivery.setCourierID(courierId);
        delivery.setRestaurantID(vendorId);
        delivery.setCurrentLocation(coords);

        sut.insert(delivery);

        when(usersCommunication.getUserAccountType(clientId)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        when(usersCommunication.getUserAccountType(courierId)).thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersCommunication.getUserAccountType(vendorId)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        when(usersCommunication.getUserAccountType(admin)).thenReturn(UsersAuthenticationService.AccountType.ADMIN);
        when(usersCommunication.checkUserAccessToDelivery(clientId, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(courierId, delivery)).thenReturn(true);
        when(usersCommunication.checkUserAccessToDelivery(vendorId, delivery)).thenReturn(true);

        ResponseEntity<List<Double>> res = sut.deliveriesDeliveryIdCurrentLocationGet(deliveryId, courierId);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), coords);

        res = sut.deliveriesDeliveryIdCurrentLocationGet(deliveryId, vendorId);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), coords);

        res = sut.deliveriesDeliveryIdCurrentLocationGet(deliveryId, admin);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), coords);

        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCurrentLocationGet(deliveryId, clientId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdCurrentLocationGet(deliveryId, clientId))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");
    }

    @Test
    void pickup_get_found() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        String customerId = "user@user.com";
        delivery.setCustomerID(customerId);
        String restaurantId = "pizzahut@yahoo.com";
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantID(restaurantId);
        restaurant.setLocation(new ArrayList<>(Arrays.asList(100.0, 100.0)));

        delivery.setRestaurantID(restaurantId);
        restaurantController.insert(restaurant);
        sut.insert(delivery);

        when(usersCommunication.checkUserAccessToDelivery(customerId, delivery)).thenReturn(true);
        when(usersCommunication.getUserAccountType(customerId)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);

        List<Double> deliveryAddress
                = sut.deliveriesDeliveryIdPickupLocationGet(delivery.getDeliveryID(), customerId).getBody();
        assertThat(deliveryAddress).isEqualTo(new ArrayList<>(Arrays.asList(100.0, 100.0)));
        assertThat(sut.deliveriesDeliveryIdPickupLocationGet(delivery.getDeliveryID(),
                customerId).getStatusCode()).isEqualTo(HttpStatus.OK);

    }

    @Test
    void pickup_get_not_found() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setDeliveryAddress(new ArrayList<>(Arrays.asList(100.0, 100.0)));
        String userId = "user@user.com";
        delivery.setCustomerID(userId);
        sut.insert(delivery);
        UUID invalidDeliveryId = UUID.randomUUID();
        assertThrows(DeliveryService.DeliveryNotFoundException.class,
                () -> sut.deliveriesDeliveryIdPickupLocationGet(invalidDeliveryId, userId));
    }

    @Test
    void deliveriesDeliveryIdPickupGetAdmin() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        String customerId = "customer@testmail.com";
        delivery.setCustomerID(customerId);
        String restaurantId = "restaurant@testmail.com";
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantID(restaurantId);
        restaurant.setLocation(new ArrayList<>(Arrays.asList(100.0, 100.0)));
        delivery.setPickupTime(OffsetDateTime.parse("2021-09-30T15:30:00+01:00"));
        delivery.setRestaurantID(restaurantId);
        restaurantController.insert(restaurant);
        sut.insert(delivery);

        when(usersCommunication.checkUserAccessToDelivery(customerId, delivery)).thenReturn(true);
        when(usersCommunication.getUserAccountType(customerId)).thenReturn(UsersAuthenticationService.AccountType.ADMIN);

        OffsetDateTime pickupTime = sut.deliveriesDeliveryIdPickupGet(delivery.getDeliveryID(), customerId).getBody();
        assertThat(pickupTime).isEqualTo(OffsetDateTime.parse("2021-09-30T15:30:00+01:00"));
        assertThat(sut.deliveriesDeliveryIdPickupLocationGet(delivery.getDeliveryID(), customerId)
                .getStatusCode()).isEqualTo(HttpStatus.OK);

    }

    @Test
    void addressGetFound() {
        Delivery delivery = new Delivery();
        UUID deliveryId = UUID.randomUUID();
        delivery.setDeliveryAddress(new ArrayList<>(Arrays.asList(100.0, 100.0)));
        delivery.setDeliveryID(deliveryId);
        String userId = "user@user.com";
        delivery.setCustomerID(userId);
        sut.insert(delivery);
        when(usersCommunication.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        when(usersCommunication.checkUserAccessToDelivery(userId, delivery)).thenReturn(true);

        List<Double> deliveryAddress
                = sut.deliveriesDeliveryIdDeliveryAddressGet(delivery.getDeliveryID(), userId).getBody();
        assertThat(deliveryAddress).isEqualTo(new ArrayList<>(Arrays.asList(100.0, 100.0)));
        assertThat(sut.deliveriesDeliveryIdDeliveryAddressGet(delivery.getDeliveryID(), userId)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void addressGetUnauthorized() {
        Delivery delivery = new Delivery();
        UUID deliveryId = UUID.randomUUID();
        delivery.setDeliveryAddress(new ArrayList<>(Arrays.asList(100.0, 100.0)));
        delivery.setDeliveryID(deliveryId);
        String userId = "user@user.com";
        String otherUserId = "newUser@user.com";
        delivery.setCustomerID(otherUserId);
        sut.insert(delivery);
        when(usersCommunication.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdDeliveryAddressGet(delivery.getDeliveryID(), userId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdDeliveryAddressGet(delivery.getDeliveryID(), userId))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");

    }

    @Test
    void pickupGetUnauthorized() {
        Delivery delivery = new Delivery();
        UUID deliveryId = UUID.randomUUID();
        delivery.setDeliveryAddress(new ArrayList<>(Arrays.asList(100.0, 100.0)));
        delivery.setDeliveryID(deliveryId);
        String userId = "user@user.com";
        String otherUserId = "newUser@user.com";
        delivery.setCustomerID(otherUserId);
        sut.insert(delivery);
        when(usersCommunication.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdPickupLocationGet(delivery.getDeliveryID(), userId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.deliveriesDeliveryIdPickupLocationGet(delivery.getDeliveryID(), userId))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");

    }

    @Test
    void address_get_notFound() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setDeliveryAddress(new ArrayList<>(Arrays.asList(100.0, 100.0)));
        sut.insert(delivery);
        UUID invalidDeliveryId = UUID.randomUUID();
        assertThrows(DeliveryService.DeliveryNotFoundException.class,
                () -> sut.deliveriesDeliveryIdDeliveryAddressGet(invalidDeliveryId, null));
    }

    @Test
    void getDeliveryAuthorized() {
        UUID deliveryId = UUID.randomUUID();
        String customerId = "test@test.com";
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setRestaurantID(customerId);
        delivery.setCourierID(customerId);
        delivery.setCustomerID(customerId);
        sut.insert(delivery);
        when(usersCommunication.getUserAccountType(customerId)).thenReturn(UsersAuthenticationService.AccountType.ADMIN);

        ResponseEntity<Delivery> res = sut.deliveriesDeliveryIdGet(deliveryId, customerId);

        assertEquals(res.getBody(), delivery);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
    }

    @Test
    void testDeliveriesDeliveryIdEstimatedDeliveryTimeGet_AdminAccess() {
        // Arrange
        UUID deliveryId = UUID.randomUUID();
        String userId = "admin@example.org";
        String vendorId = "vendor@example.org";
        OffsetDateTime time = OffsetDateTime.of(2023, 12, 13, 14, 0, 0, 0, ZoneOffset.UTC);
        // Define default delivery error
        Error error = new Error();
        error.setType(ErrorType.NONE);

        // Create a Delivery object
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setRestaurantID(vendorId);
        delivery.setEstimatedPrepTime(0);
        delivery.setRatingRestaurant(5);
        delivery.setDeliveryAddress(List.of(1d, 2d));
        delivery.setOrderTime(time);
        delivery.setStatus(DeliveryStatus.PENDING);
        delivery.setError(error);
        sut.insert(delivery);

        // Create a corresponding restaurant
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantID(vendorId);
        restaurant.setLocation(List.of(0.9, 1.9d));
        restaurantController.insert(restaurant);

        when(usersCommunication.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.ADMIN);
        when(usersCommunication.checkUserAccessToDelivery(eq(userId), any(Delivery.class))).thenReturn(true);

        // Act
        ResponseEntity<OffsetDateTime> response = sut.deliveriesDeliveryIdEstimatedDeliveryTimeGet(deliveryId, userId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(time.plusMinutes(61), response.getBody());
    }

    @Test
    void testDeliveriesDeliveryIdEstimatedDeliveryTimeGet_VendorAccess() {
        // Arrange
        UUID deliveryId = UUID.randomUUID();
        String userId = "vendor@example.org";
        String vendorId = "vendor@example.org";
        OffsetDateTime time = OffsetDateTime.of(2023, 12, 13, 14, 0, 0, 0, ZoneOffset.UTC);
        // Define default delivery error
        Error error = new Error();
        error.setType(ErrorType.NONE);

        // Create a Delivery object
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setRestaurantID(vendorId);
        delivery.setEstimatedPrepTime(0);
        delivery.setRatingRestaurant(5);
        delivery.setDeliveryAddress(List.of(1d, 2d));
        delivery.setOrderTime(time);
        delivery.setStatus(DeliveryStatus.PENDING);
        delivery.setError(error);
        sut.insert(delivery);

        // Create a corresponding restaurant
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantID(vendorId);
        restaurant.setLocation(List.of(0.9, 1.9d));
        restaurantController.insert(restaurant);

        when(usersCommunication.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        when(usersCommunication.checkUserAccessToDelivery(eq(userId), any(Delivery.class))).thenReturn(true);

        // Act
        ResponseEntity<OffsetDateTime> response = sut.deliveriesDeliveryIdEstimatedDeliveryTimeGet(deliveryId, userId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(time.plusMinutes(61), response.getBody());
    }

    @Test
    void testDeliveriesDeliveryIdEstimatedDeliveryTimeGet_CourierAccess() {
        // Arrange
        UUID deliveryId = UUID.randomUUID();
        String userId = "courier@example.org";
        String vendorId = "vendor@example.org";
        OffsetDateTime time = OffsetDateTime.of(2023, 12, 13, 14, 0, 0, 0, ZoneOffset.UTC);
        // Define default delivery error
        Error error = new Error();
        error.setType(ErrorType.NONE);

        // Create a Delivery object
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setRestaurantID(vendorId);
        delivery.setEstimatedPrepTime(0);
        delivery.setRatingRestaurant(5);
        delivery.setDeliveryAddress(List.of(1d, 2d));
        delivery.setOrderTime(time);
        delivery.setStatus(DeliveryStatus.PENDING);
        delivery.setError(error);
        delivery.setCourierID(userId);
        sut.insert(delivery);

        // Create a corresponding restaurant
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantID(vendorId);
        restaurant.setLocation(List.of(0.9, 1.9d));
        restaurantController.insert(restaurant);

        when(usersCommunication.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersCommunication.checkUserAccessToDelivery(eq(userId), any(Delivery.class))).thenReturn(true);

        // Act
        ResponseEntity<OffsetDateTime> response = sut.deliveriesDeliveryIdEstimatedDeliveryTimeGet(deliveryId, userId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(time.plusMinutes(61), response.getBody());
    }

    @Test
    void testDeliveriesDeliveryIdEstimatedDeliveryTimeGet_CustomerAccess() {
        // Arrange
        UUID deliveryId = UUID.randomUUID();
        String userId = "customer@example.org";
        String vendorId = "vendor@example.org";
        OffsetDateTime time = OffsetDateTime.of(2023, 12, 13, 14, 0, 0, 0, ZoneOffset.UTC);
        // Define default delivery error
        Error error = new Error();
        error.setType(ErrorType.NONE);

        // Create a Delivery object
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setRestaurantID(vendorId);
        delivery.setEstimatedPrepTime(0);
        delivery.setRatingRestaurant(5);
        delivery.setDeliveryAddress(List.of(1d, 2d));
        delivery.setOrderTime(time);
        delivery.setStatus(DeliveryStatus.PENDING);
        delivery.setError(error);
        delivery.setCustomerID(userId);
        sut.insert(delivery);

        // Create a corresponding restaurant
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantID(vendorId);
        restaurant.setLocation(List.of(0.9, 1.9d));
        restaurantController.insert(restaurant);

        when(usersCommunication.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        when(usersCommunication.checkUserAccessToDelivery(eq(userId), any(Delivery.class))).thenReturn(true);

        // Act
        ResponseEntity<OffsetDateTime> response = sut.deliveriesDeliveryIdEstimatedDeliveryTimeGet(deliveryId, userId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(time.plusMinutes(61), response.getBody());
    }

    @Test
    void testDeliveriesDeliveryIdEstimatedDeliveryTimeGet_Forbidden() {
        // Arrange
        UUID deliveryId = UUID.randomUUID();
        String userId = "vendor1@example.org";
        String vendorId = "vendor2@example.org";
        OffsetDateTime time = OffsetDateTime.of(2023, 12, 13, 14, 0, 0, 0, ZoneOffset.UTC);
        // Define default delivery error
        Error error = new Error();
        error.setType(ErrorType.NONE);

        // Create a Delivery object
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setRestaurantID(vendorId);
        delivery.setEstimatedPrepTime(0);
        delivery.setRatingRestaurant(5);
        delivery.setDeliveryAddress(List.of(1d, 2d));
        delivery.setOrderTime(time);
        delivery.setStatus(DeliveryStatus.PENDING);
        delivery.setError(error);
        sut.insert(delivery);

        // Create a corresponding restaurant
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantID(vendorId);
        restaurant.setLocation(List.of(0.9, 1.9d));
        restaurantController.insert(restaurant);

        when(usersCommunication.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);

        // Act
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.deliveriesDeliveryIdEstimatedDeliveryTimeGet(deliveryId, userId));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("403 FORBIDDEN \"User lacks necessary permission levels.\"", exception.getMessage());
    }

    @Test
    void testDeliveriesDeliveryIdEstimatedDeliveryTimeGet_Unauthorized() {
        // Arrange
        UUID deliveryId = UUID.randomUUID();
        String userId = "invalid@example.org";
        String vendorId = "vendor@example.org";
        OffsetDateTime time = OffsetDateTime.of(2023, 12, 13, 14, 0, 0, 0, ZoneOffset.UTC);
        // Define default delivery error
        Error error = new Error();
        error.setType(ErrorType.NONE);

        // Create a Delivery object
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setRestaurantID(vendorId);
        delivery.setEstimatedPrepTime(0);
        delivery.setRatingRestaurant(5);
        delivery.setDeliveryAddress(List.of(1d, 2d));
        delivery.setOrderTime(time);
        delivery.setStatus(DeliveryStatus.PENDING);
        delivery.setError(error);
        sut.insert(delivery);

        // Create a corresponding restaurant
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantID(vendorId);
        restaurant.setLocation(List.of(0.9, 1.9d));
        restaurantController.insert(restaurant);

        when(usersCommunication.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.INVALID);

        // Act
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.deliveriesDeliveryIdEstimatedDeliveryTimeGet(deliveryId, userId));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("401 UNAUTHORIZED \"Unauthorized access. User cannot be authorized.\"", exception.getMessage());
    }

    @Test
    void getDeliveryForbidden() {
        UUID deliveryId = UUID.randomUUID();
        String customerId = "test@test.com";
        String newCustomerId = "newtest@test.com";

        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setRestaurantID(customerId);
        delivery.setCourierID(customerId);
        delivery.setCustomerID(newCustomerId);
        sut.insert(delivery);
        when(usersCommunication.getUserAccountType(customerId)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> sut.deliveriesDeliveryIdGet(deliveryId, customerId));
        assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
        assertEquals("User lacks necessary permissions.", exception.getReason());
    }

    @Test
    void getDeliveryUnauthorized() {
        UUID deliveryId = UUID.randomUUID();
        String customerId = "test@test.com";
        String newCustomerId = "newtest@test.com";

        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setRestaurantID(customerId);
        delivery.setCourierID(customerId);
        delivery.setCustomerID(newCustomerId);
        sut.insert(delivery);
        when(usersCommunication.getUserAccountType(customerId))
                .thenReturn(UsersAuthenticationService.AccountType.fromValue("UNAUTHORIZED"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.deliveriesDeliveryIdGet(deliveryId, customerId));
        assertEquals(exception.getStatus(), HttpStatus.UNAUTHORIZED);
        assertEquals("User lacks valid authentication credentials.", exception.getReason());
    }

    @Test
    void getDeliveryPrepTimeAuthorized() {
        UUID deliveryId = UUID.randomUUID();
        String customerId = "test@test.com";

        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setRestaurantID(customerId);
        delivery.setEstimatedPrepTime(30);
        delivery.setCourierID(customerId);
        delivery.setCustomerID(customerId);
        sut.insert(delivery);
        when(usersCommunication.getUserAccountType(customerId)).thenReturn(UsersAuthenticationService.AccountType.ADMIN);

        ResponseEntity<Integer> res = sut.deliveriesDeliveryIdPrepGet(deliveryId, customerId);

        assertEquals(res.getBody(), 30);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
    }

    @Test
    void getDeliveryPrepTimeForbidden() {
        UUID deliveryId = UUID.randomUUID();
        String customerId = "test@test.com";
        String newCustomerId = "newtest@test.com";

        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setRestaurantID(customerId);
        delivery.setCourierID(customerId);
        delivery.setEstimatedPrepTime(30);
        delivery.setCustomerID(newCustomerId);
        sut.insert(delivery);
        when(usersCommunication.getUserAccountType(customerId)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.deliveriesDeliveryIdPrepGet(deliveryId, customerId));
        assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
        assertEquals("User lacks necessary permissions.", exception.getReason());
    }

    @Test
    void getDeliveryPrepTimeUnauthorized() {
        UUID deliveryId = UUID.randomUUID();
        String customerId = "test@test.com";
        String newCustomerId = "newtest@test.com";

        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setRestaurantID(customerId);
        delivery.setCourierID(customerId);
        delivery.setCustomerID(newCustomerId);
        delivery.setEstimatedPrepTime(30);
        sut.insert(delivery);
        when(usersCommunication.getUserAccountType(customerId))
                .thenReturn(UsersAuthenticationService.AccountType.fromValue("UNAUTHORIZED"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.deliveriesDeliveryIdPrepGet(deliveryId, customerId));
        assertEquals(exception.getStatus(), HttpStatus.UNAUTHORIZED);
        assertEquals("User lacks valid authentication credentials.", exception.getReason());
    }

    @Test
    void deliveriesDeliveryIdPickupGetNotFound() {
        UUID deliveryId = UUID.randomUUID();
        String userId = "user@testmail.com";

        when(usersCommunication.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);

        ResponseStatusException exception = assertThrows(DeliveryService.DeliveryNotFoundException.class,
                () -> sut.deliveriesDeliveryIdPickupGet(deliveryId, userId));
        assertEquals(exception.getStatus(), HttpStatus.NOT_FOUND);
        assertEquals(exception.getReason(), "Delivery with specified id not found");
    }

    @Test
    void deliveriesDeliveryIdPickupGetCourier() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        String customerId = "customer@testmail.com";
        delivery.setCustomerID(customerId);
        String restaurantId = "restaurant@testmail.com";
        delivery.setRestaurantID(restaurantId);
        delivery.setPickupTime(OffsetDateTime.parse("2021-09-30T15:30:00+01:00"));
        String courierId = "actual_courier@testmail.com";
        delivery.setCourierID(courierId);
        String fakeCourierId = "another_courier@testmail.com";
        sut.insert(delivery);

        when(usersCommunication.getUserAccountType(courierId))
                .thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersCommunication.getUserAccountType(fakeCourierId))
                .thenReturn(UsersAuthenticationService.AccountType.COURIER);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.deliveriesDeliveryIdPickupGet(deliveryId, fakeCourierId));
        assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
        assertEquals(exception.getReason(), "Courier does not correspond to the order.");

        OffsetDateTime pickupTime = sut.deliveriesDeliveryIdPickupGet(delivery.getDeliveryID(), courierId).getBody();
        assertThat(pickupTime).isEqualTo(OffsetDateTime.parse("2021-09-30T15:30:00+01:00"));
        assertThat(sut.deliveriesDeliveryIdPickupGet(delivery.getDeliveryID(), courierId).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void deliveriesDeliveryIdPickupGetVendor() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        String customerId = "customer@testmail.com";
        delivery.setCustomerID(customerId);
        String restaurantId = "restaurant@testmail.com";
        String fakeRestaurantId = "another_restaurant@testmail.com";
        delivery.setRestaurantID(restaurantId);
        delivery.setPickupTime(OffsetDateTime.parse("2021-09-30T15:30:00+01:00"));
        sut.insert(delivery);

        when(usersCommunication.getUserAccountType(restaurantId))
                .thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        when(usersCommunication.getUserAccountType(fakeRestaurantId))
                .thenReturn(UsersAuthenticationService.AccountType.VENDOR);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.deliveriesDeliveryIdPickupGet(deliveryId, fakeRestaurantId));
        assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
        assertEquals(exception.getReason(), "Vendor does not correspond to the order.");

        OffsetDateTime pickupTime = sut.deliveriesDeliveryIdPickupGet(delivery.getDeliveryID(), restaurantId).getBody();
        assertThat(pickupTime).isEqualTo(OffsetDateTime.parse("2021-09-30T15:30:00+01:00"));
        assertThat(sut.deliveriesDeliveryIdPickupGet(delivery.getDeliveryID(), restaurantId).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void deliveriesDeliveryIdPickupGetClient() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        String customerId = "customer@testmail.com";
        String fakeCustomerId = "another_customer@testmail.com";
        delivery.setCustomerID(customerId);
        String restaurantId = "restaurant@testmail.com";
        delivery.setRestaurantID(restaurantId);
        delivery.setPickupTime(OffsetDateTime.parse("2021-09-30T15:30:00+01:00"));
        sut.insert(delivery);

        when(usersCommunication.getUserAccountType(customerId))
                .thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        when(usersCommunication.getUserAccountType(fakeCustomerId))
                .thenReturn(UsersAuthenticationService.AccountType.CLIENT);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.deliveriesDeliveryIdPickupGet(deliveryId, fakeCustomerId));
        assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
        assertEquals(exception.getReason(), "Client does not correspond to the order.");

        OffsetDateTime pickupTime = sut.deliveriesDeliveryIdPickupGet(delivery.getDeliveryID(), customerId).getBody();
        assertThat(pickupTime).isEqualTo(OffsetDateTime.parse("2021-09-30T15:30:00+01:00"));
        assertThat(sut.deliveriesDeliveryIdPickupGet(delivery.getDeliveryID(), customerId).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void deliveriesDeliveryIdPickupGetUnauthorized() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        String customerId = "customer@testmail.com";
        delivery.setCustomerID(customerId);
        String restaurantId = "restaurant@testmail.com";
        delivery.setRestaurantID(restaurantId);
        delivery.setPickupTime(OffsetDateTime.parse("2021-09-30T15:30:00+01:00"));
        sut.insert(delivery);
        String userId = "user@testmail.com";

        when(usersCommunication.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.INVALID);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.deliveriesDeliveryIdPickupGet(deliveryId, userId));
        assertEquals(exception.getStatus(), HttpStatus.UNAUTHORIZED);
        assertEquals(exception.getReason(), "Account could not be verified.");
    }

    @Test
    void deliveriesDeliveryIdPickupPutNotFound() {
        OffsetDateTime time = OffsetDateTime.parse("2021-09-30T15:30:00+01:00");
        UUID deliveryId = UUID.randomUUID();
        String customerId = "customer@testmail.com";

        when(usersCommunication.getUserAccountType(customerId)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);

        ResponseStatusException exception = assertThrows(DeliveryService.DeliveryNotFoundException.class,
                () -> sut.deliveriesDeliveryIdPickupPut(deliveryId, customerId, time));
        assertEquals(exception.getStatus(), HttpStatus.NOT_FOUND);
        assertEquals(exception.getReason(), "Delivery with specified id not found");
    }

    @Test
    void deliveriesDeliveryIdPickupPutAdmin() {
        OffsetDateTime time = OffsetDateTime.parse("2021-09-30T15:30:00+01:00");
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        String customerId = "customer@testmail.com";
        delivery.setCustomerID(customerId);
        String restaurantId = "restaurant@testmail.com";
        delivery.setRestaurantID(restaurantId);
        sut.insert(delivery);
        String userId = "user@testmail.com";

        when(usersCommunication.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.ADMIN);

        OffsetDateTime pickupTime = Objects.requireNonNull(
                sut.deliveriesDeliveryIdPickupPut(deliveryId, userId, time).getBody()).getPickupTime();
        assertThat(pickupTime).isEqualTo(time);
        assertThat(sut.deliveriesDeliveryIdPickupPut(deliveryId, userId, time).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deliveriesDeliveryIdPickupPutCourier() {
        OffsetDateTime time = OffsetDateTime.parse("2021-09-30T15:30:00+01:00");
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        String customerId = "customer@testmail.com";
        delivery.setCustomerID(customerId);
        String restaurantId = "restaurant@testmail.com";
        delivery.setRestaurantID(restaurantId);
        String courierId = "courier@testmail.com";
        delivery.setCourierID(courierId);
        String fakeCourierId = "another_courier@testmail.com";
        sut.insert(delivery);

        when(usersCommunication.getUserAccountType(courierId))
                .thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersCommunication.getUserAccountType(fakeCourierId))
                .thenReturn(UsersAuthenticationService.AccountType.COURIER);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.deliveriesDeliveryIdPickupPut(deliveryId, fakeCourierId, time));
        assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
        assertEquals(exception.getReason(), "Courier does not correspond to the order.");

        OffsetDateTime pickupTime = Objects.requireNonNull(
                sut.deliveriesDeliveryIdPickupPut(deliveryId, courierId, time).getBody()).getPickupTime();
        assertThat(pickupTime).isEqualTo(time);
        assertThat(sut.deliveriesDeliveryIdPickupPut(deliveryId, courierId, time).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deliveriesDeliveryIdPickupPutVendor() {
        OffsetDateTime time = OffsetDateTime.parse("2021-09-30T15:30:00+01:00");
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        String customerId = "customer@testmail.com";
        delivery.setCustomerID(customerId);
        String restaurantId = "restaurant@testmail.com";
        String fakeRestaurantId = "another_restaurant@testmail.com";
        delivery.setRestaurantID(restaurantId);
        sut.insert(delivery);

        when(usersCommunication.getUserAccountType(restaurantId))
                .thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        when(usersCommunication.getUserAccountType(fakeRestaurantId))
                .thenReturn(UsersAuthenticationService.AccountType.VENDOR);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.deliveriesDeliveryIdPickupPut(deliveryId, fakeRestaurantId, time));
        assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
        assertEquals(exception.getReason(), "Vendor does not correspond to the order.");

        OffsetDateTime pickupTime = Objects.requireNonNull(
                sut.deliveriesDeliveryIdPickupPut(deliveryId, restaurantId, time).getBody()).getPickupTime();
        assertThat(pickupTime).isEqualTo(time);
        assertThat(sut.deliveriesDeliveryIdPickupPut(deliveryId, restaurantId, time)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deliveriesDeliveryIdPickupPutUnauthorized() {
        OffsetDateTime time = OffsetDateTime.parse("2021-09-30T15:30:00+01:00");
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        String customerId = "customer@testmail.com";
        delivery.setCustomerID(customerId);
        String restaurantId = "restaurant@testmail.com";
        delivery.setRestaurantID(restaurantId);
        sut.insert(delivery);
        String userId = "user@testmail.com";

        when(usersCommunication.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.INVALID);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.deliveriesDeliveryIdPickupPut(deliveryId, userId, time));
        assertEquals(exception.getStatus(), HttpStatus.UNAUTHORIZED);
        assertEquals(exception.getReason(), "Account could not be verified.");
    }

    @Test
    void deliveriesDeliveryIdDeliveryAddressPutRequestNullOrSizeOrNotFound() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setDeliveryAddress(List.of(0.1, 0.2));
        String customerId = "customer@testmail.com";
        delivery.setCustomerID(customerId);
        String restaurantId = "restaurant@testmail.com";
        delivery.setCustomerID(restaurantId);

        sut.insert(delivery);

        String userId = "user@testmail.com";

        when(usersCommunication.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.ADMIN);

        ResponseStatusException exception1 = assertThrows(ResponseStatusException.class,
                () -> sut.deliveriesDeliveryIdDeliveryAddressPut(deliveryId, userId, null));
        assertEquals(exception1.getStatus(), HttpStatus.BAD_REQUEST);
        assertEquals(exception1.getReason(), "Delivery Address not set correctly.");

        ResponseStatusException exception2 = assertThrows(ResponseStatusException.class,
                () -> sut.deliveriesDeliveryIdDeliveryAddressPut(deliveryId, userId, List.of(0.1)));
        assertEquals(exception2.getStatus(), HttpStatus.BAD_REQUEST);
        assertEquals(exception2.getReason(), "Delivery Address not set correctly.");

        ResponseStatusException exception3 = assertThrows(DeliveryService.DeliveryNotFoundException.class,
                () -> sut.deliveriesDeliveryIdDeliveryAddressPut(UUID.randomUUID(), userId, List.of(0.1, 0.2)));
        assertEquals(exception3.getStatus(), HttpStatus.NOT_FOUND);
        assertEquals(exception3.getReason(), "Delivery with specified id not found");
    }

    @Test
    void deliveriesDeliveryIdDeliveryAddressPutNotSameClient() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setDeliveryAddress(List.of(0.1, 0.2));
        String customerId = "customer@testmail.com";
        String otherCustomerId = "other_customer@testmail.com";
        delivery.setCustomerID(customerId);
        String restaurantId = "restaurant@testmail.com";
        delivery.setCustomerID(restaurantId);

        sut.insert(delivery);

        when(usersCommunication.getUserAccountType(otherCustomerId))
                .thenReturn(UsersAuthenticationService.AccountType.CLIENT);

        ResponseStatusException exception1 = assertThrows(ResponseStatusException.class,
                () -> sut.deliveriesDeliveryIdDeliveryAddressPut(deliveryId, otherCustomerId, List.of(0.1, 0.2)));
        assertEquals(exception1.getStatus(), HttpStatus.FORBIDDEN);
        assertEquals(exception1.getReason(), "Customer does not correspond to the order.");
    }

    @Test
    void deliveriesDeliveryIdDeliveryAddressPutCourierVendor() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setDeliveryAddress(List.of(0.1, 0.2));
        String customerId = "customer@testmail.com";
        delivery.setCustomerID(customerId);
        String restaurantId = "restaurant@testmail.com";
        delivery.setCustomerID(restaurantId);

        String courierId = "courier@testmail.com";
        String vendorId = "vendor@testmail.com";
        sut.insert(delivery);

        when(usersCommunication.getUserAccountType(courierId)).thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersCommunication.getUserAccountType(vendorId)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);

        ResponseStatusException exception1 = assertThrows(ResponseStatusException.class,
                () -> sut.deliveriesDeliveryIdDeliveryAddressPut(deliveryId, courierId, List.of(0.1, 0.2)));
        assertEquals(exception1.getStatus(), HttpStatus.FORBIDDEN);
        assertEquals(exception1.getReason(), "Only customers can update the delivery address.");

        ResponseStatusException exception2 = assertThrows(ResponseStatusException.class,
                () -> sut.deliveriesDeliveryIdDeliveryAddressPut(deliveryId, vendorId, List.of(0.1, 0.2)));
        assertEquals(exception2.getStatus(), HttpStatus.FORBIDDEN);
        assertEquals(exception2.getReason(), "Only customers can update the delivery address.");
    }

    @Test
    void deliveriesDeliveryIdDeliveryAddressPutInvalid() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setDeliveryAddress(List.of(0.1, 0.2));
        String customerId = "customer@testmail.com";
        delivery.setCustomerID(customerId);
        String restaurantId = "restaurant@testmail.com";
        delivery.setCustomerID(restaurantId);

        String userId = "user@testmail.com";
        sut.insert(delivery);

        when(usersCommunication.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.INVALID);

        ResponseStatusException exception1 = assertThrows(ResponseStatusException.class,
                () -> sut.deliveriesDeliveryIdDeliveryAddressPut(deliveryId, userId, List.of(0.1, 0.2)));
        assertEquals(exception1.getStatus(), HttpStatus.UNAUTHORIZED);
        assertEquals(exception1.getReason(), "Account could not be verified.");
    }

    @Test
    void deliveriesDeliveryIdDeliveryAddressPutSameClientAdmin() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setDeliveryAddress(List.of(0.1, 0.2));
        String customerId = "customer@testmail.com";
        delivery.setCustomerID(customerId);
        String restaurantId = "restaurant@testmail.com";
        delivery.setRestaurantID(restaurantId);

        String admin = "admin@testmail.com";

        sut.insert(delivery);

        when(usersCommunication.getUserAccountType(customerId)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        when(usersCommunication.getUserAccountType(admin)).thenReturn(UsersAuthenticationService.AccountType.ADMIN);

        ResponseEntity<Delivery> res2
                = sut.deliveriesDeliveryIdDeliveryAddressPut(deliveryId, customerId, List.of(0.1, 0.4));
        assertEquals(HttpStatus.OK, res2.getStatusCode());
        assertTrue(Objects.requireNonNull(res2.getBody()).getDeliveryAddress().containsAll(List.of(0.1, 0.4)));

        ResponseEntity<Delivery> res1 = sut.deliveriesDeliveryIdDeliveryAddressPut(deliveryId, admin, List.of(0.1, 0.3));
        assertEquals(HttpStatus.OK, res1.getStatusCode());
        assertTrue(Objects.requireNonNull(res1.getBody()).getDeliveryAddress().containsAll(List.of(0.1, 0.3)));
    }
}