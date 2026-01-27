package utilities;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

public class ExcelUtils {

    /**
     * Reads one row of data from an Excel sheet as a key-value map.
     *
     * @param filePath  Path to the Excel file
     * @param sheetName Name of the sheet
     * @param rowNum    Zero-based data row number (header is always row 0)
     * @return Map<String, String> where keys are column headers and values are row values
     */
    public static Map<String, String> getRowData(String filePath, String sheetName, int rowNum) {
        Map<String, String> rowData = new HashMap<>();

        System.out.println("📘 Reading Excel File: " + filePath);
        System.out.println("🔎 Target Sheet: " + sheetName + " | Row Index: " + rowNum);

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalArgumentException("❌ Sheet not found: " + sheetName);
            }
            System.out.println("✅ Sheet found: " + sheetName);

            Row headerRow = sheet.getRow(0);
            Row dataRow = sheet.getRow(rowNum);

            if (headerRow == null || dataRow == null) {
                throw new IllegalArgumentException("❌ Header or Data Row is missing at index: " + rowNum);
            }
            System.out.println("✅ Header and Data row fetched successfully.");

            DataFormatter formatter = new DataFormatter();

            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell headerCell = headerRow.getCell(i);
                Cell valueCell = dataRow.getCell(i);

                if (headerCell != null) {
                    String key = formatter.formatCellValue(headerCell).trim();
                    String value = "";

                    if (valueCell != null) {
                        switch (valueCell.getCellType()) {
                            case STRING:
                                value = valueCell.getStringCellValue().trim();
                                break;
                            case NUMERIC:
                                double numericValue = valueCell.getNumericCellValue();
                                if (numericValue == (long) numericValue) {
                                    value = String.valueOf((long) numericValue);
                                } else {
                                    value = String.valueOf(numericValue);
                                }
                                break;
                            case BOOLEAN:
                                value = String.valueOf(valueCell.getBooleanCellValue());
                                break;
                            case FORMULA:
                                value = formatter.formatCellValue(valueCell);
                                break;
                            default:
                                value = formatter.formatCellValue(valueCell);
                                break;
                        }
                    }

                    rowData.put(key, value);
                    System.out.println("🟢 Column: " + key + " | Value: " + value);
                } else {
                    System.out.println("⚠️ Skipped column index: " + i + " due to missing header.");
                }
            }

            System.out.println("✅ Row data extraction complete.");

        } catch (Exception e) {
            System.err.println("❌ Exception while reading Excel: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error reading Excel file: " + e.getMessage());
        }

        return rowData;
    }
}
