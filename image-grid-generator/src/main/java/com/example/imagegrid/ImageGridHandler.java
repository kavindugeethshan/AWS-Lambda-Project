
package com.example.imagegrid;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.S3Event;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

/**
 * AWS Lambda Handler for generating image grids.
 *
 * Lambda Handler Configuration in AWS:
 * com.example.imagegrid.ImageGridHandler::handleRequest
 */
public class ImageGridHandler implements RequestHandler<S3Event, String> {

    @Override
    public String handleRequest(S3Event input, Context context) {

        // Get Lambda logger for CloudWatch logging.
        LambdaLogger logger = context.getLogger();
        logger.log("ImageGridHandler invoked with payload: " + input);

        // Capture all records from the incoming S3 event.
        var records = input.getRecords();

        // Check whether the event contains any records.
        if (records == null || records.isEmpty()) {
            logger.log("No records found in the S3 event.");
            return "Ignored: No records found.";
        }

        // Get the first record from the event.
        var record = records.get(0);

        // Extract S3 details from the selected record.
        var s3 = record.getS3();

        // Get the bucket name.
        var bucketName = s3.getBucket().getName();

        // Get the object key.
        var objectKey = s3.getObject().getKey();

        // Log the bucket name and object key for debugging in CloudWatch.
        logger.log("Bucket Name: " + bucketName);
        logger.log("Object Key: " + objectKey);

        // Check whether the uploaded object is manifest.json.
        // Ignore the event if it belongs to another file.
        if (!objectKey.endsWith("manifest.json")) {
            logger.log("Ignoring event: Not a manifest file.");
            return "Ignored: Not a manifest file.";
        }

        // Create an AWS S3 client to communicate with the S3 service.
        S3Client s3Client = S3Client.builder().build();

        // Prepare a request to identify the file we want to download.
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        // Execute the request and download the S3 object.
        // ResponseTransformer.toBytes() collects the downloaded content into memory.
        var manifestBytes = s3Client.getObject(
                getObjectRequest,
                software.amazon.awssdk.core.sync.ResponseTransformer.toBytes());

        // Log the successful download of the manifest file.
        logger.log("manifest.json downloaded successfully.");

        // Convert downloaded bytes into a readable UTF-8 string.
        String manifestContent = manifestBytes.asUtf8String();

        // Display manifest content in CloudWatch logs.
        logger.log("Manifest content: " + manifestContent);

        return "Manifest downloaded successfully!";
    }
}

/*
 * ============================================================
 * LEARNING NOTE: IMAGE GRID LAMBDA HANDLER PROGRESS
 * ============================================================
 *
 * STEP 1: Lambda Handler Configuration
 * - Implemented RequestHandler<S3Event, String>.
 * - Prepared the handler to receive S3 events.
 *
 * STEP 2: Receive S3 Event
 * - handleRequest() receives the incoming S3 event.
 *
 * STEP 3: Extract Event Records
 * - input.getRecords() retrieves event records.
 * - Checks whether the records list is null or empty.
 * - Stops processing if no records are available.
 * - records.get(0) selects the first record.
 *
 * STEP 4: Extract S3 Information
 * - record.getS3() retrieves S3-related details.
 *
 * STEP 5: Get Bucket Name
 * - s3.getBucket().getName() retrieves the bucket name.
 *
 * STEP 6: Get Object Key
 * - s3.getObject().getKey() retrieves the uploaded object's key.
 *
 * STEP 7: CloudWatch Logging
 * - Logs the bucket name, object key and download status.
 *
 * STEP 8: Validate Manifest
 * - Checks whether the uploaded object is manifest.json.
 * - Ignores events for other files.
 *
 * STEP 9: Create S3 Client
 * - Creates an AWS S3 client using the AWS SDK.
 *
 * STEP 10: Build GetObjectRequest
 * - Builds a request using the bucket name and object key.
 * - Identifies the manifest file to retrieve from S3.
 *
 * STEP 11: Download manifest.json
 * - s3Client.getObject() executes the download request.
 * - ResponseTransformer.toBytes() stores downloaded content as bytes.
 * - manifestBytes contains the downloaded file content in memory.
 *
 * NEXT STEP:
 * - Convert manifestBytes into a String.
 * - Read and understand the manifest.json content.
 *
 * NOTE:
 * - manifest.json is now downloaded successfully when the S3
 * request completes without an exception.
 * - The downloaded content has NOT been converted into a String.
 * - The manifest JSON has NOT been parsed yet.
 * - Multiple S3 event records are NOT handled yet.
 * - Image downloading and image processing are NOT implemented yet.
 *
 * ============================================================
 */
