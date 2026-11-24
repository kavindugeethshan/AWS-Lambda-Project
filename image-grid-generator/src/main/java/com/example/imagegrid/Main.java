
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
                "input-images/15a98e80-266a-4ef8-b942-d00c379b50d3.JPG",
                "input-images/4ab5c92d-779a-409f-840a-b172c7642b2a.JPG",
                "input-images/61c28817-29d8-4436-befe-877ca05cc9c1.JPG",
                "input-images/77523128-f01d-4232-a8f9-1dedd6180b8c.JPG",
                "input-images/8b886f25-0896-4887-a7f1-fa7774f4a2eb.JPG",
                "input-images/8cf0313c-b42a-4b7f-87c3-dd865855f2f5.JPG",
                "input-images/9f86dcfe-2239-48ba-89fb-43d5a3c24f55.JPG",
                "input-images/IMG_1219.JPG",
                "input-images/IMG_2232.JPG",
                "input-images/IMG_2233.JPG",
                "input-images/IMG_6130.JPG",
                "input-images/IMG_6202.JPG",
                "input-images/a0c5d7ec-d2aa-40fd-8d32-f5b7e8fd216d.JPG",
                "input-images/a5a6dc10-fc98-4c43-b76f-acfdbe872525.JPG",
                "input-images/d1e4adb4-578b-4485-8077-603daaaab4f7.JPG",
                "input-images/fdbd2661-23ec-445b-ade3-5707b7f0af65.JPG"
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
