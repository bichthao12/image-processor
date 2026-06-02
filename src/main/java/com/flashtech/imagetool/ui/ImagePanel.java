package com.flashtech.imagetool.ui;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class ImagePanel extends JPanel {

    private BufferedImage image;

    private Rectangle imageDrawRect;
    private Point dragStart;
    private Point dragEnd;
    private Rectangle selectionRect;

    public ImagePanel() {
        setBackground(new Color(35, 35, 35));
        setPreferredSize(new Dimension(900, 650));

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (image == null || imageDrawRect == null) {
                    return;
                }

                if (!imageDrawRect.contains(e.getPoint())) {
                    clearSelection();
                    return;
                }

                dragStart = clampPointToImage(e.getPoint());
                dragEnd = dragStart;
                updateSelection();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStart == null) {
                    return;
                }

                dragEnd = clampPointToImage(e.getPoint());
                updateSelection();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragStart == null) {
                    return;
                }

                dragEnd = clampPointToImage(e.getPoint());
                updateSelection();

                dragStart = null;
                dragEnd = null;
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        clearSelection();
        repaint();
    }

    public BufferedImage getImage() {
        return image;
    }

    public void clearSelection() {
        selectionRect = null;
        dragStart = null;
        dragEnd = null;
        repaint();
    }

    /**
     * Trả về vùng crop theo tọa độ pixel của ảnh đang hiển thị trong panel.
     * Lưu ý: ảnh đang hiển thị thường là preview image, chưa phải ảnh gốc full-size.
     */
    public Rectangle getSelectedImageRect() {
        if (image == null || imageDrawRect == null || selectionRect == null) {
            return null;
        }

        if (selectionRect.width <= 2 || selectionRect.height <= 2) {
            return null;
        }

        int viewX = selectionRect.x - imageDrawRect.x;
        int viewY = selectionRect.y - imageDrawRect.y;

        double scaleX = image.getWidth() / (double) imageDrawRect.width;
        double scaleY = image.getHeight() / (double) imageDrawRect.height;

        int imageX = (int) Math.round(viewX * scaleX);
        int imageY = (int) Math.round(viewY * scaleY);
        int imageW = (int) Math.round(selectionRect.width * scaleX);
        int imageH = (int) Math.round(selectionRect.height * scaleY);

        imageX = clamp(imageX, 0, image.getWidth() - 1);
        imageY = clamp(imageY, 0, image.getHeight() - 1);

        imageW = Math.min(imageW, image.getWidth() - imageX);
        imageH = Math.min(imageH, image.getHeight() - imageY);

        if (imageW <= 0 || imageH <= 0) {
            return null;
        }

        return new Rectangle(imageX, imageY, imageW, imageH);
    }

    private void updateSelection() {
        if (dragStart == null || dragEnd == null) {
            selectionRect = null;
            repaint();
            return;
        }

        int x = Math.min(dragStart.x, dragEnd.x);
        int y = Math.min(dragStart.y, dragEnd.y);
        int w = Math.abs(dragEnd.x - dragStart.x);
        int h = Math.abs(dragEnd.y - dragStart.y);

        selectionRect = new Rectangle(x, y, w, h);
        repaint();
    }

    private Point clampPointToImage(Point point) {
        int x = clamp(point.x, imageDrawRect.x, imageDrawRect.x + imageDrawRect.width);
        int y = clamp(point.y, imageDrawRect.y, imageDrawRect.y + imageDrawRect.height);

        return new Point(x, y);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (image == null) {
            drawEmptyText(g);
            return;
        }

        imageDrawRect = calculateImageDrawRect();

        Graphics2D g2d = (Graphics2D) g.create();

        g2d.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
        );

        g2d.drawImage(
                image,
                imageDrawRect.x,
                imageDrawRect.y,
                imageDrawRect.width,
                imageDrawRect.height,
                null
        );

        drawSelection(g2d);

        g2d.dispose();
    }

    private Rectangle calculateImageDrawRect() {
        int panelWidth = getWidth();
        int panelHeight = getHeight();

        int imgWidth = image.getWidth();
        int imgHeight = image.getHeight();

        double scale = Math.min(
                panelWidth / (double) imgWidth,
                panelHeight / (double) imgHeight
        );

        int drawWidth = Math.max(1, (int) (imgWidth * scale));
        int drawHeight = Math.max(1, (int) (imgHeight * scale));

        int x = (panelWidth - drawWidth) / 2;
        int y = (panelHeight - drawHeight) / 2;

        return new Rectangle(x, y, drawWidth, drawHeight);
    }

    private void drawSelection(Graphics2D g2d) {
        if (selectionRect == null) {
            return;
        }

        g2d.setColor(new Color(0, 120, 215, 80));
        g2d.fill(selectionRect);

        g2d.setColor(new Color(0, 120, 215));
        g2d.setStroke(new BasicStroke(2f));
        g2d.draw(selectionRect);
    }

    private void drawEmptyText(Graphics g) {
        g.setColor(Color.LIGHT_GRAY);
        g.drawString("Preview", 30, 30);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}