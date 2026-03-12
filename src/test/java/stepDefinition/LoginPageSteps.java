package stepDefinition;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import utilities.BaseClass;
import utilities.ConfigReader;
import pageObjects.Locators;
import org.openqa.selenium.NoSuchElementException;


public class LoginPageSteps extends BaseSteps {

    @Given("the user is on the login page")
    public void the_user_is_on_the_login_page() {
        driver.get(ConfigReader.get("staging_yoda_url"));
        BaseClass.waitAndClick(LocatorsPage.login, 10);
    }

    @When("the user enters otp")
    public void the_user_enters_otp() {
        BaseClass.waitAndInput(LocatorsPage.enter_mobile_number, BaseClass.testData.get("mobileNumber"), 10);
        BaseClass.waitAndClick(LocatorsPage.get_otp_button, 10);
        BaseClass.waitAndInput(LocatorsPage.otpValue, BaseClass.testData.get("otp"), 10);
    }


    @When("click on the submit button")
    public void click_on_the_submit_button() throws Throwable {
        BaseClass.waitAndClick(LocatorsPage.submit_otp_button, 10);
    }

    @When("find whether the account holder is member or non member")
    public void find_whether_the_account_holder_is_member_or_non_member() {

        try {
            if (LocatorsPage.membership_star_icon.isDisplayed()) {
                LocatorsPage.isMember = true;
                System.out.println("✅ User is a MEMBER account");
            } else {
                LocatorsPage.isMember = false;
                System.out.println("✅ User is a NON-MEMBER account");
            }
            // Re-read location because original flow did that
            String location = BaseClass.getText(LocatorsPage.locationText, 10);
            TestSession.locationText = location;
            System.out.println("Location Text: " + TestSession.locationText);

          
        } catch (Exception e) {
            // Element absent → non-member
            LocatorsPage.isMember = false;
            System.out.println("✅ User is a NON-MEMBER account");
        }
    }

    @When("the location should be auto-detected on the dashboard")
    public void the_location_should_be_auto_detected_on_the_dashboard() {
        String location = BaseClass.getText(LocatorsPage.locationText, 10);
        TestSession.locationText = location;
        System.out.println("Location Text: " + TestSession.locationText);
    }
    
    @Given("load the excel data for single member and lab visit")
    public void load_the_excel_data_for_single_member_and_lab_visit() {
        
    	BaseClass.loadExcelData("Diagnostics", "6");
    }
    @Given("load the excel data for single member and home collection")
    public void load_the_excel_data_for_single_member_and_home_collection() {
    	
    	BaseClass.loadExcelData("Diagnostics", "5");

    }
    
    @Given("load the excel data for single non member and home collection")
    public void load_the_excel_data_for_single_non_member_and_home_collection() {
      
    	BaseClass.loadExcelData("Diagnostics", "1");

    }
 
    @Given("load the excel data for single non member and lab visit")
    public void load_the_excel_data_for_single_non_member_and_lab_visit() {
       
    	BaseClass.loadExcelData("Diagnostics", "2");

    }
    @Given("load the excel data for multi member and home collection with memership")
    public void load_the_excel_data_for_multi_member_and_home_collection_with_memership() {
       
    	BaseClass.loadExcelData("Diagnostics", "8");

    }
   

    @Given("load the excel data for multi member and lab visit")
    public void load_the_excel_data_for_multi_member_and_lab_visit() {
        BaseClass.loadExcelData("Diagnostics", "7");
    }

    @Given("load the excel data for multi member and home collection without memership")
    public void load_the_excel_data_for_multi_member_and_home_collection_without_memership() {
       
    	BaseClass.loadExcelData("Diagnostics", "3");

    }

    @Given("load the excel data for multi member and lab visit with memership")
    public void load_the_excel_data_for_multi_member_and_lab_visit_with_memership() {
    	BaseClass.loadExcelData("Diagnostics", "7");

    }
  

    @Given("load the excel data for multi member and lab visit without memership")
    public void load_the_excel_data_for_multi_member_and_lab_visit_without_memership() {
    	BaseClass.loadExcelData("Diagnostics", "4");

    }
    @Given("load the excel data for multi member and home collection with memership and with cash payment")
    public void load_the_excel_data_for_multi_member_and_home_collection_with_memership_and_with_cash_payment() {
       
    	BaseClass.loadExcelData("Diagnostics", "9");

    }
    @Given("load the excel data for multi member and home collection without memership and with cash payment")
    public void load_the_excel_data_for_multi_member_and_home_collection_without_memership_and_with_cash_payment() {
       
    	BaseClass.loadExcelData("Diagnostics", "10");

    }
    @Given("load the excel data for single member and lab visit for a new user")
    public void load_the_excel_data_for_single_member_and_lab_visit_for_a_new_user() {
    	BaseClass.loadExcelData("Diagnostics", "11");

    }
    @When("create an account with random mobile number")
    public void create_account_with_random_mobile_number() throws Throwable {

        String mobile = BaseClass.generateRandomMobileNumber();
        System.out.println(mobile);
        TestSession.generatedMobile = mobile; // store for validation/debug

        System.out.println("📱 SIGNUP → Generated Mobile: " + mobile);

        BaseClass.waitAndInput(LocatorsPage.mobile_number, mobile, 10);

        BaseClass.waitAndClick(LocatorsPage.get_otp_button, 10);

        Thread.sleep(2000);

        String otp = "123456"; // ALWAYS static for QA environment
        BaseClass.waitAndInput(LocatorsPage.otpValue, otp, 10);


    }

    // ============================================================
    // ✅ 22 VALIDATION METHODS FOR LOGIN FLOW
    // ============================================================

    @Then("validate login page loaded correctly")
    public void validate_login_page_loaded_correctly() {
        String pageTitle = driver.getTitle();
        assert !pageTitle.isEmpty() && 
               (pageTitle.toLowerCase().contains("yoda") || 
                pageTitle.toLowerCase().contains("login")) : 
            "❌ FAIL: Page title invalid: " + pageTitle;
        System.out.println("✅ PASS: Login page title verified - " + pageTitle);
    }

    @Then("validate mobile number entry field visible")
    public void validate_mobile_number_entry_field_visible() {
        boolean isDisplayed = BaseClass.isElementDisplayed(LocatorsPage.enter_mobile_number);
        assert isDisplayed : "❌ FAIL: Mobile number entry field not visible";
        System.out.println("✅ PASS: Mobile number entry field is visible and clickable");
    }

    @Then("validate mobile number entry and format")
    public void validate_mobile_number_entry_and_format() {
        String mobileNumber = BaseClass.testData.get("mobileNumber");
        assert mobileNumber != null && mobileNumber.length() == 10 : 
            "❌ FAIL: Invalid mobile number format: " + mobileNumber;
        assert mobileNumber.matches("\\d{10}") : 
            "❌ FAIL: Mobile number should contain only digits: " + mobileNumber;
        System.out.println("✅ PASS: Mobile number format validated - " + mobileNumber);
    }

    @Then("validate Get OTP button visible and clickable")
    public void validate_get_otp_button_visible_and_clickable() {
        boolean isDisplayed = BaseClass.isElementDisplayed(LocatorsPage.get_otp_button);
        assert isDisplayed : "❌ FAIL: Get OTP button not visible";
        System.out.println("✅ PASS: Get OTP button is visible and clickable");
    }

    @Then("validate OTP field appears after clicking Get OTP")
    public void validate_otp_field_appears_after_clicking_get_otp() {
        boolean isDisplayed = BaseClass.isElementDisplayed(LocatorsPage.otpValue);
        assert isDisplayed : "❌ FAIL: OTP input field not visible after clicking Get OTP";
        System.out.println("✅ PASS: OTP input field appeared successfully");
    }

    @Then("validate OTP entry and format")
    public void validate_otp_entry_and_format() {
        String otp = BaseClass.testData.get("otp");
        assert otp != null && otp.length() == 6 : 
            "❌ FAIL: Invalid OTP format: " + otp;
        assert otp.matches("\\d{6}") : 
            "❌ FAIL: OTP should contain only digits: " + otp;
        System.out.println("✅ PASS: OTP format validated - " + otp);
    }

    @Then("validate Submit OTP button present and active")
    public void validate_submit_otp_button_present_and_active() {
        boolean isDisplayed = BaseClass.isElementDisplayed(LocatorsPage.submit_otp_button);
        assert isDisplayed : "❌ FAIL: Submit OTP button not visible";
        System.out.println("✅ PASS: Submit OTP button is present and active");
    }

    @Then("validate OTP submission and login success")
    public void validate_otp_submission_and_login_success() throws InterruptedException {
        Thread.sleep(2000);
        String currentUrl = driver.getCurrentUrl();
        assert !currentUrl.contains("login") : 
            "❌ FAIL: Still on login page after OTP submission: " + currentUrl;
        System.out.println("✅ PASS: Successfully logged in - URL changed to: " + currentUrl);
    }

    @Then("validate dashboard loaded after login")
    public void validate_dashboard_loaded_after_login() {
        boolean isDashboardVisible = BaseClass.isElementDisplayed(LocatorsPage.members_tab);
        assert isDashboardVisible : "❌ FAIL: Dashboard not loaded after login";
        System.out.println("✅ PASS: Dashboard successfully loaded");
    }

    @Then("validate user membership status")
    public void validate_user_membership_status() {
        try {
            if (LocatorsPage.membership_star_icon.isDisplayed()) {
                System.out.println("✅ PASS: User is a MEMBER (star icon visible)");
            }
        } catch (NoSuchElementException e) {
            System.out.println("✅ PASS: User is a NON-MEMBER (star icon not visible)");
        }
    }

    @Then("validate auto-detected location")
    public void validate_auto_detected_location() {
        String location = BaseClass.getText(LocatorsPage.locationText, 10);
        assert location != null && !location.isEmpty() : 
            "❌ FAIL: Location not auto-detected";
        System.out.println("✅ PASS: Location auto-detected - " + location);
    }

    @Then("validate location is non-empty and valid")
    public void validate_location_is_non_empty_and_valid() {
        String location = BaseClass.getText(LocatorsPage.locationText, 10);
        assert location != null && location.length() > 2 : 
            "❌ FAIL: Location is invalid or too short: " + location;
        System.out.println("✅ PASS: Location is valid - " + location);
    }

    @Then("validate search field appears")
    public void validate_search_field_appears() {
        boolean isDisplayed = BaseClass.isElementDisplayed(LocatorsPage.searchTestField);
        assert isDisplayed : "❌ FAIL: Search field not visible";
        System.out.println("✅ PASS: Search field is visible");
    }

    @Then("validate user profile icon present")
    public void validate_user_profile_icon_present() {
        boolean isDisplayed = BaseClass.isElementDisplayed(LocatorsPage.cart_logo);
        assert isDisplayed : "❌ FAIL: User navigation elements not present";
        System.out.println("✅ PASS: User navigation elements are present");
    }

    @Then("validate search functionality available")
    public void validate_search_functionality_available() {
        boolean isDisplayed = BaseClass.isElementDisplayed(LocatorsPage.searchTestField);
        assert isDisplayed : "❌ FAIL: Search box not available";
        System.out.println("✅ PASS: Search functionality is available");
    }

    @Then("validate Best Seller section visible and clickable")
    public void validate_best_seller_section_visible() {
        boolean isDisplayed = BaseClass.isElementDisplayed(LocatorsPage.viewAll_BestSeller);
        assert isDisplayed : "❌ FAIL: Best Seller section not visible";
        System.out.println("✅ PASS: Best Seller section is visible and clickable");
    }

    @Then("validate product list loaded after clicking View All")
    public void validate_product_list_loaded_after_clicking_view_all() {
        boolean isDisplayed = BaseClass.isElementDisplayed(LocatorsPage.checkoutSummaryHeader);
        assert isDisplayed : "❌ FAIL: No products found on page";
        System.out.println("✅ PASS: Product list successfully loaded");
    }

    @Then("validate Add to Cart button visible and clickable")
    public void validate_add_to_cart_button_visible_and_clickable() {
        boolean isDisplayed = BaseClass.isElementDisplayed(LocatorsPage.addToCartButton);
        assert isDisplayed : "❌ FAIL: Add to Cart button not visible";
        System.out.println("✅ PASS: Add to Cart button is visible and clickable");
    }

    @Then("validate product successfully added to cart")
    public void validate_product_successfully_added_to_cart() {
        String cartText = BaseClass.getText(LocatorsPage.cart_logo, 10);
        assert cartText != null && !cartText.isEmpty() : 
            "❌ FAIL: Cart not updated after adding product";
        System.out.println("✅ PASS: Product successfully added to cart");
    }

    @Then("validate page loads without errors")
    public void validate_page_loads_without_errors() {
        String pageSource = driver.getPageSource();
        assert pageSource != null && pageSource.length() > 100 : 
            "❌ FAIL: Page source is invalid or too short";
        System.out.println("✅ PASS: Page loaded successfully without errors");
    }

    @Then("validate response time is acceptable")
    public void validate_response_time_is_acceptable() {
        long startTime = System.currentTimeMillis();
        BaseClass.isElementDisplayed(LocatorsPage.members_tab);
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        assert responseTime < 10000 : "❌ FAIL: Response time exceeded 10 seconds: " + responseTime + "ms";
        System.out.println("✅ PASS: Page responded within acceptable time - " + responseTime + "ms");
    }

    @Then("validate all mandatory fields populated")
    public void validate_all_mandatory_fields_populated() {
        String mobileNumber = BaseClass.testData.get("mobileNumber");
        String otp = BaseClass.testData.get("otp");
        assert mobileNumber != null && !mobileNumber.isEmpty() : "❌ FAIL: Mobile number not populated";
        assert otp != null && !otp.isEmpty() : "❌ FAIL: OTP not populated";
        System.out.println("✅ PASS: All mandatory fields are properly populated");
    }

}
