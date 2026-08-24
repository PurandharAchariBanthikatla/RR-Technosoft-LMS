package com.rrtechnosoft.lms.service.storage;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Renders the certificate as a landscape A4 PDF with an embedded QR code
 * pointing at the public verification page (verificationUrl) — that URL,
 * not the PDF itself, is what proves the certificate is genuine when
 * scanned; the PDF is just the human-readable presentation of it.
 */
@Service
public class CertificatePdfService {

    private static final float PAGE_WIDTH = PDRectangle.A4.getHeight();  // landscape
    private static final float PAGE_HEIGHT = PDRectangle.A4.getWidth();

    public byte[] generate(String studentName, String courseTitle, String certificateNo,
                            OffsetDateTime issuedAt, String verificationUrl) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(PAGE_WIDTH, PAGE_HEIGHT));
            document.addPage(page);

            byte[] qrPng = generateQrPng(verificationUrl, 220);
            PDImageXObject qrImage = PDImageXObject.createFromByteArray(document, qrPng, "verification-qr");

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                // Border
                cs.setLineWidth(2f);
                cs.addRect(24, 24, PAGE_WIDTH - 48, PAGE_HEIGHT - 48);
                cs.stroke();

                drawCentered(cs, PDType1Font.HELVETICA_BOLD, 30, PAGE_HEIGHT - 110, "CERTIFICATE OF COMPLETION");
                drawCentered(cs, PDType1Font.HELVETICA, 14, PAGE_HEIGHT - 145, "RR Technosoft — This is to certify that");

                drawCentered(cs, PDType1Font.HELVETICA_BOLD, 26, PAGE_HEIGHT - 200, studentName);
                drawCentered(cs, PDType1Font.HELVETICA, 14, PAGE_HEIGHT - 230, "has successfully completed the course");
                drawCentered(cs, PDType1Font.HELVETICA_BOLD, 20, PAGE_HEIGHT - 265, courseTitle);

                String issuedLine = "Issued on " + issuedAt.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
                drawCentered(cs, PDType1Font.HELVETICA, 12, 150, issuedLine);
                drawCentered(cs, PDType1Font.HELVETICA, 10, 130, "Certificate No: " + certificateNo);

                float qrSize = 90;
                cs.drawImage(qrImage, PAGE_WIDTH - 150, 40, qrSize, qrSize);
                cs.setFont(PDType1Font.HELVETICA, 8);
                cs.beginText();
                cs.newLineAtOffset(PAGE_WIDTH - 150, 32);
                cs.showText("Scan to verify");
                cs.endText();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to render certificate PDF", e);
        }
    }

    private void drawCentered(PDPageContentStream cs, PDType1Font font, float fontSize, float y, String text) throws IOException {
        float textWidth = font.getStringWidth(text) / 1000 * fontSize;
        float x = (PAGE_WIDTH - textWidth) / 2;
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private byte[] generateQrPng(String content, int size) throws IOException {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", out);
            return out.toByteArray();
        } catch (WriterException e) {
            throw new IOException("Failed to generate QR code", e);
        }
    }
}
