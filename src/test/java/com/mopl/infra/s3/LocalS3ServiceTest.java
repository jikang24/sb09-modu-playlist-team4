package com.mopl.infra.s3;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LocalS3Service 테스트")
class LocalS3ServiceTest {

    private static final Path UPLOAD_DIR = Paths.get(System.getProperty("user.dir"), "uploads");

    private LocalS3Service localS3Service;

    @BeforeEach
    void setUp() {
        localS3Service = new LocalS3Service();
    }

    @AfterEach
    void cleanUp() throws IOException {
        if (Files.exists(UPLOAD_DIR)) {
            try (Stream<Path> files = Files.list(UPLOAD_DIR)) {
                files.forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
            }
        }
    }

    @Test
    @DisplayName("실패: 파일이 null이면 null을 반환한다")
    void upload_nullFile_returnsNull() {
        assertThat(localS3Service.upload(null)).isNull();
    }

    @Test
    @DisplayName("실패: 빈 파일이면 null을 반환한다")
    void upload_emptyFile_returnsNull() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThat(localS3Service.upload(emptyFile)).isNull();
    }

    private byte[] pngBytes() throws IOException {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    @Test
    @DisplayName("성공: 파일을 업로드하면 uploads 디렉터리에 저장되고 경로를 반환한다")
    void upload_success_savesFileAndReturnsPath() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "profile.png", "image/png", pngBytes());

        String result = localS3Service.upload(file);

        assertThat(result).startsWith("/uploads/").endsWith(".png");
        String fileName = result.substring("/uploads/".length());
        assertThat(Files.exists(UPLOAD_DIR.resolve(fileName))).isTrue();
    }

    @Test
    @DisplayName("실패: 확장자가 없으면 INVALID_FILE_EXTENSION 예외가 발생한다")
    void upload_noExtension_throws() {
        MockMultipartFile file = new MockMultipartFile("file", "noext", "application/octet-stream", "data".getBytes());

        assertThatThrownBy(() -> localS3Service.upload(file))
                .isInstanceOf(MoplException.class)
                .extracting(e -> ((MoplException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_FILE_EXTENSION);
    }

    @Test
    @DisplayName("실패: 허용되지 않는 확장자면 INVALID_FILE_EXTENSION 예외가 발생한다")
    void upload_disallowedExtension_throws() {
        MockMultipartFile file = new MockMultipartFile("file", "malware.exe", "application/octet-stream", "data".getBytes());

        assertThatThrownBy(() -> localS3Service.upload(file))
                .isInstanceOf(MoplException.class)
                .extracting(e -> ((MoplException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_FILE_EXTENSION);
    }

    @Test
    @DisplayName("실패: 확장자는 허용되지만 실제 이미지가 아니면 INVALID_IMAGE_FILE 예외가 발생한다")
    void upload_notActuallyAnImage_throws() {
        MockMultipartFile fakeImage = new MockMultipartFile("file", "fake.png", "image/png", "this-is-not-an-image".getBytes());

        assertThatThrownBy(() -> localS3Service.upload(fakeImage))
                .isInstanceOf(MoplException.class)
                .extracting(e -> ((MoplException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_IMAGE_FILE);
    }

    @Test
    @DisplayName("성공: 매 업로드마다 고유한 파일명이 생성된다")
    void upload_generatesUniqueFileNames() throws IOException {
        MockMultipartFile file1 = new MockMultipartFile("file", "a.png", "image/png", pngBytes());
        MockMultipartFile file2 = new MockMultipartFile("file", "a.png", "image/png", pngBytes());

        String result1 = localS3Service.upload(file1);
        String result2 = localS3Service.upload(file2);

        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    @DisplayName("성공: fileUrl이 null이면 아무 것도 하지 않는다")
    void delete_nullUrl_noOp() {
        localS3Service.delete(null);
    }

    @Test
    @DisplayName("성공: fileUrl이 빈 문자열이면 아무 것도 하지 않는다")
    void delete_blankUrl_noOp() {
        localS3Service.delete("  ");
    }

    @Test
    @DisplayName("성공: 존재하는 파일을 삭제한다")
    void delete_existingFile_removesIt() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "to-delete.png", "image/png", pngBytes());
        String uploadedPath = localS3Service.upload(file);
        String fileName = uploadedPath.substring("/uploads/".length());
        assertThat(Files.exists(UPLOAD_DIR.resolve(fileName))).isTrue();

        localS3Service.delete(uploadedPath);

        assertThat(Files.exists(UPLOAD_DIR.resolve(fileName))).isFalse();
    }

    @Test
    @DisplayName("성공: 존재하지 않는 파일을 삭제해도 예외가 발생하지 않는다")
    void delete_nonexistentFile_noException() {
        localS3Service.delete("/uploads/nonexistent-file.png");
    }
}
