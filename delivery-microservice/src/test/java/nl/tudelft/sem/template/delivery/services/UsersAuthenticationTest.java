package nl.tudelft.sem.template.delivery.services;

import nl.tudelft.sem.template.delivery.communication.UsersCommunication;
import nl.tudelft.sem.template.model.Delivery;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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

    @Test
    public void returnsBadRequestWhenNull() {
        Pair<HttpStatus, String> result = usersAuthentication.checkUserAccessToRestaurant(null,
            null, "Location");
        assertEquals(HttpStatus.BAD_REQUEST, result.getLeft());
        assertEquals("User ID or Restaurant ID is invalid.", result.getRight());
    }

    @Test
    public void returnsForbiddenWhenNotVendor() {
        when(usersCommunication.getAccountType("not_right_vendor")).thenReturn("vendor");
        Pair<HttpStatus, String> result = usersAuthentication.checkUserAccessToRestaurant("not_right_vendor",
            "right_vendor", "Location");
        assertEquals(HttpStatus.FORBIDDEN, result.getLeft());
        assertEquals("User lacks necessary permissions.", result.getRight());
    }

    @Test
    public void returnsOkWhenVendor() {
        when(usersCommunication.getAccountType("right_vendor")).thenReturn("vendor");
        Pair<HttpStatus, String> result = usersAuthentication.checkUserAccessToRestaurant("right_vendor",
            "right_vendor", "Location");
        assertEquals(HttpStatus.OK, result.getLeft());
        assertEquals("OK", result.getRight());
    }

    @Test
    public void returnsOkWhenAdmin() {
        when(usersCommunication.getAccountType("admin")).thenReturn("admin");
        Pair<HttpStatus, String> result = usersAuthentication.checkUserAccessToRestaurant("admin",
            "right_vendor", "Location");
        assertEquals(HttpStatus.OK, result.getLeft());
        assertEquals("OK", result.getRight());
    }

    @Test
    public void returnsLocationOkWhenCourierClient() {
        when(usersCommunication.getAccountType("courier")).thenReturn("courier");
        when(usersCommunication.getAccountType("client")).thenReturn("customer");
        Pair<HttpStatus, String> result = usersAuthentication.checkUserAccessToRestaurant("courier",
            "right_vendor", "Restaurant");
        assertEquals(HttpStatus.OK, result.getLeft());
        assertEquals("OK", result.getRight());

        result = usersAuthentication.checkUserAccessToRestaurant("client",
            "right_vendor", "Restaurant");
        assertEquals(HttpStatus.OK, result.getLeft());
        assertEquals("OK", result.getRight());
    }

    @Test
    public void returnsDeliveryZoneForbiddenWhenCourierClient() {
        String expected = "Only vendors and admins can change the delivery zone of a restaurant.";
        when(usersCommunication.getAccountType("courier")).thenReturn("courier");
        when(usersCommunication.getAccountType("client")).thenReturn("customer");
        Pair<HttpStatus, String> result = usersAuthentication.checkUserAccessToRestaurant("courier",
            "right_vendor", "Delivery Zone");
        assertEquals(HttpStatus.FORBIDDEN, result.getLeft());
        assertEquals(expected, result.getRight());

        result = usersAuthentication.checkUserAccessToRestaurant("client",
            "right_vendor", "Delivery Zone");
        assertEquals(HttpStatus.FORBIDDEN, result.getLeft());
        assertEquals(expected, result.getRight());
    }

    @Test
    public void returnsLocationUnauthorizedWhenInvalid() {
        String expected = "User lacks valid authentication credentials.";
        when(usersCommunication.getAccountType(any())).thenReturn("weird value");
        Pair<HttpStatus, String> result = usersAuthentication.checkUserAccessToRestaurant("qwerty",
            "right_vendor", "Location");
        assertEquals(HttpStatus.UNAUTHORIZED, result.getLeft());
        assertEquals(expected, result.getRight());
    }

    @Test
    public void returnsLocationForbiddenWhenCourierClient() {
        String expected = "Only vendors and admins can change the restaurant's address";
        when(usersCommunication.getAccountType("courier")).thenReturn("courier");
        when(usersCommunication.getAccountType("client")).thenReturn("customer");
        Pair<HttpStatus, String> result = usersAuthentication.checkUserAccessToRestaurant("courier",
            "right_vendor", "Location");
        assertEquals(HttpStatus.FORBIDDEN, result.getLeft());
        assertEquals(expected, result.getRight());

        result = usersAuthentication.checkUserAccessToRestaurant("client",
            "right_vendor", "Location");
        assertEquals(HttpStatus.FORBIDDEN, result.getLeft());
        assertEquals(expected, result.getRight());
    }

    @Test
    public void returnsCouriersForbiddenWhenCourierClient() {
        String expected = "User lacks necessary permissions.";
        when(usersCommunication.getAccountType("courier")).thenReturn("courier");
        when(usersCommunication.getAccountType("client")).thenReturn("customer");
        Pair<HttpStatus, String> result = usersAuthentication.checkUserAccessToRestaurant("courier",
            "right_vendor", "Couriers");
        assertEquals(HttpStatus.FORBIDDEN, result.getLeft());
        assertEquals(expected, result.getRight());

        result = usersAuthentication.checkUserAccessToRestaurant("client",
            "right_vendor", "Couriers");
        assertEquals(HttpStatus.FORBIDDEN, result.getLeft());
        assertEquals(expected, result.getRight());
    }

    @Test
    public void returnsDphForbiddenWhenCourierClient() {
        String expected = "User lacks necessary permissions.";
        when(usersCommunication.getAccountType("courier")).thenReturn("courier");
        when(usersCommunication.getAccountType("client")).thenReturn("customer");
        Pair<HttpStatus, String> result = usersAuthentication.checkUserAccessToRestaurant("courier",
            "right_vendor", "DPH");
        assertEquals(HttpStatus.FORBIDDEN, result.getLeft());
        assertEquals(expected, result.getRight());

        result = usersAuthentication.checkUserAccessToRestaurant("client",
            "right_vendor", "DPH");
        assertEquals(HttpStatus.FORBIDDEN, result.getLeft());
        assertEquals(expected, result.getRight());
    }

    @Test
    public void returnsNewOrderForbiddenWhenCourierClient() {
        String expected = "User lacks necessary permissions.";
        when(usersCommunication.getAccountType("courier")).thenReturn("courier");
        when(usersCommunication.getAccountType("client")).thenReturn("customer");
        Pair<HttpStatus, String> result = usersAuthentication.checkUserAccessToRestaurant("courier",
            "right_vendor", "New Order");
        assertEquals(HttpStatus.FORBIDDEN, result.getLeft());
        assertEquals(expected, result.getRight());

        result = usersAuthentication.checkUserAccessToRestaurant("client",
            "right_vendor", "New Order");
        assertEquals(HttpStatus.FORBIDDEN, result.getLeft());
        assertEquals(expected, result.getRight());
    }
}