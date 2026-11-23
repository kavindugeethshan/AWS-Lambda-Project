package com.example.imagegrid;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class ImageGridGenerator {

    // Read an image from the given file path
    public BufferedImage readImage(String imagePath) throws IOException {

        // Create a File object using the image path
        File file = new File(imagePath);

        // Read the image and store it in memory
        BufferedImage image = ImageIO.read(file);

        // Return the loaded image
        return image;
    }
}

// new File(imagePath) → Image file එකට reference කරන File object එකක් හදනවා.
// මෙතන image data එක තවම RAM එකට load කරන්නේ නැහැ.
// ImageIO.read(file) → File එකෙන් image data read කරලා RAM එකට load කරනවා.
// BufferedImage image → Load කරපු image එක reference කරන variable එක.
// return image → ඒ image එක method එකෙන් පිටතට ලබාදෙනවා.