package api.user;

public class UserEndpoints {
    public static final String OTP_REQUEST = "/otps/getOtp";
    public static final String OTP_VERIFY = "/otps/getOtp"; // Verify uses same endpoint with different signature usually or it was a typo in original file
    public static final String USER_CREATE = "/users/addUser";
    public static final String USER_PROFILE = "/user/profile";
    public static final String UPDATE_PROFILE = "/user/update";
    public static final String GET_USER = "/users/getUser/{user_id}";
    public static final String GET_BY_GUIDS = "/users/getByGuids";
    public static final String PHLEBO_LOGIN = "/phlebo/loginPhlebo";
}
