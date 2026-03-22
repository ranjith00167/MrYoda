import io.restassured.response.Response;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.*;

public class ReportValidationTest {

    @Test(description = "QA Automation: Validate Reports From API")
    public void validateReportsFromAPI() throws Exception {

        // 1️⃣ Call Report API
        Response response =
                given()
                        .header("Content-Type", "application/json")
                .when()
                        .get("YOUR_REPORT_API_URL")
                .then()
                        .statusCode(200)
                        .extract()
                        .response();

        // 2️⃣ Extract user data
        String userName = response.jsonPath().getString("data.userName");
        String visitNumber = response.jsonPath().getString("data.visitNumber");

        System.out.println("User Name: " + userName);
        System.out.println("Visit Number: " + visitNumber);

        // 3️⃣ Extract all test reports
        List<Map<String, Object>> tests =
                response.jsonPath().getList("data.tests");

        System.out.println("Total Reports: " + tests.size());

        // 4️⃣ Loop through each report
        for (Map<String, Object> test : tests) {

            String testName = test.get("testName").toString();
            String testCode = test.get("testCode").toString();
            String department = test.get("department_name").toString();
            String reportUrl = test.get("url").toString();

            System.out.println("\n------------------------------------");
            System.out.println("Test Code: " + testCode);
            System.out.println("Test Name: " + testName);
            System.out.println("Department: " + department);
            System.out.println("Report URL: " + reportUrl);

            // 5️⃣ Download PDF from S3
            Response pdfResponse = given().get(reportUrl);

            Assert.assertEquals(pdfResponse.statusCode(), 200);

            byte[] pdfBytes = pdfResponse.asByteArray();

            // 6️⃣ Parse PDF
            PDDocument document = PDDocument.load(pdfBytes);

            PDFTextStripper stripper = new PDFTextStripper();
            String pdfText = stripper.getText(document);

            document.close();

            System.out.println("\nParsed PDF Content:");
            System.out.println(pdfText);

            // 7️⃣ Validate report content
            Assert.assertTrue(pdfText.contains(userName));
            Assert.assertTrue(pdfText.contains(testName));

            System.out.println("✅ Report validated successfully");

        }
    }
}