package api.membership;

import com.mryoda.diagnostics.api.builders.RequestBuilder;
import io.restassured.response.Response;

import java.util.Map;

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

    /**
     * Fetch all membership transactions (paginated).
     * POST /membership/transaction/getAllTransactions
     */
    public Response getAllTransactions(String token, Map<String, Object> body) {
        return new RequestBuilder()
                .setEndpoint(MembershipEndpoints.GET_ALL_TRANSACTIONS)
                .addHeader("Authorization", "Bearer " + token)
                .setRequestBody(body)
                .postWithoutStatusCheck();
    }

    /**
     * Fetch all membership transactions without auth (for negative testing).
     */
    public Response getAllTransactionsNoAuth(Map<String, Object> body) {
        return new RequestBuilder()
                .setEndpoint(MembershipEndpoints.GET_ALL_TRANSACTIONS)
                .setRequestBody(body)
                .postWithoutStatusCheck();
    }

    /**
     * Fetch membership transactions by mobile number.
     * GET /membership/transaction/getTransactionByMobile/{mobile_number}
     */
    public Response getTransactionByMobile(String token, String mobile) {
        return new RequestBuilder()
                .setEndpoint(MembershipEndpoints.GET_TRANSACTION_BY_MOBILE.replace("{mobile_number}", mobile))
                .addHeader("Authorization", "Bearer " + token)
                .getWithoutStatusCheck();
    }
}
