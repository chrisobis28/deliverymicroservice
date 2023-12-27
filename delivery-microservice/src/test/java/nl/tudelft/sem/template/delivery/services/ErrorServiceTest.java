package nl.tudelft.sem.template.delivery.services;

import nl.tudelft.sem.template.delivery.TestRepos.TestDeliveryRepository;
import nl.tudelft.sem.template.delivery.TestRepos.TestErrorRepository;
import nl.tudelft.sem.template.delivery.TestRepos.TestRestaurantRepository;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import nl.tudelft.sem.template.model.Error;
import nl.tudelft.sem.template.model.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ErrorServiceTest {

    private ErrorService sut;
    private DeliveryService deliveryService;


    @BeforeEach
    void setUp() {
        TestErrorRepository repo1 = new TestErrorRepository();
        TestDeliveryRepository repo2 = new TestDeliveryRepository();
        TestRestaurantRepository repo3 = new TestRestaurantRepository();
        sut = new ErrorService(repo1, repo2);
        deliveryService = new DeliveryService(repo2, repo3);
    }

    @Test
    void getErrorNotFound() {
        UUID deliveryId = UUID.randomUUID();
        assertThrows(ErrorService.ErrorNotFoundException.class, () -> sut.getError(deliveryId));

    }

    @Test
    void getError() {
        UUID deliveryId = UUID.randomUUID();
        Error error = new Error();
        error.setErrorId(deliveryId);
        error.setReason("Some compelling reason to get no food.");
        error.setType(ErrorType.OTHER);
        sut.insert(error);
        assertEquals(error, sut.getError(deliveryId));
    }

    @Test
    void updateErrorDelivered() {
        // Set Data
        UUID deliveryId = UUID.randomUUID();
        Error error = new Error();
        error.setErrorId(deliveryId);
        error.setType(ErrorType.OTHER);
        error.setReason("Some compelling reason to get no food.");

        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setStatus(DeliveryStatus.DELIVERED);
        delivery.setError(error);
        error.setDelivery(delivery);

        // Persist in repositories
        sut.insert(error);
        deliveryService.insert(delivery);

        // Create updated entity
        UUID updateId = UUID.randomUUID();
        String updateReason = "Even more compelling reason not to get food.";
        ErrorType updateType = ErrorType.CANCELLED;
        Integer updateValue = Integer.MAX_VALUE;
        Error updateError = new Error();
        updateError.setErrorId(updateId);
        updateError.setReason(updateReason);
        updateError.setType(updateType);
        updateError.setValue(updateValue);

        // Act and Assert
        assertThrows(DeliveryService.OrderAlreadyDeliveredException.class, () -> sut.updateError(deliveryId, updateError));
    }

    @Test
    void updateError() {
        // Set Data
        UUID deliveryId = UUID.randomUUID();
        Error error = new Error();
        error.setErrorId(deliveryId);
        error.setType(ErrorType.OTHER);
        error.setReason("Some compelling reason to get no food.");

        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setStatus(DeliveryStatus.ACCEPTED);
        delivery.setError(error);
        error.setDelivery(delivery);

        // Persist in repositories
        sut.insert(error);
        deliveryService.insert(delivery);

        // Create updated entity
        UUID updateId = UUID.randomUUID();
        String updateReason = "Even more compelling reason not to get food.";
        ErrorType updateType = ErrorType.CANCELLED;
        Integer updateValue = Integer.MAX_VALUE;
        Error updateError = new Error();
        updateError.setErrorId(updateId);
        updateError.setReason(updateReason);
        updateError.setType(updateType);
        updateError.setValue(updateValue);
        // Act
        Error result = sut.updateError(deliveryId, updateError);

        // Assert
        assertEquals(deliveryId, result.getErrorId());
        assertEquals(updateReason, result.getReason());
        assertEquals(updateType, result.getType());
        assertEquals(updateValue, result.getValue());
    }

    @Test
    void insertThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> sut.insert(null));
    }
}