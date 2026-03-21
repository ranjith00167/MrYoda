package stepDefinition;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import utilities.BaseClass;

public class FamilyAndFriendsPage extends BaseSteps {
	@Given("the user navigates to Family and Friends page")
	public void the_user_navigates_to_family_and_friends_page() {
		BaseClass.waitAndClick(LocatorsPage.profileIcon, 10);
		BaseClass.waitAndClick(LocatorsPage.addNewMember_Button, 10);
	}
	@Given("the user clicks on Add New Member button")
	public void the_user_clicks_on_add_new_member_button() {


	}
	@When("the user uploads profile photo")
	public void the_user_uploads_profile_photo() {


	}
	@When("the user selects title from the dropdown")
	public void the_user_selects_title_from_the_dropdown() {

		BaseClass.waitAndClick(LocatorsPage.title_Dropdown, 10);
		BaseClass.waitAndClick(LocatorsPage.Title_Option_Mr, 10);
	}
	@When("the user enters first name")
	public void the_user_enters_first_name() {
 		BaseClass.waitAndInput(LocatorsPage.firstName_Input, BaseClass.testData.get("firstName"), 10);

	}
	@When("the user enters middle name")
	public void the_user_enters_middle_name() {
 		BaseClass.waitAndInput(LocatorsPage.middleName_Input, BaseClass.testData.get("middleName"), 10);


	}
	@When("the user enters last name")
	public void the_user_enters_last_name() {

BaseClass.waitAndInput(LocatorsPage.lastName_Input,  BaseClass.testData.get("lastName"), 10);
	}
	@When("the user selects country code")
	public void the_user_selects_country_code() {


	}
	@When("the user enters mobile number")
	public void the_user_enters_mobile_number() {

BaseClass.waitAndInput(LocatorsPage.mobileNumber_Input, "9876543210", 10);
	}
	@When("the user selects gender")
	public void the_user_selects_gender() {

	    BaseClass.scrollByOffset(0, 300);
	    BaseClass.waitAndClick(LocatorsPage.male_GenderImage, 10);

	    // Capture gender from the clicked icon
	    BaseClass.selectedGender = LocatorsPage.male_GenderImage.getDomAttribute("alt").trim();

	    System.out.println("Selected Gender from Attribute: " + BaseClass.selectedGender);
	}

	@When("the user selects relation")
	public void the_user_selects_relation() throws Throwable {
		Thread.sleep(2000);
BaseClass.waitAndClickWithJSFallback(LocatorsPage.selectRelation, 10);
BaseClass.waitAndClick(LocatorsPage.relation_Dropdown, 10);
	}
	@When("the user enters date of birth")
	public void the_user_enters_date_of_birth() {

BaseClass.waitAndInput(LocatorsPage.dateOfBirth_Input, BaseClass.testData.get("DOB"), 10);
	}
	@When("the user enters email")
	public void the_user_enters_email() {


	}
	@When("the user clicks on Save button")
	public void the_user_clicks_on_save_button() {

BaseClass.waitAndClick(LocatorsPage.save_Button, 10);
	}
	@Then("the new member should be added successfully")
	public void the_new_member_should_be_added_successfully() {

		System.out.println("New member added successfully");

	}
	@Then("the member card should display the profile photo")
	public void the_member_card_should_display_the_profile_photo() {


	}
	@Then("the user should see complete member details in the list")
	public void the_user_should_see_complete_member_details_in_the_list() {


	}
	@Then("the user should see the member in the family list")
	public void the_user_should_see_the_member_in_the_family_list() {


	}
	@Then("a success message should be displayed")
	public void a_success_message_should_be_displayed() {


	}
	@When("the user fills the form with randomly generated unique name")
	public void the_user_fills_the_form_with_randomly_generated_unique_name() {


	}
	@When("the user enters all mandatory fields with valid data")
	public void the_user_enters_all_mandatory_fields_with_valid_data() {


	}
	@Then("the user clicks on Add New Member button again")
	public void the_user_clicks_on_add_new_member_button_again() {


	}
	@When("the user fills the form with another randomly generated unique name")
	public void the_user_fills_the_form_with_another_randomly_generated_unique_name() {


	}
	@Then("the second member should be added successfully")
	public void the_second_member_should_be_added_successfully() {


	}
	@Then("both members should be visible in the family list")
	public void both_members_should_be_visible_in_the_family_list() {


	}
	@When("the user leaves first name field empty")
	public void the_user_leaves_first_name_field_empty() {


	}
	@When("the user leaves last name field empty")
	public void the_user_leaves_last_name_field_empty() {


	}
	@Then("the form should not be submitted")
	public void the_form_should_not_be_submitted() {


	}
	@Then("validation error for first name should be displayed")
	public void validation_error_for_first_name_should_be_displayed() {


	}
	@Then("validation error for last name should be displayed")
	public void validation_error_for_last_name_should_be_displayed() {


	}

@When("the user fills all mandatory fields except mobile number")
public void the_user_fills_all_mandatory_fields_except_mobile_number() {


}
@When("the user enters invalid mobile number")
public void the_user_enters_invalid_mobile_number() {


}


@When("the user fills all mandatory fields")
public void the_user_fills_all_mandatory_fields() {


}
@When("the user enters invalid email")
public void the_user_enters_invalid_email() {


}

@Then("validation error for email should be displayed")
public void validation_error_for_email_should_be_displayed() {


}
@When("the user clicks on Cancel button")
public void the_user_clicks_on_cancel_button() {


}
@Then("the add member form should be closed")
public void the_add_member_form_should_be_closed() {


}
@Then("the member should not be added to the list")
public void the_member_should_not_be_added_to_the_list() {


}
@Then("the user should remain on Family and Friends page")
public void the_user_should_remain_on_family_and_friends_page() {


}
@Given("the user has successfully added a member")
public void the_user_has_successfully_added_a_member() {


}
@When("the user views the Family and Friends page")
public void the_user_views_the_family_and_friends_page() {


}
@Then("the member should be visible in the list")
public void the_member_should_be_visible_in_the_list() {


}
@Then("the member card should display correct name")
public void the_member_card_should_display_correct_name() {


}
@Then("the member card should display correct relation")
public void the_member_card_should_display_correct_relation() {


}
@Then("the member card should display correct age and gender")
public void the_member_card_should_display_correct_age_and_gender() {


}
@Given("the user has multiple members in the family list")
public void the_user_has_multiple_members_in_the_family_list() {


}
@When("the user enters member name in the search field")
public void the_user_enters_member_name_in_the_search_field() {


}
@Then("only matching members should be displayed")
public void only_matching_members_should_be_displayed() {


}
@Then("other members should be filtered out")
public void other_members_should_be_filtered_out() {


}
@Given("the user has more than three members in the family list")
public void the_user_has_more_than_three_members_in_the_family_list() {


}
@When("the user clicks on See All link")
public void the_user_clicks_on_see_all_link() {


}
@Then("all family members should be displayed")
public void all_family_members_should_be_displayed() {


}
@Then("the complete list should be visible")
public void the_complete_list_should_be_visible() {


}
@Then("the member should be added with correct relation")
public void the_member_should_be_added_with_correct_relation() {


}
@When("the user enters minimum allowed date of birth")
public void the_user_enters_minimum_allowed_date_of_birth() {


}
@Then("the date should be accepted")
public void the_date_should_be_accepted() {


}
@When("the user enters maximum allowed date of birth")
public void the_user_enters_maximum_allowed_date_of_birth() {


}
@When("the user enters invalid date")
public void the_user_enters_invalid_date() {


}
@Then("validation error for date should be displayed")
public void validation_error_for_date_should_be_displayed() {


}

}
