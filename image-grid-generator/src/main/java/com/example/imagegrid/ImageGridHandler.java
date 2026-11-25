
package com.example.imagegrid;

import java.awt.image.BufferedImage;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

public class ImageGridHandler
        implements RequestHandler<Map<String, Object>, String> {

    @Override
    public String handleRequest(Map<String, Object> input, Context context) {

        LambdaLogger logger = context.getLogger();
        logger.log("ImageGridHandler invoked.");

        // STEP 1: Read the S3 event without S3Event deserialization.
        if (input == null || !(input.get("Records") instanceof List<?> records)
                || records.isEmpty()) {

            logger.log("Invalid event: No S3 records found.");
            return "Ignored: No S3 records found.";
        }

        Object firstRecord = records.get(0);

        if (!(firstRecord instanceof Map<?, ?> record)
                || !(record.get("s3") instanceof Map<?, ?> s3)
                || !(s3.get("bucket") instanceof Map<?, ?> bucket)
                || !(s3.get("object") instanceof Map<?, ?> object)) {

            throw new IllegalArgumentException("Invalid S3 event structure.");
        }

        String bucketName = String.valueOf(bucket.get("name"));
        String encodedKey = String.valueOf(object.get("key"));

        String objectKey = URLDecoder.decode(
                encodedKey,
                StandardCharsets.UTF_8);

        logger.log("Input bucket: " + bucketName);
        logger.log("Uploaded object: " + objectKey);

        // STEP 2: Process only manifest.json.
        if (!objectKey.equals("manifest.json")
                && !objectKey.endsWith("/manifest.json")) {

            logger.log("Ignoring event: Not manifest.json.");
            return "Ignored: Not a manifest file.";
        }

        // STEP 3: Read output bucket environment variable.
        String outputBucket = System.getenv("OUTPUT_BUCKET_NAME");

        if (outputBucket == null || outputBucket.isBlank()) {
            throw new IllegalStateException(
                    "OUTPUT_BUCKET_NAME environment variable is missing.");
        }

        // STEP 4: Prepare temporary directory.
        Path tempDirectory = Path.of("/tmp/image-grid");

        try {
            Files.createDirectories(tempDirectory);

            logger.log("Temporary directory ready: " + tempDirectory);

            // STEP 5: Create AWS clients.
            try (S3Client s3Client = S3Client.builder().build();
                    S3Presigner presigner = S3Presigner.builder().build()) {

                // STEP 6: Download manifest.json.
                GetObjectRequest manifestRequest = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .build();

                var manifestBytes = s3Client.getObject(
                        manifestRequest,
                        ResponseTransformer.toBytes());

                String manifestContent = manifestBytes.asUtf8String();

                logger.log("manifest.json downloaded successfully.");

                // STEP 7: Parse manifest.
                ObjectMapper objectMapper = new ObjectMapper();

                Manifest manifest = objectMapper.readValue(
                        manifestContent,
                        Manifest.class);

                if (manifest == null) {
                    throw new IllegalArgumentException(
                            "Manifest content is empty.");
                }

                List<String> imageNames = manifest.getImages();

                // STEP 8: Validate manifest.
                if (imageNames == null || imageNames.size() != 16) {
                    throw new IllegalArgumentException(
                            "Manifest must contain exactly 16 images.");
                }

                if (imageNames.stream().distinct().count() != imageNames.size()) {
                    throw new IllegalArgumentException(
                            "Duplicate image filenames found.");
                }

                for (String imageName : imageNames) {

                    if (imageName == null
                            || imageName.isBlank()
                            || imageName.contains("/")
                            || imageName.contains("\\")
                            || !Path.of(imageName).getFileName()
                                    .toString().equals(imageName)) {

                        throw new IllegalArgumentException(
                                "Invalid image filename: " + imageName);
                    }
                }

                logger.log("Manifest validation successful.");

                // STEP 9: Prepare image generator.
                ImageGridGenerator generator = new ImageGridGenerator();
                BufferedImage[] images = new BufferedImage[16];

                int imageIndex = 0;

                // STEP 10: Download and process all 16 images.
                for (String imageName : imageNames) {

                    GetObjectRequest imageRequest = GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(imageName)
                            .build();

                    var imageBytes = s3Client.getObject(
                            imageRequest,
                            ResponseTransformer.toBytes());

                    Path imagePath = tempDirectory.resolve(imageName);

                    Files.write(imagePath, imageBytes.asByteArray());

                    logger.log("Downloaded image: " + imageName);

                    BufferedImage originalImage = generator.readImage(imagePath.toString());

                    if (originalImage == null) {
                        throw new IllegalArgumentException(
                                "Unable to read image: " + imageName);
                    }

                    BufferedImage processedImage = generator.resizeAndCrop(originalImage);

                    if (processedImage == null) {
                        throw new IllegalStateException(
                                "Image processing failed: " + imageName);
                    }

                    images[imageIndex] = processedImage;
                    imageIndex++;

                    logger.log("Processed image: " + imageName);
                }

                logger.log("All 16 images processed successfully.");

                // STEP 11: Create 4x4 image grid.
                BufferedImage canvas = generator.createCanvas();

                generator.arrangeImages(canvas, images);

                logger.log("4x4 image grid created successfully.");

                // STEP 12: Save final JPG.
                Path outputPath = tempDirectory.resolve("output-grid.jpg");

                boolean imageSaved = ImageIO.write(
                        canvas,
                        "jpg",
                        outputPath.toFile());

                if (!imageSaved) {
                    throw new IllegalStateException(
                            "Unable to save output JPG image.");
                }

                logger.log("Final JPG saved: " + outputPath);

                // STEP 13: Generate unique output key.
                String outputKey = "grids/"
                        + UUID.randomUUID()
                        + "-output-grid.jpg";

                // STEP 14: Upload JPG to output bucket.
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(outputBucket)
                        .key(outputKey)
                        .contentType("image/jpeg")
                        .build();

                s3Client.putObject(
                        putObjectRequest,
                        RequestBody.fromFile(outputPath));

                logger.log("Output JPG uploaded to S3.");
                logger.log("Output object key: " + outputKey);

                // STEP 15: Prepare presigned URL.
                GetObjectRequest outputGetRequest = GetObjectRequest.builder()
                        .bucket(outputBucket)
                        .key(outputKey)
                        .build();

                GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(15))
                        .getObjectRequest(outputGetRequest)
                        .build();

                // STEP 16: Generate presigned URL.
                PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);

                String imageUrl = presignedRequest.url().toString();

                logger.log("Presigned URL generated successfully.");

                return "Success: Image grid generated.\n"
                        + "Output bucket: " + outputBucket + "\n"
                        + "Output key: " + outputKey + "\n"
                        + "Presigned URL (valid for 15 minutes): "
                        + imageUrl;
            }

        } catch (Exception e) {

            logger.log("Image grid generation failed: "
                    + e.getClass().getSimpleName()
                    + " - " + e.getMessage());

            throw new RuntimeException(
                    "Image grid generation failed.", e);
        }
    }
}

/*
 * ============================================================
 * LEARNING NOTE: IMAGE GRID LAMBDA HANDLER
 * ============================================================
 *
 * PROJECT TARGET:
 * 16 input images -> 4x4 image grid -> 800x800 JPG
 * -> Output S3 bucket -> Presigned URL
 *
 * STEP 1: Lambda Handler Configuration
 * - Implements RequestHandler<S3Event, String>.
 * - Receives S3 events through handleRequest().
 *
 * STEP 2: Receive and Validate S3 Event
 * - Checks whether the incoming event is null.
 * - Checks whether event records exist.
 * - Currently processes only the first event record.
 *
 * STEP 3: Extract S3 Information
 * - Gets the input bucket name.
 * - Gets the uploaded object's key.
 * - Decodes the URL-encoded S3 object key.
 *
 * STEP 4: CloudWatch Logging
 * - Logs the input bucket and object key.
 * - Logs processing progress and errors.
 *
 * STEP 5: Validate S3 Event
 * - Processes only manifest.json uploads.
 * - Ignores events belonging to other files.
 *
 * STEP 6: Read Output Bucket Configuration
 * - Reads OUTPUT_BUCKET_NAME from Lambda environment variables.
 * - Avoids hardcoding the output bucket name.
 *
 * STEP 7: Create Temporary Directory
 * - Creates /tmp/image-grid.
 * - Stores downloaded images and generated JPG locally.
 *
 * STEP 8: Create AWS Clients
 * - Creates S3Client for S3 operations.
 * - Creates S3Presigner for temporary download URLs.
 * - Uses try-with-resources to close both clients.
 *
 * STEP 9: Download manifest.json
 * - Builds a GetObjectRequest.
 * - Downloads manifest.json from the input bucket.
 *
 * STEP 10: Parse Manifest JSON
 * - Uses Jackson ObjectMapper.
 * - Converts JSON into a Manifest Java object.
 * - Retrieves filenames using manifest.getImages().
 *
 * STEP 11: Validate Manifest
 * - Requires exactly 16 image filenames.
 * - Rejects duplicate filenames.
 * - Rejects empty or unsafe filenames.
 *
 * STEP 12: Initialize Image Processing
 * - Creates one ImageGridGenerator object.
 * - Creates a BufferedImage array with 16 positions.
 *
 * STEP 13: Download Images from S3
 * - Loops through the 16 image filenames.
 * - Downloads each image from the input bucket.
 * - Saves each downloaded image inside /tmp/image-grid.
 *
 * STEP 14: Read, Resize and Crop Images
 * - Reads each downloaded image.
 * - Resizes and center-crops it to 200x200.
 * - Stores each processed image in the array.
 *
 * STEP 15: Create Image Canvas
 * - Creates an 800x800 image canvas.
 *
 * STEP 16: Generate 4x4 Image Grid
 * - Arranges all 16 processed images.
 * - Produces the final 800x800 grid.
 *
 * STEP 17: Save Final JPG
 * - Saves the generated grid as output-grid.jpg.
 * - Checks whether ImageIO successfully saved it.
 *
 * STEP 18: Create Output Object Key
 * - Generates a unique filename using UUID.
 * - Stores the output under the grids/ prefix.
 *
 * STEP 19: Upload JPG to Output S3 Bucket
 * - Creates a PutObjectRequest.
 * - Sets the content type to image/jpeg.
 * - Uploads the generated JPG file.
 *
 * STEP 20: Prepare Presigned URL Request
 * - Creates a GetObjectRequest for the output image.
 * - Sets the URL validity duration to 15 minutes.
 *
 * STEP 21: Generate Presigned URL
 * - Uses S3Presigner to create a temporary download URL.
 * - The URL allows access without making the bucket public.
 *
 * STEP 22: Return Result
 * - Returns output bucket name.
 * - Returns output object key.
 * - Returns the presigned URL.
 *
 * STEP 23: Error Handling
 * - Logs the exception type and message.
 * - Throws RuntimeException so Lambda records a failure.
 *
 * ============================================================
 * CODE REVIEW NOTES
 * ============================================================
 *
 * 1. EVENT RECORDS
 * - Currently processes only the first event record.
 * - Future improvement: Support multiple event records.
 *
 * 2. IMAGE STORAGE
 * - Downloads image bytes into memory before writing to disk.
 * - Large images may increase Lambda memory usage.
 *
 * 3. IMAGE FILE VALIDATION
 * - Filename validation is implemented.
 * - Future improvement: Validate actual file formats and file sizes.
 *
 * 4. TEMPORARY STORAGE
 * - Uses Lambda /tmp directory.
 * - Future improvement: Clean up temporary files after processing.
 *
 * 5. OUTPUT FILE
 * - Generates a unique output key for each execution.
 * - Prevents overwriting previous grid images.
 *
 * 6. PRESIGNED URL
 * - URL expires after 15 minutes.
 * - Anyone with the URL can access the image until it expires.
 *
 * 7. ERROR HANDLING
 * - Logs errors and throws RuntimeException.
 * - Lambda can identify the invocation as failed.
 *
 * 8. SECURITY
 * - Output bucket is configured through an environment variable.
 * - Input and output buckets should be separate.
 * - IAM permissions must follow least-privilege principles.
 *
 * ============================================================
 * IMPLEMENTED IN THIS HANDLER
 * ============================================================
 *
 * - S3 event handling
 * - Manifest download
 * - JSON parsing
 * - Manifest validation
 * - Downloading 16 images
 * - Reading images
 * - Resizing and center-cropping
 * - 4x4 grid generation
 * - Saving 800x800 JPG
 * - Uploading JPG to output S3 bucket
 * - Generating a 15-minute presigned URL
 *
 * NOT YET VERIFIED
 * ============================================================
 *
 * - Maven build with the updated code
 * - Real S3 downloads and uploads
 * - Lambda deployment
 * - IAM permissions
 * - Lambda environment variable configuration
 * - S3 event notification
 * - End-to-end AWS testing
 *
 * ============================================================
 */
