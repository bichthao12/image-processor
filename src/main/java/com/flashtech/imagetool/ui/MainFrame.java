package com.flashtech.imagetool.ui;

import com.flashtech.imagetool.service.ImageIOService;
import com.flashtech.imagetool.service.ImageProcessingService;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

import com.flashtech.imagetool.service.PdfExportService;

public class MainFrame extends JFrame {

    private final ImageIOService imageIOService = new ImageIOService();
    private final ImageProcessingService processingService = new ImageProcessingService();

    private final ImagePanel imagePanel = new ImagePanel();

    private BufferedImage currentImage;
    private File currentFile;

    private final JLabel statusLabel = new JLabel("Ready");
    private final JProgressBar progressBar = new JProgressBar();
    private final PdfExportService pdfExportService = new PdfExportService();

    private final JComboBox<String> operationCombo = new JComboBox<>(new String[]{
            "GRAYSCALE",
            "BLACK_WHITE",
            "NEGATIVE",
            "BRIGHTNESS_CONTRAST",
            "RESIZE",
            "BLUR",
            "DENOISE",
            "SHARPEN",
            "SOBEL_EDGE"
    });

    private final JSlider thresholdSlider = new JSlider(0, 255, 128);
    private final JSlider brightnessSlider = new JSlider(-255, 255, 0);
    private final JSlider contrastSlider = new JSlider(-100, 100, 0);
    private final JSlider blurKernelSlider = new JSlider(3, 31, 5);
    private JPanel noParamBlock;
    private JPanel thresholdBlock;
    private JPanel brightnessContrastBlock;
    private JPanel blurBlock;
    private JPanel resizeBlock;

    private JButton processButton;
    private JButton cropButton;
    private JButton openButton;
    private JButton saveButton;
    private JButton resetButton;
    private JButton exportPdfButton;

    private boolean busy = false;

    private final JSpinner resizeW = new JSpinner(new SpinnerNumberModel(1000, 1, Integer.MAX_VALUE, 1));
    private final JSpinner resizeH = new JSpinner(new SpinnerNumberModel(1000, 1, Integer.MAX_VALUE, 1));
    private final JSpinner pdfWidthMm = new JSpinner(
            new SpinnerNumberModel(210.0, 1.0, 10000.0, 1.0)
    );

    private final JSpinner pdfHeightMm = new JSpinner(
            new SpinnerNumberModel(297.0, 1.0, 10000.0, 1.0)
    );

    private final JComboBox<String> pdfFitModeCombo = new JComboBox<>(new String[]{
            "Fit full page",
            "Keep ratio center"
    });
    private final JTextField imageInfoField = new JTextField();

    public MainFrame() {
        setTitle("FlashTech Java Swing Image Processing Tool");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1250, 800);
        setLocationRelativeTo(null);

        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);

        imageInfoField.setEditable(false);

        setLayout(new BorderLayout());
        add(createTopBar(), BorderLayout.NORTH);
        add(new JScrollPane(imagePanel), BorderLayout.CENTER);
        add(createRightPanel(), BorderLayout.EAST);
        add(createBottomBar(), BorderLayout.SOUTH);
    }

    private JPanel createTopBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        openButton = new JButton("Upload Image");
        saveButton = new JButton("Save As");
        resetButton = new JButton("Reload Original");

        openButton.addActionListener(e -> openImage());
        saveButton.addActionListener(e -> saveImage());
        resetButton.addActionListener(e -> reloadOriginal());

        panel.add(openButton);
        panel.add(saveButton);
        panel.add(resetButton);

        return panel;
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setPreferredSize(new Dimension(360, 0));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        setupSlider(thresholdSlider, 64);
        setupSlider(brightnessSlider, 128);
        setupSlider(contrastSlider, 50);
        setupSlider(blurKernelSlider, 4);

        imageInfoField.setEditable(false);
        setFixedHeight(imageInfoField, 28);
        setFixedHeight(operationCombo, 28);
        setFixedHeight(pdfFitModeCombo, 28);
        setFixedHeight(pdfWidthMm, 28);
        setFixedHeight(pdfHeightMm, 28);

        operationCombo.addActionListener(e -> updateOperationControls());

        form.add(createImageInfoBlock());
        form.add(Box.createVerticalStrut(8));

        form.add(createOperationBlock());
        form.add(Box.createVerticalStrut(8));

        noParamBlock = createSimpleBlock(
                "Parameters",
                new JLabel("This operation has no parameters.")
        );

        thresholdBlock = createSimpleBlock(
                "Black & White",
                new JLabel("Threshold"),
                thresholdSlider
        );

        brightnessContrastBlock = createSimpleBlock(
                "Brightness / Contrast",
                new JLabel("Brightness"),
                brightnessSlider,
                new JLabel("Contrast"),
                contrastSlider
        );

        blurBlock = createSimpleBlock(
                "Blur / Denoise",
                new JLabel("Kernel Size"),
                blurKernelSlider
        );

        resizeBlock = createSimpleBlock(
                "Resize",
                new JLabel("Resize Width (px)"),
                resizeW,
                new JLabel("Resize Height (px)"),
                resizeH
        );

        form.add(noParamBlock);
        form.add(thresholdBlock);
        form.add(brightnessContrastBlock);
        form.add(blurBlock);
        form.add(resizeBlock);
        form.add(Box.createVerticalStrut(8));
        form.add(createPdfBlock());

        JScrollPane scrollPane = new JScrollPane(form);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        processButton = new JButton("Apply");
        processButton.addActionListener(e -> applyOperation());

        cropButton = new JButton("Crop Selected");
        cropButton.setToolTipText("Kéo chuột trên ảnh để chọn vùng cần giữ, sau đó bấm nút này để crop.");
        cropButton.addActionListener(e -> cropSelectedArea());

        exportPdfButton = new JButton("Export PDF");
        exportPdfButton.addActionListener(e -> exportPdf());

        setFixedHeight(processButton, 32);
        setFixedHeight(cropButton, 32);
        setFixedHeight(exportPdfButton, 32);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        bottomPanel.add(processButton);
        bottomPanel.add(Box.createVerticalStrut(6));
        bottomPanel.add(cropButton);
        bottomPanel.add(Box.createVerticalStrut(6));
        bottomPanel.add(exportPdfButton);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        updateOperationControls();

        return panel;
    }

    private JPanel createImageInfoBlock() {
        JPanel block = new JPanel(new BorderLayout(0, 5));
        block.setBorder(BorderFactory.createTitledBorder("Image Info"));
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        block.add(imageInfoField, BorderLayout.CENTER);

        return block;
    }

    private JPanel createOperationBlock() {
        JPanel block = new JPanel(new BorderLayout(0, 5));
        block.setBorder(BorderFactory.createTitledBorder("Operation"));
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        block.add(operationCombo, BorderLayout.CENTER);

        return block;
    }

    private JPanel createPdfBlock() {
        JPanel block = new JPanel(new GridBagLayout());
        block.setBorder(BorderFactory.createTitledBorder("Export PDF"));
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        addFormRow(block, gbc, 0, "PDF Width (mm)", pdfWidthMm);
        addFormRow(block, gbc, 1, "PDF Height (mm)", pdfHeightMm);
        addFormRow(block, gbc, 2, "PDF Fit Mode", pdfFitModeCombo);

        return block;
    }

    private JPanel createSimpleBlock(String title, JComponent... components) {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBorder(BorderFactory.createTitledBorder(title));
        block.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (JComponent component : components) {
            component.setAlignmentX(Component.LEFT_ALIGNMENT);

            if (component instanceof JSlider) {
                component.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));
            } else if (component instanceof JLabel) {
                component.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            } else {
                setFixedHeight(component, 28);
            }

            block.add(component);
            block.add(Box.createVerticalStrut(4));
        }

        block.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        return block;
    }

    private void addFormRow(
            JPanel panel,
            GridBagConstraints gbc,
            int row,
            String label,
            JComponent component
    ) {
        JLabel jLabel = new JLabel(label);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(jLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(component, gbc);
    }

    private void setFixedHeight(JComponent component, int height) {
        Dimension preferred = component.getPreferredSize();
        Dimension size = new Dimension(Integer.MAX_VALUE, height);

        component.setPreferredSize(new Dimension(preferred.width, height));
        component.setMaximumSize(size);
    }

    private void setupSlider(JSlider slider, int majorTickSpacing) {
        slider.setMajorTickSpacing(majorTickSpacing);
        slider.setPaintLabels(true);
        slider.setPaintTicks(true);
        slider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));
    }


    private void updateOperationControls() {
        String operation = String.valueOf(operationCombo.getSelectedItem());

        boolean isBlackWhite = isOperation(operation, "BLACK_WHITE", "Black & White");
        boolean isBrightnessContrast = isOperation(operation, "BRIGHTNESS_CONTRAST", "Brightness / Contrast");
        boolean isBlur = isOperation(operation, "BLUR", "Blur", "DENOISE", "Denoise");
        boolean isResize = isOperation(operation, "RESIZE", "Resize");

        boolean hasParams =
                isBlackWhite
                        || isBrightnessContrast
                        || isBlur
                        || isResize;

        noParamBlock.setVisible(!hasParams);
        thresholdBlock.setVisible(isBlackWhite);
        brightnessContrastBlock.setVisible(isBrightnessContrast);
        blurBlock.setVisible(isBlur);
        resizeBlock.setVisible(isResize);

        processButton.setVisible(true);
        cropButton.setVisible(true);

        revalidate();
        repaint();
    }

    private JPanel createParamBlock(String title, JComponent... components) {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setBorder(BorderFactory.createTitledBorder(title));
        block.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (JComponent component : components) {
            component.setAlignmentX(Component.LEFT_ALIGNMENT);
            block.add(component);
        }

        return block;
    }

    private boolean isOperation(String current, String... names) {
        for (String name : names) {
            if (name.equalsIgnoreCase(current)) {
                return true;
            }
        }

        return false;
    }
    private void exportPdf() {
        if (busy) {
            return;
        }
        if (currentImage == null) {
            showWarning("Open an image first.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("output.pdf"));

        int result = chooser.showSaveDialog(this);

        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File outputFile = chooser.getSelectedFile();

        if (!outputFile.getName().toLowerCase().endsWith(".pdf")) {
            outputFile = new File(outputFile.getAbsolutePath() + ".pdf");
        }

        float widthMm = ((Number) pdfWidthMm.getValue()).floatValue();
        float heightMm = ((Number) pdfHeightMm.getValue()).floatValue();

        String fitMode = String.valueOf(pdfFitModeCombo.getSelectedItem());

        File finalOutputFile = outputFile;

        runWorker(
                "Exporting PDF...",
                () -> {
                    if ("Keep ratio center".equals(fitMode)) {
                        pdfExportService.exportImageToPdfKeepRatio(
                                currentImage,
                                finalOutputFile,
                                widthMm,
                                heightMm
                        );
                    } else {
                        pdfExportService.exportImageToPdfFitPage(
                                currentImage,
                                finalOutputFile,
                                widthMm,
                                heightMm
                        );
                    }

                    return currentImage;
                },
                image -> {
                    setStatus("PDF exported: " + finalOutputFile.getAbsolutePath());

                    JOptionPane.showMessageDialog(
                            this,
                            "PDF created successfully:\n" + finalOutputFile.getAbsolutePath(),
                            "Export PDF Completed",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                }
        );
    }

    private void cropSelectedArea() {
        if (busy) {
            return;
        }
        if (currentImage == null) {
            showWarning("Open an image first.");
            return;
        }

        Rectangle cropRect = getCropRectFromMouseSelection();

        if (cropRect == null) {
            showWarning("Please drag on the image to select crop area first.");
            return;
        }

        runWorker(
                "Cropping selected area...",
                () -> processingService.crop(
                        currentImage,
                        cropRect.x,
                        cropRect.y,
                        cropRect.width,
                        cropRect.height
                ),
                image -> {
                    currentImage = image;
                    imagePanel.clearSelection();
                    updatePreview();
                    updateImageInfo();
                    initDefaultCropAndResizeValues();
                }
        );
    }

    private Rectangle getCropRectFromMouseSelection() {
        Rectangle previewRect = imagePanel.getSelectedImageRect();

        if (previewRect == null) {
            return null;
        }

        BufferedImage previewImage = imagePanel.getImage();

        if (previewImage == null || currentImage == null) {
            return null;
        }

        double scaleX = currentImage.getWidth() / (double) previewImage.getWidth();
        double scaleY = currentImage.getHeight() / (double) previewImage.getHeight();

        int x = (int) Math.round(previewRect.x * scaleX);
        int y = (int) Math.round(previewRect.y * scaleY);
        int width = (int) Math.round(previewRect.width * scaleX);
        int height = (int) Math.round(previewRect.height * scaleY);

        x = Math.max(0, Math.min(x, currentImage.getWidth() - 1));
        y = Math.max(0, Math.min(y, currentImage.getHeight() - 1));

        width = Math.min(width, currentImage.getWidth() - x);
        height = Math.min(height, currentImage.getHeight() - y);

        if (width <= 0 || height <= 0) {
            return null;
        }

        return new Rectangle(x, y, width, height);
    }

    private JPanel createBottomBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(statusLabel, BorderLayout.CENTER);
        panel.add(progressBar, BorderLayout.EAST);
        return panel;
    }

    private void openImage() {
        if (busy) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Images",
                "png", "jpg", "jpeg", "bmp", "gif", "tif", "tiff", "webp"
        ));

        int result = chooser.showOpenDialog(this);

        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();

        runWorker(
                "Loading image...",
                () -> {
                    BufferedImage image = imageIOService.readFullImage(file);
                    return image;
                },
                image -> {
                    currentFile = file;
                    currentImage = image;
                    updatePreview();
                    updateImageInfo();
                    initDefaultCropAndResizeValues();
                }
        );
    }

    private void saveImage() {
        if (busy) {
            return;
        }
        if (currentImage == null) {
            showWarning("No image to save.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Image As");

        /*
         * Chỉ hiển thị các format có thể ghi được.
         * Không đưa WEBP vào đây vì WebP hiện chỉ đọc tốt, không ghi ổn định.
         */
        chooser.setAcceptAllFileFilterUsed(false);

        FileNameExtensionFilter pngFilter =
                new FileNameExtensionFilter("PNG Image (*.png)", "png");

        FileNameExtensionFilter jpgFilter =
                new FileNameExtensionFilter("JPEG Image (*.jpg, *.jpeg)", "jpg", "jpeg");

        FileNameExtensionFilter bmpFilter =
                new FileNameExtensionFilter("BMP Image (*.bmp)", "bmp");

        FileNameExtensionFilter gifFilter =
                new FileNameExtensionFilter("GIF Image (*.gif)", "gif");

        FileNameExtensionFilter tiffFilter =
                new FileNameExtensionFilter("TIFF Image (*.tif, *.tiff)", "tif", "tiff");

        chooser.addChoosableFileFilter(pngFilter);
        chooser.addChoosableFileFilter(jpgFilter);
        chooser.addChoosableFileFilter(bmpFilter);
        chooser.addChoosableFileFilter(gifFilter);
        chooser.addChoosableFileFilter(tiffFilter);

        chooser.setFileFilter(pngFilter);
        chooser.setSelectedFile(new File("output.png"));

        int result = chooser.showSaveDialog(this);

        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = chooser.getSelectedFile();
        String extension = getFileExtension(selectedFile);

        /*
         * Nếu user không nhập extension:
         *
         * output + PNG filter  -> output.png
         * output + JPG filter  -> output.jpg
         * output + BMP filter  -> output.bmp
         * output + GIF filter  -> output.gif
         * output + TIFF filter -> output.tiff
         */
        if (extension.isBlank()) {
            String defaultExtension = "png";

            if (chooser.getFileFilter() == jpgFilter) {
                defaultExtension = "jpg";
            } else if (chooser.getFileFilter() == bmpFilter) {
                defaultExtension = "bmp";
            } else if (chooser.getFileFilter() == gifFilter) {
                defaultExtension = "gif";
            } else if (chooser.getFileFilter() == tiffFilter) {
                defaultExtension = "tiff";
            }

            selectedFile = new File(selectedFile.getAbsolutePath() + "." + defaultExtension);
            extension = defaultExtension;
        }

        /*
         * Chặn trường hợp user tự gõ output.webp thủ công.
         */
        if ("webp".equalsIgnoreCase(extension)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Saving as WebP is not supported.\n"
                            + "Please save as PNG, JPG, BMP, GIF, or TIFF instead.",
                    "Unsupported Output Format",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        File outputFile = selectedFile;

        runWorker(
                "Saving image...",
                () -> {
                    imageIOService.saveImage(currentImage, outputFile);
                    return currentImage;
                },
                image -> setStatus("Saved: " + outputFile.getAbsolutePath())
        );
    }
    private String getFileExtension(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');

        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }

        return name.substring(dot + 1).toLowerCase();
    }

    private void reloadOriginal() {
        if (busy) {
            return;
        }
        if (currentFile == null) {
            showWarning("No original file.");
            return;
        }

        runWorker(
                "Reloading original image...",
                () -> imageIOService.readFullImage(currentFile),
                image -> {
                    currentImage = image;
                    updatePreview();
                    updateImageInfo();
                    initDefaultCropAndResizeValues();
                }
        );
    }

    private void applyOperation() {
        if (busy) {
            return;
        }
        if (currentImage == null) {
            showWarning("Open an image first.");
            return;
        }

        String operation = String.valueOf(operationCombo.getSelectedItem());

        runWorker(
                "Processing: " + operation,
                () -> process(operation),
                image -> {
                    currentImage = image;
                    updatePreview();
                    updateImageInfo();
                    initDefaultCropAndResizeValues();
                }
        );
    }

    private BufferedImage process(String operation) {
        return switch (operation) {
            case "GRAYSCALE" -> processingService.grayscale(currentImage);

            case "BLACK_WHITE" -> processingService.blackWhite(
                    currentImage,
                    thresholdSlider.getValue()
            );

            case "NEGATIVE" -> processingService.negative(currentImage);

            case "BRIGHTNESS_CONTRAST" -> processingService.brightnessContrast(
                    currentImage,
                    brightnessSlider.getValue(),
                    contrastSlider.getValue()
            );


            case "RESIZE" -> processingService.resize(
                    currentImage,
                    spinnerInt(resizeW),
                    spinnerInt(resizeH)
            );

            case "BLUR" -> processingService.blur(
                    currentImage,
                    blurKernelSlider.getValue()
            );
            case "DENOISE" -> processingService.medianDenoise(
                    currentImage,
                    blurKernelSlider.getValue()
            );
            case "SHARPEN" -> processingService.sharpen(currentImage);

            case "SOBEL_EDGE" -> processingService.sobelEdge(currentImage);

            default -> throw new IllegalArgumentException("Unsupported operation: " + operation);
        };
    }

    private void updatePreview() {
        BufferedImage preview = imageIOService.createPreview(currentImage);
        imagePanel.setImage(preview);
    }

    private void updateImageInfo() {
        if (currentImage == null) {
            imageInfoField.setText("");
            return;
        }

        imageInfoField.setText(
                currentImage.getWidth() + " x " + currentImage.getHeight()
        );
    }

    private void initDefaultCropAndResizeValues() {
        if (currentImage == null) {
            return;
        }

        int width = currentImage.getWidth();
        int height = currentImage.getHeight();

        resizeW.setValue(width);
        resizeH.setValue(height);
    }
    private int spinnerInt(JSpinner spinner) {
        return ((Number) spinner.getValue()).intValue();
    }

    private <T> void runWorker(
            String startMessage,
            WorkerTask<T> task,
            WorkerSuccess<T> success
    ) {
        setBusy(true);
        setStatus(startMessage);

        SwingWorker<T, Void> worker = new SwingWorker<>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.run();
            }

            @Override
            protected void done() {
                try {
                    T result = get();

                    String statusBeforeSuccessCallback = statusLabel.getText();

                    success.accept(result);

                    /*
                     * Nếu success callback không tự setStatus(),
                     * thì mới set mặc định là Done.
                     *
                     * Nếu callback đã set:
                     * - Saved: C:\...
                     * - PDF exported: C:\...
                     * thì không ghi đè nữa.
                     */
                    if (statusLabel.getText().equals(statusBeforeSuccessCallback)) {
                        setStatus("Done");
                    }

                } catch (OutOfMemoryError e) {
                    showError("Not enough memory. Try running with -Xmx4g or reduce image size.");
                    setStatus("Out of memory");
                } catch (Exception e) {
                    showError(e.getMessage());
                    setStatus("Failed");
                } finally {
                    setBusy(false);
                }
            }
        };

        worker.execute();
    }

    private void setBusy(boolean busy) {
        this.busy = busy;

        progressBar.setVisible(busy);
        progressBar.setIndeterminate(busy);

        setOperationControlsEnabled(!busy);
    }

    private void setOperationControlsEnabled(boolean enabled) {
        setEnabledIfNotNull(openButton, enabled);
        setEnabledIfNotNull(saveButton, enabled);
        setEnabledIfNotNull(resetButton, enabled);

        setEnabledIfNotNull(processButton, enabled);
        setEnabledIfNotNull(cropButton, enabled);
        setEnabledIfNotNull(exportPdfButton, enabled);

        setEnabledIfNotNull(operationCombo, enabled);

        setEnabledIfNotNull(thresholdSlider, enabled);
        setEnabledIfNotNull(brightnessSlider, enabled);
        setEnabledIfNotNull(contrastSlider, enabled);
        setEnabledIfNotNull(blurKernelSlider, enabled);

        setEnabledIfNotNull(resizeW, enabled);
        setEnabledIfNotNull(resizeH, enabled);

        setEnabledIfNotNull(pdfWidthMm, enabled);
        setEnabledIfNotNull(pdfHeightMm, enabled);
        setEnabledIfNotNull(pdfFitModeCombo, enabled);
    }

    private void setEnabledIfNotNull(JComponent component, boolean enabled) {
        if (component != null) {
            component.setEnabled(enabled);
        }
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    @FunctionalInterface
    private interface WorkerTask<T> {
        T run() throws Exception;
    }

    @FunctionalInterface
    private interface WorkerSuccess<T> {
        void accept(T result);
    }
}