package com.flashtech.imagetool.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PdfExportService {

    public void exportImageToPdfFitPage(
            BufferedImage image,
            File outputFile,
            float pageWidthMm,
            float pageHeightMm
    ) throws IOException {

        if (image == null) {
            throw new IllegalArgumentException("Image is null.");
        }

        if (pageWidthMm <= 0 || pageHeightMm <= 0) {
            throw new IllegalArgumentException("PDF width and height must be greater than 0.");
        }

        float pageWidthPt = mmToPoint(pageWidthMm);
        float pageHeightPt = mmToPoint(pageHeightMm);

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(pageWidthPt, pageHeightPt));
            document.addPage(page);

            PDImageXObject pdfImage = LosslessFactory.createFromImage(document, image);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                /*
                 * Fit full image to PDF page.
                 * PDF output size = pageWidthMm x pageHeightMm.
                 * Image will fill the page.
                 */
                contentStream.drawImage(
                        pdfImage,
                        0,
                        0,
                        pageWidthPt,
                        pageHeightPt
                );
            }

            document.save(outputFile);
        }
    }

    public void exportImageToPdfKeepRatio(
            BufferedImage image,
            File outputFile,
            float pageWidthMm,
            float pageHeightMm
    ) throws IOException {

        if (image == null) {
            throw new IllegalArgumentException("Image is null.");
        }

        if (pageWidthMm <= 0 || pageHeightMm <= 0) {
            throw new IllegalArgumentException("PDF width and height must be greater than 0.");
        }

        float pageWidthPt = mmToPoint(pageWidthMm);
        float pageHeightPt = mmToPoint(pageHeightMm);

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(pageWidthPt, pageHeightPt));
            document.addPage(page);

            PDImageXObject pdfImage = LosslessFactory.createFromImage(document, image);

            float imageRatio = image.getWidth() / (float) image.getHeight();
            float pageRatio = pageWidthPt / pageHeightPt;

            float drawWidth;
            float drawHeight;

            if (imageRatio > pageRatio) {
                drawWidth = pageWidthPt;
                drawHeight = pageWidthPt / imageRatio;
            } else {
                drawHeight = pageHeightPt;
                drawWidth = pageHeightPt * imageRatio;
            }

            float drawX = (pageWidthPt - drawWidth) / 2f;
            float drawY = (pageHeightPt - drawHeight) / 2f;

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                /*
                 * Keep original image ratio.
                 * PDF page size is exact.
                 * Image is centered inside the page.
                 */
                contentStream.drawImage(
                        pdfImage,
                        drawX,
                        drawY,
                        drawWidth,
                        drawHeight
                );
            }

            document.save(outputFile);
        }
    }

    private float mmToPoint(float mm) {
        return mm * 72f / 25.4f;
    }
}