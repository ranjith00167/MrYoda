package utilities;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelAllRows {

    public static List<Map<String, String>> getAllRows(String filePath, String sheetName) {
        List<Map<String, String>> allData = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) throw new IllegalArgumentException("❌ Sheet not found: " + sheetName);

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) throw new IllegalArgumentException("❌ Header row is missing");

            DataFormatter formatter = new DataFormatter();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row dataRow = sheet.getRow(i);
                if (dataRow == null) continue;

                Map<String, String> rowData = new HashMap<>();
                for (int j = 0; j < headerRow.getLastCellNum(); j++) {
                    Cell headerCell = headerRow.getCell(j);
                    Cell valueCell = dataRow.getCell(j);

                    if (headerCell != null) {
                        String key = formatter.formatCellValue(headerCell).trim().replace(",", "");
                        String value = (valueCell != null) ? formatter.formatCellValue(valueCell).trim().replace(",", "") : "";
                        rowData.put(key, value);
                    }
                }

                if (!rowData.isEmpty()) {
                    allData.add(rowData);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("❌ Failed to read Excel rows: " + e.getMessage());
        }

        return allData;
    }
}
