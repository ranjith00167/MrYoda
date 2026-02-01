package api.membership;

import com.mryoda.diagnostics.api.builders.RequestBuilder;
import io.restassured.response.Response;

public class MembershipClient {

    public Response getAllBrands() {
        return new RequestBuilder()
                .setEndpoint(MembershipEndpoints.GET_ALL_BRANDS)
                .get();
    }

    public Response getRewardsByMobile(String token, String mobile) {
        return new RequestBuilder()
                .setEndpoint(MembershipEndpoints.GET_REWARDS_BY_MOBILE.replace("{mobile_number}", mobile))
                .addHeader("Authorization", "Bearer " + token)
                .get();
    }
}
