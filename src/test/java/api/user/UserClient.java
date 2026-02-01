package api.user;

import com.mryoda.diagnostics.api.builders.RequestBuilder;
import io.restassured.response.Response;
import java.util.Map;

public class UserClient {

    public Response getOtp(Map<String, Object> payload) {
        return new RequestBuilder()
                .setEndpoint(UserEndpoints.OTP_REQUEST)
                .setRequestBody(payload)
                .post();
    }
    
    // Often verify is a diff endpoint or same with different body
    public Response verifyOtp(Map<String, Object> payload) {
        return new RequestBuilder()
                .setEndpoint(UserEndpoints.OTP_VERIFY)
                .setRequestBody(payload)
                .post();
    }

    public Response createUser(String token, Object payload) {
        return new RequestBuilder()
                .setEndpoint(UserEndpoints.USER_CREATE)
                .addHeader("Authorization", "Bearer " + token)
                .setRequestBody(payload)
                .post();
    }

    public Response getUser(String token, String userId) {
        return new RequestBuilder()
                .setEndpoint(UserEndpoints.GET_USER.replace("{user_id}", userId))
                .addHeader("Authorization", "Bearer " + token)
                .get();
    }
    
    public Response getUserProfile(String token) {
        return new RequestBuilder()
                .setEndpoint(UserEndpoints.USER_PROFILE)
                .addHeader("Authorization", "Bearer " + token)
                .get();
    }

    public Response loginPhlebo(Map<String, Object> payload) {
        return new RequestBuilder()
                .setEndpoint(UserEndpoints.PHLEBO_LOGIN)
                .setRequestBody(payload)
                .post();
    }
}
