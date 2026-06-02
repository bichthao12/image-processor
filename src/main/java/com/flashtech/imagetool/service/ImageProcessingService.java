package com.flashtech.imagetool.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.DataBufferInt;
import java.awt.image.Kernel;
import java.util.Arrays;

public class ImageProcessingService {

    public BufferedImage grayscale(BufferedImage source) {
        BufferedImage src = toArgb(source);
        BufferedImage output = newArgb(src.getWidth(), src.getHeight());

        int[] input = pixels(src);
        int[] out = pixels(output);

        for (int i = 0; i < input.length; i++) {
            int argb = input[i];

            int a = (argb >>> 24) & 0xff;
            int r = (argb >>> 16) & 0xff;
            int g = (argb >>> 8) & 0xff;
            int b = argb & 0xff;

            int gray = (77 * r + 150 * g + 29 * b) >> 8;

            out[i] = (a << 24) | (gray << 16) | (gray << 8) | gray;
        }

        return output;
    }

    public BufferedImage blackWhite(BufferedImage source, int threshold) {
        BufferedImage src = toArgb(source);
        BufferedImage output = newArgb(src.getWidth(), src.getHeight());

        int[] input = pixels(src);
        int[] out = pixels(output);

        threshold = clamp(threshold, 0, 255);

        for (int i = 0; i < input.length; i++) {
            int argb = input[i];

            int a = (argb >>> 24) & 0xff;
            int r = (argb >>> 16) & 0xff;
            int g = (argb >>> 8) & 0xff;
            int b = argb & 0xff;

            int gray = (77 * r + 150 * g + 29 * b) >> 8;
            int value = gray >= threshold ? 255 : 0;

            out[i] = (a << 24) | (value << 16) | (value << 8) | value;
        }

        return output;
    }

    public BufferedImage negative(BufferedImage source) {
        BufferedImage src = toArgb(source);
        BufferedImage output = newArgb(src.getWidth(), src.getHeight());

        int[] input = pixels(src);
        int[] out = pixels(output);

        for (int i = 0; i < input.length; i++) {
            int argb = input[i];

            int a = (argb >>> 24) & 0xff;
            int r = 255 - ((argb >>> 16) & 0xff);
            int g = 255 - ((argb >>> 8) & 0xff);
            int b = 255 - (argb & 0xff);

            out[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }

        return output;
    }

    public BufferedImage brightnessContrast(
            BufferedImage source,
            int brightness,
            int contrast
    ) {
        BufferedImage src = toArgb(source);
        BufferedImage output = newArgb(src.getWidth(), src.getHeight());

        int[] input = pixels(src);
        int[] out = pixels(output);

        brightness = clamp(brightness, -255, 255);
        contrast = clamp(contrast, -100, 100);

        double c = contrast * 2.55;
        double factor = (259.0 * (c + 255.0)) / (255.0 * (259.0 - c));

        for (int i = 0; i < input.length; i++) {
            int argb = input[i];

            int a = (argb >>> 24) & 0xff;

            int r = (argb >>> 16) & 0xff;
            int g = (argb >>> 8) & 0xff;
            int b = argb & 0xff;

            r = clamp((int) Math.round(factor * (r - 128) + 128 + brightness), 0, 255);
            g = clamp((int) Math.round(factor * (g - 128) + 128 + brightness), 0, 255);
            b = clamp((int) Math.round(factor * (b - 128) + 128 + brightness), 0, 255);

            out[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }

        return output;
    }

    public BufferedImage resize(BufferedImage source, int targetWidth, int targetHeight) {
        if (targetWidth <= 0 || targetHeight <= 0) {
            throw new IllegalArgumentException("Resize width and height must be greater than 0.");
        }

        BufferedImage output = new BufferedImage(
                targetWidth,
                targetHeight,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2d = output.createGraphics();
        g2d.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC
        );
        g2d.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
        );
        g2d.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        return output;
    }

    public BufferedImage crop(
            BufferedImage source,
            int x,
            int y,
            int width,
            int height
    ) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Crop width and height must be greater than 0.");
        }

        x = clamp(x, 0, source.getWidth() - 1);
        y = clamp(y, 0, source.getHeight() - 1);

        width = Math.min(width, source.getWidth() - x);
        height = Math.min(height, source.getHeight() - y);

        BufferedImage croppedView = source.getSubimage(x, y, width, height);

        BufferedImage output = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2d = output.createGraphics();
        g2d.drawImage(croppedView, 0, 0, null);
        g2d.dispose();

        return output;
    }

    public BufferedImage blur(BufferedImage source, int kernelSize) {
        kernelSize = normalizeOddKernel(kernelSize);

        float value = 1.0f / (kernelSize * kernelSize);
        float[] kernel = new float[kernelSize * kernelSize];

        for (int i = 0; i < kernel.length; i++) {
            kernel[i] = value;
        }

        ConvolveOp op = new ConvolveOp(
                new Kernel(kernelSize, kernelSize, kernel),
                ConvolveOp.EDGE_NO_OP,
                null
        );

        return op.filter(toArgb(source), null);
    }

    public BufferedImage sharpen(BufferedImage source) {
        /*
         * Sharpen nhẹ bằng Unsharp Mask.
         * Không dùng kernel sharpen quá gắt để tránh làm nhiễu rõ hơn.
         */
        return unsharpMask(source, 0.5f);
    }

    private BufferedImage unsharpMask(BufferedImage source, float amount) {
        BufferedImage original = toArgb(source);
        BufferedImage blurred = gaussianBlur3x3(original);

        int width = original.getWidth();
        int height = original.getHeight();

        BufferedImage output = newArgb(width, height);

        int[] originalPixels = pixels(original);
        int[] blurredPixels = pixels(blurred);
        int[] outputPixels = pixels(output);

        amount = Math.max(0.0f, Math.min(amount, 2.0f));

        for (int i = 0; i < originalPixels.length; i++) {
            int originalArgb = originalPixels[i];
            int blurredArgb = blurredPixels[i];

            int a = (originalArgb >>> 24) & 0xff;

            int or = (originalArgb >>> 16) & 0xff;
            int og = (originalArgb >>> 8) & 0xff;
            int ob = originalArgb & 0xff;

            int br = (blurredArgb >>> 16) & 0xff;
            int bg = (blurredArgb >>> 8) & 0xff;
            int bb = blurredArgb & 0xff;

            int r = clamp((int) (or + amount * (or - br)), 0, 255);
            int g = clamp((int) (og + amount * (og - bg)), 0, 255);
            int b = clamp((int) (ob + amount * (ob - bb)), 0, 255);

            outputPixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }

        return output;
    }

    private BufferedImage gaussianBlur3x3(BufferedImage source) {
        float[] kernel = {
                1f / 16f, 2f / 16f, 1f / 16f,
                2f / 16f, 4f / 16f, 2f / 16f,
                1f / 16f, 2f / 16f, 1f / 16f
        };

        ConvolveOp op = new ConvolveOp(
                new Kernel(3, 3, kernel),
                ConvolveOp.EDGE_NO_OP,
                null
        );

        return op.filter(toArgb(source), null);
    }

    public BufferedImage sobelEdge(BufferedImage source) {
        BufferedImage src = toArgb(source);

        int width = src.getWidth();
        int height = src.getHeight();

        BufferedImage output = newArgb(width, height);

        int[] input = pixels(src);
        int[] out = pixels(output);

        for (int y = 1; y < height - 1; y++) {
            int row = y * width;

            for (int x = 1; x < width - 1; x++) {
                int p00 = gray(input[row - width + x - 1]);
                int p01 = gray(input[row - width + x]);
                int p02 = gray(input[row - width + x + 1]);

                int p10 = gray(input[row + x - 1]);
                int p12 = gray(input[row + x + 1]);

                int p20 = gray(input[row + width + x - 1]);
                int p21 = gray(input[row + width + x]);
                int p22 = gray(input[row + width + x + 1]);

                int gx =
                        -p00 + p02
                                - 2 * p10 + 2 * p12
                                - p20 + p22;

                int gy =
                        -p00 - 2 * p01 - p02
                                + p20 + 2 * p21 + p22;

                int magnitude = clamp((int) Math.sqrt(gx * gx + gy * gy), 0, 255);

                out[row + x] =
                        (255 << 24)
                                | (magnitude << 16)
                                | (magnitude << 8)
                                | magnitude;
            }
        }

        return output;
    }

    private int gray(int argb) {
        int r = (argb >>> 16) & 0xff;
        int g = (argb >>> 8) & 0xff;
        int b = argb & 0xff;

        return (77 * r + 150 * g + 29 * b) >> 8;
    }

    private BufferedImage toArgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_ARGB) {
            return source;
        }

        BufferedImage converted = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2d = converted.createGraphics();
        g2d.drawImage(source, 0, 0, null);
        g2d.dispose();

        return converted;
    }

    private BufferedImage newArgb(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    private int[] pixels(BufferedImage image) {
        if (image.getType() != BufferedImage.TYPE_INT_ARGB) {
            throw new IllegalArgumentException(
                    "pixels() requires BufferedImage.TYPE_INT_ARGB. "
                            + "Call toArgb(image) before accessing raw pixels. Actual type: "
                            + image.getType()
            );
        }

        return ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
    }

    private int normalizeOddKernel(int kernelSize) {
        if (kernelSize < 3) {
            return 3;
        }

        if (kernelSize % 2 == 0) {
            kernelSize++;
        }

        return Math.min(kernelSize, 31);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public BufferedImage medianDenoise(BufferedImage source, int kernelSize) {
        BufferedImage src = toArgb(source);

        kernelSize = normalizeOddKernel(kernelSize);

        // Median filter lớn quá sẽ rất chậm với ảnh lớn
        if (kernelSize > 7) {
            kernelSize = 7;
        }

        int radius = kernelSize / 2;

        int width = src.getWidth();
        int height = src.getHeight();

        BufferedImage output = newArgb(width, height);

        int[] input = pixels(src);
        int[] out = pixels(output);

        int windowSize = kernelSize * kernelSize;

        int[] redValues = new int[windowSize];
        int[] greenValues = new int[windowSize];
        int[] blueValues = new int[windowSize];

        for (int y = 0; y < height; y++) {
            int row = y * width;

            for (int x = 0; x < width; x++) {
                int count = 0;

                for (int ky = -radius; ky <= radius; ky++) {
                    int sampleY = clamp(y + ky, 0, height - 1);

                    for (int kx = -radius; kx <= radius; kx++) {
                        int sampleX = clamp(x + kx, 0, width - 1);

                        int argb = input[sampleY * width + sampleX];

                        redValues[count] = (argb >>> 16) & 0xff;
                        greenValues[count] = (argb >>> 8) & 0xff;
                        blueValues[count] = argb & 0xff;

                        count++;
                    }
                }

                Arrays.sort(redValues, 0, count);
                Arrays.sort(greenValues, 0, count);
                Arrays.sort(blueValues, 0, count);

                int originalArgb = input[row + x];
                int a = (originalArgb >>> 24) & 0xff;

                int medianIndex = count / 2;

                int r = redValues[medianIndex];
                int g = greenValues[medianIndex];
                int b = blueValues[medianIndex];

                out[row + x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }

        return output;
    }
}