package nl.tudelft.sem.template.delivery.services;

import nl.tudelft.sem.template.delivery.domain.RestaurantRepository;
import nl.tudelft.sem.template.model.Restaurant;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UpdateRestaurantService {

    private final transient RestaurantRepository restaurantRepository;

    /**
    * Constructor.
    *
    * @param restaurantRepository restaurant repo
    */
    public UpdateRestaurantService(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    /**
     * Retrieve restaurant or throw an exception if not found.
     *
     * @param restaurantId restaurant id
     * @return the restaurant
     */
    public Restaurant getRestaurant(String restaurantId) {
        if (restaurantId == null) {
            throw new RestaurantService.RestaurantNotFoundException();
        }
        return restaurantRepository.findById(restaurantId).orElseThrow(RestaurantService.RestaurantNotFoundException::new);
    }


  /**
   * Updates the location.
   *
   * @param restaurantId the ID of the restaurant to be updated
   * @param requestBody location
   */
  public void updateLocation(String restaurantId, List<Double> requestBody) {
    Restaurant r = restaurantRepository.findById(restaurantId).orElseThrow(RestaurantService.RestaurantNotFoundException::new);
    r.setLocation(requestBody);
    restaurantRepository.save(r);
  }

  /**
   * Updates the delivery zone.
   *
   * @param restaurantId ID of restaurant to be updated
   * @param requestBody delivery zone
   */
  public void updateDeliverZone(String restaurantId, Double requestBody) {
    Restaurant r = restaurantRepository.findById(restaurantId).orElseThrow(RestaurantService.RestaurantNotFoundException::new);
    r.setDeliveryZone(requestBody);
    restaurantRepository.save(r);
  }

  /**
   * sets the new list of couriers or throws  an exception.
   *
   * @param restaurantId the id of the restaurant
   * @param couriers     the new couriers
   * @return the changed restaurant entity
   */
  public Restaurant setListOfCouriers(String restaurantId, List<String> couriers) {
    Restaurant r = restaurantRepository.findById(restaurantId)
        .orElseThrow(RestaurantService.RestaurantNotFoundException::new);
    r.couriers(couriers);
    restaurantRepository.save(r);
    return r;
  }
}
