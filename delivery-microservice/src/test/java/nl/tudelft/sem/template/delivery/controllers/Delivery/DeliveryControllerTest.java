package nl.tudelft.sem.template.delivery.controllers.Delivery;

import nl.tudelft.sem.template.delivery.TestRepos.TestDeliveryRepository;
import nl.tudelft.sem.template.delivery.TestRepos.TestRestaurantRepository;
import nl.tudelft.sem.template.delivery.communication.UsersCommunication;
import nl.tudelft.sem.template.delivery.controllers.DeliveryController;
import nl.tudelft.sem.template.delivery.controllers.RestaurantController;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.delivery.services.RestaurantService;
import nl.tudelft.sem.template.model.DeliveriesPostRequest;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import nl.tudelft.sem.template.model.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryControllerTest {

    private UsersCommunication usersCommunication;

    private DeliveryController sut;

    private TestDeliveryRepository repo1;

    private TestRestaurantRepository repo2;
    private RestaurantController restaurantController;

    String userId, userType;
    UUID deliveryId;
    Integer prepTime;
    Delivery delivery;

    @BeforeEach
    void setUp() {
        // Mock data
        userId = "user@example.org";
        deliveryId = UUID.randomUUID();
        prepTime = 25;
        delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setEstimatedPrepTime(prepTime);
        repo2 = new TestRestaurantRepository();
        restaurantController = new RestaurantController(new RestaurantService(repo2));
        repo1 = new TestDeliveryRepository();
        usersCommunication = mock(UsersCommunication.class);
        sut = new DeliveryController(new DeliveryService(repo1,repo2), usersCommunication);
    }

    @Test
    void addDeliveryEntityNotRealStatus() {
        DeliveriesPostRequest dpr = new DeliveriesPostRequest();
        dpr.setStatus("invalid");
        dpr.setCustomerId(userId);
        dpr.setVendorId("hi_im_a_vendor@testmail.com");
        dpr.setOrderId(UUID.randomUUID().toString());
        dpr.setDeliveryAddress(List.of(50.4, 32.6));

        ResponseEntity<Delivery> result = sut.deliveriesPost(dpr);
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void addDeliveryEntityEmptyEmail() {
        DeliveriesPostRequest dpr = new DeliveriesPostRequest();
        dpr.setStatus("pending");
        dpr.setCustomerId(userId);
        dpr.setOrderId(deliveryId.toString());
        dpr.setDeliveryAddress(List.of(50.4, 32.6));

        ResponseEntity<Delivery> result = sut.deliveriesPost(dpr);
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void addDeliveryEntityNoAddr() {
        DeliveriesPostRequest dpr = new DeliveriesPostRequest();
        dpr.setStatus("pending");
        dpr.setCustomerId(userId);
        dpr.setVendorId("hi_im_a_vendor@testmail.com");
        dpr.setOrderId(deliveryId.toString());
        dpr.setDeliveryAddress(new ArrayList<>());

        ResponseEntity<Delivery> result = sut.deliveriesPost(dpr);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }
    @Test
    void addDeliveryEntity() {
        DeliveriesPostRequest dpr = new DeliveriesPostRequest();
        dpr.setStatus("pending");
        dpr.setCustomerId(userId);
        dpr.setVendorId("hi_im_a_vendor@testmail.com");
        dpr.setOrderId(deliveryId.toString());
        dpr.setDeliveryAddress(List.of(50.4, 32.6));

        Delivery d = new Delivery();
        d.setDeliveryID(deliveryId);
        d.setRestaurantID("hi_im_a_vendor@testmail.com");
        d.setCustomerID(userId);
        d.setStatus(DeliveryStatus.PENDING);
        d.setDeliveryAddress(List.of(50.4, 32.6));
        sut.insert(d);

        ResponseEntity<Delivery> result = sut.deliveriesPost(dpr);
        Delivery added = result.getBody();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(added.getError().getType(), ErrorType.NONE);
        assertEquals(added.getStatus(), DeliveryStatus.PENDING);
    }

    @Test
    void addDeliveryEntityAccepted() {
        DeliveriesPostRequest dpr = new DeliveriesPostRequest();
        dpr.setStatus("accepted");
        dpr.setCustomerId(userId);
        dpr.setVendorId("hi_im_a_vendor@testmail.com");
        dpr.setOrderId(deliveryId.toString());
        dpr.setDeliveryAddress(List.of(50.4, 32.6));

        Delivery d = new Delivery();
        d.setDeliveryID(deliveryId);
        d.setRestaurantID("hi_im_a_vendor@testmail.com");
        d.setCustomerID(userId);
        d.setStatus(DeliveryStatus.ACCEPTED);
        d.setDeliveryAddress(List.of(50.4, 32.6));
        sut.insert(d);

        ResponseEntity<Delivery> result = sut.deliveriesPost(dpr);
        Delivery added = result.getBody();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(added.getStatus(), DeliveryStatus.ACCEPTED);
    }

    @Test
    void addDeliveryEntityRejected() {
        DeliveriesPostRequest dpr = new DeliveriesPostRequest();
        dpr.setStatus("reJEcTeD");
        dpr.setCustomerId(userId);
        dpr.setVendorId("hi_im_a_vendor@testmail.com");
        dpr.setOrderId(deliveryId.toString());
        dpr.setDeliveryAddress(List.of(50.4, 32.6));

        Delivery d = new Delivery();
        d.setDeliveryID(deliveryId);
        d.setRestaurantID("hi_im_a_vendor@testmail.com");
        d.setCustomerID(userId);
        d.setStatus(DeliveryStatus.REJECTED);
        d.setDeliveryAddress(List.of(50.4, 32.6));
        sut.insert(d);

        ResponseEntity<Delivery> result = sut.deliveriesPost(dpr);
        Delivery added = result.getBody();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(added.getStatus(), DeliveryStatus.REJECTED);
    }

    @Test
    void addDeliveryEntityPrep() {
        DeliveriesPostRequest dpr = new DeliveriesPostRequest();
        dpr.setStatus("PrEpARInG");
        dpr.setCustomerId(userId);
        dpr.setVendorId("hi_im_a_vendor@testmail.com");
        dpr.setOrderId(deliveryId.toString());
        dpr.setDeliveryAddress(List.of(50.4, 32.6));

        Delivery d = new Delivery();
        d.setDeliveryID(deliveryId);
        d.setRestaurantID("hi_im_a_vendor@testmail.com");
        d.setCustomerID(userId);
        d.setStatus(DeliveryStatus.PREPARING);
        d.setDeliveryAddress(List.of(50.4, 32.6));
        sut.insert(d);

        ResponseEntity<Delivery> result = sut.deliveriesPost(dpr);
        Delivery added = result.getBody();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(added.getStatus(), DeliveryStatus.PREPARING);
    }

    @Test
    void addDeliveryEntityGTC() {
        DeliveriesPostRequest dpr = new DeliveriesPostRequest();
        dpr.setStatus("gIven_tO_cOURiER");
        dpr.setCustomerId(userId);
        dpr.setVendorId("hi_im_a_vendor@testmail.com");
        dpr.setOrderId(deliveryId.toString());
        dpr.setDeliveryAddress(List.of(50.4, 32.6));

        Delivery d = new Delivery();
        d.setDeliveryID(deliveryId);
        d.setRestaurantID("hi_im_a_vendor@testmail.com");
        d.setCustomerID(userId);
        d.setStatus(DeliveryStatus.GIVEN_TO_COURIER);
        d.setDeliveryAddress(List.of(50.4, 32.6));
        sut.insert(d);

        ResponseEntity<Delivery> result = sut.deliveriesPost(dpr);
        Delivery added = result.getBody();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(added.getStatus(), DeliveryStatus.GIVEN_TO_COURIER);
    }

    @Test
    void addDeliveryEntityOT() {
        DeliveriesPostRequest dpr = new DeliveriesPostRequest();
        dpr.setStatus("On_TRanSiT");
        dpr.setCustomerId(userId);
        dpr.setVendorId("hi_im_a_vendor@testmail.com");
        dpr.setOrderId(deliveryId.toString());
        dpr.setDeliveryAddress(List.of(50.4, 32.6));

        Delivery d = new Delivery();
        d.setDeliveryID(deliveryId);
        d.setRestaurantID("hi_im_a_vendor@testmail.com");
        d.setCustomerID(userId);
        d.setStatus(DeliveryStatus.ON_TRANSIT);
        d.setDeliveryAddress(List.of(50.4, 32.6));
        sut.insert(d);

        ResponseEntity<Delivery> result = sut.deliveriesPost(dpr);
        Delivery added = result.getBody();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(added.getStatus(), DeliveryStatus.ON_TRANSIT);
    }

    @Test
    void addDeliveryEntityDelivered() {
        DeliveriesPostRequest dpr = new DeliveriesPostRequest();
        dpr.setStatus("deLIVERED");
        dpr.setCustomerId(userId);
        dpr.setVendorId("hi_im_a_vendor@testmail.com");
        dpr.setOrderId(deliveryId.toString());
        dpr.setDeliveryAddress(List.of(50.4, 32.6));

        Delivery d = new Delivery();
        d.setDeliveryID(deliveryId);
        d.setRestaurantID("hi_im_a_vendor@testmail.com");
        d.setCustomerID(userId);
        d.setStatus(DeliveryStatus.DELIVERED);
        d.setDeliveryAddress(List.of(50.4, 32.6));
        sut.insert(d);

        ResponseEntity<Delivery> result = sut.deliveriesPost(dpr);
        Delivery added = result.getBody();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(added.getStatus(), DeliveryStatus.DELIVERED);
    }

    @Test
    void deliveriesDeliveryIdPrepPut() {
        // Mock data
        userType = "vendor";

        // Mock ratings and user type
        when(usersCommunication.getAccountType(userId)).thenReturn(userType);
        sut.insert(delivery);
        // Call the method
        ResponseEntity<Delivery> responseEntity = sut.deliveriesDeliveryIdPrepPut(deliveryId, userId, prepTime);

        // Verify the response
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        // Verify the returned delivery
        Delivery returned = responseEntity.getBody();
        assertEquals(returned, delivery);

        // Verify that we called the service methods and checked the user type
        verify(usersCommunication, times(1)).getAccountType(userId);
    }

    @Test
    void deliveriesDeliveryIdPrepPutForbidden() {
        // Mock data
        String userType = "courier";

        // Mock ratings and user type
        when(usersCommunication.getAccountType(userId)).thenReturn(userType);

        // Call the method
        ResponseEntity<Delivery> responseEntity = sut.deliveriesDeliveryIdPrepPut(deliveryId, userId, prepTime);

        // Verify the response
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());

        // Verify the returned delivery
        Delivery returned = responseEntity.getBody();
        assertNull(returned);

        // Verify that no inner methods were called
////        verify(deliveryService, never()).updateEstimatedPrepTime(deliveryId, prepTime);
////        verify(deliveryService, never()).getDelivery(deliveryId);
        verify(usersCommunication, times(1)).getAccountType(userId);
    }

    @Test
    void deliveriesDeliveryIdPrepPutUnauthorized() {
        // Mock data
        String userType = "non-existent";

        // Mock ratings and user type
        when(usersCommunication.getAccountType(userId)).thenReturn(userType);

        // Call the method
        ResponseEntity<Delivery> responseEntity = sut.deliveriesDeliveryIdPrepPut(deliveryId, userId, prepTime);

        // Verify the response
        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());

        // Verify the returned delivery
        Delivery returned = responseEntity.getBody();
        assertNull(returned);

        // Verify that no inner methods were called
//        verify(deliveryService, never()).updateEstimatedPrepTime(deliveryId, prepTime);
//        verify(deliveryService, never()).getDelivery(deliveryId);
        verify(usersCommunication, times(1)).getAccountType(userId);
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
        String type = "customer";

//        //Mock deliveryService/userRepo methods
        when(usersCommunication.getAccountType(userId)).thenReturn(type);
//        when(deliveryService.getDelivery(deliveryId)).thenReturn(m);
        sut.insert(m);
        ResponseEntity<Delivery> result = sut.deliveriesDeliveryIdRatingCourierPut(deliveryId, userId, 5);

        Delivery resultBody = result.getBody();
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assert resultBody != null;
        assertEquals(5, resultBody.getRatingCourier());

//        verify(deliveryService, times(1)).updateCourierRating(deliveryId, rating);
//        verify(deliveryService, times(2)).getDelivery(deliveryId);
        verify(usersCommunication, times(1)).getAccountType(userId);
    }

    @Test
    void deliveriesDeliveryIdRatingCourierPutAdmin() {
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
        String type = "admin";

//        //Mock deliveryService/userRepo methods
        when(usersCommunication.getAccountType(userId)).thenReturn(type);
        sut.insert(m);
        ResponseEntity<Delivery> result = sut.deliveriesDeliveryIdRatingCourierPut(deliveryId, userId, 0);

        Delivery resultBody = result.getBody();
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assert resultBody != null;
        assertEquals(0, resultBody.getRatingCourier());

//        verify(deliveryService, times(1)).updateCourierRating(deliveryId, rating);
//        verify(deliveryService, times(2)).getDelivery(deliveryId);
        verify(usersCommunication, times(1)).getAccountType(userId);
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
        String type = "courier";
        Integer rating = 5;

//        //Mock deliveryService/userRepo methods
        when(usersCommunication.getAccountType(courierId)).thenReturn(type);
//        when(deliveryService.getDelivery(deliveryId)).thenReturn(m);
        sut.insert(m);
        ResponseEntity<Delivery> result = sut.deliveriesDeliveryIdRatingCourierPut(deliveryId, courierId, rating);

        assertEquals(HttpStatus.valueOf(403), result.getStatusCode());

//        verify(deliveryService, times(1)).getDelivery(deliveryId);
        verify(usersCommunication, times(1)).getAccountType(courierId);
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
        String type = "customer";

//        //Mock deliveryService/userRepo methods
        when(usersCommunication.getAccountType(userId)).thenReturn(type);
        sut.insert(m);
        ResponseEntity<Delivery> result = sut.deliveriesDeliveryIdRatingRestaurantPut(deliveryId, userId, 5);

        Delivery resultBody = result.getBody();
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assert resultBody != null;
        assertEquals(5, resultBody.getRatingRestaurant());

//        verify(deliveryService, times(1)).updateRestaurantRating(deliveryId, rating);
//        verify(deliveryService, times(2)).getDelivery(deliveryId);
        verify(usersCommunication, times(1)).getAccountType(userId);
    }

    @Test
    void deliveriesDeliveryIdRatingRestaurantPutDiffCustomer() {
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
        String type = "customer";
        Integer rating = 5;

//        //Mock deliveryService/userRepo methods
        when(usersCommunication.getAccountType(userId)).thenReturn(type);
//        when(deliveryService.getDelivery(deliveryId)).thenReturn(m);
        sut.insert(m);
        ResponseEntity<Delivery> result = sut.deliveriesDeliveryIdRatingRestaurantPut(deliveryId, userId, rating);

        assertEquals(HttpStatus.valueOf(403), result.getStatusCode());

//        verify(deliveryService, times(1)).getDelivery(deliveryId);
        verify(usersCommunication, times(1)).getAccountType(userId);
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
        String type = "admin";

//        //Mock deliveryService/userRepo methods
        when(usersCommunication.getAccountType(userId)).thenReturn(type);
        sut.insert(m);
        ResponseEntity<Delivery> result = sut.deliveriesDeliveryIdRatingRestaurantPut(deliveryId, userId, 0);

        Delivery resultBody = result.getBody();
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assert resultBody != null;
        assertEquals(0, resultBody.getRatingRestaurant());

//        verify(deliveryService, times(1)).updateRestaurantRating(deliveryId, rating);
//        verify(deliveryService, times(2)).getDelivery(deliveryId);
        verify(usersCommunication, times(1)).getAccountType(userId);
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
        String type = "customer";
        Integer rating = 5;

//        //Mock deliveryService/userRepo methods
        when(usersCommunication.getAccountType(userId)).thenReturn(type);
//        when(deliveryService.getDelivery(deliveryId)).thenReturn(m);
        sut.insert(m);
        ResponseEntity<Delivery> result = sut.deliveriesDeliveryIdRatingRestaurantPut(deliveryId, userId, rating);

        assertEquals(HttpStatus.valueOf(403), result.getStatusCode());

//        verify(deliveryService, times(1)).getDelivery(deliveryId);
        verify(usersCommunication, times(1)).getAccountType(userId);
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
        String type = "vendor";

//        //Mock deliveryService/userRepo methods
        when(usersCommunication.getAccountType(restaurantId)).thenReturn(type);
//        when(deliveryService.getDelivery(deliveryId)).thenReturn(m);
        sut.insert(m);
        ResponseEntity<Integer> result = sut.deliveriesDeliveryIdRatingRestaurantGet(deliveryId, restaurantId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNull(result.getBody());

//        verify(deliveryService, times(1)).getDelivery(deliveryId);
        verify(usersCommunication, times(1)).getAccountType(restaurantId);
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
        String type = "vendor";

//        //Mock deliveryService/userRepo methods
        when(usersCommunication.getAccountType(restaurantId)).thenReturn(type);
//        when(deliveryService.getDelivery(deliveryId)).thenReturn(m);
        sut.insert(m);
        ResponseEntity<Integer> result = sut.deliveriesDeliveryIdRatingRestaurantGet(deliveryId, restaurantId);

        assertEquals(HttpStatus.valueOf(403), result.getStatusCode());

//        verify(deliveryService, times(1)).getDelivery(deliveryId);
        verify(usersCommunication, times(1)).getAccountType(restaurantId);
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
        String type = "customer";

//        //Mock deliveryService/userRepo methods
        when(usersCommunication.getAccountType(customerId)).thenReturn(type);
//        when(deliveryService.getDelivery(deliveryId)).thenReturn(m);
        sut.insert(m);
        ResponseEntity<Integer> result = sut.deliveriesDeliveryIdRatingRestaurantGet(deliveryId, customerId);

        assertEquals(HttpStatus.OK, result.getStatusCode());

//        verify(deliveryService, times(1)).getDelivery(deliveryId);
        verify(usersCommunication, times(1)).getAccountType(customerId);
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
        String type = "courier";

//        //Mock deliveryService/userRepo methods
        when(usersCommunication.getAccountType(courierId)).thenReturn(type);
//        when(deliveryService.getDelivery(deliveryId)).thenReturn(m);
        sut.insert(m);
        ResponseEntity<Integer> result = sut.deliveriesDeliveryIdRatingCourierGet(deliveryId, courierId);

        assertEquals(HttpStatus.OK, result.getStatusCode());

//        verify(deliveryService, times(1)).getDelivery(deliveryId);
        verify(usersCommunication, times(1)).getAccountType(courierId);
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
        String type = "courier";

//        //Mock deliveryService/userRepo methods
        when(usersCommunication.getAccountType(diffCourierId)).thenReturn(type);
//        when(deliveryService.getDelivery(deliveryId)).thenReturn(m);
        sut.insert(m);
        ResponseEntity<Integer> result = sut.deliveriesDeliveryIdRatingCourierGet(deliveryId, diffCourierId);

        assertEquals(HttpStatus.valueOf(403), result.getStatusCode());

//        verify(deliveryService, times(1)).getDelivery(deliveryId);
        verify(usersCommunication, times(1)).getAccountType(diffCourierId);
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
        String type = "customer";

//        //Mock deliveryService/userRepo methods
        when(usersCommunication.getAccountType(customerId)).thenReturn(type);
//        when(deliveryService.getDelivery(deliveryId)).thenReturn(m);
        sut.insert(m);

        ResponseEntity<Integer> result = sut.deliveriesDeliveryIdRatingCourierGet(deliveryId, customerId);

        assertEquals(HttpStatus.OK, result.getStatusCode());

//        verify(deliveryService, times(1)).getDelivery(deliveryId);
        verify(usersCommunication, times(1)).getAccountType(customerId);
    }
}