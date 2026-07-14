package com.mopl.infra.s3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    @DisplayName("성공: 파일을 업로드하면 uploads 디렉터리에 저장되고 경로를 반환한다")
    void upload_success_savesFileAndReturnsPath() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "profile.png", "image/png", "image-bytes".getBytes());

        String result = localS3Service.upload(file);

        assertThat(result).startsWith("/uploads/").endsWith(".png");
        String fileName = result.substring("/uploads/".length());
        assertThat(Files.exists(UPLOAD_DIR.resolve(fileName))).isTrue();
    }

    @Test
    @DisplayName("성공: 확장자가 없는 파일도 업로드된다")
    void upload_noExtension_savesWithoutExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "noext", "application/octet-stream", "data".getBytes());

        String result = localS3Service.upload(file);

        assertThat(result).startsWith("/uploads/");
        assertThat(result).doesNotContain(".");
    }

    @Test
    @DisplayName("성공: 매 업로드마다 고유한 파일명이 생성된다")
    void upload_generatesUniqueFileNames() {
        MockMultipartFile file1 = new MockMultipartFile("file", "a.png", "image/png", "data1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file", "a.png", "image/png", "data2".getBytes());

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
        MockMultipartFile file = new MockMultipartFile("file", "to-delete.png", "image/png", "data".getBytes());
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
