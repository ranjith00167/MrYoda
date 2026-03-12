package stepDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import utilities.BaseClass;
import utilities.BasePriceManager;

public class DNADecoder extends BaseSteps {
	
	@Given("load the excel data for single member and lab visit with memership in DNA Decoder")
	public void load_the_excel_data_for_single_member_and_lab_visit_with_memership_in_dna_decoder() {
	   
		BaseClass.loadExcelData("DNADecoder", "6");
	}
    @When("the user navigates to DNA Decoder Lab Visit page")
    public void the_user_navigates_to_dna_decoder_lab_visit_page() {
        BaseClass.waitAndClick(LocatorsPage.DNADecoder_TestImage, 10);
    }

    @When("the user selects DNA Decoder tests and adds them to cart")
    public void the_user_selects_DNA_Decoder_tests_and_adds_them_to_cart() throws Exception {

        System.out.println("========== 🧬 DNA DECODER TEST SELECTION ==========");

        BasePriceManager.resetAll(); // Clear previous data

        String testNamesData = BaseClass.testData.get("testName");

        if (testNamesData == null || testNamesData.trim().isEmpty()) {
            throw new AssertionError("❌ Test name(s) missing in Excel for this scenario!");
        }

        String[] testNames = testNamesData.split(",");

        for (String testName : testNames) {
            testName = testName.trim();
            System.out.println("🔍 Executing: " + testName);

            BaseClass.waitAndInput(LocatorsPage.DNADecoder_SearchField, testName, 10);
            Thread.sleep(2000);

            String priceText = LocatorsPage.pricetext_DNADecoder.getText().trim();
            double price = BasePriceManager.cleanAndConvert(priceText);

            BasePriceManager.addSelectionTestPrice(testName, price);

            System.out.println("💰 Captured price → " + priceText + " (" + price + ")");

            BaseClass.waitAndClick(LocatorsPage.addToCartButton, 10);
            Thread.sleep(1000);

            BaseClass.clear(LocatorsPage.DNADecoder_SearchField);
            Thread.sleep(700);
        }

        System.out.println("🟩 SUCCESS → All DNA Decoder tests added & prices captured!");
    }

    }

