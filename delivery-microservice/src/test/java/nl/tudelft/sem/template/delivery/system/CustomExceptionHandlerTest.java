package nl.tudelft.sem.template.delivery.system;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.tudelft.sem.template.delivery.config.CustomExceptionHandler;
import nl.tudelft.sem.template.delivery.config.CustomExceptionHandler.ApiError;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("custom-exception-handler-test")
public class CustomExceptionHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    @Autowired
    private TestRestTemplate restTemplate;
    @SpyBean
    private CustomExceptionHandler exceptionHandler;

    @Test
    void when_ResponseStatusException_with_client_side_error_is_thrown_then_ApiError_with_full_message_should_be_returned() throws JsonProcessingException {
        ResponseEntity<String> response = restTemplate.getForEntity("/test/client-error", String.class);

        verify(exceptionHandler).handleResponseStatusException(any(ResponseStatusException.class));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(mapper.readValue(response.getBody(), ApiError.class).getReason()).isNotEmpty();
    }

    @Test
    void when_ResponseStatusException_with_server_side_error_is_thrown_then_ApiError_with_no_message_should_be_returned() throws JsonProcessingException {
        ResponseEntity<String> response = restTemplate.getForEntity("/test/server-error", String.class);

        verify(exceptionHandler).handleResponseStatusException(any(ResponseStatusException.class));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(mapper.readValue(response.getBody(), ApiError.class).getReason()).isEmpty();
    }

    @Test
    void when_application_exception_is_thrown_then_ApiError_with_no_message_and_code_500_should_be_returned() throws JsonProcessingException {
        ResponseEntity<String> response = restTemplate.getForEntity("/test/application-error", String.class);

        verify(exceptionHandler).handleResponseStatusException(any(RuntimeException.class));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(mapper.readValue(response.getBody(), ApiError.class).getReason()).isEmpty();
    }


}
