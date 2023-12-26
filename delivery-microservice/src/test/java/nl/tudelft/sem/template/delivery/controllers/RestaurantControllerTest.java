package nl.tudelft.sem.template.delivery.controllers;

import nl.tudelft.sem.template.delivery.AddressAdapter;
import nl.tudelft.sem.template.delivery.GPS;
import nl.tudelft.sem.template.delivery.TestRepos.TestRestaurantRepository;
import nl.tudelft.sem.template.delivery.services.RestaurantService;
import nl.tudelft.sem.template.model.Restaurant;
import nl.tudelft.sem.template.model.RestaurantsPostRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.when;

class RestaurantControllerTest {

  private TestRestaurantRepository repo2;
  private RestaurantController sut;

  List<String> addr;
  List<Double> co_ord;

  @BeforeEach
  void setUp() {
    // Mock data
    addr = List.of("NL","1234AB","Amsterdam","Kalverstraat","36B");
    co_ord = List.of(32.6, 50.4);

    repo2 = new TestRestaurantRepository();
    sut = new RestaurantController(new RestaurantService(repo2), new AddressAdapter(new GPS()));
  }

  @Test
  void restaurantsPostNullRpr() {
    ResponseEntity<Restaurant> result = sut.restaurantsPost(null);

    assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
  }

  @Test
  void restaurantsInvalidAddr() {
    RestaurantsPostRequest rpr = new RestaurantsPostRequest();
    rpr.setRestaurantID("hi_im_a_vendor@testmail.com");
    rpr.setLocation(List.of());

    ResponseEntity<Restaurant> result = sut.restaurantsPost(rpr);

    assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
  }

  @Test
  void restaurantsInvalidEmail() {
    RestaurantsPostRequest rpr = new RestaurantsPostRequest();
    rpr.setRestaurantID(null);
    rpr.setLocation(addr);

    ResponseEntity<Restaurant> result = sut.restaurantsPost(rpr);

    assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
  }

  @Test
  void restaurantsEmptyEmail() {
    RestaurantsPostRequest rpr = new RestaurantsPostRequest();
    rpr.setRestaurantID("");
    rpr.setLocation(addr);

    ResponseEntity<Restaurant> result = sut.restaurantsPost(rpr);

    assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
  }

  @Test
  void restaurantsInvalidAddr2() {
    RestaurantsPostRequest rpr = new RestaurantsPostRequest();
    rpr.setRestaurantID("hi_im_a_vendor@testmail.com");
    rpr.setLocation(List.of("NL","1234AB","Amsterdam",""," "));

    ResponseEntity<Restaurant> result = sut.restaurantsPost(rpr);

    assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
  }

  @Test
  void restaurantsPost() {
    RestaurantsPostRequest rpr = new RestaurantsPostRequest();
    rpr.setRestaurantID("hi_im_a_vendor@testmail.com");
    rpr.setLocation(addr);
    //when(sut.mockGPS.getCoordinatesOfAddress(addr)).thenReturn(co_ord);
    Restaurant r = new Restaurant();
    r.setRestaurantID("hi_im_a_vendor@testmail.com");
    r.setLocation(co_ord);
    sut.insert(r);

    ResponseEntity<Restaurant> result = sut.restaurantsPost(rpr);
    Restaurant added = result.getBody();

    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertTrue(added.getLocation().get(0) > 52.3);
    assertTrue(added.getLocation().get(1) > 4.97);
  }
}