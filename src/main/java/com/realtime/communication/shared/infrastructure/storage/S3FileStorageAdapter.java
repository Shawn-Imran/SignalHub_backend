package com.realtime.communication.shared.infrastructure.storage;

import io.minio.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * S3-compatible file storage adapter using MinIO client.
 * Supports file upload, download, and presigned URL generation.
 */
@Component
public class S3FileStorageAdapter {

    private static final Logger logger = LoggerFactory.getLogger(S3FileStorageAdapter.class);

    private final MinioClient minioClient;
    private final String bucketName;

    public S3FileStorageAdapter(MinioClient minioClient, String storageBucketName) {
        this.minioClient = minioClient;
        this.bucketName = storageBucketName;
        ensureBucketExists();
    }

    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );
                logger.info("Created storage bucket: {}", bucketName);
            }
        } catch (Exception e) {
            logger.error("Failed to ensure bucket exists: {}", bucketName, e);
        }
    }

    /**
     * Upload a file to storage.
     */
    public void uploadFile(String objectName, InputStream inputStream, String contentType, long size) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );
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
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
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
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(expiryMinutes, TimeUnit.MINUTES)
                            .build()
            );
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
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            logger.info("Deleted file: {}", objectName);
        } catch (Exception e) {
            logger.error("Failed to delete file: {}", objectName, e);
            throw new RuntimeException("File deletion failed", e);
        }
    }
}

