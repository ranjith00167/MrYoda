package com.mryoda.diagnostics.api.report;

import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import io.restassured.response.Response;

import java.util.function.Supplier;

/** {@link ReportApiClient} backed by the framework's {@link RequestBuilder} (so crypto, logging, SLA apply). */
public class RestReportApiClient implements ReportApiClient {

    private final Supplier<String> tokenSupplier;

    public RestReportApiClient(Supplier<String> tokenSupplier) {
        this.tokenSupplier = tokenSupplier;
    }

    @Override
    public ApiResponse getReport(String visitNumber) {
        long started = System.currentTimeMillis();
        String token = tokenSupplier.get();
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException(
                    "Authorization token is missing. Pass -DauthToken=<token> for standalone runs.");
        }
        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL
                        + APIEndpoints.GET_REPORT_DETAILS.replace("{visit_Number}", visitNumber))
                .addHeader("Authorization", "Bearer " + token)
                .get();
        return new ApiResponse(response.getStatusCode(), response.getBody().asString(),
                System.currentTimeMillis() - started);
    }
}
