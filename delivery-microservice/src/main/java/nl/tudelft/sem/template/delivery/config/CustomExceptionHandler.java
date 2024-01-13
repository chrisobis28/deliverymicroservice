package nl.tudelft.sem.template.delivery.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class CustomExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Handles an exception thrown while serving a request. Creates an error report consisting of the code
     * and the message that is forwarded to the user. If the exception is an internal server error then
     * its reason is logged on the server and not forwarded to the user for security reasons.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleResponseStatusException(ResponseStatusException ex) {

        HttpStatus status = ex.getStatus();
        String reason;
        if (status.is5xxServerError()) {
            logger.error(ex);
            reason = "";
        } else {
            reason = ex.getReason();
        }

        ApiError apiError = new ApiError(status.name(), reason);
        return new ResponseEntity<>(apiError, status);
    }

    /**
     * Handles uncaught exceptions thrown while serving a request. Returns an error code 500
     * - internal server error to the user and logs the error message in the server console.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleResponseStatusException(Exception ex) {

        logger.error(ex);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ApiError apiError = new ApiError(status.name(), "");
        return new ResponseEntity<>(apiError, status);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ApiError {
        String status;
        String reason;
    }
}
