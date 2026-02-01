package stepDefinition;

import java.util.Map;
import org.openqa.selenium.WebDriver;
import pageObjects.Locators;
import utilities.BaseClass;
import utilities.ScenarioContext;

public class BaseSteps {
    protected WebDriver driver;
    protected Locators LocatorsPage;
    protected Map<String, String> testData;

    public BaseSteps() {
        this.driver = BaseClass.driver;
        this.LocatorsPage = new Locators(driver);

        // ✅ Inject test data from ScenarioContext (populated in Hooks)
        this.testData = ScenarioContext.testData;
    }
}
