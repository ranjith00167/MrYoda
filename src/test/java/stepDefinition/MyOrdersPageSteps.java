package stepDefinition;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.testng.Assert;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import utilities.BaseClass;

/**
 * My Orders page navigation and view order actions.
 */
public class MyOrdersPageSteps extends BaseSteps {
	public static String previouslySelectedSlot = null;

	@When("click the go to orders button")
	public void click_the_go_to_orders_button() throws Throwable {
		// Wait for the button to be present in DOM
		BaseClass.scrollByOffset(0, 300);
		System.out.println("Clicking on Go to Orders button");
		BaseClass.waitAndClickWithJSFallback(LocatorsPage.goToOrdersButton, 10);
		System.out.println("Clicked on Go to Orders button");

	}

	@When("click the view button in orders page")
	public void click_the_view_button_in_orders_page() {
		BaseClass.waitAndClick(LocatorsPage.viewOrderButton, 10);
	}

	@Then("click on rechedule button in orders page")
	public void click_on_rechedule_button_in_orders_page() throws Throwable {

		BaseClass.waitAndClick(LocatorsPage.rescheduleOrderButton, 10);
	}

	@Then("enter the reschedule otp")
	public void enter_the_reschedule_otp() {
		BaseClass.waitAndInput(LocatorsPage.reschedule_otp_input, BaseClass.testData.get("otp"), 10);

	}

	@Then("click on verify &Reschedule button")
	public void click_on_verify_reschedule_button() {

		BaseClass.waitAndClick(LocatorsPage.verifyAndRescheduleButton, 10);

	}

	@Then("select the slot for reschedule")
	public void select_the_slot_for_reschedule() throws Throwable {

		if (TestSession.previouslySelectedSlot == null)
			throw new RuntimeException("❌ No previous slot stored → Cannot reschedule!");

		List<WebElement> dates = driver.findElements(By.xpath("//div[contains(@class,'swiper-slide')]//button"));

		for (WebElement date : dates) {

			BaseClass.scrollIntoView(date, 10);
			BaseClass.waitAndClickWithJSFallback(date, 10);
			Thread.sleep(800);

			// 👉 Extract date in correct UI format
			String dateText = date.getText().trim(); // Ex: "Dec 01 Mon"
			String[] parts = dateText.split("\\s+");
			String formattedDate = (parts.length >= 2) ? parts[1] + " " + parts[0] // "01 Dec"
					: dateText;

			List<WebElement> enabledSlots = driver
					.findElements(By.xpath("//div[contains(@class,'grid')]//button[not(@disabled)]"));

			if (enabledSlots.isEmpty())
				continue;

			for (WebElement slot : enabledSlots) {

				String timeText = slot.getText().trim();

				if (!timeText.equals(TestSession.previouslySelectedSlot)) {

					BaseClass.scrollIntoView(slot, 10);
					BaseClass.waitAndClickWithJSFallback(slot, 10);

					System.out.println("🔁 Rescheduled Slot Time → " + timeText);
					System.out.println("📅 Rescheduled Slot Date → " + formattedDate);

					// For next re-reschedule skip
					TestSession.previouslySelectedSlot = timeText;

					// 👉 Save latest slot details for UI validation
					TestSession.selectedSlotTime = timeText;
					TestSession.selectedSlotDate = formattedDate;

					BaseClass.waitAndClick(LocatorsPage.slot_proceed, 10);
					return;
				}
			}
		}

		throw new RuntimeException("❌ No alternate slot found to reschedule!");
	}

	@Then("click on proceed to reschedule button")
	public void click_on_proceed_to_reschedule_button() {

		BaseClass.waitAndClick(LocatorsPage.slot_proceed, 10);
	}

	@Then("enter the reschedule reason")
	public void enter_the_reschedule_reason() {
		BaseClass.waitAndClick(LocatorsPage.remarks_Textarea, 10);
		System.out.println("Entering reschedule reason: ");
		BaseClass.typeSlow(LocatorsPage.remarks_Textarea, BaseClass.testData.get("rescheduleRemarks"), 10);
	}

	@Then("click on submit button")
	public void click_on_submit_button() {

		BaseClass.waitAndClick(LocatorsPage.submit_Button, 10);
		BaseClass.waitAndClick(LocatorsPage.ok_Button, 10);
	}

	@Then("click on cancel order button in orders page")
	public void click_on_cancel_order_button_in_orders_page() {
		BaseClass.scrollByOffset(0, 600);
		BaseClass.waitAndClick(LocatorsPage.cancelOrder_Button, 10);
		BaseClass.waitAndClick(LocatorsPage.cancelOrder_ConfirmButton, 10);
	}

	@Then("validate the tests in shows admin approval pending state")
	public void validate_the_tests_in_shows_admin_approval_pending_state() {

		String expectedText = "Admin approval pending";
		BaseClass.assertElementTextEquals(LocatorsPage.adminApprovalPending_Text, expectedText, 10);
		System.out.println("Validated that the test shows Admin Approval Pending state.");
	}

	@Then("validate the rescheduled slot on UI")
	public void validate_the_rescheduled_slot_on_UI() throws Throwable {
		
		BaseClass.refreshPage();

		if (TestSession.selectedSlotTime == null || TestSession.selectedSlotDate == null) {
			throw new RuntimeException("❌ Slot time/date not stored — cannot validate!");
		}

		String uiSlotRaw = LocatorsPage.uiSlotElement.getText().trim();
		System.out.println("📌 Raw UI Slot Text → " + uiSlotRaw);
		// Example: "12:30 AM - 01:30 AM , 02 Dec, 2025"

		// Split: [0] = time, [1] = "02 Dec", [2] = "2025"
		String[] parts = uiSlotRaw.split(",");
		String uiTime = parts[0].trim(); // "12:30 AM - 01:30 AM"
		String uiDayMonth = parts.length > 1 ? parts[1].trim() : ""; // "02 Dec"
		// year is parts[2], we don't care right now

		System.out.println("📌 UI Time      → " + uiTime);
		System.out.println("📌 UI Day/Month → " + uiDayMonth);
		System.out.println("📌 Expected Time      → " + TestSession.selectedSlotTime);
		System.out.println("📌 Expected Day/Month → " + TestSession.selectedSlotDate);

		// Compare time
		if (!uiTime.equalsIgnoreCase(TestSession.selectedSlotTime)) {
			throw new RuntimeException("❌ Slot TIME mismatch! Expected: '" + TestSession.selectedSlotTime
					+ "' but UI shows: '" + uiTime + "'");
		}

		// Compare date (dd MMM) — ignore year
		if (!uiDayMonth.equalsIgnoreCase(TestSession.selectedSlotDate)) {
			throw new RuntimeException("❌ Slot DATE mismatch! Expected: '" + TestSession.selectedSlotDate
					+ "' but UI shows: '" + uiDayMonth + "'");
		}

		System.out.println("🎯 Slot Time + Date validation PASSED");
	}

}
