package com.mopl.infra.s3;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Component
@Profile("!local")
@RequiredArgsConstructor
public class AwsS3Service implements S3Service {
    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Override
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        String fileName = generateFileName(file.getOriginalFilename());
        String key = "images/" + fileName;

        try (InputStream is = file.getInputStream()) {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(is, file.getSize()));
            log.info("S3 업로드 완료: {}", key);
            return buildUrl(key);

        } catch (IOException e) {
            log.error("S3 업로드 실패", e);
            throw new MoplException(ErrorCode.S3_UPLOAD_FAILED);
        }
    }

    @Override
    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;
        try {
            String key = extractKeyFromUrl(fileUrl);
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket).key(key).build());
            log.info("S3 삭제 완료: {}", key);
        } catch (Exception e) {
            log.error("S3 삭제 실패", e);
            throw new MoplException(ErrorCode.S3_DELETE_FAILED);
        }
    }

    private String generateFileName(String originalFilename) {
        String ext = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
        return UUID.randomUUID() + ext;
    }

    private String buildUrl(String key) {
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }

    private String extractKeyFromUrl(String url) {
        String prefix = "amazonaws.com/";
        int idx = url.indexOf(prefix);
        if (idx == -1) throw new IllegalArgumentException("올바르지 않은 S3 URL: " + url);
        return url.substring(idx + prefix.length());
    }
}