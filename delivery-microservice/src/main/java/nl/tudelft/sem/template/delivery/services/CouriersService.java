package nl.tudelft.sem.template.delivery.services;

import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.RestaurantRepository;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.Restaurant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CouriersService {

  private final DeliveryRepository deliveryRepository;

  private final RestaurantRepository restaurantRepository;

  /**
   * Constructor for CouriersService
    * @param deliveryRepository database for deliveries
    * @param restaurantRepository database for restaurants
   */
  @Autowired
  public CouriersService(DeliveryRepository deliveryRepository, RestaurantRepository restaurantRepository) {
    this.deliveryRepository = deliveryRepository;
    this.restaurantRepository = restaurantRepository;
  }

  /**
   * Retrieves a list of all deliveries assigned to a courier
   * @param courierId the id of the courier
   * @return the list of delivery ids
   */
  public List<UUID> getDeliveriesForACourier(String courierId){
    return deliveryRepository.findAllByCourierID(courierId)
        .stream()
        .map(Delivery::getDeliveryID)
        .collect(Collectors.toList());
  }

  /**
   * Checks if courier belongs to a restaurant
   * @param courierId id of courier being checked
   * @return boolean value indicating if courier belongs to a restaurant
   */
  public boolean courierBelongsToRestaurant(String courierId) {
    Optional<Restaurant> restaurant = restaurantRepository.findRestaurantByCouriersContains(courierId);
    return restaurant.isPresent();
  }

  public List<Delivery> getDeliveriesForCourierRatings(String courierId){
    return deliveryRepository.findAllByCourierID(courierId);
  }
}
