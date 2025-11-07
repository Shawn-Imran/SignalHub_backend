package com.realtime.communication.shared.infrastructure.storage;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;

/**
 * S3-compatible file storage adapter using AWS S3 SDK.
 * Supports file upload, download, and presigned URL generation.
 */
@Component
public class S3FileStorageAdapter {

    private static final Logger logger = LoggerFactory.getLogger(S3FileStorageAdapter.class);

    private final AmazonS3 amazonS3Client;
    private final String bucketName;

    public S3FileStorageAdapter(AmazonS3 amazonS3Client, String storageBucketName) {
        this.amazonS3Client = amazonS3Client;
        this.bucketName = storageBucketName;
        ensureBucketExists();
    }

    private void ensureBucketExists() {
        try {
            if (!amazonS3Client.doesBucketExistV2(bucketName)) {
                amazonS3Client.createBucket(bucketName);
                logger.info("Created storage bucket: {}", bucketName);
            }
        } catch (Exception e) {
            logger.warn("Could not create bucket (may already exist): {}", bucketName);
        }
    }

    /**
     * Upload a file to storage.
     */
    public void uploadFile(String objectName, InputStream inputStream, String contentType, long size) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(size);

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, inputStream, metadata);
            amazonS3Client.putObject(putObjectRequest);

            logger.info("Uploaded file: {}", objectName);
        } catch (Exception e) {
            logger.error("Failed to upload file: {}", objectName, e);
            throw new RuntimeException("File upload failed", e);
        }
    }

    /**
     * Download a file from storage.
     */
    public InputStream downloadFile(String objectName) {
        try {
            S3Object s3Object = amazonS3Client.getObject(bucketName, objectName);
            return s3Object.getObjectContent();
        } catch (Exception e) {
            logger.error("Failed to download file: {}", objectName, e);
            throw new RuntimeException("File download failed", e);
        }
    }

    /**
     * Generate a presigned URL for file download.
     */
    public String getPresignedUrl(String objectName, int expiryMinutes) {
        try {
            Date expiration = new Date();
            long expiryTimeMillis = expiration.getTime();
            expiryTimeMillis += 1000L * 60 * expiryMinutes;
            expiration.setTime(expiryTimeMillis);

            GeneratePresignedUrlRequest generatePresignedUrlRequest =
                    new GeneratePresignedUrlRequest(bucketName, objectName)
                            .withMethod(com.amazonaws.HttpMethod.GET)
                            .withExpiration(expiration);

            URL url = amazonS3Client.generatePresignedUrl(generatePresignedUrlRequest);
            return url.toString();
        } catch (Exception e) {
            logger.error("Failed to generate presigned URL for: {}", objectName, e);
            throw new RuntimeException("Presigned URL generation failed", e);
        }
    }

    /**
     * Delete a file from storage.
     */
    public void deleteFile(String objectName) {
        try {
            amazonS3Client.deleteObject(bucketName, objectName);
            logger.info("Deleted file: {}", objectName);
        } catch (Exception e) {
            logger.error("Failed to delete file: {}", objectName, e);
            throw new RuntimeException("File deletion failed", e);
        }
    }
}
