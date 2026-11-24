
package com.example.imagegrid;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

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

                int targetWidth = 200;
                int targetHeight = 200;

                // Get the original image dimensions
                int originalWidth = original.getWidth();
                int originalHeight = original.getHeight();

                // Calculate the resize scale
                double scale = Math.max(
                                (double) targetWidth / originalWidth,
                                (double) targetHeight / originalHeight);

                // Calculate the resized image dimensions
                int resizedWidth = (int) (originalWidth * scale);
                int resizedHeight = (int) (originalHeight * scale);

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

                // Calculate the center crop starting coordinates
                int cropX = (resizedWidth - targetWidth) / 2;
                int cropY = (resizedHeight - targetHeight) / 2;

                // Extract the center portion with the target dimensions
                BufferedImage croppedImage = resizedImage.getSubimage(
                                cropX,
                                cropY,
                                targetWidth,
                                targetHeight);

                // Return the final cropped image
                return croppedImage;
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
 * - Original image එකේ width සහ height ලබා ගැනීම.
 * - Target dimensions වලට ගැළපෙන scale එක ගණනය කිරීම.
 * - Aspect ratio එක ආරක්ෂා කරමින් image එක resize කිරීම.
 * - Resize කළ image එක ගබඩා කිරීමට අලුත් BufferedImage එකක් සෑදීම.
 * - Graphics2D භාවිතයෙන් image එක resize කර ඇඳීම.
 * - Bicubic interpolation භාවිතයෙන් resize quality එක වැඩි කිරීම.
 * - Image එකේ මැද කොටස crop කළ යුතු X සහ Y ස්ථාන ගණනය කිරීම.
 * - Target width සහ height අනුව මැද කොටස ලබා ගැනීම.
 * - අවසාන cropped image එක නැවත ලබා දීම.
 */
