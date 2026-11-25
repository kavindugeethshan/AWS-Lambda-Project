package com.example.imagegrid;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ImageGridGenerator {

    // Read an image from the given file path
    public BufferedImage readImage(String imagePath) throws IOException {

        // Create a File object using the image path
        File file = new File(imagePath);

        // Read the image and load it into memory
        BufferedImage image = ImageIO.read(file);

        // Return the loaded image
        return image;
    }

    // Resize an image while preserving its aspect ratio and crop the center
    public BufferedImage resizeAndCrop(BufferedImage original) {

        // Set fixed target dimensions
        int targetWidth = 200;
        int targetHeight = 200;

        // Get the original image dimensions
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        // Calculate the resize scale
        double scale = Math.max(
                (double) targetWidth / originalWidth,
                (double) targetHeight / originalHeight);

        // Calculate the resized image dimensions (ensuring dimensions are at least target dimensions)
        int resizedWidth = Math.max(targetWidth, (int) Math.ceil(originalWidth * scale));
        int resizedHeight = Math.max(targetHeight, (int) Math.ceil(originalHeight * scale));

        // Create a new BufferedImage with the resized dimensions
        BufferedImage resizedImage = new BufferedImage(
                resizedWidth,
                resizedHeight,
                BufferedImage.TYPE_INT_RGB);

        // Create a Graphics2D object to draw the image
        Graphics2D graphics = resizedImage.createGraphics();

        // Set high-quality interpolation for image resizing
        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // Resize and draw the original image onto the new image
        graphics.drawImage(
                original,
                0,
                0,
                resizedWidth,
                resizedHeight,
                null);

        // Release graphics resources
        graphics.dispose();

        // Calculate the center crop starting coordinates (safely clamped within raster boundaries)
        int cropX = Math.max(0, Math.min((resizedWidth - targetWidth) / 2, resizedWidth - targetWidth));
        int cropY = Math.max(0, Math.min((resizedHeight - targetHeight) / 2, resizedHeight - targetHeight));

        // Extract the center portion with the target dimensions
        BufferedImage croppedImage = resizedImage.getSubimage(
                cropX,
                cropY,
                targetWidth,
                targetHeight);

        // Return the final cropped image
        return croppedImage;
    }

    // Create a blank 800x800 canvas
    public BufferedImage createCanvas() {

        // Create a new BufferedImage with 800x800 dimensions
        BufferedImage canvas = new BufferedImage(
                800,
                800,
                BufferedImage.TYPE_INT_RGB);

        // Return the created canvas
        return canvas;
    }

    // Arrange a resized image on the canvas at the given position
    public void arrangeImage(
            BufferedImage canvas,
            BufferedImage image,
            int x,
            int y) {

        // Create a Graphics2D object to draw on the canvas
        Graphics2D graphics = canvas.createGraphics();

        // Draw the image on the canvas at the specified X and Y coordinates
        graphics.drawImage(image, x, y, null);

        // Release graphics resources
        graphics.dispose();
    }

    // Arrange 16 images in a 4x4 grid
    public void arrangeImages(BufferedImage canvas, BufferedImage[] images) {

        // Loop through 4 rows
        for (int row = 0; row < 4; row++) {

            // Loop through 4 columns
            for (int column = 0; column < 4; column++) {

                // Calculate the image position
                int x = column * 200;
                int y = row * 200;

                // Calculate the image array index
                int index = row * 4 + column;

                // Arrange the image on the canvas
                arrangeImage(canvas, images[index], x, y);
            }
        }
    }
}

/*
 * 1. readImage()
 * - ලබා දුන් file path එකෙන් File object එකක් සෑදීම.
 * - ImageIO භාවිතයෙන් image එක කියවීම.
 * - Image data එක RAM එකට BufferedImage එකක් ලෙස load කිරීම.
 * - Load කරගත් image එක නැවත ලබා දීම.
 *
 * 2. resizeAndCrop()
 * - Target image size එක 200x200 ලෙස fix කිරීම.
 * - Original image එකේ width සහ height ලබා ගැනීම.
 * - Target dimensions වලට ගැළපෙන scale එක ගණනය කිරීම.
 * - Aspect ratio එක ආරක්ෂා කරමින් image එක resize කිරීම.
 * - Resize කළ image එක ගබඩා කිරීමට අලුත් BufferedImage එකක් සෑදීම.
 * - Graphics2D භාවිතයෙන් image එක resize කර ඇඳීම.
 * - Bicubic interpolation භාවිතයෙන් resize quality එක වැඩි කිරීම.
 * - Image එකේ මැද කොටස crop කළ යුතු X සහ Y ස්ථාන ගණනය කිරීම.
 * - Target width සහ height අනුව මැද කොටස ලබා ගැනීම.
 * - අවසාන cropped image එක නැවත ලබා දීම.
 *
 * 3. createCanvas()
 * - 800x800 dimensions සහිත අලුත් BufferedImage එකක් සෑදීම.
 * - TYPE_INT_RGB භාවිතයෙන් RGB image format එකක් තෝරා ගැනීම.
 * - සෑදූ blank canvas එක නැවත ලබා දීම.
 *
 * 4. arrangeImage()
 * - Canvas එක මත draw කිරීම සඳහා Graphics2D object එකක් සෑදීම.
 * - ලබා දුන් X සහ Y coordinates අනුව image එක place කිරීම.
 * - Graphics resources release කිරීම.
 *
 * 5. arrangeImages()
 * - 4x4 grid එකක් සඳහා row 4ක් සහ column 4ක් loop කිරීම.
 * - එක් එක් image එකට අදාළ X (column * 200) සහ Y (row * 200) coordinates ගණනය කිරීම.
 * - Array index (row * 4 + column) මඟින් අදාළ image එක ලබා ගැනීම.
 * - arrangeImage() method එක මඟින් canvas එක මත අදාළ ස්ථානයේ image එක ඇඳීම.
 */
