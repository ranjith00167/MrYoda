package stepDefinition;

import java.lang.reflect.Proxy;
import java.util.Map;
import org.openqa.selenium.WebDriver;
import pageObjects.Locators;
import utilities.BaseClass;
import utilities.ScenarioContext;

public class BaseSteps {
    /**
     * A JDK dynamic proxy that forwards every WebDriver call to the CURRENT
     * BaseClass.driver at the time of invocation. This avoids capturing a null
     * reference in the constructor (Cucumber creates step-def instances before
     * the @Before hook in Hooks.java has a chance to start the browser).
     */
    protected final WebDriver driver = (WebDriver) Proxy.newProxyInstance(
        WebDriver.class.getClassLoader(),
        new Class<?>[] { WebDriver.class },
        (proxy, method, args) -> method.invoke(BaseClass.driver, args)
    );

    protected Locators LocatorsPage;
    protected Map<String, String> testData;

    public BaseSteps() {
        // LocatorsPage is created with the proxy driver so PageFactory elements
        // are also lazily resolved against the live browser instance.
        this.LocatorsPage = new Locators(driver);
        this.testData     = ScenarioContext.testData;
    }
}
