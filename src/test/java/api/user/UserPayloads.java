package api.user;

import org.json.JSONObject;
import com.mryoda.diagnostics.api.utils.RandomDataUtil;
import com.mryoda.diagnostics.api.utils.RequestContext;
import com.mryoda.diagnostics.api.config.ConfigLoader;
import java.util.HashMap;
import java.util.Map;

public class UserPayloads {

    public static JSONObject buildNewUserPayload() {
        JSONObject body = new JSONObject();
        body.put("first_name", RandomDataUtil.getRandomFirstName());
        body.put("last_name", RandomDataUtil.getRandomLastName());
        body.put("middle_name", RandomDataUtil.getRandomMiddleName());
        String gender = RandomDataUtil.getRandomGender();
        body.put("gender", gender);
        body.put("title", gender.equalsIgnoreCase("male") ? "Mr" : "Mrs");
        body.put("dob", RandomDataUtil.getRandomDOB());
        body.put("mobile", RequestContext.getMobile());
        body.put("country_code", ConfigLoader.getConfig().countryCode());
        body.put("email", RandomDataUtil.getRandomEmail());
        body.put("profile_pic", RandomDataUtil.getRandomProfilePic());
        body.put("alt_mobile", RandomDataUtil.getRandomMobile());
        return body;
    }

    public static Map<String, Object> buildOtpRequestPayload(String mobile, String countryCode) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("mobile_number", mobile);
        payload.put("country_code", countryCode);
        return payload;
    }
    
    public static Map<String, Object> buildVerifyOtpPayload(String mobile, String countryCode, String otp) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("mobile_number", mobile);
        payload.put("country_code", countryCode);
        payload.put("otp", otp);
        return payload;
    }
}
