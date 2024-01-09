package nl.tudelft.sem.template.delivery.services;

import nl.tudelft.sem.template.delivery.TestRepos.TestDeliveryRepository;
import nl.tudelft.sem.template.delivery.TestRepos.TestRestaurantRepository;
import nl.tudelft.sem.template.model.Restaurant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RestaurantServiceTest {
    private TestRestaurantRepository rr;
    private RestaurantService rs;
    private TestDeliveryRepository dr;
    private DeliveryService ds;

    @BeforeEach
    public void setup() {
        rr = new TestRestaurantRepository();
        dr = new TestDeliveryRepository();
        rs = new RestaurantService(rr,dr);
        ds = new DeliveryService(dr,rr);

    }
    @Test
    public void getRestaurantThrowsExceptionTest(){
        assertThrows(RestaurantService.RestaurantNotFoundException.class, () -> {
            rs.getRestaurant("bla");
        });
    }

    @Test
    public void getRestaurantThrowsExceptionNullTest(){
        assertThrows(RestaurantService.RestaurantNotFoundException.class, () -> {
            rs.getRestaurant(null);
        });
    }

    @Test
    public void getRestaurantTest(){
        Restaurant r = new Restaurant();
        r.setRestaurantID("bla");
        r.setLocation(List.of(12.2,13.3));
        rs.insert("bla", List.of(12.2,13.3));
        assertThat(rs.getRestaurant("bla")).isEqualTo(r);

    }
    @Test
    public void insertThrowsExceptionNullIdTest(){
        assertThrows(RestaurantService.IllegalRestaurantParametersException.class, () -> {
            rs.insert(null, List.of(12.2,13.3));
        });
    }
    @Test
    public void insertThrowsExceptionNullLocationTest(){
        assertThrows(RestaurantService.IllegalRestaurantParametersException.class, () -> {
            rs.insert("bla", null);
        });
    }

    @Test
    public void insertThrowsExceptionWrongCoordinatesTest(){
        assertThrows(RestaurantService.IllegalRestaurantParametersException.class, () -> {
            rs.insert("bla", List.of(11.1,12.2,342.3));
        });
    }
    @Test
    public void insertTestThrowsRestaurantAlreadyThere(){
        rs.insert("bla", List.of(12.2,13.3));
        assertThrows(RestaurantService.IllegalRestaurantParametersException.class, () -> {
            rs.insert("bla", List.of(11.1,12.2));
        });

    }

    @Test
    public void setListOfCouriersTest(){
        rs.insert("bla", List.of(12.2,13.3));
        rs.setListOfCouriers("bla", List.of("bdsjkbds"));
        assertThat(rs.getRestaurant("bla").getCouriers()).isEqualTo(List.of("bdsjkbds"));

    }





}
