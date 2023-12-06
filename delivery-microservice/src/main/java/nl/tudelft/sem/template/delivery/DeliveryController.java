package nl.tudelft.sem.template.delivery;

//import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import nl.tudelft.sem.template.api.DeliveriesApi;
import nl.tudelft.sem.template.model.Delivery;
//import org.hibernate.annotations.Parameter;
import org.springframework.http.ResponseEntity;
import static org.mockito.Mockito.*;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
//import java.util.Optional;
import java.util.UUID;

//Mock of the User repository
//@Component
interface UserRepository {
  String getUserType(String email);
}

//Mock of the order repository - we'll need it later, so I'm keeping it in delivery controller, even though I don't need it atm
/*@Component
interface OrderRepository {
  String getUserEmail(UUID orderId);
}*/

@RestController
@RequestMapping("/deliveries")
public class DeliveryController implements DeliveriesApi {
  private final DeliveryRepository repo;
  //@Mock
  UserRepository userMockRepo = mock(UserRepository.class);
  //private final OrderRepository orderMockRepo;
  /**
   * Constructor for DeliveryController class
   * @param repo - repository where delivery objects are stored
   */
  public DeliveryController(DeliveryRepository repo/*, UserRepository userMockRepo, OrderRepository orderMockRepo*/) {
    this.repo = repo;
  }

  /**
   * Checks if a string is null or empty
   * @param str string to check
   * @return boolean value indicating whether string is empty or not
   */
  public boolean isNullOrEmpty(String str) {
    return str == null || str.isEmpty() || str.equals(" ");
  }

  /**
   * Allows the customer to update a courier's rating
   * @param deliveryId ID of the Delivery entity (required)
   * @param userId User ID for authorization (required)
   * @param body Update rating of delivery (required)
   * @return Response entity containing the updated Delivery object
   */
  @Override
  @RequestMapping(
      method = {RequestMethod.PUT},
      value = {"/{deliveryId}/rating-courier"},
      produces = {"application/json"},
      consumes = {"application/json"}
  )
  public ResponseEntity<Delivery> deliveriesDeliveryIdRatingCourierPut(@PathVariable("deliveryId") UUID deliveryId, @RequestHeader @NotNull String userId, @RequestBody @Valid Integer body) {
    if (isNullOrEmpty(userId)) {
      return ResponseEntity.badRequest().build();
    }
    if (repo.findById(deliveryId).isEmpty() || !repo.existsById(deliveryId)) {
      return ResponseEntity.notFound().build();
    } else {
      Delivery delivery = repo.findById(deliveryId).get();
      //Call user endpoint that verifies the role of user path:"/account/type"
      String type = userMockRepo.getUserType(userId);
      String email = delivery.getCustomerID();
      boolean isCustomer = userId.equals(email) && type.equals("customer");
      if (!isCustomer && !type.equals("admin")) {
        return ResponseEntity.status(403).build();
      } else {
        delivery.setRatingCourier(body);
        repo.save(delivery);
        return ResponseEntity.ok(delivery);
      }
    }
  }

  @Override
  @RequestMapping(
      method = {RequestMethod.PUT},
      value = {"/{deliveryId}/rating-restaurant"},
      produces = {"application/json"},
      consumes = {"application/json"}
  )
  public ResponseEntity<Delivery> deliveriesDeliveryIdRatingRestaurantPut(/*@Parameter(name = "deliveryId",description = "ID of the Delivery entity",required = true,in = ParameterIn.PATH) */@PathVariable("deliveryId") UUID deliveryId, /*@Parameter(name = "userId",description = "User ID for authorization",required = true,in = ParameterIn.HEADER)*/ @RequestHeader @NotNull String userId, /*@Parameter(name = "body",description = "Update rating of restaurant for delivery",required = true) */@RequestBody @Valid Integer body) {
    if (isNullOrEmpty(userId)) {
      return ResponseEntity.badRequest().build();
    }
    if (repo.findById(deliveryId).isEmpty() || !repo.existsById(deliveryId)) {
      return ResponseEntity.notFound().build();
    } else {
      Delivery delivery = repo.findById(deliveryId).get();
      //Call user endpoint that verifies the role of user path:"/account/type"
      String type = userMockRepo.getUserType(userId);
      String email = delivery.getCustomerID();//orderMockRepo.getUserEmail(deliveryId);
      boolean isCustomer = userId.equals(email) && type.equals("customer");
      if (!isCustomer && !type.equals("admin")) {
        return ResponseEntity.status(403).build();
      } else {
        delivery.setRatingRestaurant(body);
        repo.save(delivery);
        return ResponseEntity.ok(delivery);
      }
    }
  }

  @Override
  @RequestMapping(
      method = {RequestMethod.GET},
      value = {"/{deliveryId}/rating-restaurant"},
      produces = {"application/json"}
  )
  public ResponseEntity<Integer> deliveriesDeliveryIdRatingRestaurantGet(@Parameter(name = "deliveryId",description = "ID of the Delivery entity",required = true,in = ParameterIn.PATH) @PathVariable("deliveryId") UUID deliveryId, @Parameter(name = "userId",description = "User ID for authorization",required = true,in = ParameterIn.HEADER) @RequestHeader @NotNull String userId) {
    //Only people that can see rating is the customer who left the rating, the vendor and the admin
    if (isNullOrEmpty(userId)) {
      return ResponseEntity.badRequest().build();
    }if (repo.findById(deliveryId).isEmpty() || !repo.existsById(deliveryId)) {
      return ResponseEntity.notFound().build();
    } else {
      Delivery delivery = repo.findById(deliveryId).get();
      String type = userMockRepo.getUserType(userId);
      String restaurantEmail = delivery.getRestaurantID();
      String customerEmail = delivery.getRestaurantID();
      boolean isVendor = type.equals("vendor") && restaurantEmail.equals(userId);
      boolean isCustomer = type.equals("customer") && customerEmail.equals(userId);
      if (!type.equals("admin") && !isVendor && !isCustomer) {
        return ResponseEntity.status(403).build();
      } else {
        return ResponseEntity.ok(delivery.getRatingRestaurant());
      }
    }
  }

  @Override
  @RequestMapping(
      method = {RequestMethod.GET},
      value = {"/{deliveryId}/rating-courier"},
      produces = {"application/json"}
  )
  public ResponseEntity<Integer> deliveriesDeliveryIdRatingCourierGet(@Parameter(name = "deliveryId",description = "ID of the Delivery entity",required = true,in = ParameterIn.PATH) @PathVariable("deliveryId") UUID deliveryId, @Parameter(name = "userId",description = "User ID for authorization",required = true,in = ParameterIn.HEADER) @RequestHeader @NotNull String userId) {
    //Only people that can see rating is the customer who left the rating, the courier and the admin
    if (isNullOrEmpty(userId)) {
      return ResponseEntity.badRequest().build();
    }if (repo.findById(deliveryId).isEmpty() || !repo.existsById(deliveryId)) {
      return ResponseEntity.notFound().build();
    } else {
      Delivery delivery = repo.findById(deliveryId).get();
      String type = userMockRepo.getUserType(userId);
      String courierEmail = delivery.getCourierID();
      String customerEmail = delivery.getRestaurantID();
      boolean isCourier = type.equals("courier") && courierEmail.equals(userId);
      boolean isCustomer = type.equals("customer") && customerEmail.equals(userId);
      if (!type.equals("admin") && !isCourier && !isCustomer) {
        return ResponseEntity.status(403).build();
      } else {
        return ResponseEntity.ok(delivery.getRatingCourier());
      }
    }
  }
  }

/*
* @RequestMapping(
    method = {RequestMethod.PUT},
    value = {"/deliveries/{deliveryId}/rating-courier"},
    produces = {"application/json"},
    consumes = {"application/json"}
  )
  default ResponseEntity<Delivery> deliveriesDeliveryIdRatingCourierPut(@Parameter(name = "deliveryId",description = "ID of the Delivery entity",required = true,in = ParameterIn.PATH) @PathVariable("deliveryId") UUID deliveryId, @Parameter(name = "userId",description = "User ID for authorization",required = true,in = ParameterIn.HEADER) @RequestHeader(value = "userId",required = true) @NotNull String userId, @Parameter(name = "body",description = "Update rating of delivery",required = true) @RequestBody @Valid Integer body) {
    this.getRequest().ifPresent((request) -> {
      Iterator var1 = MediaType.parseMediaTypes(request.getHeader("Accept")).iterator();

      while(var1.hasNext()) {
        MediaType mediaType = (MediaType)var1.next();
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          String exampleString = "{ \"ratingRestaurant\" : 5, \"deliveryID\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"deliveryAddress\" : [ 1.4658129805029452, 1.4658129805029452 ], \"unexpectedEvent\" : { \"reason\" : \"reason\", \"errorId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"value\" : 6 }, \"delivered_time\" : \"2000-01-23T04:56:07.000+00:00\", \"customerID\" : \"customerID\", \"estimatedPrepTime\" : 0, \"courierID\" : \"courierID\", \"pickup_time\" : \"2000-01-23T04:56:07.000+00:00\", \"ratingCourier\" : 5, \"restaurantID\" : \"restaurantID\" }";
          ApiUtil.setExampleResponse(request, "application/json", exampleString);
          break;
        }
      }

    });
    return new ResponseEntity(HttpStatus.NOT_IMPLEMENTED);
  }

  @Operation(
    operationId = "deliveriesDeliveryIdRatingRestaurantGet",
    summary = "Retrieve the rating of restaurant for a specific Delivery entity",
    description = "Returns the rating of restaurant for Delivery entity visible to your User role. You must specify the Delivery id and your User id.",
    tags = {"Delivery"},
    responses = {@ApiResponse(
  responseCode = "200",
  description = "Successful response",
  content = {@Content(
  mediaType = "application/json",
  schema = @Schema(
  implementation = Integer.class
)
)}
), @ApiResponse(
  responseCode = "400",
  description = "Bad Request. Invalid input format or missing parameters.",
  content = {@Content(
  mediaType = "application/json",
  schema = @Schema(
  implementation = String.class
)
)}
), @ApiResponse(
  responseCode = "401",
  description = "Unauthorized access",
  content = {@Content(
  mediaType = "application/json",
  schema = @Schema(
  implementation = String.class
)
)}
), @ApiResponse(
  responseCode = "403",
  description = "Forbidden. User lacks necessary permissions.",
  content = {@Content(
  mediaType = "application/json",
  schema = @Schema(
  implementation = String.class
)
)}
), @ApiResponse(
  responseCode = "404",
  description = "Delivery not found",
  content = {@Content(
  mediaType = "application/json",
  schema = @Schema(
  implementation = String.class
)
)}
), @ApiResponse(
  responseCode = "500",
  description = "Server error",
  content = {@Content(
  mediaType = "application/json",
  schema = @Schema(
  implementation = String.class
)
)}
)}
  )

  @Operation(
    operationId = "deliveriesDeliveryIdRatingRestaurantPut",
    summary = "Update rating for Delivery entity",
    description = "Update rating of Delivery entity visible to your user role. For example, a Customer can give a rating to a delivered order.",
    tags = {"Delivery"},
    responses = {@ApiResponse(
  responseCode = "200",
  description = "Update successful",
  content = {@Content(
  mediaType = "application/json",
  schema = @Schema(
  implementation = Delivery.class
)
)}
), @ApiResponse(
  responseCode = "400",
  description = "Bad Request. Invalid input format or missing parameters.",
  content = {@Content(
  mediaType = "application/json",
  schema = @Schema(
  implementation = String.class
)
)}
), @ApiResponse(
  responseCode = "401",
  description = "Unauthorized access",
  content = {@Content(
  mediaType = "application/json",
  schema = @Schema(
  implementation = String.class
)
)}
), @ApiResponse(
  responseCode = "403",
  description = "Forbidden. User lacks necessary permissions. Only admins and customers are allowed to update the rating of an order.",
  content = {@Content(
  mediaType = "application/json",
  schema = @Schema(
  implementation = String.class
)
)}
), @ApiResponse(
  responseCode = "404",
  description = "Delivery not found",
  content = {@Content(
  mediaType = "application/json",
  schema = @Schema(
  implementation = String.class
)
)}
), @ApiResponse(
  responseCode = "500",
  description = "Server error",
  content = {@Content(
  mediaType = "application/json",
  schema = @Schema(
  implementation = String.class
)
)}
)}
  )
  @RequestMapping(
    method = {RequestMethod.PUT},
    value = {"/deliveries/{deliveryId}/rating-restaurant"},
    produces = {"application/json"},
    consumes = {"application/json"}
  )
  default ResponseEntity<Delivery> deliveriesDeliveryIdRatingRestaurantPut(@Parameter(name = "deliveryId",description = "ID of the Delivery entity",required = true,in = ParameterIn.PATH) @PathVariable("deliveryId") UUID deliveryId, @Parameter(name = "userId",description = "User ID for authorization",required = true,in = ParameterIn.HEADER) @RequestHeader(value = "userId",required = true) @NotNull String userId, @Parameter(name = "body",description = "Update rating of restaurant for delivery",required = true) @RequestBody @Valid Integer body) {
    this.getRequest().ifPresent((request) -> {
      Iterator var1 = MediaType.parseMediaTypes(request.getHeader("Accept")).iterator();

      while(var1.hasNext()) {
        MediaType mediaType = (MediaType)var1.next();
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          String exampleString = "{ \"ratingRestaurant\" : 5, \"deliveryID\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"deliveryAddress\" : [ 1.4658129805029452, 1.4658129805029452 ], \"unexpectedEvent\" : { \"reason\" : \"reason\", \"errorId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\", \"value\" : 6 }, \"delivered_time\" : \"2000-01-23T04:56:07.000+00:00\", \"customerID\" : \"customerID\", \"estimatedPrepTime\" : 0, \"courierID\" : \"courierID\", \"pickup_time\" : \"2000-01-23T04:56:07.000+00:00\", \"ratingCourier\" : 5, \"restaurantID\" : \"restaurantID\" }";
          ApiUtil.setExampleResponse(request, "application/json", exampleString);
          break;
        }
      }

    });
    return new ResponseEntity(HttpStatus.NOT_IMPLEMENTED);
  }

  @Operation(
    operationId = "deliveriesDeliveryIdRestaurantGet",
    summary = "Retrieve the restaurant id of a specific Delivery entity",
    description = "Returns the restaurant id of Delivery entity visible to your User role. You must specify the Delivery id and your User id.",
    tags = {"Delivery"},
    responses = {@ApiResponse(
  responseCode = "200",
  description = "Successful response",
  content = {@Content(
  mediaType = "application/json",
  schema = @Schema(
  implementation = String.class
)
)}
), @ApiResponse(
  responseCode = "400",
  description = "Bad Request. Invalid input format or missing parameters.",
  content = {@Content(
  mediaType = "application/json",
  schema = @Schema(
  implementation = String.class
)
)}
), @ApiResponse(
  responseCode = "401",
  description = "Unauthorized access",
  content = {@Content(
  mediaType = "application/json",
  schema = @Schema(
  implementation = String.class
)
)}
), @ApiResponse(
  responseCode = "404",
  description = "Delivery not found",
  content = {@Content(
  mediaType = "application/json",
  schema = @Schema(
  implementation = String.class
)
)}
), @ApiResponse(
  responseCode = "500",
  description = "Server error",
  content = {@Content(
  mediaType = "application/json",
  schema = @Schema(
  implementation = String.class
)
)}
)}
*
* */
