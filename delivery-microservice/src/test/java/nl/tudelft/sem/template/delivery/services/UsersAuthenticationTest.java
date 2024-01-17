package nl.tudelft.sem.template.delivery.services;

import nl.tudelft.sem.template.delivery.communication.UsersCommunication;
import nl.tudelft.sem.template.model.Delivery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsersAuthenticationTest {

    @Mock
    private UsersCommunication usersCommunication;
    @InjectMocks
    private UsersAuthenticationService usersAuthentication;

    private Delivery delivery;

    @BeforeEach
    public void init() {
        delivery = new Delivery();
        delivery.setDeliveryID(UUID.randomUUID());
        delivery.setCustomerID("customerId");
        delivery.setCourierID("courierId");
        delivery.setRestaurantID("vendorId");
    }

    @Test
    public void returnsTrueWhenVendorIsAssignedToDelivery() {
        when(usersCommunication.getAccountType("vendorId")).thenReturn("vendor");
        assertThat(usersAuthentication.checkUserAccessToDelivery("vendorId", delivery)).isTrue();
    }

    @Test
    public void returnsTrueWhenCourierIsAssignedToDelivery() {
        when(usersCommunication.getAccountType("courierId")).thenReturn("courier");
        assertThat(usersAuthentication.checkUserAccessToDelivery("courierId", delivery)).isTrue();
    }

    @Test
    public void returnsTrueWhenCustomerIsAssignedToDelivery() {
        when(usersCommunication.getAccountType("customerId")).thenReturn("customer");
        assertThat(usersAuthentication.checkUserAccessToDelivery("customerId", delivery)).isTrue();
    }

    @Test
    public void returnsTrueForAdminAccess() {
        when(usersCommunication.getAccountType("adminId")).thenReturn("admin");
        assertThat(usersAuthentication.checkUserAccessToDelivery("adminId", delivery)).isTrue();
    }

    @Test
    public void returnsFalseWhenAnotherClientIsAssignedToDelivery() {
        when(usersCommunication.getAccountType("anotherClientId")).thenReturn("client");
        assertThat(usersAuthentication.checkUserAccessToDelivery("anotherClientId", delivery)).isFalse();
    }

    @Test
    public void returnsFalseWhenNoCourierIsAssignedToDelivery() {
        delivery.setCourierID(null);
        when(usersCommunication.getAccountType("courierId")).thenReturn("courier");
        assertThat(usersAuthentication.checkUserAccessToDelivery("courierId", delivery)).isFalse();
    }

    @Test
    public void returnsFalseWhenUserIsNotRecognized() {
        when(usersCommunication.getAccountType("userId")).thenReturn("Some weird value");
        assertThat(usersAuthentication.checkUserAccessToDelivery("userId", delivery)).isFalse();
    }

    @Test
    public void returnsFalseWhenUserIsNull() {
        assertThat(usersAuthentication.checkUserAccessToDelivery(null, delivery)).isFalse();
    }

}