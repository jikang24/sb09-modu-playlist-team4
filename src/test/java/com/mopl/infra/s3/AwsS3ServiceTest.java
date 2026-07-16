package com.mopl.infra.s3;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AwsS3Service 테스트")
class AwsS3ServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    private AwsS3Service awsS3Service;

    @BeforeEach
    void setUp() {
        awsS3Service = new AwsS3Service(s3Client, s3Presigner);
        ReflectionTestUtils.setField(awsS3Service, "bucket", "mopl-bucket");
        ReflectionTestUtils.setField(awsS3Service, "region", "ap-northeast-2");
    }

    private byte[] pngBytes() throws IOException {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    @Test
    @DisplayName("실패: 파일이 null이면 null을 반환한다")
    void upload_nullFile_returnsNull() {
        assertThat(awsS3Service.upload(null)).isNull();
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("실패: 빈 파일이면 null을 반환한다")
    void upload_emptyFile_returnsNull() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThat(awsS3Service.upload(emptyFile)).isNull();
    }

    @Test
    @DisplayName("실패: 허용되지 않는 확장자면 INVALID_FILE_EXTENSION 예외가 발생한다")
    void upload_disallowedExtension_throws() {
        MockMultipartFile file = new MockMultipartFile("file", "malware.exe", "application/octet-stream", "data".getBytes());

        assertThatThrownBy(() -> awsS3Service.upload(file))
                .isInstanceOf(MoplException.class)
                .extracting(e -> ((MoplException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_FILE_EXTENSION);
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("실패: 확장자가 없으면 INVALID_FILE_EXTENSION 예외가 발생한다")
    void upload_noExtension_throws() {
        MockMultipartFile file = new MockMultipartFile("file", "noext", "application/octet-stream", "data".getBytes());

        assertThatThrownBy(() -> awsS3Service.upload(file))
                .isInstanceOf(MoplException.class)
                .extracting(e -> ((MoplException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_FILE_EXTENSION);
    }

    @Test
    @DisplayName("실패: 확장자는 허용되지만 실제 이미지가 아니면 INVALID_IMAGE_FILE 예외가 발생한다")
    void upload_notActuallyAnImage_throws() {
        MockMultipartFile fakeImage = new MockMultipartFile("file", "fake.png", "image/png", "this-is-not-an-image".getBytes());

        assertThatThrownBy(() -> awsS3Service.upload(fakeImage))
                .isInstanceOf(MoplException.class)
                .extracting(e -> ((MoplException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_IMAGE_FILE);
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("성공: 유효한 PNG 이미지를 업로드하면 URL을 반환한다")
    void upload_validPng_success() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "profile.png", "image/png", pngBytes());
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String result = awsS3Service.upload(file);

        assertThat(result).startsWith("https://mopl-bucket.s3.ap-northeast-2.amazonaws.com/");
        assertThat(result).endsWith(".png");
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("실패: S3 업로드 중 오류가 발생하면 S3_UPLOAD_FAILED 예외가 발생한다")
    void upload_s3Exception_throwsUploadFailed() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "profile.jpg", "image/jpeg", pngBytes());
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("boom").build());

        assertThatThrownBy(() -> awsS3Service.upload(file))
                .isInstanceOf(MoplException.class)
                .extracting(e -> ((MoplException) e).getErrorCode())
                .isEqualTo(ErrorCode.S3_UPLOAD_FAILED);
    }

    @Test
    @DisplayName("성공: fileUrl이 null이면 아무 것도 하지 않는다")
    void delete_nullUrl_noOp() {
        awsS3Service.delete(null);
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("성공: fileUrl이 빈 문자열이면 아무 것도 하지 않는다")
    void delete_blankUrl_noOp() {
        awsS3Service.delete("   ");
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("성공: 정상적인 URL이면 key를 추출해 삭제한다")
    void delete_validUrl_deletesObject() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(DeleteObjectResponse.builder().build());

        awsS3Service.delete("https://mopl-bucket.s3.ap-northeast-2.amazonaws.com/profile/abc-123.png");

        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("실패: URL 형식이 올바르지 않으면 S3_DELETE_FAILED 예외가 발생한다")
    void delete_malformedUrl_throwsDeleteFailed() {
        assertThatThrownBy(() -> awsS3Service.delete("https://not-a-valid-s3-url.example.com/file.png"))
                .isInstanceOf(MoplException.class)
                .extracting(e -> ((MoplException) e).getErrorCode())
                .isEqualTo(ErrorCode.S3_DELETE_FAILED);
    }

    @Test
    @DisplayName("실패: S3 삭제 중 클라이언트 오류가 발생하면 S3_DELETE_FAILED 예외가 발생한다")
    void delete_s3ClientThrows_throwsDeleteFailed() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("boom").build());

        assertThatThrownBy(() -> awsS3Service.delete("https://mopl-bucket.s3.ap-northeast-2.amazonaws.com/profile/abc-123.png"))
                .isInstanceOf(MoplException.class)
                .extracting(e -> ((MoplException) e).getErrorCode())
                .isEqualTo(ErrorCode.S3_DELETE_FAILED);
    }
}
