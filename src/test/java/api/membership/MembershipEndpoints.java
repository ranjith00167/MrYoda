package api.membership;

public class MembershipEndpoints {
    public static final String GET_ALL_BRANDS = "https://staging-api-membership.yodaprojects.com/brand/getAllBrands";
    public static final String GET_REWARDS_BY_MOBILE = "/reward/getRewardsByMobile/{mobile_number}";
    /** Fetch all membership transactions (paginated). */
    public static final String GET_ALL_TRANSACTIONS = "/membership/transaction/getAllTransactions";
    /** Fetch membership transactions by mobile number. */
    public static final String GET_TRANSACTION_BY_MOBILE = "/membership/transaction/getTransactionByMobile/{mobile_number}";
}
