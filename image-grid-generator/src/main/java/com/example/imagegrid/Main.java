package com.example.imagegrid;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        ImageGridGenerator generator = new ImageGridGenerator();

        try {
            // Read the first image
            BufferedImage image = generator.readImage(
                    "input-images/4ab5c92d-779a-409f-840a-b172c7642b2a.JPG");

            // Display image dimensions
            System.out.println("Width: " + image.getWidth());
            System.out.println("Height: " + image.getHeight());

        } catch (IOException e) {
            // Display error details
            e.printStackTrace();
        }
    }
}
