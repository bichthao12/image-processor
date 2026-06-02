package com.flashtech.imagetool.service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

public class ImageIOService {

    private static final int DEFAULT_PREVIEW_MAX_SIDE = 1800;

    public BufferedImage readFullImage(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);

        if (image == null) {
            throw new IOException("Unsupported or corrupted image file: " + file.getAbsolutePath());
        }

        return image;
    }

    public BufferedImage createPreview(BufferedImage source) {
        return createPreview(source, DEFAULT_PREVIEW_MAX_SIDE);
    }

    public BufferedImage createPreview(BufferedImage source, int maxSide) {
        int width = source.getWidth();
        int height = source.getHeight();

        int longerSide = Math.max(width, height);

        if (longerSide <= maxSide) {
            return source;
        }

        double scale = maxSide / (double) longerSide;

        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage preview = new BufferedImage(
                targetWidth,
                targetHeight,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2d = preview.createGraphics();
        g2d.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
        );
        g2d.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
        );
        g2d.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        return preview;
    }

    public void saveImage(BufferedImage image, File outputFile) throws IOException {
        String format = getExtension(outputFile.getName());

        if (format == null) {
            throw new IOException("Output file must have extension, for example .png, .jpg, .bmp, .tiff");
        }

        format = format.toLowerCase(Locale.ROOT);

        if ("webp".equals(format)) {
            throw new IOException(
                    "Saving as WebP is not supported. Please save as PNG, JPG, BMP, GIF, or TIFF."
            );
        }

        if (format.equals("jpg") || format.equals("jpeg")) {
            saveJpeg(image, outputFile, 0.95f);
            return;
        }

        boolean ok = ImageIO.write(image, format, outputFile);

        if (!ok) {
            throw new IOException("No ImageIO writer found for format: " + format);
        }
    }
    private void saveJpeg(BufferedImage image, File outputFile, float quality) throws IOException {
        BufferedImage rgbImage = removeAlpha(image);

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");

        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer found.");
        }

        ImageWriter writer = writers.next();

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile)) {
            writer.setOutput(ios);

            ImageWriteParam param = writer.getDefaultWriteParam();

            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }

            writer.write(null, new IIOImage(rgbImage, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    private BufferedImage removeAlpha(BufferedImage source) {
        BufferedImage rgb = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g2d = rgb.createGraphics();
        g2d.drawImage(source, 0, 0, null);
        g2d.dispose();

        return rgb;
    }

    private String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');

        if (dot < 0 || dot == fileName.length() - 1) {
            return null;
        }

        return fileName.substring(dot + 1);
    }
}