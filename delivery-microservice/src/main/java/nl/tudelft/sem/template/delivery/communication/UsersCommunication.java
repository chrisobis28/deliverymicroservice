package nl.tudelft.sem.template.delivery.communication;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
public class UsersCommunication {

  public String getAccountType(String userId) {

    // Users belong to port 8081, Orders to 8080, we are at 8082
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
      if (response.statusCode() == 200) {
        return response.body();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "in-existent";
  }

}
