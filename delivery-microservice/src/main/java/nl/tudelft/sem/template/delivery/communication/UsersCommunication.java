package nl.tudelft.sem.template.delivery.communication;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@NoArgsConstructor
public class UsersCommunication {
    private static final ObjectMapper mapperReceive = new ObjectMapper();

    /**
     * Gets the account type of user.
     *
     * @param userId ID of user
     * @return type of the user
     */
    public String getAccountType(String userId) {

        // Users belong to port 8082, Orders to 8080, we are at 8081
        String SERVER = "http://localhost:8081";
        URI uri = URI.create(SERVER + "/account/type?email=" + userId);
        HttpClient httpClient = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", APPLICATION_JSON.toString())
                .GET()
                .build();

        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == HttpStatus.OK.value()) {
                JsonNode json = mapperReceive.readTree(response.body());
                return json.get("type").asText();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "non-existent";
    }


    /**
     * Updates the order status in the Order people's service.
     *
     * @param orderId ID of order
     * @param orderStatus status of order
     */
    public void updateOrderStatus(UUID orderId, String orderStatus) {
        // Users belong to port 8082, Orders to 8080, we are at 8081
        String SERVER = "http://localhost:8080";
        URI uri = URI.create(SERVER + "/internal/order/" + orderId + "/status?orderStatus=" + orderStatus);
        HttpClient httpClient = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json")  // Update content type if needed
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != HttpStatus.OK.value()) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Failed to update the other service");

            }


        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Failed to update the other service");
        }
    }

}
