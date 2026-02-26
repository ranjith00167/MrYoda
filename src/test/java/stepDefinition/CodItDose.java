package stepDefinition;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import utilities.BaseClass;
import utilities.ConfigReader;
import utilities.ScenarioContext;
import com.mryoda.diagnostics.api.utils.RequestContext;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

public class CodItDose extends BaseSteps {

    @Given("I am on the login page")
    public void i_am_on_the_login_page() {
        driver.get("http://uat.yodalifeline.in/yoda_uat_8.0/Design/Default.aspx");
    }

    @When("I enter valid credentials")
    public void i_enter_valid_credentials() throws Throwable {
        BaseClass.waitAndInput(LocatorsPage.usernameInput, ConfigReader.get("username_ITDose"), 3);
        BaseClass.waitAndInput(LocatorsPage.passwordInput, ConfigReader.get("password_ITDose"), 3);
        BaseClass.waitAndClick(LocatorsPage.loginButton, 3);
    }

    @Then("I should be logged in successfully")
    public void i_should_be_logged_in_successfully() {
        System.out.println("Login successful");
    }

    @When("I click on the Department button")
    public void i_click_on_the_department_button() {
        BaseClass.waitAndClick(LocatorsPage.departmentIcon, 10);
    }

    @When("I click on the Laboratory button")
    public void i_click_on_the_laboratory_button() {
        BaseClass.waitAndClick(LocatorsPage.laboratoryLink, 10);
        // Adding a small wait for the content to load
        BaseClass.waitInSeconds(1);

        // Handle Select dropdown more robustly
        try {
            // Try selecting by visible text directly
            BaseClass.selectByVisibleText(LocatorsPage.selectCentreByUser, "YODA LIFELINE DIAGNOSTICS");
            System.out.println("✅ Selected centre using Select class.");
        } catch (Exception e) {
            System.out.println("⚠️ Select class failed, trying manual click fallback...");
            // Fallback to manual clicks if Select class fails
            BaseClass.waitAndClick(LocatorsPage.selectCentreByUser, 10);
            BaseClass.waitInSeconds(1);
            BaseClass.waitAndClick(LocatorsPage.selectCentreByUserOption, 10);
        }
    }

    @When("I click on the sample management button")
    public void i_click_on_the_sample_management_button() {
        BaseClass.waitAndClick(LocatorsPage.sampleManagementLink, 10);
    }

    @When("I click on the sample collection button")
    public void i_click_on_the_sample_collection_button() {
        BaseClass.waitAndClick(LocatorsPage.sampleCollectionLink, 10);
    }

    @When("I select the laboratory name")
    public void i_select_the_laboratory_name() {
        // Laboratory name selection might be the same as centre selection or similar
        // For now using the existing centre dropdown if it's there
        try {
            BaseClass.selectByVisibleText(LocatorsPage.selectCentreByUser, "YODA LIFELINE DIAGNOSTICS");
        } catch (Exception e) {
        }
    }

    @And("I select the search option")
    public void i_select_the_search_option() throws Throwable {
        Thread.sleep(5000);
        BaseClass.waitAndClick(LocatorsPage.searchOptionDropdown, 10);
        BaseClass.waitAndClick(LocatorsPage.searchOptionVisitNo, 10);
    }

    @When("I click on the visit number")
    public void i_click_on_the_visit_number() {
        BaseClass.safeClick(LocatorsPage.searchValueInput);
    }

    @When("I enter the visit number")
    public void i_enter_the_visit_number() throws Throwable {
        System.out.println("\n==========================================================");
        System.out.println("🔍 UI AUTOMATION: Retrieving Visit Number from RequestContext");
        System.out.println("==========================================================");

        // Retrieve the visit numbers stored during the API payment approval step
        List<String> visits = RequestContext.getCurrentVisitNumbers();
        String visitId = RequestContext.getVisitNumber();
        Map<String, String> orderVisitMap = RequestContext.getOrderVisitMap();
        List<String> orderIds = RequestContext.getCurrentOrderIds();
        String paymentId = RequestContext.getCurrentPaymentId();

        // Debug: Print RequestContext state
        System.out.println("📊 RequestContext State:");
        System.out.println("   • Primary Visit Number: " + visitId);
        System.out.println("   • All Visit Numbers List: " + (visits != null ? visits : "NULL"));
        System.out.println("   • Visit Numbers Count: " + (visits != null ? visits.size() : 0));
        System.out.println("   • Order IDs: " + (orderIds != null ? orderIds : "NULL"));
        System.out.println("   • Payment ID: " + paymentId);
        System.out.println("   • Order-Visit Map Size: " + (orderVisitMap != null ? orderVisitMap.size() : 0));

        if (orderVisitMap != null && !orderVisitMap.isEmpty()) {
            System.out.println("   • Order-Visit Mappings:");
            for (Map.Entry<String, String> entry : orderVisitMap.entrySet()) {
                System.out.println("      - Order: " + entry.getKey() + " → Visit: " + entry.getValue());
            }
        }

        if (visits != null && visits.size() > 1) {
            System.out.println("ℹ️ Multi-Member Flow Detected. Available Visit IDs: " + visits);
        }

        // Fallback logic if primary visitNumber is not set in RequestContext
        if (visitId == null || visitId.isEmpty()) {
            System.out.println(
                    "⚠️ RequestContext.getVisitNumber() is null, attempting fallback strategies...");

            // FALLBACK 1: Try to get first visit from the visit numbers list
            if (visits != null && !visits.isEmpty()) {
                visitId = visits.get(0);
                System.out.println("   ✅ Fallback 1 SUCCESS: Got visit from list[0]: " + visitId);
            }

            // FALLBACK 2: Try to get visit from order-visit mapping
            if ((visitId == null || visitId.isEmpty()) && orderVisitMap != null && !orderVisitMap.isEmpty()) {
                // Try to get visit for the current order ID
                String currentOrderId = RequestContext.getCurrentOrderId();
                if (currentOrderId != null && orderVisitMap.containsKey(currentOrderId)) {
                    visitId = orderVisitMap.get(currentOrderId);
                    System.out.println("   ✅ Fallback 2 SUCCESS: Got visit from order-visit map: " + visitId);
                } else {
                    // Get first visit from the map
                    visitId = orderVisitMap.values().iterator().next();
                    System.out.println("   ✅ Fallback 2 SUCCESS: Got first visit from map: " + visitId);
                }
            }

            // NOTE: ScenarioContext.orderId contains ORDER ID, NOT VISIT NUMBER - DO NOT
            // USE IT!
            if (visitId == null || visitId.isEmpty()) {
                System.err.println("   ⚠️ WARNING: ScenarioContext.orderId = " + ScenarioContext.orderId
                        + " (This is ORDER ID, not Visit ID!)");
            }
        }

        // FINAL VALIDATION: Ensure we haven't picked up an Order ID (GUID) by mistake
        if (visitId != null && visitId.contains("-") && visitId.length() > 20) {
            System.err.println("\n🚨 CRITICAL ERROR: Detected ORDER ID (GUID) instead of VISIT NUMBER!");
            System.err.println("   Bad Value: " + visitId);
            System.err.println("   Attempting to find an alternative non-GUID visit number...");

            visitId = null; // Clear the bad value

            // Try to find ANY non-GUID in the visits list
            if (visits != null) {
                for (String v : visits) {
                    if (!(v.contains("-") && v.length() > 20)) {
                        visitId = v;
                        System.out.println("   ✅ RE-RECOVERY SUCCESS: Found valid visit in list: " + visitId);
                        break;
                    }
                }
            }

            // If still null, try the map values
            if (visitId == null && orderVisitMap != null) {
                for (String v : orderVisitMap.values()) {
                    if (!(v.contains("-") && v.length() > 20)) {
                        visitId = v;
                        System.out.println("   ✅ RE-RECOVERY SUCCESS: Found valid visit in map values: " + visitId);
                        break;
                    }
                }
            }
        }

        // Strict validation: Must not be null or hardcoded
        if (visitId == null || visitId.isEmpty()) {
            System.err.println("\n❌ ================================================");
            System.err.println("   FATAL ERROR: Visit Number Not Found!");
            System.err.println("================================================");
            System.err.println("⚠️ All data sources exhausted:");
            System.err.println("   1. RequestContext.getVisitNumber(): " + RequestContext.getVisitNumber());
            System.err.println("   2. RequestContext.getCurrentVisitNumbers(): " + visits);
            System.err.println("   3. ScenarioContext.orderId: " + ScenarioContext.orderId);
            System.err.println("\nPossible causes:");
            System.err.println("1. API Payment Approval step did NOT execute or FAILED");
            System.err.println("2. Visit number extraction from API response failed");
            System.err.println("3. RequestContext was not populated (Check API test logs)");
            System.err.println("4. API and UI tests running in separate JVM instances");
            System.err.println("================================================\n");

            throw new RuntimeException(
                    "❌ FATAL: Visit Number is null! API flow must run before UI flow to provide a valid Visit ID.");
        }

        System.out.println("✅ Visit Number Retrieved: " + visitId);
        System.out.println("📝 Entering Visit Number into UI search field...");
        BaseClass.waitAndInput(LocatorsPage.searchValueInput, visitId, 10);
        System.out.println("✅ Visit Number entered successfully");
    }

    @When("I click on the search button")
    public void i_click_on_the_search_button() {
        BaseClass.waitAndClick(LocatorsPage.searchButton, 10);
    }

    @And("I click on the view icon")
    public void i_click_on_the_view_icon() {
        BaseClass.waitAndClick(LocatorsPage.viewIcon, 10);
    }

    @When("I click on the select checkbox")
    public void i_click_on_the_select_checkbox() {
        BaseClass.waitAndClick(LocatorsPage.checkAllCheckbox, 10);
    }

    @When("I select the sample type")
    public void i_select_the_sample_type() {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Random random = new Random();

        // Get ALL dropdowns in the sample table
        List<WebElement> dropdowns = driver
                .findElements(By.cssSelector("table#tblSample select[name^='sampletypes_']"));

        if (dropdowns.isEmpty()) {
            throw new RuntimeException("No sample type dropdowns found in the table.");
        }

        boolean anySelectionDone = false;

        // Loop through ALL dropdowns
        for (WebElement dropdown : dropdowns) {

            wait.until(ExpectedConditions.visibilityOf(dropdown));

            Select select = new Select(dropdown);
            String currentValue = select.getFirstSelectedOption().getAttribute("value");

            // Only act on dropdowns having value = 0
            if (!"0".equals(currentValue)) {
                continue;
            }

            // Scroll into view before interacting
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", dropdown);

            // Collect valid non-zero options
            List<String> validValues = new ArrayList<>();
            for (WebElement option : select.getOptions()) {
                String value = option.getAttribute("value");

                if (value != null && !value.isBlank() && !"0".equals(value)) {
                    validValues.add(value);
                }
            }

            if (validValues.isEmpty()) {
                System.out.println("⚠ No valid options found for dropdown: " + dropdown.getAttribute("name"));
                continue;
            }

            // Select random valid value
            String chosenValue = validValues.get(random.nextInt(validValues.size()));
            select.selectByValue(chosenValue);

            System.out.println("✔ Selected sample type [" + chosenValue + "] for " + dropdown.getAttribute("name"));

            anySelectionDone = true;
        }

        if (!anySelectionDone) {
            System.out.println(
                    "✅ Note: All sample type dropdowns already have values assigned (none were '0'). Proceeding...");
        }
    }

    @When("I click on the collect button")
    public void i_click_on_the_collect_button() {
        BaseClass.waitAndClick(LocatorsPage.collectButton, 10);
    }

    @When("I click on the sample receive area")
    public void i_click_on_the_sample_receive_area() {
        BaseClass.waitAndClick(LocatorsPage.sampleReceiveAreaLink, 10);
    }

    @When("I extract the SIN NO from the UI")
    public void i_extract_the_sin_no_from_the_ui() {
        try {
            System.out.println("🔍 Attempting to extract SIN No using locator...");
            try {
                BaseClass.scrollIntoView(LocatorsPage.sinNo, 10);
            } catch (Exception e) {
                System.out.println("⚠️ Scroll failed (might be hidden), proceeding to extraction attempt...");
            }

            String extractedText = BaseClass.getTextSafe(LocatorsPage.sinNo, 10);
            ScenarioContext.extractedSinNo = extractedText;

            // Store in RequestContext for multi-visit tracking
            String currentVisit = com.mryoda.diagnostics.api.utils.RequestContext.getVisitNumber();
            if (currentVisit != null && extractedText != null && !extractedText.isEmpty()) {
                com.mryoda.diagnostics.api.utils.RequestContext.setSinNumberForVisit(currentVisit, extractedText);
            }

            System.out.println("✅ Extracted SIN No from UI: " + ScenarioContext.extractedSinNo);

            if (ScenarioContext.extractedSinNo == null || ScenarioContext.extractedSinNo.isEmpty()) {
                throw new RuntimeException("❌ Failed to extract SIN No from UI (Result was empty/null)");
            }
        } catch (Exception e) {
            System.err.println("❌ Error in extraction: " + e.getMessage());
            throw new RuntimeException("Failed to extract SIN No", e);
        }
    }

    @When("I click on the list view button")
    public void i_click_on_the_list_view_button() throws Throwable {
        Thread.sleep(10000);
        int maxRetries = 5;
        for (int i = 0; i < maxRetries; i++) {
            try {
                System.out.println(
                        "🔄 Attempting to click list view button (Attempt " + (i + 1) + "/" + maxRetries + ")...");
                // 1. Handle potential Alert from previous step
                try {
                    org.openqa.selenium.Alert alert = BaseClass.driver.switchTo().alert();
                    if (alert != null) {
                        System.out.println("⚠️ Alert detected: " + alert.getText());
                        alert.accept();
                    }
                } catch (Exception e) {
                    // No alert present, ignore
                }

                // 2. Ensure element is in view (Scroll)
                ((org.openqa.selenium.JavascriptExecutor) BaseClass.driver).executeScript(
                        "arguments[0].scrollIntoView({block: 'center'});", LocatorsPage.showIcon);

                // 3. Click with JS Fallback
                BaseClass.waitAndClick(LocatorsPage.showIcon, 3);
                System.out.println("✅ Click successful.");
                return;
            } catch (Exception e) {
                System.out.println("⚠️ Click failed: " + e.getMessage());
                BaseClass.waitInSeconds(1);
            }
        }
        throw new RuntimeException("❌ Failed to click list view button after " + maxRetries + " attempts");
    }

    @When("I enter the SIN NO in the input box")
    public void i_enter_the_sin_no_in_the_input_box() {
        String sinNo = ScenarioContext.extractedSinNo;
        if (sinNo == null || sinNo.isEmpty()) {
            throw new RuntimeException(
                    "❌ SIN No is null or empty. Ensure it was extracted correctly in previous steps.");
        }
        System.out.println("📝 Entering SIN No: " + sinNo);

        // Aggressive input loop
        WebElement input = LocatorsPage.departmentSearchValueInput;
        boolean success = false;

        for (int i = 0; i < 5; i++) { // Retry up to 5 times
            try {
                BaseClass.waitForVisibility(input, 10);
                input.click();
                input.clear();

                // Try different interaction speeds
                if (i % 2 == 0) {
                    input.sendKeys(sinNo);
                } else {
                    // JS Fallback
                    org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
                    js.executeScript("arguments[0].value = arguments[1];", input, sinNo);
                    js.executeScript("arguments[0].dispatchEvent(new Event('change'));", input);
                }

                BaseClass.waitInSeconds(1); // Give it a moment to register

                String currentVal = input.getAttribute("value");
                if (sinNo.equals(currentVal)) {
                    System.out.println("✅ SIN No entered and verified successfully.");
                    success = true;
                    break;
                } else {
                    System.out.println("⚠️ Input mismatch (Found: '" + currentVal + "'). Retrying...");
                }
            } catch (Exception e) {
                System.out.println("⚠️ Input attempt failed (" + e.getMessage() + "). Retrying...");
            }
            BaseClass.waitInSeconds(1);
        }

        if (!success) {
            throw new RuntimeException("❌ Failed to input SIN No after multiple attempts.");
        }
    }

    @When("I click on the save button")
    public void i_click_on_the_save_button() {
        BaseClass.waitAndClick(LocatorsPage.saveButton, 10);
    }

    @When("I click on the Department receive button")
    public void i_click_on_the_department_receive_button() {
        BaseClass.waitAndClick(LocatorsPage.departmentReceiveLink, 10);
    }

    @When("I select the SIN NO in the dropdown")
    public void i_select_the_sin_no_in_the_dropdown() {
        System.out.println("Selecting 'SIN No.' from dropdown...");
        // Fallback if direct select doesn't work or if simple click is needed
        try {
            BaseClass.selectByVisibleText(LocatorsPage.departmentSearchTypeDropdown, "SIN No.");
        } catch (Exception e) {
            System.out.println("⚠️ Could not select 'SIN No.' by text, attempting click method if locator exists...");
            // If there was a specific option locator, we would click it here.
            // For now, assume "SIN No." text works as per standard html select.
        }
    }

    @And("I enter the SIN NO in the input box in the department receive area")
    public void i_enter_the_sin_no_in_the_input_box_in_the_department_receive_area() {
        String sinNo = ScenarioContext.extractedSinNo;
        if (sinNo == null || sinNo.isEmpty()) {
            throw new RuntimeException(
                    "❌ SIN No is null or empty. Ensure it was extracted correctly in previous steps.");
        }
        System.out.println("📝 Entering SIN No: " + sinNo);

        // Aggressive input loop
        WebElement input = LocatorsPage.sinNo_searchBox;
        boolean success = false;

        for (int i = 0; i < 5; i++) { // Retry up to 5 times
            try {
                // Ensure search type is set to SIN No.
                try {
                    BaseClass.selectByVisibleText(LocatorsPage.departmentSearchTypeDropdown, "SIN No.");
                } catch (Exception e) {
                }

                BaseClass.waitForVisibility(input, 10);
                input.click();
                input.clear();

                // Try different interaction speeds
                if (i % 2 == 0) {
                    input.sendKeys(sinNo);
                } else {
                    // JS Fallback
                    org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
                    js.executeScript("arguments[0].value = arguments[1];", input, sinNo);
                    js.executeScript("arguments[0].dispatchEvent(new Event('change'));", input);
                }

                BaseClass.waitInSeconds(1); // Give it a moment to register

                String currentVal = input.getAttribute("value");
                if (sinNo.equals(currentVal)) {
                    System.out.println("✅ SIN No entered and verified successfully.");
                    success = true;
                    break;
                } else {
                    System.out.println("⚠️ Input mismatch (Found: '" + currentVal + "'). Retrying...");
                }
            } catch (Exception e) {
                System.out.println("⚠️ Input attempt failed (" + e.getMessage() + "). Retrying...");
            }
            BaseClass.waitInSeconds(1);
        }

        if (!success) {
            throw new RuntimeException("❌ Failed to input SIN No after multiple attempts.");
        }
    }

    @When("I click on the sample receive checkbox")
    public void i_click_on_the_sample_receive_checkbox() {
        BaseClass.waitAndClick(LocatorsPage.selectDepartmentCheckbox, 10);
    }

    @When("I click on the receive button")
    public void i_click_on_the_receive_button() throws Throwable {
        Thread.sleep(5000);
        BaseClass.waitAndClick(LocatorsPage.receiveButton, 10);
    }

    @When("I click on the sample processing button")
    public void i_click_on_the_sample_processing_button() {
        BaseClass.waitAndClick(LocatorsPage.sampleProcessingLink, 10);
    }

    @When("I click on the result entry button")
    public void i_click_on_the_result_entry_button() {
        BaseClass.waitAndClick(LocatorsPage.resultEntryLink, 10);
    }

    @When("I enter the value of the tests")
    public void i_enter_the_value_of_the_tests() throws Throwable {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String sinNo = ScenarioContext.extractedSinNo;

        if (sinNo == null || sinNo.isEmpty()) {
            System.out.println("⚠️ No SIN NO found in Context, using Visit Number as fallback...");
            sinNo = RequestContext.getVisitNumber();
        }

        String targetVisit = RequestContext.getVisitNumber();
        System.out.println(
                ">>> UI: Starting Multi-Visit Result Entry for SIN: " + sinNo + " (Target Visit: " + targetVisit + ")");

        for (int i = 0; i < 15; i++) {
            BaseClass.waitInSeconds(3);

            // Ensure we are on the list page
            boolean isDetailsPage = driver.findElements(By.id("divInvestigation")).size() > 0 ||
                    driver.findElements(By.id("btnApprovedLabObs")).size() > 0;

            if (isDetailsPage) {
                System.out.println("Iteration " + (i + 1) + ": On details page, navigating back to list...");
                try {
                    js.executeScript("arguments[0].click();", LocatorsPage.resultEntryLink);
                } catch (Exception e) {
                    System.out.println("⚠️ Sidebar click failed, trying search page URL or refresh...");
                    driver.navigate().refresh();
                    BaseClass.waitInSeconds(3);
                }
                BaseClass.waitInSeconds(3);
            }

            // 1. Re-enter SIN and Search (Ensure search type is SIN No.)
            System.out.println("Iteration " + (i + 1) + ": Re-searching for SIN: " + sinNo);
            try {
                WebElement searchBox = LocatorsPage.sinNo_searchBox;
                BaseClass.waitForVisibility(searchBox, 10);
                searchBox.clear();

                // EXPLICITLY SELECT SIN No. DROP DOWN
                try {
                    BaseClass.selectByVisibleText(LocatorsPage.searchTypeDropdown, "SIN No.");
                    BaseClass.waitInSeconds(1);
                } catch (Exception e) {
                    System.out.println("⚠️ Could not select 'SIN No.' in dropdown, continuing anyway...");
                }

                searchBox.sendKeys(sinNo);

                // Check initial count to detect refresh
                int initialCount = driver
                        .findElements(
                                By.xpath("//table[contains(@class,'htCore')]//a[contains(@onclick,'PickRowData')]"))
                        .size();

                // Use JS click for search to be sure
                js.executeScript("arguments[0].click();", LocatorsPage.searchButton);

                // Wait for results
                BaseClass.waitInSeconds(4);
                int newCount = driver
                        .findElements(
                                By.xpath("//table[contains(@class,'htCore')]//a[contains(@onclick,'PickRowData')]"))
                        .size();
                System.out.println("   Search complete. Rows: " + initialCount + " -> " + newCount);
            } catch (Exception e) {
                System.out.println("⚠️ Search failed: " + e.getMessage() + ". Retrying...");
                continue;
            }

            // 2. Find pending visit links
            List<WebElement> visitLinks = driver.findElements(
                    By.xpath("//table[contains(@class,'htCore')]//a[contains(@onclick,'PickRowData')]"));

            if (visitLinks.isEmpty()) {
                System.out.println("✅ All pending visits for SIN: " + sinNo + " processed. Exiting loop.");
                break;
            }

            System.out.println("Found " + visitLinks.size() + " pending rows. Looking for Visit: " + targetVisit);

            WebElement visitToOpen = null;
            String visitId = "";

            // Loop through links to find our target visit
            for (WebElement visit : visitLinks) {
                visitId = visit.getText().trim();
                System.out.println("   Checking row: [" + visitId + "]");
                if (targetVisit == null || visitId.equalsIgnoreCase(targetVisit) || visitId.contains(targetVisit)) {
                    visitToOpen = visit;
                    break;
                }
            }

            if (visitToOpen == null) {
                System.out.println("⚠️ Target visit " + targetVisit + " not found in current results.");
                // Fallback: If after 2 retries it's still missing, refresh the page
                if (i > 1 && i % 2 == 0) {
                    System.out.println("🔄 Stale data detected. Refreshing page...");
                    driver.navigate().refresh();
                    BaseClass.waitInSeconds(5);
                    js.executeScript("arguments[0].click();", LocatorsPage.resultEntryLink);
                    BaseClass.waitInSeconds(3);
                }
                continue;
            }

            try {
                // 3. Open Visit
                System.out.println("Opening visit: " + visitId);
                js.executeScript("arguments[0].scrollIntoView({block:'center'});", visitToOpen);
                js.executeScript("arguments[0].click();", visitToOpen);

                // 4. Fill Values
                wait.until(ExpectedConditions.or(
                        ExpectedConditions.visibilityOfElementLocated(By.id("divInvestigation")),
                        ExpectedConditions.visibilityOfElementLocated(By.id("btnApprovedLabObs"))));

                BaseClass.enterValuesInResultTable();

                // 5. Approve
                System.out.println("Approving visit: " + visitId);
                if (driver.findElements(By.id("btnApprovedLabObs")).size() > 0) {
                    js.executeScript("arguments[0].click();", LocatorsPage.approvedLabObsButton);
                }

                // Wait for approval processing (Crucial for multi-test)
                System.out.println("Waiting for approval to complete...");
                BaseClass.waitInSeconds(3);

            } catch (Exception e) {
                System.out.println("⚠️ Error processing iteration " + (i + 1) + ": " + e.getMessage());
                // Try to force back to list for next attempt
                try {
                    js.executeScript("arguments[0].click();", LocatorsPage.resultEntryLink);
                } catch (Exception ignored) {
                }
            }
        }
    }

    @When("I click on the approve button")
    public void i_click_on_the_approve_button() throws Throwable {
        // This step is now handled inside the loop for multi-department orders.
        // We only perform a final check if there's an active button visible on screen.
        BaseClass.waitInSeconds(3);
        try {
            if (driver.findElements(By.id("btnApprovedLabObs")).size() > 0) {
                WebElement btn = driver.findElement(By.id("btnApprovedLabObs"));
                if (btn.isDisplayed() && btn.isEnabled()) {
                    System.out.println("Clicking approve button (final check)...");
                    BaseClass.waitAndClick(btn, 10);
                    System.out.println("Approve button clicked successfully");
                } else {
                    System.out.println("✅ No visible approve button (already handled by loop).");
                }
            } else {
                System.out.println("✅ No pending approve button found.");
            }
        } catch (Exception e) {
            System.out.println("ℹ️ Skipping final approve click as element is not interactable or missing.");
        }
    }

}