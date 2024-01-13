package nl.tudelft.sem.template.delivery.system;

import nl.tudelft.sem.template.delivery.services.RestaurantService.RestaurantNotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Profile("custom-exception-handler-test")
@RestController
@RequestMapping("/test")
public class CustomExceptionController {

    @GetMapping("/client-error")
    public ResponseEntity<String> throwClientErrorException() {
        throw new RestaurantNotFoundException();
    }

    @GetMapping("/server-error")
    public ResponseEntity<String> throwInternalServerErrorException() {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping("/application-error")
    public ResponseEntity<String> throwApplicationException() {
        throw new RuntimeException("Hey now!");
    }
}
