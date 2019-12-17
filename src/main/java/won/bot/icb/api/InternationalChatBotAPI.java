package won.bot.icb.api;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class InternationalChatBotAPI {

    static String locationiqKey = "pk.5e23130e4da787061f15047f154604c5";

    public static Optional<String> countryCodeOfGPS(String lat, String lon) {

        try {

            String infoURI = String.format("https://eu1.locationiq.com/v1/reverse.php?key=%s&lat=%s&lon=%s&format=json", locationiqKey, lat, lon);

            System.out.println(infoURI);
            RestTemplate restTemplate = new RestTemplate();
            String rawResponse = restTemplate.getForObject(infoURI, String.class);
            JsonParser jsonParser = new JsonParser();
            JsonElement parsedJson = jsonParser.parse(rawResponse);

            String countryCode = parsedJson.getAsJsonObject().get("address").getAsJsonObject().get("country_code").getAsString();

            return Optional.of(countryCode);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
