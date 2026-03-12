package pageObjects;

import org.openqa.selenium.By;

/**
 * Static locators for page elements.
 * This class provides static By references for WebElements used across tests.
 */
public class LocatorsPage {
    public static By searchTestField = By.xpath("//input[@placeholder='Search Test Here']");
    public static By addToCartButton = By.xpath("//button[normalize-space()='Add to cart' or normalize-space()='Add To Cart']");
    public static By cart_logo = By.xpath("//div[contains(@class,'cursor-pointer')][.//img[@alt='Cart']]");
}
