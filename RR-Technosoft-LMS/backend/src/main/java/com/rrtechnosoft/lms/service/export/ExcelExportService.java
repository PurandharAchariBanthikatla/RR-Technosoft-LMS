package com.rrtechnosoft.lms.service.export;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generic ".xlsx" export used by every report in the Reports & Analytics
 * module (see {@link com.rrtechnosoft.lms.controller.ReportsController}).
 * Each report builds its own header row + data rows (List&lt;List&lt;Object&gt;&gt;)
 * and hands them to {@link #export}; this class only knows how to lay a
 * table out on a sheet, style the header, and auto-size columns — it has no
 * knowledge of what a "student" or "assignment" is.
 */
@Service
public class ExcelExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

    /**
     * @param title      sheet title and first-row report heading
     * @param generatedAtLine subtitle line under the heading (e.g. filter summary, generated-at timestamp)
     * @param headers    column headers
     * @param rows       row data; cell values may be String, Number, Boolean, LocalDate, OffsetDateTime, or null
     */
    public byte[] export(String title, String generatedAtLine, List<String> headers, List<List<Object>> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(safeSheetName(title));

            CellStyle titleStyle = titleStyle(workbook);
            CellStyle subtitleStyle = subtitleStyle(workbook);
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle numberStyle = numberStyle(workbook);

            int rowIdx = 0;
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("RR TECHNOSOFT LMS — " + title);
            titleCell.setCellStyle(titleStyle);

            Row subtitleRow = sheet.createRow(rowIdx++);
            Cell subtitleCell = subtitleRow.createCell(0);
            subtitleCell.setCellValue(generatedAtLine);
            subtitleCell.setCellStyle(subtitleStyle);

            rowIdx++; // blank spacer row

            Row headerRow = sheet.createRow(rowIdx++);
            for (int c = 0; c < headers.size(); c++) {
                Cell cell = headerRow.createCell(c);
                cell.setCellValue(headers.get(c));
                cell.setCellStyle(headerStyle);
            }

            for (List<Object> row : rows) {
                Row dataRow = sheet.createRow(rowIdx++);
                for (int c = 0; c < row.size(); c++) {
                    Cell cell = dataRow.createCell(c);
                    writeCell(cell, row.get(c), numberStyle);
                }
            }

            for (int c = 0; c < headers.size(); c++) {
                sheet.autoSizeColumn(c);
                if (sheet.getColumnWidth(c) > 12000) sheet.setColumnWidth(c, 12000);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate Excel export", e);
        }
    }

    private void writeCell(Cell cell, Object value, CellStyle numberStyle) {
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof Number n) {
            cell.setCellValue(n.doubleValue());
            cell.setCellStyle(numberStyle);
        } else if (value instanceof Boolean b) {
            cell.setCellValue(b);
        } else if (value instanceof LocalDate d) {
            cell.setCellValue(d.format(DATE_FMT));
        } else if (value instanceof OffsetDateTime dt) {
            cell.setCellValue(dt.format(DATETIME_FMT));
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private CellStyle titleStyle(Workbook wb) {
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(IndexedColors.DARK_RED.getIndex());
        CellStyle style = wb.createCellStyle();
        style.setFont(font);
        return style;
    }

    private CellStyle subtitleStyle(Workbook wb) {
        Font font = wb.createFont();
        font.setItalic(true);
        font.setFontHeightInPoints((short) 10);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        CellStyle style = wb.createCellStyle();
        style.setFont(font);
        return style;
    }

    private CellStyle headerStyle(Workbook wb) {
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        CellStyle style = wb.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.RED.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle numberStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00"));
        return style;
    }

    private String safeSheetName(String title) {
        String name = title.replaceAll("[\\[\\]:*?/\\\\]", " ").trim();
        return name.length() > 31 ? name.substring(0, 31) : name;
    }
}
