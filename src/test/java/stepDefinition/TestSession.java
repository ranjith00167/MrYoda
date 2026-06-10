package stepDefinition;

import java.util.ArrayList;
import java.util.List;

public class TestSession {

	public static String locationText = null;
    public static int totalCheckoutAmount = 0;
    public static double uiAmountCheckout = 0.0;
    public static double expectedUIAmount = 0.0;
    public static double razorpayAmount = 0.0;
    public static double membershipDiscount = 0.0;
    public static double rewardUsed = 0.0;
    public static String previouslySelectedSlot = null;
//    public static String previousSlotBeforeReschedule = null;
    public static String currentRescheduledSlot = null;
    public static String selectedSlotTime = null;         // time for validation
    public static String selectedSlotDate = null;         // date for validation (e.g. "01 Dec")
    public static String generatedMobile;

    /**
     * Stores the actual full names used during registration and family-member creation.
     * Populated at runtime (not from Excel) so validation always reflects what was
     * actually entered in the UI / sent to the API.
     * Index 0 = profile owner, Index 1+ = family members added in order.
     */
    public static List<String> registeredMemberNames = new ArrayList<>();

    /** Temporary holder for first name while the add-member form is being filled,
     *  cleared once last name is captured and full name is assembled. */
    public static String _pendingMemberFirstName = null;

}
