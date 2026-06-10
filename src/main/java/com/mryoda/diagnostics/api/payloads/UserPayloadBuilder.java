package com.mryoda.diagnostics.api.payloads;

import org.json.JSONObject;
import com.mryoda.diagnostics.api.utils.RandomDataUtil;
import com.mryoda.diagnostics.api.utils.RequestContext;
import com.mryoda.diagnostics.api.config.ConfigLoader;

public class UserPayloadBuilder {

    public static JSONObject buildNewUserPayload() {
        JSONObject body = new JSONObject();
        // Prefer Excel-supplied names so the registered name is predictable and
        // can be validated against the UI later. Fall back to random only when
        // the Excel row does not supply them (e.g. pure API tests).
        String excelFirst  = RequestContext.getExcelFirstName();
        String excelLast   = RequestContext.getExcelLastName();
        String excelMiddle = RequestContext.getExcelMiddleName();
        body.put("first_name",  (excelFirst  != null && !excelFirst.isBlank())  ? excelFirst  : RandomDataUtil.getRandomFirstName());
        body.put("last_name",   (excelLast   != null && !excelLast.isBlank())   ? excelLast   : RandomDataUtil.getRandomLastName());
        body.put("middle_name", (excelMiddle != null && !excelMiddle.isBlank()) ? excelMiddle : RandomDataUtil.getRandomMiddleName());
        String gender = RandomDataUtil.getRandomGender();
        body.put("gender", gender);
        // Add title based on gender as requested
        body.put("title", gender.equalsIgnoreCase("male") ? "Mr" : "Mrs");
        body.put("dob", RandomDataUtil.getRandomDOB());
        body.put("mobile", RequestContext.getMobile());
        body.put("country_code", ConfigLoader.getConfig().countryCode());
        body.put("email", RandomDataUtil.getRandomEmail());
        body.put("profile_pic", RandomDataUtil.getRandomProfilePic());
        body.put("alt_mobile", RandomDataUtil.getRandomMobile());
        return body;
    }

    /**
     * Builds a full update-user payload.
     * Uses random data for mutable fields so each test run is unique.
     *
     * @param guid   the GUID of the user being updated (required by the API)
     * @param mobile the registered mobile number of the user
     */
    public static JSONObject buildUpdateUserPayload(String guid, String mobile) {
        String gender = RandomDataUtil.getRandomGender();
        JSONObject body = new JSONObject();
        body.put("guid",        guid);
        body.put("mobile",      mobile);
        body.put("first_name",  RandomDataUtil.getRandomFirstName());
        body.put("last_name",   RandomDataUtil.getRandomLastName());
        body.put("middle_name", "");
        body.put("dob",         RandomDataUtil.getRandomDOB());
        body.put("gender",      gender);
        body.put("title",       gender.equalsIgnoreCase("Male") ? "Mr." : "Mrs.");
        body.put("email",       "");
        body.put("alt_mobile",  "");
        return body;
    }
}
