
package com.example.imagegrid;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Main {

    public static void main(String[] args) {

        System.out.println("Image Grid Generator local runner started.");

        // Create an ImageGridGenerator object
        ImageGridGenerator generator = new ImageGridGenerator();

        try {

            // Read the original image
            BufferedImage originalImage = generator.readImage(
                    "input-images/4ab5c92d-779a-409f-840a-b172c7642b2a.JPG");

            // Resize and crop the image to 200x200
            BufferedImage croppedImage = generator.resizeAndCrop(originalImage);

            // Save the cropped image as a JPG file
            ImageIO.write(croppedImage, "jpg", new File("test-output.jpg"));

            // Print the final image dimensions
            System.out.println("Final Width: " + croppedImage.getWidth());
            System.out.println("Final Height: " + croppedImage.getHeight());

        } catch (IOException e) {

            // Print the error if image processing fails
            e.printStackTrace();
        }
    }
}
