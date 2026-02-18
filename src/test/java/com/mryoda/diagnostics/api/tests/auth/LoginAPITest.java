package com.mryoda.diagnostics.api.tests.auth;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.config.ConfigLoader;
import com.mryoda.diagnostics.api.utils.RequestContext;
import com.mryoda.diagnostics.api.utils.TokenManager;
import org.testng.annotations.Test;

public class LoginAPITest extends BaseTest {

    @Test(priority = 1)
    public void testLoginWithOTP() {

        System.out.println("\n==========================================================");
        System.out.println("            MEMBER LOGIN TEST (Paid Member)");
        System.out.println("==========================================================");
        RequestContext.setCurrentFlowName("member_flow");

        String mobile = ConfigLoader.getConfig().memberMobile(); // 9003730394 - Paid member
        RequestContext.setMobile(mobile);
        String token = TokenManager.generateToken(mobile, TokenManager.MEMBER);

        System.out.println("🟢 MEMBER LOGIN SUCCESS");
        System.out.println("   Token: " + token);
        System.out.println("   Mobile: " + mobile + " (Member Benefits Enabled)");
        System.out.println("   Stored in: RequestContext.getMemberToken()");
        System.out.println("   First Name: " + RequestContext.getMemberFirstName());
        System.out.println("   Last Name: " + RequestContext.getMemberLastName());
        System.out.println("   User ID: " + RequestContext.getMemberUserId());

        RequestContext.storeExpectedPatient(RequestContext.getMemberUserId(),
                RequestContext.getMemberFirstName() + " " + RequestContext.getMemberLastName());
    }

    @Test(priority = 1)
    public void testLoginWithOTP_NonMember() {

        System.out.println("\n==========================================================");
        System.out.println("         NON-MEMBER LOGIN TEST (No Membership)");
        System.out.println("==========================================================");
        RequestContext.setCurrentFlowName("non_member_flow");

        String mobile = ConfigLoader.getConfig().nonMemberMobile(); // 8220220227 - NOT a paid member
        RequestContext.setMobile(mobile);
        String token = TokenManager.generateToken(mobile, TokenManager.NON_MEMBER);

        System.out.println("🟢 NON-MEMBER LOGIN SUCCESS");
        System.out.println("   Token: " + token);
        System.out.println("   Mobile: 8220220227 (NOT a paid member)");
        System.out.println("   Stored in: RequestContext.getNonMemberToken()");
        System.out.println("   First Name: " + RequestContext.getNonMemberFirstName());
        System.out.println("   Last Name: " + RequestContext.getNonMemberLastName());
        System.out.println("   User ID: " + RequestContext.getNonMemberUserId());

        RequestContext.storeExpectedPatient(RequestContext.getNonMemberUserId(),
                RequestContext.getNonMemberFirstName() + " " + RequestContext.getNonMemberLastName());
    }

    @Test(priority = 2, dependsOnMethods = "com.mryoda.diagnostics.api.tests.auth.UserCreateAPITest.testUserRegistration_CreateNewUser")
    public void testLoginWithOTP_NewlyRegisteredUser() {

        System.out.println("\n==========================================================");
        System.out.println("            NEW USER LOGIN TEST");
        System.out.println("==========================================================");
        RequestContext.setCurrentFlowName("new_user_flow");

        String mobile = RequestContext.getMobile();
        String token = TokenManager.generateToken(mobile, TokenManager.NEW_USER);

        System.out.println("🟢 NEW USER LOGIN SUCCESS");
        System.out.println("   Token: " + token);
        System.out.println("   Stored in: RequestContext.getNewUserToken()");
        System.out.println("   First Name: " + RequestContext.getNewUserFirstName());
        System.out.println("   Last Name: " + RequestContext.getNewUserLastName());
        System.out.println("   User ID: " + RequestContext.getNewUserUserId());

        RequestContext.storeExpectedPatient(RequestContext.getNewUserUserId(),
                RequestContext.getNewUserFirstName() + " " + RequestContext.getNewUserLastName());
    }

}
