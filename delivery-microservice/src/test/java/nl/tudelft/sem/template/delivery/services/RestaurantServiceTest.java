package nl.tudelft.sem.template.delivery.services;

import nl.tudelft.sem.template.delivery.TestRepos.TestDeliveryRepository;
import nl.tudelft.sem.template.delivery.TestRepos.TestRestaurantRepository;
import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.RestaurantRepository;
import nl.tudelft.sem.template.model.Restaurant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
@EntityScan("nl.tudelft.sem.template.*")
@ExtendWith(MockitoExtension.class)
@Transactional
@DataJpaTest
public class RestaurantServiceTest {
    @Autowired
    private RestaurantRepository rr;
    private RestaurantService rs;
    @Autowired
    private DeliveryRepository dr;
    private DeliveryService ds;

    @BeforeEach
    public void setup() {
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
        rs.insert(r);
        assertThat(rs.getRestaurant("bla")).isEqualTo(r);

    }
    @Test
    public void insertThrowsExceptionNullIdTest(){
        assertThrows(RestaurantService.IllegalRestaurantParametersException.class, () -> {
            Restaurant r = new Restaurant();
            r.setRestaurantID(null);
            r.setLocation(List.of(12.2,13.3));
            rs.insert(r);
        });
    }
    @Test
    public void insertThrowsExceptionNullLocationTest(){
        assertThrows(RestaurantService.IllegalRestaurantParametersException.class, () -> {
            Restaurant r = new Restaurant();
            r.setRestaurantID("bla");
            rs.insert(r);
        });
    }

    @Test
    public void insertThrowsExceptionWrongCoordinatesTest(){
        assertThrows(RestaurantService.IllegalRestaurantParametersException.class, () -> {
            Restaurant r = new Restaurant();
            r.setRestaurantID("bla");
            r.setLocation(List.of(11.1,12.2,342.3));
            rs.insert(r);
        });
    }
    @Test
    public void insertTestThrowsRestaurantAlreadyThere(){
        Restaurant r = new Restaurant();
        r.setRestaurantID("bla");
        r.setLocation(List.of(11.1,12.2));
        rs.insert(r);

        assertThrows(RestaurantService.IllegalRestaurantParametersException.class, () -> {
            Restaurant r1 = new Restaurant();
            r1.setRestaurantID("bla");
            r1.setLocation(List.of(11.1,12.3));
            rs.insert(r1);
        });

    }

    @Test
    public void setListOfCouriersTest(){
        Restaurant r = new Restaurant();
        r.setRestaurantID("bla");
        r.setLocation(List.of(11.1,12.2));
        List<String> list = new ArrayList<>();
        list.add("sjdfhbuwfbieg");
        rs.insert(r);
        rs.setListOfCouriers("bla", list);
        assertThat(rs.getRestaurant("bla").getCouriers()).isEqualTo(List.of("sjdfhbuwfbieg"));
    }





}
