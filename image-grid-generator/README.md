# Serverless Image Grid Generator (Java 21 + AWS Lambda)

A serverless, event-driven image processing application built with **Java 21**, **AWS Lambda**, and **Amazon S3**. The application is designed to take exactly 16 input images referenced by a `manifest.json` file in an S3 input bucket, resize and center-crop each image to 200 × 200 pixels, composite them into a single 4 × 4 grid (800 × 800 pixels JPG), upload the output image to a separate S3 output bucket, and generate a temporary presigned URL for secure access.

> [!NOTE]
> **Current Project Status:** Local image processing and grid composition are fully implemented and verified via `Main.java`. The AWS Lambda handler (`ImageGridHandler.java`) has its initial foundation completed (S3 event receiving, validation, downloading `manifest.json`, and converting it to a UTF-8 string). Full cloud pipeline features (JSON parsing, batch image download from S3, S3 upload, and presigned URL generation) are actively in progress.

---

## 📋 Table of Contents

- [1. Project Overview](#1-project-overview)
- [2. Technology Stack](#2-technology-stack)
- [3. Project Structure](#3-project-structure)
- [4. Completed Work](#4-completed-work)
  - [ImageGridGenerator.java (Core Processing Engine)](#imagegridgeneratorjava-core-processing-engine)
  - [Main.java (Local Runner & Verification)](#mainjava-local-runner--verification)
  - [ImageGridHandler.java (AWS Lambda Entry Point)](#imagegridhandlerjava-aws-lambda-entry-point)
- [5. Current Project Progress](#5-current-project-progress)
- [6. Remaining Development Tasks](#6-remaining-development-tasks)
- [7. AWS Deployment Tasks](#7-aws-deployment-tasks)
- [8. Reliability, Security, and Optimization](#8-reliability-security-and-optimization)
- [9. Final System Architecture](#9-final-system-architecture)
  - [Intended Workflow Flow](#intended-workflow-flow)
  - [Architecture Flowchart (Mermaid)](#architecture-flowchart-mermaid)
- [10. Local Development & Testing](#10-local-development--testing)
  - [Prerequisites](#prerequisites)
  - [Compiling and Running Locally](#compiling-and-running-locally)
  - [Building for AWS Lambda (Fat / Uber JAR)](#building-for-aws-lambda-fat--uber-jar)
  - [AWS Lambda Configuration Reference](#aws-lambda-configuration-reference)
- [11. Learning Progress](#11-learning-progress)

---

## 1. Project Overview

The goal of this project is to construct a scalable, serverless microservice using AWS Lambda and Amazon S3 to automate the generation of image photo grids.

### Core Functional Requirements
- **Input:** Accept exactly 16 input images stored in an Amazon S3 input bucket.
- **Manifest File:** Read and extract image filenames and their grid order from a `manifest.json` file.
- **Trigger Mechanism:** Process image grids on demand, triggered when `manifest.json` is uploaded to S3.
- **Image Processing:** Download the 16 images from S3, resize and center-crop each image to 200 × 200 pixels (preserving aspect ratio without distortion).
- **Grid Composition:** Assemble all 16 images into a 4 × 4 layout on an 800 × 800 pixel canvas.
- **Output Storage:** Save the resulting composite image as an 800 × 800 JPG file in a dedicated Amazon S3 output bucket.
- **Secure Retrieval:** Generate an AWS S3 presigned URL granting temporary, secure read access to the generated image grid.

---

## 2. Technology Stack

The project relies on modern Java standards and AWS enterprise SDK libraries:

| Technology | Role & Purpose |
| :--- | :--- |
| **Java 21 (LTS)** | Core programming language leveraging modern Java performance and runtime features |
| **Apache Maven** | Dependency management, lifecycle automation, and project build tool |
| **AWS Lambda** | Serverless compute service executing code without provisioning or managing servers |
| **Amazon S3** | Scalable object storage for input images, manifest files, and generated output grids |
| **AWS SDK for Java v2 (`s3`)** | Modern, non-blocking Java SDK for Amazon S3 object retrieval and presigned URL creation |
| **AWS Lambda Java Core (`1.2.3`)** | Core interfaces for AWS Lambda runtime (`RequestHandler`, `Context`, `LambdaLogger`) |
| **AWS Lambda Java Events (`3.14.0`)** | Predefined event models to parse S3 Event notifications (`S3Event`, `S3EventNotification`) |
| **JUnit 5 (`5.10.2`)** | Modern unit testing framework for handler and logic verification |
| **Maven Shade Plugin (`3.6.0`)** | Packages compiled classes and external dependencies into a single deployable Uber/Fat JAR |

---

## 3. Project Structure

The codebase is organized according to standard Maven conventions:

```text
image-grid-generator/
├── README.md                                          # Project documentation and progress tracking
├── pom.xml                                            # Maven project configuration and dependencies
├── .gitignore                                         # Git ignore rules for build artifacts and IDE files
├── input-images/                                      # Sample local images (16 JPGs) for local testing
├── output-grid.jpg                                    # Local test output generated by Main.java
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── example/
    │   │           └── imagegrid/
    │   │               ├── ImageGridGenerator.java    # Core image reading, resizing, cropping & grid logic
    │   │               ├── ImageGridHandler.java      # AWS Lambda entry point (RequestHandler)
    │   │               └── Main.java                  # Local development runner & test harness
    │   └── resources/                                 # Application resources and configs
    └── test/
        └── java/
            └── com/
                └── example/
                    └── imagegrid/
                        └── ImageGridHandlerTest.java  # Unit tests for the handler
```

---

## 4. Completed Work

Detailed breakdown of currently implemented and tested components:

### ImageGridGenerator.java (Core Processing Engine)
A dedicated, reusable image processing class built using standard Java 2D APIs (`BufferedImage`, `Graphics2D`, `ImageIO`):
- **`readImage(String imagePath)`**: Loads an image file from the specified file path into memory as a `BufferedImage`.
- **`resizeAndCrop(BufferedImage original)`**:
  - Calculates the optimal scale factor using `Math.max(200.0 / width, 200.0 / height)` to maintain the original aspect ratio.
  - Resizes the image using high-quality bicubic interpolation (`RenderingHints.VALUE_INTERPOLATION_BICUBIC`).
  - Calculates center crop offsets `(resizedWidth - 200) / 2` and `(resizedHeight - 200) / 2`.
  - Extracts and returns the exact 200 × 200 center-cropped `BufferedImage`.
- **`createCanvas()`**: Instantiates a blank 800 × 800 pixel `BufferedImage` with `BufferedImage.TYPE_INT_RGB`.
- **`arrangeImage(BufferedImage canvas, BufferedImage image, int x, int y)`**: Uses `Graphics2D.drawImage()` to place an image at precise pixel coordinates on the canvas and safely releases graphics resources (`graphics.dispose()`).
- **`arrangeImages(BufferedImage canvas, BufferedImage[] images)`**: Iterates across 4 rows and 4 columns, calculating pixel offsets (`x = column * 200`, `y = row * 200`) and array indices (`row * 4 + column`) to arrange all 16 images into a seamless 4 × 4 grid.

### Main.java (Local Runner & Verification)
A standalone Java application to test and verify the image grid logic on the local developer machine:
- Declares an array with the file paths of all 16 local images in `input-images/`.
- Iterates through the paths, reads each image, and performs 200 × 200 resizing and center-cropping.
- Creates an 800 × 800 canvas, arranges the 16 processed images using `arrangeImages()`.
- Writes the resulting composite grid to `output-grid.jpg` using `ImageIO.write()`.
- **Status:** Successfully executed and verified locally.

### ImageGridHandler.java (AWS Lambda Entry Point)
The serverless entry point invoked by AWS Lambda upon receiving S3 events:
- **Interface Implementation:** Implements `RequestHandler<S3Event, String>`.
- **Event Extraction:** Captures incoming records from `S3Event.getRecords()`.
- **Record Validation:** Validates whether records are `null` or empty, returning early if no records are found.
- **Single Record Handling:** Inspects the first event record (`records.get(0)`).
- **S3 Metadata Retrieval:** Extracts the bucket name (`s3.getBucket().getName()`) and object key (`s3.getObject().getKey()`).
- **CloudWatch Logging:** Utilizes `LambdaLogger` to record event details, bucket names, object keys, and status messages for debugging.
- **Object Key Filter:** Verifies that the uploaded object key ends with `manifest.json`, gracefully ignoring events triggered by other files.
- **S3 Client Initialization:** Instantiates an AWS SDK v2 client (`S3Client.builder().build()`).
- **Download Request:** Prepares a `GetObjectRequest` with the extracted bucket and object key.
- **Manifest Retrieval:** Downloads the object into memory using `ResponseTransformer.toBytes()`.
- **String Conversion:** Converts the downloaded raw bytes into a UTF-8 string via `manifestBytes.asUtf8String()`.
- **Log Verification:** Logs the manifest JSON string to CloudWatch.

> [!IMPORTANT]
> **Implementation Boundary:**
> `ImageGridHandler.java` currently stops after logging the downloaded `manifest.json` string. The following capabilities are **not yet implemented** in the handler:
> - JSON parsing of `manifest.json`.
> - Downloading the 16 image files from S3.
> - Processing images in Lambda memory or `/tmp`.
> - Uploading the composite image to the S3 output bucket.
> - Generating the S3 presigned URL.

---

## 5. Current Project Progress

Summary of completed milestones and work currently in flight:

### Completed Milestones
- [x] Local image loading and decoding (`ImageIO`).
- [x] Aspect-ratio-preserving resizing with bicubic interpolation.
- [x] Center-cropping to exact 200 × 200 pixels.
- [x] Blank canvas creation (800 × 800 pixels RGB).
- [x] 4 × 4 grid layout coordinate math and arrangement.
- [x] Local test harness (`Main.java`) producing `output-grid.jpg`.
- [x] Initial AWS Lambda handler structure implementing `RequestHandler<S3Event, String>`.
- [x] S3 event notification intake and record null/empty checks.
- [x] S3 bucket name and object key extraction.
- [x] `manifest.json` key validation filter.
- [x] AWS SDK v2 `S3Client` instantiation.
- [x] S3 object download via `ResponseTransformer.toBytes()`.
- [x] Conversion of downloaded manifest bytes to a UTF-8 String.
- [x] CloudWatch logging with `LambdaLogger`.

### In Progress
- [ ] Completing the full cloud execution pipeline inside `ImageGridHandler.java`.

---

## 6. Remaining Development Tasks

Tasks remaining to complete `ImageGridHandler.java`, listed in planned execution order:

1. [ ] **Parse `manifest.json`**: Integrate a lightweight JSON parser (such as Jackson or Gson) into the project to parse the manifest content into a structured Java object/list.
2. [ ] **Validate Manifest Contents**: Ensure `manifest.json` contains a valid list with **exactly 16 image filenames**, throwing an informative error if count or format is invalid.
3. [ ] **Batch Download Images from S3**: Loop through the 16 validated filenames and download each image file from the S3 input bucket using `S3Client`.
4. [ ] **Manage Ephemeral Storage (`/tmp`)**: Store downloaded image files in Lambda's local `/tmp` directory or buffer them directly into memory as `BufferedImage` instances.
5. [ ] **Integrate `ImageGridGenerator`**: Pass the 16 downloaded images through `ImageGridGenerator` to resize and center-crop each to 200 × 200 pixels.
6. [ ] **Assemble 800 × 800 Grid**: Render all 16 processed images onto the 800 × 800 canvas.
7. [ ] **Upload Grid to Output S3 Bucket**: Encode the canvas to JPG format in memory and upload it to a separate S3 output bucket using `s3Client.putObject()`.
8. [ ] **Generate S3 Presigned URL**: Use AWS SDK v2 `S3Presigner` to generate a secure, time-limited presigned URL (e.g., valid for 15 minutes) for the generated grid image.
9. [ ] **Return Presigned URL**: Return the generated URL string as the handler's output response.

---

## 7. AWS Deployment Tasks

Steps required to configure, deploy, and verify the application on AWS:

- [ ] **Create S3 Buckets**: Provision two separate Amazon S3 buckets:
  - Input Bucket: `image-grid-input-bucket` (for 16 images + `manifest.json`).
  - Output Bucket: `image-grid-output-bucket` (for generated 800 × 800 JPG grids).
- [ ] **Configure IAM Execution Role**: Create an IAM role for Lambda with least-privilege permissions:
  - `s3:GetObject` on the input bucket.
  - `s3:PutObject` on the output bucket.
  - `logs:CreateLogGroup`, `logs:CreateLogStream`, `logs:PutLogEvents` for CloudWatch.
- [ ] **Package Fat JAR**: Run `mvn clean package` to build the self-contained Uber JAR containing all AWS SDK dependencies.
- [ ] **Deploy Lambda Function**:
  - Runtime: `Java 21`.
  - Handler: `com.example.imagegrid.ImageGridHandler::handleRequest`.
  - Memory: Allocate 512 MB – 1024 MB.
  - Timeout: Set to 30 – 60 seconds to accommodate image downloading and processing.
- [ ] **Configure S3 Event Trigger**:
  - Set up an S3 Event Notification on the input bucket for `s3:ObjectCreated:*`.
  - **Critical Filter:** Configure the prefix or suffix filter so that **only `manifest.json`** triggers the Lambda function (preventing 16 individual image uploads from firing separate Lambda invocations).
- [ ] **Configure CloudWatch Monitoring**: Set up log retention and metric alarms for errors and execution durations.
- [ ] **End-to-End Cloud Testing**: Upload 16 images, then upload `manifest.json`, verify CloudWatch logs, confirm the grid image in the output bucket, and test accessing the presigned URL.

---

## 8. Reliability, Security, and Optimization

Planned architectural and code-level enhancements for production readiness:

- **Multiple Event Records**: Handle cases where S3 batches multiple event records in a single notification payload.
- **Strict Manifest Validation**: Guard against malformed JSON, missing files, or incorrect image counts before beginning download operations.
- **Resilient Error Handling**: Implement structured try-catch blocks to catch `NoSuchKeyException`, image decoding errors, or network timeouts, logging meaningful diagnostics to CloudWatch.
- **Idempotency & Duplicate Events**: Ensure handling is idempotent so that duplicate S3 notifications for the same manifest do not corrupt output states.
- **Resource Cleanup**:
  - Ensure `S3Client` and `S3Presigner` instances are reused or properly closed.
  - Call `Graphics2D.dispose()` and delete temporary files in `/tmp` to avoid disk exhaustion across warm container executions.
- **IAM Principle of Least Privilege**: Strictly restrict Lambda permissions so it cannot read from the output bucket or write to the input bucket.
- **Secure Image Access**: Keep the output bucket private; distribute access exclusively via short-lived Presigned URLs.
- **Performance & Cost Tuning**:
  - Profile Lambda memory allocation (evaluating 512 MB vs. 1024 MB) to find the optimal price-performance balance.
  - Stream image bytes directly through `ImageIO.read(InputStream)` to minimize disk I/O latency.

---

## 9. Final System Architecture

### Intended Workflow Flow

```text
16 Images + manifest.json
        │
        ▼
 S3 Input Bucket
        │
        ▼
 S3 Event Notification (ObjectCreated on manifest.json only)
        │
        ▼
 AWS Lambda Function (Java 21)
        │
        ├─► 1. Download & parse manifest.json
        ├─► 2. Validate 16 image filenames
        ├─► 3. Download 16 input images from S3
        ├─► 4. Process via ImageGridGenerator (Resize/Crop to 200x200)
        ├─► 5. Composite into 800x800 JPG
        │
        ▼
 S3 Output Bucket (Stores composite JPG)
        │
        ▼
 Generate Presigned URL (Returned to caller / logged)
```

### Architecture Flowchart (Mermaid)

```mermaid
flowchart TD
    subgraph S3_Input ["Amazon S3 (Input Bucket)"]
        Images["16 Input Images (image1.jpg ... image16.jpg)"]
        Manifest["manifest.json (Image manifest file)"]
    end

    subgraph Event_System ["S3 Event Notification"]
        Notification["S3 ObjectCreated Event (Triggered by manifest.json upload only)"]
    end

    subgraph Lambda ["AWS Lambda Environment"]
        Handler["ImageGridHandler.java (Lambda Entry Point)"]
        Generator["ImageGridGenerator.java (Grid Engine)"]
    end

    subgraph S3_Output ["Amazon S3 (Output Bucket)"]
        OutputImage["Generated Grid Image (800x800 JPG)"]
    end

    Client["Client / User"]
    PresignedURL["Presigned URL (Temporary Access Link)"]

    %% Step 1 & 2: Client uploads to S3
    Client -->|"1. Uploads 16 images"| Images
    Client -->|"2. Uploads manifest.json"| Manifest

    %% Step 3 & 4: S3 Notification triggers Lambda
    Manifest -->|"3. Triggers event on manifest upload"| Notification
    Notification -->|"4. Sends S3Event metadata (Bucket & Key)"| Handler

    %% Step 5 & 6: Lambda downloads manifest and images
    Handler -->|"5. Reads & parses manifest.json"| Manifest
    Handler -->|"6. Downloads 16 images"| Images

    %% Step 7 & 8: Lambda delegates grid processing
    Handler -->|"7. Passes images for processing"| Generator
    Generator -->|"8. Resizes & center-crops (200x200), arranges 4x4 grid (800x800)"| Handler

    %% Step 9, 10 & 11: Output storage and Presigned URL
    Handler -->|"9. Uploads combined 800x800 JPG"| OutputImage
    Handler -->|"10. Generates presigned URL"| PresignedURL
    PresignedURL -->|"11. Provides temporary access URL"| Client
```

---

## 10. Local Development & Testing

### Prerequisites
- **Java JDK 21** or higher.
- **Apache Maven 3.8+** (or IDE-bundled Maven).
- Sample images (16 JPG files) placed in the `input-images/` directory.

### Compiling and Running Locally
To test the image grid composition logic locally on your machine without an AWS account:

```bash
# 1. Navigate to the project root
cd image-grid-generator

# 2. Compile the project
mvn clean compile

# 3. Execute the local test harness
java -cp target/classes com.example.imagegrid.Main
```

Upon successful completion, you will see console output detailing image dimensions, canvas dimensions, and an `output-grid.jpg` file created in `image-grid-generator/`.

### Building for AWS Lambda (Fat / Uber JAR)
To package the project with all necessary dependencies into a deployment-ready Fat JAR:

```bash
cd image-grid-generator
mvn clean package
```

The compiled Fat JAR will be generated at:
```text
target/image-grid-generator-1.0-SNAPSHOT.jar
```

### AWS Lambda Configuration Reference

| Parameter | Recommended Setting | Explanation |
| :--- | :--- | :--- |
| **Runtime** | `Java 21` | Supported AWS Lambda managed Java runtime |
| **Handler** | `com.example.imagegrid.ImageGridHandler::handleRequest` | Class and method where execution begins |
| **Architecture** | `x86_64` or `arm64` | `arm64` provides enhanced cost/performance on Graviton |
| **Memory** | `512 MB – 1024 MB` | Image manipulation requires sufficient heap space |
| **Timeout** | `30 – 60 seconds` | Allows adequate time for network transfers of 16 images |

---

## 11. Learning Progress

This section tracks technical competencies mastered and those scheduled for upcoming stages:

- [x] **Java Image Processing**: Working with Java 2D APIs, `BufferedImage`, `Graphics2D`, bicubic interpolation, and center-crop coordinate calculations.
- [x] **AWS Lambda Event Handling**: Implementing `RequestHandler<S3Event, String>`, extracting records, and validating event payload integrity.
- [x] **Amazon S3 Object Retrieval**: Leveraging AWS SDK v2 `GetObjectRequest` and collecting streamed data via `ResponseTransformer.toBytes()`.
- [x] **AWS SDK for Java v2**: Configuring clients (`S3Client`), managing SDK lifecycles, and byte conversions (`asUtf8String()`).
- [x] **Local Test Automation**: Building executable test runners (`Main.java`) to validate algorithms prior to cloud deployment.
- [ ] **JSON Parsing**: Integrating and parsing structured JSON documents (`manifest.json`) in a serverless environment.
- [ ] **AWS IAM Least Privilege**: Authoring granular IAM execution policies specific to input/output bucket operations.
- [ ] **Serverless Event-Driven Architectures**: Decoupling file uploads from processing triggers using event filters.
- [ ] **CloudWatch Observability**: Utilizing structured logging, error tracing, and metric collection for serverless workloads.
- [ ] **Cloud Deployment & Live Debugging**: Deploying packaged Fat JARs, diagnosing cloud execution logs, and verifying presigned URLs.
