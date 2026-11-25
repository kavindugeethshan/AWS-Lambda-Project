
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

        // Store the paths of 16 input images
        String[] imagePaths = {
                "input-images/image1.jpg",
                "input-images/image2.jpg",
                "input-images/image3.jpg",
                "input-images/image4.jpg",
                "input-images/image5.jpg",
                "input-images/image6.jpg",
                "input-images/image7.jpg",
                "input-images/image8.jpg",
                "input-images/image9.jpg",
                "input-images/image10.jpg",
                "input-images/image11.jpg",
                "input-images/image12.jpg",
                "input-images/image13.jpg",
                "input-images/image14.jpg",
                "input-images/image15.jpg",
                "input-images/image16.jpg"
        };

        try {

            // Create an array to store 16 processed images
            BufferedImage[] images = new BufferedImage[16];

            // Loop through all 16 image paths
            for (int i = 0; i < imagePaths.length; i++) {

                // Read the original image
                BufferedImage original = generator.readImage(imagePaths[i]);

                // Resize and crop the image to 200x200
                images[i] = generator.resizeAndCrop(original);
            }

            // Create an 800x800 canvas
            BufferedImage canvas = generator.createCanvas();

            // Arrange all 16 images in a 4x4 grid
            generator.arrangeImages(canvas, images);

            // Save the final image grid as a JPG file
            ImageIO.write(canvas, "jpg", new File("output-grid.jpg"));

            // Print the final grid dimensions
            System.out.println("Grid Width: " + canvas.getWidth());
            System.out.println("Grid Height: " + canvas.getHeight());

            System.out.println("Image grid generated successfully.");

        } catch (IOException e) {

            // Print the error if image processing fails
            e.printStackTrace();
        }
    }
}
