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
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class AwsS3Service implements S3Service {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");
    private static final Duration PRESIGN_DURATION = Duration.ofHours(2);


    @Override
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        String key = generateFileName(file.getOriginalFilename());
        validateImageContent(file);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(resolveSafeContentType(key))
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("S3 업로드 완료: {}", key);
            return buildUrl(key);

        } catch (IOException | S3Exception e) {
            log.error("S3 업로드 실패", e);
            throw new MoplException(ErrorCode.S3_UPLOAD_FAILED);
        }
    }

    @Override
    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;
        try {
            String key = extractKey(fileUrl);
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket).key(key).build());
            log.info("S3 삭제 완료: {}", key);
        } catch (Exception e) {
            log.error("S3 삭제 실패", e);
            throw new MoplException(ErrorCode.S3_DELETE_FAILED);
        }
    }

    @Override
    public String extractKey(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return fileUrl;
        String prefix = "amazonaws.com/";
        int idx = fileUrl.indexOf(prefix);
        if (idx == -1) return fileUrl;
        return fileUrl.substring(idx + prefix.length());
    }

    @Override
    public String getPresignedUrl(String key) {
        if (key == null || key.isBlank()) return key;
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(PRESIGN_DURATION)
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build())
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    // 허용 확장자 검증 후 충돌 방지용 UUID 파일명 반환
    private String generateFileName(String originalFilename) {
        String ext = extractExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
            throw new MoplException(ErrorCode.INVALID_FILE_EXTENSION);
        }
        return UUID.randomUUID() + "." + ext;
    }

    // 파일명에서 확장자 추출
    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new MoplException(ErrorCode.INVALID_FILE_EXTENSION);
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1);
    }

    //파일의 공개 URL 생성
    private String buildUrl(String key) {
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }

    // ImageIO로 실제 바이트 디코딩 시도 — null이면 이미지가 아닌 파일
    private void validateImageContent(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                throw new MoplException(ErrorCode.INVALID_IMAGE_FILE);
            }
        } catch (IOException e) {
            throw new MoplException(ErrorCode.INVALID_IMAGE_FILE);
        }
    }

    // 확장자 기반 Content-Type 재계산
    private String resolveSafeContentType(String key) {
        String ext = key.substring(key.lastIndexOf('.') + 1).toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            default -> throw new MoplException(ErrorCode.INVALID_FILE_EXTENSION);
        };
    }
}