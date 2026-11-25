package com.example.imagegrid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Model representing the structure of manifest.json.
 * 
 * Example JSON:
 * {
 *   "images": [
 *     "image1.jpg",
 *     "image2.jpg"
 *   ]
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Manifest {

    // Stores the image filenames from manifest.json.
    private List<String> images;

    // Default no-argument constructor required by Jackson
    public Manifest() {
    }

    public Manifest(List<String> images) {
        this.images = images;
    }

    // Getter: allows us to access the image filenames.
    public List<String> getImages() {
        return images;
    }

    // Setter: allows Jackson to populate the image filenames.
    public void setImages(List<String> images) {
        this.images = images;
    }

    @Override
    public String toString() {
        return "Manifest{" +
                "images=" + images +
                '}';
    }
}
