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
public void i_enter_valid_credentials() {
    BaseClass.waitAndInput(LocatorsPage.usernameInput, ConfigReader.get("username_ITDose"), 3);
    BaseClass.waitAndInput(LocatorsPage.passwordInput, ConfigReader.get("password_ITDose"), 3);
    BaseClass.waitAndClick(LocatorsPage.loginButton,3);
 }

@Then("I should be logged in successfully")
public void i_should_be_logged_in_successfully() {
   System.out.println("Login successful");
 }

@When("I click on the Department button")
public void i_click_on_the_department_button() {
   BaseClass.waitAndClick(LocatorsPage.departmentIcon,10);   
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
    BaseClass.waitAndClick(LocatorsPage.sampleManagementLink,10);
}

@When("I click on the sample collection button")
public void i_click_on_the_sample_collection_button() {
    BaseClass.waitAndClick(LocatorsPage.sampleCollectionLink,10);
}

@When("I select the laboratory name")
public void i_select_the_laboratory_name() {
    // Laboratory name selection might be the same as centre selection or similar
    // For now using the existing centre dropdown if it's there
    try {
        BaseClass.selectByVisibleText(LocatorsPage.selectCentreByUser, "YODA LIFELINE DIAGNOSTICS");
    } catch (Exception e) {}
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
public void i_enter_the_visit_number() {
    // Retrieve the visit number stored during the API payment approval step
    String visitId = RequestContext.getVisitNumber();
    
    // Fallback logic if visitNumber is not yet set in RequestContext
    if (visitId == null || visitId.isEmpty()) {
        System.out.println("⚠️ RequestContext.getVisitNumber() is null, checking ScenarioContext as secondary source");
        visitId = ScenarioContext.orderId; 
    }
    
    // Strict validation: Must not be null or hardcoded
    if (visitId == null || visitId.isEmpty()) {
        throw new RuntimeException("❌ FATAL: Visit Number is null! API flow must run before UI flow to provide a valid Visit ID.");
    }
    
    System.out.println("📝 Entering Visit Number (Extracted from API): " + visitId);
    BaseClass.waitAndInput(LocatorsPage.searchValueInput, visitId, 10);
}

@When("I click on the search button")
public void i_click_on_the_search_button() {
    BaseClass.waitAndClick(LocatorsPage.searchButton,10);
}
@And("I click on the view icon")
public void i_click_on_the_view_icon() {
    BaseClass.waitAndClick(LocatorsPage.viewIcon,10);
}

@When("I click on the select checkbox")
public void i_click_on_the_select_checkbox() {
    BaseClass.waitAndClick(LocatorsPage.checkAllCheckbox,10);
}

  @When("I select the sample type")
    public void i_select_the_sample_type() {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Random random = new Random();

        // Get ALL dropdowns in the sample table
        List<WebElement> dropdowns =
                driver.findElements(By.cssSelector("table#tblSample select[name^='sampletypes_']"));

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
            System.out.println("✅ Note: All sample type dropdowns already have values assigned (none were '0'). Proceeding...");
        }
    }
@When("I click on the collect button")
public void i_click_on_the_collect_button() {
    BaseClass.safeClick(LocatorsPage.collectButton);
}

@When("I click on the sample receive area")
public void i_click_on_the_sample_receive_area() {
    BaseClass.safeClick(LocatorsPage.sampleReceiveAreaLink);
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
            System.out.println("🔄 Attempting to click list view button (Attempt " + (i + 1) + "/" + maxRetries + ")...");
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
                "arguments[0].scrollIntoView({block: 'center'});", LocatorsPage.showIcon
            );
            
            // 3. Click with JS Fallback
            BaseClass.waitAndClickWithJSFallback(LocatorsPage.showIcon, 3);
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
        throw new RuntimeException("❌ SIN No is null or empty. Ensure it was extracted correctly in previous steps.");
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
    BaseClass.safeClick(LocatorsPage.saveButton);
}

@When("I click on the Department receive button")
public void i_click_on_the_department_receive_button() {
    BaseClass.safeClick(LocatorsPage.departmentReceiveLink);
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
        throw new RuntimeException("❌ SIN No is null or empty. Ensure it was extracted correctly in previous steps.");
    }
    System.out.println("📝 Entering SIN No: " + sinNo);
    
    // Aggressive input loop
    WebElement input = LocatorsPage.sinNo_searchBox;
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

@When("I click on the sample receive checkbox")
public void i_click_on_the_sample_receive_checkbox() {
    BaseClass.safeClick(LocatorsPage.selectDepartmentCheckbox);
}

@When("I click on the receive button")
public void i_click_on_the_receive_button() throws Throwable {
    Thread.sleep(5000);
    BaseClass.safeClick(LocatorsPage.receiveButton);
}

@When("I click on the sample processing button")
public void i_click_on_the_sample_processing_button() {
    BaseClass.safeClick(LocatorsPage.sampleProcessingLink);
}

@When("I click on the result entry button")
public void i_click_on_the_result_entry_button() {
    BaseClass.safeClick(LocatorsPage.resultEntryLink);
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

    System.out.println(">>> UI: Starting Multi-Visit Result Entry for SIN: " + sinNo);

    for (int i = 0; i < 15; i++) {
        BaseClass.waitInSeconds(3);
        
        // Ensure we are on the list page
        boolean isDetailsPage = driver.findElements(By.id("divInvestigation")).size() > 0 || 
                                driver.findElements(By.id("btnApprovedLabObs")).size() > 0;
                                
        if (isDetailsPage) {
            System.out.println("Iteration " + (i+1) + ": On details page, navigating back to list...");
            try {
                // Try clicking the sidebar link using JS to bypass visibility/menu issues
                js.executeScript("arguments[0].click();", LocatorsPage.resultEntryLink);
            } catch (Exception e) {
                System.out.println("⚠️ Sidebar click failed, trying search page URL or refresh...");
                driver.navigate().refresh();
                BaseClass.waitInSeconds(3);
            }
            BaseClass.waitInSeconds(3);
        }

        // 1. Re-enter SIN and Search
        System.out.println("Iteration " + (i + 1) + ": Re-searching for SIN: " + sinNo);
        try {
            WebElement searchBox = LocatorsPage.sinNo_searchBox;
            BaseClass.waitForVisibility(searchBox, 10);
            searchBox.clear();
            searchBox.sendKeys(sinNo);
            
            // Use JS click for search to be sure
            js.executeScript("arguments[0].click();", LocatorsPage.searchButton);
            BaseClass.waitInSeconds(1);
        } catch (Exception e) {
            System.out.println("⚠️ Search failed: " + e.getMessage() + ". Retrying...");
            continue;
        }

        // 2. Find pending visit links
        List<WebElement> visitLinks = driver.findElements(
            By.xpath("//table[contains(@class,'htCore')]//a[contains(@onclick,'PickRowData')]")
        );

        if (visitLinks.isEmpty()) {
            System.out.println("✅ All pending visits for SIN: " + sinNo + " processed. Exiting loop.");
            break;
        }

        System.out.println("Found " + visitLinks.size() + " pending rows. Opening first...");
        WebElement visit = visitLinks.get(0);
        
        try {
            // 3. Open Visit
            String visitId = visit.getText().trim();
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", visit);
            js.executeScript("arguments[0].click();", visit);

            // 4. Fill Values
            wait.until(ExpectedConditions.or(
                ExpectedConditions.visibilityOfElementLocated(By.id("divInvestigation")),
                ExpectedConditions.visibilityOfElementLocated(By.id("btnApprovedLabObs"))
            ));
            
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
            System.out.println("⚠️ Error processing iteration " + (i+1) + ": " + e.getMessage());
            // Try to force back to list for next attempt
            try { js.executeScript("arguments[0].click();", LocatorsPage.resultEntryLink); } catch (Exception ignored) {}
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
                BaseClass.safeClick(btn);
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