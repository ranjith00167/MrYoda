import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.util.Scanner;

public class TestGetCenters {
    public static void main(String[] args) throws Exception {
        String urlString = "https://staging-api-diagnostics.yodaprojects.com/slot/getCentersByadd";
        String token = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyR3VpZCI6IjYwY2RmZjQ0LWU4MDEtNGM3NS1hODczLTY5Y2FmOTY1ZTc2MyIsIm1vYmlsZSI6IjgwNTY0Nzc4ODQiLCJmaXJzdF9uYW1lIjoiUmFuaml0aCIsImxhc3RfbmFtZSI6Ikt1bWFyIiwiZW1haWwiOm51bGwsInJvbGUiOiJ1c2VyX29ubHkiLCJ0eXBlIjoiQUNDRVNTIiwiaWF0IjoxNzcyMTgxODI5LCJleHAiOjE3NzQ3NzM4Mjl9.eMM0w38vakL8iWCnskUD6UQ_bMwrIGx1saoPA9Lv-3w"; 

        String[] payloads = {
            "{\"location_id\":\"676a5fa720093d2807af03a5\",\"addressid\":\"78cc2354-4bd7-4d43-8601-d20cf8adeccd\"}",
            "{\"lab_id\":\"676a5fa720093d2807af03a5\",\"addressid\":\"78cc2354-4bd7-4d43-8601-d20cf8adeccd\"}",
            "{\"location_id\":\"676a5fa720093d2807af03a5\",\"addressguid\":\"78cc2354-4bd7-4d43-8601-d20cf8adeccd\"}",
            "{\"location_id\":\"676a5fa720093d2807af03a5\",\"address_id\":\"78cc2354-4bd7-4d43-8601-d20cf8adeccd\"}",
            "{\"lab_id\":\"676a5fa720093d2807af03a5\",\"address_guid\":\"78cc2354-4bd7-4d43-8601-d20cf8adeccd\"}"
        };

        for (String payload : payloads) {
            System.out.println("Payload: " + payload);
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Authorization", token);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            System.out.println("Status: " + code);

            InputStream in = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            if (in != null) {
                try (Scanner scanner = new Scanner(in, StandardCharsets.UTF_8.name())) {
                    String responseBody = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                    System.out.println("Response: " + responseBody);
                }
            }
            System.out.println("------------");
        }
    }
}
