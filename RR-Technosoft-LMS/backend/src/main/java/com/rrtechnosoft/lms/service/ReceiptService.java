package com.rrtechnosoft.lms.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.rrtechnosoft.lms.entity.Payment;
import com.rrtechnosoft.lms.entity.Receipt;
import com.rrtechnosoft.lms.entity.StudentFee;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Generates the immutable payment receipt (PDF stored inline in `receipts.pdf_data`)
 * and, on demand, a live fee invoice for a student's outstanding balance.
 * Receipt numbers are minted from the `receipt_number_seq` DB sequence so
 * they stay unique and gap-free across concurrent payments.
 */
@Service
@RequiredArgsConstructor
public class ReceiptService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final ReceiptRepository receiptRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public Receipt generateForPayment(Payment payment, UUID generatedBy) {
        StudentFee studentFee = payment.getStudentFee();
        String receiptNumber = nextReceiptNumber();
        byte[] pdf = buildReceiptPdf(receiptNumber, payment, studentFee);

        Receipt receipt = Receipt.builder()
                .payment(payment)
                .studentFee(studentFee)
                .receiptNumber(receiptNumber)
                .amount(payment.getAmount())
                .pdfData(pdf)
                .generatedBy(generatedBy)
                .build();
        return receiptRepository.save(receipt);
    }

    public byte[] buildInvoicePdf(StudentFee studentFee) {
        try {
            Document document = new Document(PageSize.A4, 40, 40, 60, 60);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            addLetterhead(document, "FEE INVOICE");
            document.add(new Paragraph("Invoice generated: " + OffsetDateTime.now().format(DATE_FMT)));
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph("Student: " + studentFee.getStudent().getFullName()
                    + " (" + studentFee.getStudent().getStudentId() + ")"));
            if (studentFee.getCourse() != null) {
                document.add(new Paragraph("Course: " + studentFee.getCourse().getTitle()));
            }
            document.add(Chunk.NEWLINE);

            PdfPTable table = summaryTable();
            addRow(table, "Total Fee", format(studentFee.getTotalAmount()), studentFee.getCurrency());
            addRow(table, "Discount", format(studentFee.getDiscountAmount()), studentFee.getCurrency());
            addRow(table, "Fine / Late Charges", format(studentFee.getFineAmount()), studentFee.getCurrency());
            addRow(table, "Net Payable", format(studentFee.getNetPayable()), studentFee.getCurrency());
            addRow(table, "Amount Paid", format(studentFee.getAmountPaid()), studentFee.getCurrency());
            addRow(table, "Balance Due", format(studentFee.getBalanceDue()), studentFee.getCurrency());
            document.add(table);

            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Status: " + studentFee.getStatus()));
            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to generate invoice PDF", e);
        }
    }

    private byte[] buildReceiptPdf(String receiptNumber, Payment payment, StudentFee studentFee) {
        try {
            Document document = new Document(PageSize.A4, 40, 40, 60, 60);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            addLetterhead(document, "PAYMENT RECEIPT");
            document.add(new Paragraph("Receipt No: " + receiptNumber));
            document.add(new Paragraph("Issued: " + OffsetDateTime.now().format(DATE_FMT)));
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph("Received from: " + payment.getStudent().getFullName()
                    + " (" + payment.getStudent().getStudentId() + ")"));
            if (studentFee.getCourse() != null) {
                document.add(new Paragraph("Course: " + studentFee.getCourse().getTitle()));
            }
            document.add(Chunk.NEWLINE);

            PdfPTable table = summaryTable();
            addRow(table, "Amount Paid", format(payment.getAmount()), payment.getCurrency());
            addRow(table, "Payment Method", payment.getMethod() != null ? payment.getMethod().name() : "-", "");
            addRow(table, "Payment Reference", payment.getGatewayPaymentId() != null ? payment.getGatewayPaymentId() : payment.getId().toString(), "");
            addRow(table, "Remaining Balance", format(studentFee.getBalanceDue()), studentFee.getCurrency());
            document.add(table);

            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("This is a system-generated receipt and does not require a signature."));
            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to generate receipt PDF", e);
        }
    }

    private void addLetterhead(Document document, String title) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font orgFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
        Paragraph org = new Paragraph("RR TECHNOSOFT — Learning Management System", orgFont);
        org.setAlignment(Element.ALIGN_CENTER);
        document.add(org);
        Paragraph heading = new Paragraph(title, titleFont);
        heading.setAlignment(Element.ALIGN_CENTER);
        heading.setSpacingBefore(4);
        heading.setSpacingAfter(12);
        document.add(heading);
    }

    private PdfPTable summaryTable() {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{2f, 1.2f, 0.8f});
        } catch (DocumentException ignored) {
        }
        return table;
    }

    private void addRow(PdfPTable table, String label, String value, String currency) {
        table.addCell(new PdfPCell(new Phrase(label)));
        table.addCell(new PdfPCell(new Phrase(value)));
        table.addCell(new PdfPCell(new Phrase(currency)));
    }

    private static String format(BigDecimal amount) {
        return amount == null ? "0.00" : amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String nextReceiptNumber() {
        Long seq = jdbcTemplate.queryForObject("select nextval('receipt_number_seq')", Long.class);
        int year = OffsetDateTime.now().getYear();
        return "RRT-RCPT-" + year + "-" + String.format("%06d", seq);
    }

    @Transactional(readOnly = true)
    public Receipt getWithDetails(UUID id) {
        return receiptRepository.findWithDetailsById(id).orElseThrow(() -> ApiException.notFound("Receipt not found"));
    }
}
