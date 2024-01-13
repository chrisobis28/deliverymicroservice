package nl.tudelft.sem.template.delivery.services;

import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.RestaurantRepository;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.Restaurant;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CouriersService {

  private final DeliveryRepository deliveryRepository;

  private final RestaurantRepository restaurantRepository;

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

  public Restaurant insert(Restaurant restaurant) {
    if (restaurant.getRestaurantID() == null || restaurant.getLocation() ==null ||
        restaurant.getLocation().size()!=2 ) {
      throw new RestaurantService.IllegalRestaurantParametersException();
    }
    return restaurantRepository.save(restaurant);
  }

  public Delivery insert(Delivery delivery) {
    if (delivery == null) {
      throw new IllegalArgumentException();
    }
    return deliveryRepository.save(delivery);
  }

  /**
   * Checks if courier belongs to a restaurant
   * @param courierId id of courier being checked
   * @return boolean value indicating if courier belongs to a restaurant
   */
  public boolean courierBelongsToRestaurant(String courierId) {
    List<String> list = new ArrayList<>();
    restaurantRepository.findAll().forEach(r -> {
      if (r.getCouriers() != null) list.addAll(r.getCouriers());
    });
    return list.contains(courierId);
  }
}
