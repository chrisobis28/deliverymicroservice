package nl.tudelft.sem.template.delivery;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import nl.tudelft.sem.template.api.DeliveriesApi;
import nl.tudelft.sem.template.model.Delivery;
import org.hibernate.annotations.Parameter;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.UUID;

//Mock of the User repository
@Component
interface UserRepository {
  String getUserEmail();
  String getUserType(String email);
}

@RestController
@RequestMapping("/deliveries")
public class DeliveryController implements DeliveriesApi {
  private final DeliveryRepository repo;
  private final UserRepository repoMock;
  /**
   * Constructor for DeliveryController class
   * @param repo - repository where delivery objects are stored
   */
  public DeliveryController(DeliveryRepository repo, UserRepository repoMock) {
    this.repo = repo;
    this.repoMock = repoMock;
  }

  @Override
  @RequestMapping(
      method = {RequestMethod.PUT},
      value = {"/deliveries/{deliveryId}/rating-courier"},
      produces = {"application/json"},
      consumes = {"application/json"}
  )
  public ResponseEntity<Delivery> deliveriesDeliveryIdRatingCourierPut(@PathVariable("deliveryId") UUID deliveryId, @RequestHeader @NotNull String userId, @RequestBody @Valid Integer body) {
    return ResponseEntity.ok(new Delivery());
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
