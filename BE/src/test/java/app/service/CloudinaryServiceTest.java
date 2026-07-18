package app.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceTest {

    private static final String SECURE_URL = "https://cdn.example/image.png";

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    private CloudinaryService cloudinaryService;

    @BeforeEach
    void setUp() {
        lenient().when(cloudinary.uploader()).thenReturn(uploader);
        cloudinaryService = new CloudinaryService(cloudinary);
    }

    @Test
    @DisplayName("UT-UPLOAD-01 - Upload ảnh công ty thành công")
    void uploadCompanyImage_returnsSecureUrl() throws Exception {
        MultipartFile file = imageFile("company.png");
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of("secure_url", SECURE_URL));
        assertEquals(SECURE_URL, cloudinaryService.uploadCompanyImage(file));
        verify(uploader).upload(any(), anyMap());
    }

    @Test
    @DisplayName("UT-UPLOAD-02 - IOException khi upload ảnh công ty")
    void uploadCompanyImage_wrapsIOException() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenThrow(new IOException("read failed"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> cloudinaryService.uploadCompanyImage(file)
        );
        assertTrue(exception.getMessage().contains("Lỗi upload ảnh công ty"));
    }

    @Test
    @DisplayName("Shared service - Upload avatar thành công")
    void uploadAvatar_returnsSecureUrl() throws Exception {
        MultipartFile file = imageFile("avatar.png");
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of("secure_url", SECURE_URL));
        assertEquals(SECURE_URL, cloudinaryService.uploadAvatar(file));
    }

    @Test
    @DisplayName("Shared service - IOException khi upload avatar")
    void uploadAvatar_wrapsIOException() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getOriginalFilename()).thenReturn("avatar.png");
        when(file.getBytes()).thenThrow(new IOException("read failed"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> cloudinaryService.uploadAvatar(file)
        );
        assertTrue(exception.getMessage().contains("Loi upload avatar len Cloudinary"));
    }

    @Test
    @DisplayName("Avatar null bị từ chối")
    void uploadAvatar_rejectsNullFile() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cloudinaryService.uploadAvatar(null)
        );
        assertEquals("Avatar file is required", exception.getMessage());
    }

    @Test
    @DisplayName("Avatar có content type không phải ảnh bị từ chối")
    void uploadAvatar_rejectsNonImageContentType() {
        MultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "text/plain", new byte[]{1}
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cloudinaryService.uploadAvatar(file)
        );
        assertEquals("Avatar must be an image file", exception.getMessage());
    }

    @Test
    @DisplayName("Avatar có phần mở rộng không hỗ trợ bị từ chối")
    void uploadAvatar_rejectsUnsupportedExtension() {
        MultipartFile file = new MockMultipartFile(
                "file", "avatar.exe", "image/png", new byte[]{1}
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cloudinaryService.uploadAvatar(file)
        );
        assertEquals("Avatar file type is not supported", exception.getMessage());
    }

    @Test
    @DisplayName("Shared service - Upload file có tên thành công")
    void uploadFile_usesOriginalFilename() throws Exception {
        MultipartFile file = imageFile("cv.pdf");
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of("secure_url", SECURE_URL));
        assertEquals(SECURE_URL, cloudinaryService.uploadFile(file));
    }

    @Test
    @DisplayName("Shared service - File không có tên vẫn upload được")
    void uploadFile_usesFallbackNameWhenOriginalFilenameIsNull() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(null);
        when(file.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of("secure_url", SECURE_URL));
        assertEquals(SECURE_URL, cloudinaryService.uploadFile(file));
    }

    @Test
    @DisplayName("Shared service - IOException khi upload file")
    void uploadFile_wrapsIOException() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("cv.pdf");
        when(file.getBytes()).thenThrow(new IOException("read failed"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> cloudinaryService.uploadFile(file)
        );
        assertTrue(exception.getMessage().contains("Lỗi upload file lên Cloudinary"));
    }

    private MultipartFile imageFile(String originalFilename) {
        return new MockMultipartFile(
                "file", originalFilename, "image/png", new byte[]{1, 2, 3}
        );
    }
}
