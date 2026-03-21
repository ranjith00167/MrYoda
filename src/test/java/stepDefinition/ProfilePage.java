package stepDefinition;

import org.testng.Assert;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import utilities.BaseClass;

public class ProfilePage extends BaseSteps {
	@When("click on the profile icon")
	public void click_on_the_profile_icon() {
	    
		BaseClass.waitAndClick(LocatorsPage.profileIcon, 10);
	}
	@Then("validate whether the user is a new user by checking the presence of welcome message")
	public void validate_whether_the_user_is_a_new_user_by_checking_the_presence_of_welcome_message() {
	   
	String welcomeText=	BaseClass.getText(LocatorsPage.welcomeText, 10);
		if(welcomeText.contains("Welcome")){
		System.out.println("✅ User is a NEW USER");
	}else {
		System.out.println("✅ User is an EXISTING USER");
	}
	}
	@When("click the profile registration icon")
	public void click_the_profile_registration_icon() {
	 
		BaseClass.waitAndClick(LocatorsPage.profile_RegistrationImage, 10);
	}
	@When("the user enters first name in registration page")
	public void the_user_enters_first_name_in_registration_page() {
	    
 		BaseClass.waitAndInput(LocatorsPage.firstNameInputProfilePage, BaseClass.testData.get("firstName"), 10);

	}
	@When("the user enters middle name in registration page")
	public void the_user_enters_middle_name_in_registration_page() {
 		BaseClass.waitAndInput(LocatorsPage.middleNameInputProfilePage, BaseClass.testData.get("middleName"), 10);

	}
	@When("the user enters last name  in registration page")
	public void the_user_enters_last_name_in_registration_page() {
		BaseClass.waitAndInput(LocatorsPage.lastnameInputProfilePage,  BaseClass.testData.get("lastName"), 10);

		
	}
	@When("the user enters date of birth  in registration page")
	public void the_user_enters_date_of_birth_in_registration_page() throws Throwable {

	    String dobValue = BaseClass.testData.get("DOB");

	    if (dobValue == null || dobValue.trim().isEmpty()) {
	        throw new RuntimeException("❌ DOB in Excel is empty!");
	    }

	    System.out.println("📥 Raw DOB from Excel: " + dobValue);

	    // Convert to yyyy-MM-dd required by <input type="date">
	    String isoDate;
	    if (dobValue.matches("\\d+")) {
	        // Excel numeric serial (e.g. 33018)
	        String converted = BaseClass.convertExcelDateSerial(dobValue);
	        isoDate = parseDobToIso(converted);
	    } else {
	        isoDate = parseDobToIso(dobValue);
	    }

	    System.out.println("📅 ISO DOB for input[type=date]: " + isoDate);

	    // Use JavaScript with React's native input value setter so React
	    // does not override the value on re-render
	    org.openqa.selenium.WebElement dobField = LocatorsPage.DOBProfilePage;
	    org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) BaseClass.driver;
	    js.executeScript(
	        "var nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
	        "nativeInputValueSetter.call(arguments[0], arguments[1]);" +
	        "arguments[0].dispatchEvent(new Event('input',  { bubbles: true }));" +
	        "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
	        dobField, isoDate
	    );

	    // Verify it actually stuck — if React still overrode it, log a warning
	    String actualValue = dobField.getDomProperty("value");
	    if (!isoDate.equals(actualValue)) {
	        System.out.println("⚠️ React overrode DOB value after set (" + actualValue + "). Retrying with blur...");
	        js.executeScript(
	            "var nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
	            "nativeInputValueSetter.call(arguments[0], arguments[1]);" +
	            "arguments[0].dispatchEvent(new Event('input',  { bubbles: true }));" +
	            "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));" +
	            "arguments[0].dispatchEvent(new Event('blur',   { bubbles: true }));",
	            dobField, isoDate
	        );
	    }
	    System.out.println("📅 DOB field value after set: " + dobField.getDomProperty("value"));

	    // calculateAndStoreAge expects dd/MM/yyyy — convert from ISO
	    java.time.LocalDate parsedDate = java.time.LocalDate.parse(isoDate);
	    String ddMMYYYY = parsedDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
	    BaseClass.calculateAndStoreAge(ddMMYYYY);
	    System.out.println("✅ DOB set via JS. Age calculated = " + BaseClass.calculatedAge);
	}

	/**
	 * Parses a DOB string in any of these formats and returns yyyy-MM-dd:
	 *   M/d/yyyy  (e.g. 5/25/1990)
	 *   MM/dd/yyyy, d/M/yyyy, dd/MM/yyyy
	 *   MM-dd-yyyy, dd-MM-yyyy, M-d-yyyy
	 *   yyyy-MM-dd (already ISO — returned as-is)
	 */
	private String parseDobToIso(String raw) {
	    raw = raw.trim();
	    if (raw.matches("\\d{4}-\\d{2}-\\d{2}")) return raw; // already ISO

	    String[] formats = {"M/d/yyyy", "MM/dd/yyyy", "d/M/yyyy", "dd/MM/yyyy",
	                        "MM-dd-yyyy", "dd-MM-yyyy", "M-d-yyyy"};
	    for (String fmt : formats) {
	        try {
	            java.time.LocalDate date = java.time.LocalDate.parse(raw,
	                    java.time.format.DateTimeFormatter.ofPattern(fmt));
	            return date.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
	        } catch (Exception ignored) {}
	    }
	    throw new RuntimeException("❌ Cannot parse DOB '" + raw + "' into yyyy-MM-dd.");
	}
	
	@When("the user clicks on submit button")
	public void the_user_clicks_on_submit_button() throws Throwable{
		Thread.sleep(2000);
		BaseClass.scrollByOffset(0, 300);
	    BaseClass.waitAndClick(LocatorsPage.termsAndConditions_Checkbox, 10);
		BaseClass.waitAndClick(LocatorsPage.submit_Button, 10);
		Thread.sleep(5000);
		BaseClass.waitAndClickWithJSFallback(LocatorsPage.mrYodaLogo_Image, 10);
		System.out.println("mr.Yoda Logo clicked successfully");
	}
	
	@Then("validate the profile name matches with the registered name")
	public void validate_the_profile_name_matches_with_the_registered_name() {

	    String expectedName = BaseClass.testData.get("ProfileName").trim();
	    String actualName   = LocatorsPage.nameElement.getText().trim();

	    System.out.println("📌 Expected Name: " + expectedName);
	    System.out.println("📍 Actual Name from UI: " + actualName);

	    // Normalise to lowercase before comparing — UI may title-case each word
	    if (!actualName.equalsIgnoreCase(expectedName)) {
	        throw new AssertionError("❌ Profile name mismatch! Expected: '" + expectedName + "' | Actual: '" + actualName + "'");
	    }

	    System.out.println("✅ Profile name validation successful (case-insensitive)!");
	}

	@Then("validate the age should be correct based on DOB entered")
	public void validate_the_age_should_be_correct_based_on_dob_entered() {
	    
		 String expectedAgeString = String.valueOf(BaseClass.calculatedAge);

		    BaseClass.assertElementTextContainsWithRetry(
		            LocatorsPage.ageGenderMobileRow,
		            expectedAgeString,
		            10
		    );

		    System.out.println("Age successfully validated with UI");
	}
	@Then("validate the gender should be correct based on selection")
	public void validate_the_gender_should_be_correct_based_on_selection() {

	    // Extract gender shown in the profile row
	    String profileGender = LocatorsPage.genderElement.getText().trim();

	    System.out.println("Displayed Profile Gender: " + profileGender);

	    Assert.assertEquals(profileGender, BaseClass.selectedGender,
	            "❌ Gender mismatch! Expected: " + BaseClass.selectedGender + " | Actual: " + profileGender);

	    System.out.println("✅ Gender validation successful -> " + profileGender);
	}

	@Then("validate the mobile number should be correct based on registration")
	public void validate_the_mobile_number_should_be_correct_based_on_registration() {
	    
		BaseClass.assertElementTextContainsWithRetry(LocatorsPage.mobileElement, TestSession.generatedMobile, 10);
	   System.out.println("✅ Mobile number validation successful");
	}
	@When("the user clicks on edit profile button")
	public void the_user_clicks_on_edit_profile_button() {
	    
	}
	@When("the user updates first name in registration page")
	public void the_user_updates_first_name_in_registration_page() {
	    
	}
	@When("the user updates middle name in registration page")
	public void the_user_updates_middle_name_in_registration_page() {
	    
	}
	@When("the user updates last name  in registration page")
	public void the_user_updates_last_name_in_registration_page() {
	    
	}
	@When("the user clicks on save button")
	public void the_user_clicks_on_save_button() {
	    
	}
	@Then("the member details should be updated successfully")
	public void the_member_details_should_be_updated_successfully() {
	  
	}
	@Then("validate the profile name matches with the updated name")
	public void validate_the_profile_name_matches_with_the_updated_name() {
	    
	}

}
