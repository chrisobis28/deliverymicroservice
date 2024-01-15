package nl.tudelft.sem.template.delivery.controllers;

import nl.tudelft.sem.template.delivery.GPS;
import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.ErrorRepository;
import nl.tudelft.sem.template.delivery.domain.RestaurantRepository;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.delivery.services.ErrorService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import nl.tudelft.sem.template.model.Error;
import nl.tudelft.sem.template.model.ErrorType;
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

import javax.transaction.Transactional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@EntityScan("nl.tudelft.sem.template.*")
@ExtendWith(MockitoExtension.class)
@Transactional
@DataJpaTest
class ErrorControllerTest {
    @Mock
    private UsersAuthenticationService usersCommunication;
    @Autowired
    private ErrorRepository repo1;
    @Autowired
    private DeliveryRepository repo2;
    @Autowired
    private RestaurantRepository repo3;
    private ErrorController sut;
    private DeliveryController deliveryController;

    String userId, reason;

    UsersAuthenticationService.AccountType userType;
    UUID deliveryId;
    Integer value;
    ErrorType errorType;
    Delivery delivery;
    Error error;

    UUID updateID;
    ErrorType updateType;
    String updateReason;
    Integer updateValue;
    Error updateError;

    @BeforeEach
    void setUp() {
        // Fields
        userId = "user@example.org";
        deliveryId = UUID.randomUUID();

        // Entities
        error = new Error();
        delivery = new Delivery();
        error.setErrorId(deliveryId);

        delivery.setDeliveryID(deliveryId);
        delivery.setError(error);

        // Update Entity
        updateID = UUID.randomUUID();
        updateType = ErrorType.OTHER;
        updateReason = "Some more compelling updated reason!";
        updateValue = null;
        updateError = new Error();
        updateError.setType(updateType);
        updateError.setReason(updateReason);
        updateError.setValue(updateValue);

        // Services and dependency injections
        DeliveryService deliveryService = new DeliveryService(repo2, new GPS(), repo3);
        ErrorService errorService = new ErrorService(repo1, repo2);
        usersCommunication = mock(UsersAuthenticationService.class);

        // Controllers
        sut = new ErrorController(errorService, deliveryService, usersCommunication);
        deliveryController = new DeliveryController(deliveryService, usersCommunication, null);
    }

    @Test
    void errorsDeliveryIdGetAdmin() {
        // Set Data
        userType = UsersAuthenticationService.AccountType.ADMIN;
        errorType = ErrorType.CANCELLED;
        reason = "Food ingredients are not currently available to prepare your order. Apologies for the inconvenience.";
        value = null;
        error.setType(errorType);
        error.setReason(reason);
        when(usersCommunication.getUserAccountType(userId)).thenReturn(userType);

        // Persist in Test Repos
        sut.insert(error);
        deliveryController.insert(delivery);

        // Act
        ResponseEntity<Error> result = sut.errorsDeliveryIdGet(userId, deliveryId);
        Error getError = result.getBody();

        // Assert
        verify(usersCommunication, times(1)).getUserAccountType(userId);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(getError);
        assertEquals(deliveryId, getError.getErrorId());
        assertEquals(errorType, getError.getType());
        assertEquals(reason, getError.getReason());
        assertEquals(value, getError.getValue());
    }

    @Test
    void errorsDeliveryIdGetVendor() {
        // Set Data
        userType = UsersAuthenticationService.AccountType.VENDOR;
        errorType = ErrorType.DELAYED;
        reason = "Due to busy schedule, order will be 20 minutes late.";
        value = 20;
        error.setType(errorType);
        error.setReason(reason);
        error.setValue(value);
        delivery.setRestaurantID(userId);
        when(usersCommunication.getUserAccountType(userId)).thenReturn(userType);
        when(usersCommunication.checkUserAccessToDelivery(userId, delivery)).thenReturn(true);

        // Persist in Test Repos
        ResponseEntity<Void> res = sut.insert(error);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        deliveryController.insert(delivery);

        // Act
        ResponseEntity<Error> result = sut.errorsDeliveryIdGet(userId, deliveryId);
        Error getError = result.getBody();

        // Assert
        verify(usersCommunication, times(1)).getUserAccountType(userId);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(getError);
        assertEquals(deliveryId, getError.getErrorId());
        assertEquals(errorType, getError.getType());
        assertEquals(reason, getError.getReason());
        assertEquals(value, getError.getValue());
    }

    @Test
    void errorsDeliveryIdGetCourier() {
        // Set Data
        userType = UsersAuthenticationService.AccountType.COURIER;
        errorType = ErrorType.NONE;
        error.setType(errorType);
        delivery.setCourierID(userId);
        when(usersCommunication.getUserAccountType(userId)).thenReturn(userType);
        when(usersCommunication.checkUserAccessToDelivery(userId, delivery)).thenReturn(true);

        // Persist in Test Repos
        sut.insert(error);
        deliveryController.insert(delivery);

        // Act
        ResponseEntity<Error> result = sut.errorsDeliveryIdGet(userId, deliveryId);
        Error getError = result.getBody();

        // Assert
        verify(usersCommunication, times(1)).getUserAccountType(userId);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(getError);
        assertEquals(deliveryId, getError.getErrorId());
        assertEquals(errorType, getError.getType());
        assertEquals(reason, getError.getReason());
        assertEquals(value, getError.getValue());
    }

    @Test
    void errorsDeliveryIdGetCustomer() {
        // Set Data
        userType = UsersAuthenticationService.AccountType.CLIENT;
        errorType = ErrorType.OTHER;
        reason = "Payment was unsuccessful. Customer intervention expected.";
        value = null;
        error.setType(errorType);
        error.setReason(reason);
        delivery.setCustomerID(userId);
        when(usersCommunication.getUserAccountType(userId)).thenReturn(userType);
        when(usersCommunication.checkUserAccessToDelivery(userId, delivery)).thenReturn(true);

        // Persist in Test Repos
        sut.insert(error);
        deliveryController.insert(delivery);

        // Act
        ResponseEntity<Error> result = sut.errorsDeliveryIdGet(userId, deliveryId);
        Error getError = result.getBody();

        // Assert
        verify(usersCommunication, times(1)).getUserAccountType(userId);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(getError);
        assertEquals(deliveryId, getError.getErrorId());
        assertEquals(errorType, getError.getType());
        assertEquals(reason, getError.getReason());
        assertEquals(value, getError.getValue());
    }

    @Test
    void errorsDeliveryIdGetCustomerForbidden() {
        // Set Data
        String otherUserId = "other@user.org";
        userType = UsersAuthenticationService.AccountType.CLIENT;
        errorType = ErrorType.OTHER;
        reason = "Payment was unsuccessful. Customer intervention expected.";
        error.setType(errorType);
        error.setReason(reason);
        delivery.setCustomerID(otherUserId);
        when(usersCommunication.getUserAccountType(userId)).thenReturn(userType);

        // Persist in Test Repos
        sut.insert(error);
        deliveryController.insert(delivery);

        // Act
        assertThatThrownBy(() -> sut.errorsDeliveryIdGet(userId, deliveryId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.errorsDeliveryIdGet(userId, deliveryId))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");

        // Assert
        verify(usersCommunication, times(2)).getUserAccountType(userId);
    }

    @Test
    void errorsDeliveryIdGetUnauthorized() {
        // Set Data
        userType = UsersAuthenticationService.AccountType.INVALID;
        errorType = ErrorType.OTHER;
        reason = "Payment was unsuccessful. Customer intervention expected.";
        error.setType(errorType);
        error.setReason(reason);
        when(usersCommunication.getUserAccountType(userId)).thenReturn(userType);

        // Persist in Test Repos
        sut.insert(error);
        deliveryController.insert(delivery);

        // Act
        assertThatThrownBy(() -> sut.errorsDeliveryIdGet(userId, deliveryId))
            .extracting("status")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThatThrownBy(() -> sut.errorsDeliveryIdGet(userId, deliveryId))
            .message()
            .isEqualTo("401 UNAUTHORIZED \"User lacks valid authentication credentials.\"");

        // Assert
        verify(usersCommunication, times(2)).getUserAccountType(userId);
    }

    @Test
    void errorsDeliveryIdGetNoUserID() {
        // Set Data
        String emptyUserId = "";
        userType = UsersAuthenticationService.AccountType.ADMIN;
        errorType = ErrorType.OTHER;
        reason = "Payment was unsuccessful. Customer intervention expected.";
        error.setType(errorType);
        error.setReason(reason);

        // Persist in Test Repos
        sut.insert(error);
        deliveryController.insert(delivery);

        // Act
        assertThatThrownBy(() -> sut.errorsDeliveryIdGet(emptyUserId, deliveryId))
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> sut.errorsDeliveryIdGet(emptyUserId, deliveryId))
            .message()
            .isEqualTo("400 BAD_REQUEST \"User ID is invalid.\"");

        // Assert
        verify(usersCommunication, never()).getUserAccountType(any(String.class));
    }

    @Test
    void errorsDeliveryIdPutAdmin() {
        // Set Data
        userType = UsersAuthenticationService.AccountType.ADMIN;
        errorType = ErrorType.CANCELLED;
        reason = "Food ingredients are not currently available to prepare your order. Apologies for the inconvenience.";
        value = null;
        error.setType(errorType);
        error.setReason(reason);
        delivery.setStatus(DeliveryStatus.ON_TRANSIT);
        when(usersCommunication.getUserAccountType(userId)).thenReturn(userType);

        // Persist in Test Repos
        sut.insert(error);
        deliveryController.insert(delivery);

        // Act
        ResponseEntity<Error> result = sut.errorsDeliveryIdPut(userId, deliveryId, updateError);
        Error getError = result.getBody();

        // Assert
        verify(usersCommunication, times(1)).getUserAccountType(userId);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(getError);
        assertEquals(deliveryId, getError.getErrorId());
        assertEquals(updateType, getError.getType());
        assertEquals(updateReason, getError.getReason());
        assertEquals(updateValue, getError.getValue());
    }

    @Test
    void errorsDeliveryIdPutVendor() {
        // Set Data
        userType = UsersAuthenticationService.AccountType.VENDOR;
        errorType = ErrorType.CANCELLED;
        reason = "Food ingredients are not currently available to prepare your order. Apologies for the inconvenience.";
        value = null;
        error.setType(errorType);
        error.setReason(reason);
        delivery.setStatus(DeliveryStatus.ON_TRANSIT);
        delivery.setRestaurantID(userId);
        when(usersCommunication.getUserAccountType(userId)).thenReturn(userType);
        when(usersCommunication.checkUserAccessToDelivery(userId, delivery)).thenReturn(true);

        // Persist in Test Repos
        sut.insert(error);
        deliveryController.insert(delivery);

        // Act
        ResponseEntity<Error> result = sut.errorsDeliveryIdPut(userId, deliveryId, updateError);
        Error getError = result.getBody();

        // Assert
        verify(usersCommunication, times(1)).getUserAccountType(userId);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(getError);
        assertEquals(deliveryId, getError.getErrorId());
        assertEquals(updateType, getError.getType());
        assertEquals(updateReason, getError.getReason());
        assertEquals(updateValue, getError.getValue());
    }

    @Test
    void errorsDeliveryIdPutCourier() {
        // Set Data
        userType = UsersAuthenticationService.AccountType.COURIER;
        errorType = ErrorType.CANCELLED;
        reason = "Food ingredients are not currently available to prepare your order. Apologies for the inconvenience.";
        value = null;
        error.setType(errorType);
        error.setReason(reason);
        delivery.setStatus(DeliveryStatus.ON_TRANSIT);
        delivery.setCourierID(userId);
        when(usersCommunication.getUserAccountType(userId)).thenReturn(userType);
        when(usersCommunication.checkUserAccessToDelivery(userId, delivery)).thenReturn(true);

        // Persist in Test Repos
        sut.insert(error);
        deliveryController.insert(delivery);

        // Act
        ResponseEntity<Error> result = sut.errorsDeliveryIdPut(userId, deliveryId, updateError);
        Error getError = result.getBody();

        // Assert
        verify(usersCommunication, times(1)).getUserAccountType(userId);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(getError);
        assertEquals(deliveryId, getError.getErrorId());
        assertEquals(updateType, getError.getType());
        assertEquals(updateReason, getError.getReason());
        assertEquals(updateValue, getError.getValue());
    }

    @Test
    void errorsDeliveryIdPutCourierForbidden() {
        // Set Data
        userType = UsersAuthenticationService.AccountType.COURIER;
        errorType = ErrorType.CANCELLED;
        reason = "Food ingredients are not currently available to prepare your order. Apologies for the inconvenience.";
        value = null;
        error.setType(errorType);
        error.setReason(reason);
        delivery.setStatus(DeliveryStatus.ON_TRANSIT);
        delivery.setCourierID(userId);
        when(usersCommunication.getUserAccountType(userId)).thenReturn(userType);
        when(usersCommunication.checkUserAccessToDelivery(userId, delivery)).thenReturn(false);

        // Persist in Test Repos
        sut.insert(error);
        deliveryController.insert(delivery);

        // Act
        assertThatThrownBy(() -> sut.errorsDeliveryIdPut(userId, deliveryId, updateError))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.errorsDeliveryIdPut(userId, deliveryId, updateError))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");

        // Assert
        verify(usersCommunication, times(2)).getUserAccountType(userId);
    }

    @Test
    void errorsDeliveryIdPutCustomer() {
        // Set Data
        userType = UsersAuthenticationService.AccountType.CLIENT;
        errorType = ErrorType.CANCELLED;
        reason = "Food ingredients are not currently available to prepare your order. Apologies for the inconvenience.";
        value = null;
        error.setType(errorType);
        error.setReason(reason);
        delivery.setStatus(DeliveryStatus.ON_TRANSIT);
        delivery.setCourierID(userId);
        when(usersCommunication.getUserAccountType(userId)).thenReturn(userType);

        // Persist in Test Repos
        sut.insert(error);
        deliveryController.insert(delivery);

        // Act
        assertThatThrownBy(() -> sut.errorsDeliveryIdPut(userId, deliveryId, updateError))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.errorsDeliveryIdPut(userId, deliveryId, updateError))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");

        // Assert
        verify(usersCommunication, times(2)).getUserAccountType(userId);
    }

    @Test
    void errorsDeliveryIdPutUnauthorized() {
        // Set Data
        userType = UsersAuthenticationService.AccountType.INVALID;
        errorType = ErrorType.CANCELLED;
        reason = "Food ingredients are not currently available to prepare your order. Apologies for the inconvenience.";
        value = null;
        error.setType(errorType);
        error.setReason(reason);
        delivery.setStatus(DeliveryStatus.ON_TRANSIT);
        delivery.setCourierID(userId);
        when(usersCommunication.getUserAccountType(userId)).thenReturn(userType);

        // Persist in Test Repos
        sut.insert(error);
        deliveryController.insert(delivery);

        // Act
        assertThatThrownBy(() -> sut.errorsDeliveryIdPut(userId, deliveryId, updateError))
            .extracting("status")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThatThrownBy(() -> sut.errorsDeliveryIdPut(userId, deliveryId, updateError))
            .message()
            .isEqualTo("401 UNAUTHORIZED \"User lacks valid authentication credentials.\"");

        // Assert
        verify(usersCommunication, times(2)).getUserAccountType(userId);
    }

    @Test
    void errorsDeliveryIdPutNoUserId() {
        // Set Data
        String emptyUserId = "";
        userType = UsersAuthenticationService.AccountType.CLIENT;
        errorType = ErrorType.CANCELLED;
        reason = "Food ingredients are not currently available to prepare your order. Apologies for the inconvenience.";
        value = null;
        error.setType(errorType);
        error.setReason(reason);
        delivery.setStatus(DeliveryStatus.ON_TRANSIT);
        delivery.setCourierID(userId);

        // Persist in Test Repos
        sut.insert(error);
        deliveryController.insert(delivery);

        // Act
        assertThatThrownBy(() -> sut.errorsDeliveryIdPut(emptyUserId, deliveryId, updateError))
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> sut.errorsDeliveryIdPut(emptyUserId, deliveryId, updateError))
            .message()
            .isEqualTo("400 BAD_REQUEST \"User ID is invalid.\"");

        // Assert
        verify(usersCommunication, never()).getUserAccountType(any(String.class));

    }

    @Test
    void isWhiteSpace() {
        assertTrue(sut.isNullOrEmpty(" "));
    }

    @Test
    void isEmpty() {
        assertTrue(sut.isNullOrEmpty(""));
    }

    @Test
    void isNull() {
        assertTrue(sut.isNullOrEmpty(null));
    }

    @Test
    void insertThrowsIllegalArgument() {
        assertThatThrownBy(() -> sut.insert(null))
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> sut.insert(null))
            .message()
            .isEqualTo("400 BAD_REQUEST \"BAD REQUEST\"");
    }
}