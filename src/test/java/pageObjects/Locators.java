package pageObjects;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

public class Locators {
    private WebDriver driver; // Used only for PageFactory
    public static String visitTypeSelected = "";


    @FindBy(id = "txtUserName")
public WebElement usernameInput;

@FindBy(id = "txtPassword")
public WebElement passwordInput;

@FindBy(xpath = "//input[@id='btnLogin']")
public WebElement loginButton;
@FindBy(xpath = "//img[@alt='Department']")
public WebElement departmentIcon;

@FindBy(xpath = "//a[normalize-space()='LABORATORY']")
public WebElement laboratoryLink;
@FindBy(xpath = "//select[@id='ddlCentreByUser']")
public WebElement selectCentreByUser;
@FindBy(xpath = "//option[@value='1' and text()='YODA LIFELINE DIAGNOSTICS']")
public WebElement selectCentreByUserOption;

@FindBy(xpath = "//img[@alt='Show']")
public WebElement showIcon;
@FindBy(xpath = "//a[normalize-space()='Sample Management']")
public WebElement sampleManagementLink;
@FindBy(xpath = "//table[@id='tb_ItemList']//th[contains(text(),'SIN No.')]/following::td[4]")
public WebElement sinNo;
@FindBy(xpath = "//a[normalize-space()='Sample Collection']")
public WebElement sampleCollectionLink;
@FindBy(xpath = "//select[@id='ddlSearchType']")
public WebElement searchOptionDropdown;
@FindBy(xpath = "//option[text()='Visit No.']")
public WebElement searchOptionVisitNo;
@FindBy(xpath = "//table[@id='tb_ItemList']//img[contains(@src,'view.gif') and contains(@style,'cursor:pointer')]")
public WebElement viewIcon;
@FindBy(xpath = "//a[normalize-space()='Sample Receive Area']")
public WebElement sampleReceiveAreaLink;

@FindBy(xpath = "//a[normalize-space()='Department Receive']")
public WebElement departmentReceiveLink;

@FindBy(xpath = "//a[normalize-space()='Sample Processing']")
public WebElement sampleProcessingLink;

@FindBy(xpath = "//a[normalize-space()='Result Entry']")
public WebElement resultEntryLink;
@FindBy(id = "ddlSearchType")
public WebElement searchTypeDropdown;

@FindBy(id = "txtSearchValue")
public WebElement searchValueInput;

@FindBy(id = "btnSearch")
public WebElement searchButton;
@FindBy(xpath = "//input[@id='btnCollect']")
public WebElement collectButton;

@FindBy(name = "sampletypes_1")
public WebElement sampleType1;

@FindBy(name = "sampletypes_4")
public WebElement sampleType4;

@FindBy(name = "sampletypes_5")
public WebElement sampleType5;
@FindBy(id = "ChkAll")
public WebElement checkAllCheckbox;

@FindBy(xpath = "//input[@id='btnPopupSave']")
public WebElement saveButton;
//input[@id="txtSearchValue"] 
@FindBy(xpath ="//input[@id='txtSearchValue']")
public WebElement sinNo_searchBox;
@FindBy(id = "ddlSearchType")
public WebElement departmentSearchTypeDropdown;

@FindBy(xpath = "//input[@id='txtBarcode']")
public WebElement departmentSearchValueInput;

@FindBy(id = "btnSearch")
public WebElement departmentSearchButton;

@FindBy(xpath = "//input[@id='hd']")
public WebElement selectDepartmentCheckbox;
@FindBy(id="btnApprovedLabObs")
public WebElement approvedLabObsButton;
@FindBy(xpath = "//input[@value='Receive']")
public WebElement receiveButton;
@FindBy(xpath = "//*[@id='divInvestigation']//textarea")
public WebElement investigationTextArea;

    @FindBy(xpath = "//span[text()='Login']")
    public WebElement login;

    @FindBy(xpath = "//input[@id='phone-number']")
    public WebElement mobile_number;

    @FindBy(xpath = "//input[@placeholder='Enter Mobile Number']")
    public WebElement enter_mobile_number;
    @FindBy(xpath = " //button[@aria-label='Get OTP']")
    public WebElement get_otp_button;
    @FindBy(xpath = "//input[@id='otp']")
    public WebElement otpValue;
    @FindBy(xpath = "//button[@aria-label='Submit OTP']")
    public WebElement submit_otp_button;
    @FindBy(xpath = "//h3[contains(text(),'Best Seller')]//following::p[text()='View All'][1]")
    public WebElement viewAll_BestSeller;
    @FindBy(xpath = "//h4[@class='text-textHeading md:text-lg text-base font-bold w-full  trunk-2']")
    public List<WebElement> testNames;
    public static String packageNameXpath = "//h4[@class='text-textHeading md:text-lg text-base font-bold w-full  trunk-2']";

    // h4[@class="text-textHeading md:text-lg text-base font-bold w-full trunk-2"]
    @FindBy(xpath = "//input[@placeholder='Search Test Here']")
    public WebElement searchTestField;
    @FindBy(xpath = "//button[normalize-space()='Add to cart' or normalize-space()='Add To Cart']")
    public WebElement addToCartButton;
    // Parent div is the clickable container (img[@alt='Cart'] is the icon inside it)
    @FindBy(xpath = "//div[contains(@class,'cursor-pointer')][.//img[@alt='Cart']]")
    public WebElement cart_logo;
    @FindBy(xpath = "//button[text()='Checkout']")
    public WebElement checkoutButton;
    @FindBy(xpath = "//p[text()='Members']")
    public WebElement members_tab;
    @FindBy(xpath = "//div[contains(@class,'overflow-y-auto')]//button[normalize-space()='Proceed']")
    public WebElement proceed_cart;
    @FindBy(xpath = "//div[contains(@class,'custom-select')]//span[@title] | //span[@title and (ancestor::div[contains(@class,'select')] or ancestor::div[contains(@class,'location')])]")
    public WebElement locationText;
    @FindBy(xpath = "//input[@placeholder='Search Lab Locations...']")
    public WebElement searchLabLocationField;
    @FindBy(xpath = " //input[@placeholder='Search Lab Locations...']//following::div[1]")
    public WebElement searchlabParticularLocation;
    @FindBy(xpath = "//div[contains(@class,'overflow-y-auto')]//button[normalize-space()='Proceed']")
    public WebElement location_proceed;
    @FindBy(xpath = "//button[text()='Pay Online']")
    public WebElement payOnlineButton;
    @FindBy(xpath = "//div[contains(@class,'overflow-y-auto')]//button[normalize-space()='Proceed']")
    public WebElement slot_proceed;
    @FindBy(xpath = "(//div[@data-value='upi'])[1]")
    public WebElement paymentUpiOption;

    @FindBy(xpath = "//div[@data-value='card']")
    public WebElement paymentCardOption;
    @FindBy(xpath = "//input[@placeholder='example@okhdfcbank']")
    public WebElement paymentUpiField;
    @FindBy(xpath = "//button[@data-testid='vpa-submit']")
    public WebElement verifyAndPayButton;
    @FindBy(xpath = "//iframe[@class='razorpay-checkout-frame']")
    public WebElement paymentFrame;
    @FindBy(xpath = "//button[text()='Lab Visit']")
    public WebElement labVisitButton;
    @FindBy(xpath = "//div[text()='Go to Orders']")
    public WebElement goToOrdersButton;
    // div[text()='Go to Orders']
    @FindBy(xpath = "//button[text()='Home Sample']")
    public WebElement HomeSampleButton;
    @FindBy(xpath = "//input[@placeholder='Search Locations...']//following::div[1]")
    public WebElement particularHomeLocationBox;

    @FindBy(xpath = "(//div[contains(@class,'p-4')][.//div[contains(@class,'viewButton')]])[1]//div[contains(@class,'viewButton')][1]")
    public WebElement viewOrderButton;
    @FindBy(xpath = "//p[normalize-space()='Amount to pay']/following-sibling::p[1]")
    public WebElement amountToPay;

    // Checkout summary header — contains both test count and total price (e.g. "3
    // Tests/Packages in Cart • ₹6,700")
    @FindBy(xpath = "//button[contains(@class,'justify-between') and contains(.,'Tests/Packages in Cart')]")
    public WebElement checkoutSummaryHeader;
    @FindBy(xpath = "//div[contains(@class,'divide-y')]//div[.//div[contains(@class,'text-textHeading')] and .//div[contains(@class,'cursor-pointer')]]")
    public List<WebElement> checkoutTestRows;
    public static boolean isMember = false;
    @FindBy(xpath = "//img[contains(@src, 'profile-star')]")
    public WebElement membership_star_icon;
    @FindBy(xpath = "//p[@class='text-sm font-semibold text-textHeading']")
    public WebElement checkout_Total_price;
    @FindBy(xpath = "//span[@class='text-sm font-bold']")
    public WebElement saveAmount;
    @FindBy(xpath = "//p[normalize-space()='Actual Price']/following-sibling::p[1]")
    public WebElement actualPriceCart;
    @FindBy(xpath = "//p[normalize-space()='MRP']/following-sibling::p[1]")
    public WebElement MRP;
    //
    @FindBy(xpath = "//button[text()='Pay in Cash']")
    public WebElement payInCashButton;

    @FindBy(xpath = "//h3[contains(@class,'number-flip') and @data-value]")
    public static WebElement razorpayAmountLabel;

    // 1. ✅ TITLE DROPDOWN (WORKING)
    @FindBy(xpath = "//select[@id='title']")
    public WebElement title_Dropdown;
    // Options: Mr, Mrs, Miss, Master, Baby, Dr, Prof
    @FindBy(xpath = "//option[@value='Mr.']")
    public WebElement Title_Option_Mr;
    // 2. ✅ FIRST NAME INPUT (WORKING)
    @FindBy(xpath = "//input[@id='first_name']")
    public WebElement firstName_Input;

    // 3. ✅ MIDDLE NAME INPUT (WORKING)
    @FindBy(xpath = "//input[@id='middle_name']")
    public WebElement middleName_Input;

    // 4. ✅ LAST NAME INPUT (WORKING)
    @FindBy(xpath = "//input[@id='last_name']")
    public WebElement lastName_Input;

    // 5. ✅ COUNTRY CODE DROPDOWN (WORKING)
    @FindBy(xpath = "//*[text()='Relation *']/following::select[1]")
    public WebElement selectRelation;
    @FindBy(xpath = "//option[@value='+91']")
    public WebElement mobileCountryCode_Dropdown;
    // Options: +91, +1

    // 6. ✅ MOBILE NUMBER INPUT (WORKING)
    @FindBy(xpath = "//input[@id='mobile']")
    public WebElement mobileNumber_Input;

    // 7. ✅ GENDER - MALE (WORKING)
    @FindBy(xpath = "//img[@alt='Male']")
    public WebElement male_GenderImage;

    // 8. ✅ GENDER - FEMALE (WORKING)
    @FindBy(xpath = "//img[@alt='Female']")
    public WebElement female_GenderImage;

    // 9. ✅ RELATION DROPDOWN (WORKING)
    @FindBy(xpath = "//option[@value='Friend']")
    public WebElement relation_Dropdown;
    // Options: Father, Mother, Husband, Wife, Son, Daughter, Sibling, Friend,
    // Others

    // 10. ✅ DATE OF BIRTH INPUT (WORKING)
    @FindBy(xpath = "//input[@id='dob']")
    public WebElement dateOfBirth_Input;

    // 11. ✅ EMAIL INPUT (WORKING)
    @FindBy(xpath = "//input[@id='email']")
    public WebElement email_Input;

    // 12. ✅ CANCEL BUTTON (WORKING)
    @FindBy(xpath = "//button[text()='Cancel']")
    public WebElement cancel_Button;

    // 13. ✅ SAVE BUTTON (WORKING)
    @FindBy(xpath = "//button[text()='Save']")
    public WebElement save_Button;

    // 14. ✅ ADD NEW MEMBER BUTTON (WORKING)
    @FindBy(xpath = "//img[@alt='add member']")
    public WebElement addNewMember_Button;

    // 15. ✅ SEE ALL LINK (WORKING)
    @FindBy(xpath = "//a[text()='See All']")
    public WebElement seeAll_Link;

    // 16. ⚠️ PROFILE PHOTO UPLOAD (NOT TESTED - requires file)
    @FindBy(xpath = "//input[@id='upload-image']")
    public WebElement profilePhoto_FileInput;

    @FindBy(xpath = "//img[@alt='profile image']")
    public WebElement profilePhoto_Image;
    @FindBy(xpath = "//img[@alt='DNA Decoder'][1]")
    public WebElement DNADecoder_TestImage;
    @FindBy(xpath = "//input[@placeholder='Search for DNA Decoder tests']")
    public WebElement DNADecoder_SearchField;
    @FindBy(xpath = "//button[@title='Clear search']")
    public WebElement clearSearch_Button;
    @FindBy(xpath = "//p[@class='text-sm font-bold']")
    public WebElement pricetext_DNADecoder;
    @FindBy(xpath = "//div[@class='flex flex-wrap gap-2 mt-1']//*[self::span or self::div][normalize-space(text())]")
    public List<WebElement> memberElements;

    @FindBy(xpath = "//input[@placeholder='Card Number']")
    public WebElement cardNumber_Input;
    @FindBy(xpath = "//input[@placeholder='MM / YY']")
    public WebElement expiryDate_Input;

    @FindBy(xpath = "//input[@placeholder='CVV']")
    public WebElement cvv_Input;

    @FindBy(xpath = "//input[@placeholder='Enter name on card']")
    public WebElement nameOnCard_Input;
    @FindBy(xpath = "//button[text()='Continue']")
    public WebElement continue_PaymentButton;
    @FindBy(xpath = "//input[@inputmode='email']")
    public WebElement email_Input_razorpay;
    @FindBy(xpath = "//input[@id='dccCurrencyOption_INR']")
    public WebElement confirmation_currency_INR;
    @FindBy(xpath = "//button[starts-with(normalize-space(.),'Pay')]")
    public WebElement pay_final_confirmation_button;
    @FindBy(xpath = "//button[@name='pay_and_save_card']")
    public WebElement pay_and_save_card_button;
    @FindBy(xpath = "//button[text()='Skip OTP']")
    public WebElement skip_OTP_button;
    @FindBy(xpath = "//button[@name='pay_without_saving_card']")
    public WebElement maybe_later_button;
    @FindBy(xpath = "//button[@data-val='S']")
    public WebElement success_button;
    @FindBy(xpath = "//button[text()='Pay in Cash']")
    public WebElement pay_in_cash_button;
    public static By goToOrdersButtons = By.xpath("//div[contains(text(),'Go to Orders')]");

    @FindBy(xpath = "//span[@class='text-primaryText font-medium']")
    public WebElement coupon;
    @FindBy(xpath = "//button[text()='Apply']")
    public WebElement apply;
    //span[@class="text-bold"]
    @FindBy(xpath = "//span[@class='text-bold']")
    public WebElement couponAmount;

    @FindBy(xpath = "//button[text()='Reschedule Order'][1]")
    public WebElement rescheduleOrderButton;
    @FindBy(xpath = "//input[@id='otp']")
    public WebElement reschedule_otp_input;
    @FindBy(xpath = "//button[text()='Verify & Reschedule']")
    public WebElement verifyAndRescheduleButton;
    @FindBy(xpath = "//input[@placeholder='Search Locations...']")
    public WebElement searchLocations_Input;
    @FindBy(xpath = "//textarea[@placeholder='Enter your remarks...']")
    public WebElement remarks_Textarea;
    @FindBy(xpath = "//button[text()='Submit']")
    public WebElement submit_Button;
    @FindBy(xpath = "//button[text()='OK']")
    public WebElement ok_Button;
    @FindBy(xpath = "//button[text()='Cancel order']")
    public WebElement cancelOrder_Button;
    @FindBy(xpath = "//div[contains(@class,'justify-around')]//button[normalize-space()='Cancel order'][1]")
    public WebElement cancelOrder_ConfirmButton;
    @FindBy(xpath = "//p[contains(text(),'Admin approval pending')]")
    public WebElement adminApprovalPending_Text;
    @FindBy(xpath = "//p[text()='Slot']/following-sibling::p[contains(@class,'font-semibold')]")
    public WebElement uiSlotElement;
    @FindBy(xpath = "//img[contains(@src,'registerprofile')]//following::p[1]")
    public WebElement welcomeText;
    @FindBy(xpath = "//img[@alt='Profile Registration']")
    public WebElement profile_RegistrationImage;
    @FindBy(xpath = "//input[@placeholder='Enter first name']")
    public WebElement firstNameInputProfilePage;
    @FindBy(xpath = "//input[@placeholder='Enter last name']")
    public WebElement lastnameInputProfilePage;
    @FindBy(xpath = "//input[@name='dateOfBirth']")
    public WebElement DOBProfilePage;
    @FindBy(xpath = "//input[@placeholder='Enter middle name']")
    public WebElement middleNameInputProfilePage;
    @FindBy(xpath = "//input[@name='termsAccepted']")
    public WebElement termsAndConditions_Checkbox;
    @FindBy(xpath = "//p[text()='Home']")
    public WebElement homeButton;
    @FindBy(xpath = "//img[@alt='Mr. Yoda Logo']")
    public WebElement mrYodaLogo_Image;
    @FindBy(xpath = "//input[@placeholder='Search test, symptom, organ etc']")
    public WebElement globalSearch_Input;

    public static String getPriceXPath(String testName) {
        return "(//p[normalize-space()='" + testName +
                "']/following::p[contains(@class,'font-bold')])[1]";
    }

    public static String getAddBtnXPath(String testName) {
        return "(//p[normalize-space()='" + testName +
                "']/following::button[contains(@class,'addtocartButton')])[1]";
    }

    @FindBy(xpath = "//h3[normalize-space()='Smart Choices']/following::p[normalize-space()='View All'][1]")
    public WebElement smartChoices_ViewAllButton;
    @FindBy(xpath = "//div[contains(@class,'flex') and .//h3[normalize-space()='DNA Decoder Panels at a Glance']]     //p[normalize-space()='View All']")
    public WebElement dnaDecoder_ViewAllButton;
    @FindBy(xpath = "//button[text()='Save']")
    public WebElement saveAddress;
    @FindBy(xpath = "//span[text()='Home']")
    public WebElement homeAddressButton;
    @FindBy(xpath = "//button[text()='Confirm location']")
    public WebElement confirmLocation_Button;
    @FindBy(xpath = "//p[text()='Locate me']")
    public WebElement locateMe_Button;
    @FindBy(xpath = "//p[text()='Add your address to get test samples collected at your doorstep.']")
    public WebElement newUserAddress;
    // ============================================
    // USER ADDRESS MANAGEMENT LOCATORS (TC_13)
    // ============================================
    @FindBy(xpath = "//button[contains(@class,'dropbtn') and descendant::img[contains(@alt,'profile image')]]")
    public WebElement profileIcon;

    @FindBy(xpath = "//a[contains(@href,'/profile') and contains(.,'View Profile')]")
    public WebElement viewProfileButton;

    @FindBy(xpath = "//img[@alt='My Addresses']")
    public WebElement addAddressButton;

    // Receiver/Full Name Input
    @FindBy(xpath = "//input[@name='receiver_name' or @placeholder='Enter Full Name']")
    public WebElement receiverNameInput;

    // House/Flat/Block Number Input (address_line1)
    @FindBy(xpath = "//input[@name='address_line1' or @placeholder='Enter House / Flat / Block No']")
    public WebElement addressLine1Input;

    // Road/Area Name Input (name field - confusing naming in form)
    @FindBy(xpath = "//input[@name='name' or @placeholder='Enter Road / Area']")
    public WebElement roadAreaInput;

    // City Input
    @FindBy(xpath = "//input[@name='city' or @placeholder='Enter City Name']")
    public WebElement cityInput;

    // State Input
    @FindBy(xpath = "//input[@name='state' or @placeholder='Enter State Name']")
    public WebElement stateInput;

    // Postal Code / PIN Code Input
    @FindBy(xpath = "//input[@name='postal_code' or @placeholder='Enter Pin Code']")
    public WebElement pinCodeInput;

    // Country Input (readonly)
    @FindBy(xpath = "//input[@name='country' or @placeholder='Enter Country Name']")
    public WebElement countryInput;

    // Country Code Dropdown
    @FindBy(id = "countryCode")
    public WebElement countryCodeDropdown;

    // Mobile Number Input
    @FindBy(xpath = "//input[@name='recipient_mobile_number' or @placeholder='Enter Mobile Number']")
    public WebElement mobileNumberInput;

    // Submit/Save Button (has class 'addtocartButton')
    @FindBy(xpath = "//button[@type='submit'] | //button[contains(@class,'addtocartButton')]")
    public WebElement saveAddressButton;

    // Cancel/Close Button
    @FindBy(xpath = "//button[contains(text(), 'Cancel') or contains(text(), 'Close')]")
    public WebElement cancelAddressButton;

    // Success Message
    @FindBy(xpath = "//div[@role='alert' and (contains(.,'Success') or contains(.,'saved'))] | //span[contains(text(),'Success') or contains(text(),'saved') or contains(text(),'updated')]")
    public WebElement successMessage;

    // Address List Items
    @FindBy(css = "div.border-cartItemsBdr")
    public List<WebElement> addressListItems;

    // Edit Button
    @FindBy(xpath = "(//div[contains(@class,'border-cartItemsBdr')]//button[.//svg])[1]")
    public WebElement editButton;

    // Delete/Remove Button
    @FindBy(xpath = "//button[contains(.,'Remove')]")
    public WebElement deleteButton;

    // Default Address Checkbox/Toggle
    @FindBy(xpath = "//div[contains(@class,'cursor-pointer') and .//p[contains(text(),'Set as Default') or contains(text(),'Default Address')]]")
    public WebElement defaultAddressCheckbox;

    // Default Address Badge/Label
    @FindBy(xpath = "//p[contains(text(),'Default Address') or contains(text(),'Default')]")
    public WebElement defaultAddressBadge;

    // Error Message
    @FindBy(xpath = "//p[contains(@class,'text-[var(--danger)')] | //p[contains(@class,'text-') and contains(text(),'error')]")
    public WebElement errorMessage;

    // Confirm Delete Button
    @FindBy(xpath = "//button[contains(text(),'Yes, Delete')]")
    public WebElement confirmDeleteButton;

    @FindBy(xpath = "//p[contains(@class,'text-[var(--danger)')] and contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'address already')]")
    public WebElement duplicateAddressError;

    @FindBy(css = "div.cancelpopup")
    public WebElement addressFormModal;

    @FindBy(xpath = "//div[contains(@class,'flex flex-col justify-start')]/p[1]/button")
    public WebElement nameElement;

    @FindBy(xpath = "//div[contains(@class,'flex flex-col justify-start')]/p[2]/span[1]")
    public WebElement genderElement;

    @FindBy(xpath = "//div[contains(@class,'flex flex-col justify-start')]/p[2]/span[2]")
    public WebElement mobileElement;

    @FindBy(xpath = "//div[contains(@class,'flex flex-col justify-start')]/p[2]")
    public WebElement ageGenderMobileRow;

    public Locators(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }
}
