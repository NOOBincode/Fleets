package org.example.fleets.file.integration;

import org.example.fleets.file.service.FileService;
import org.example.fleets.user.model.dto.UserRegisterDTO;
import org.example.fleets.user.model.vo.UserVO;
import org.example.fleets.user.service.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;

/**
 * 文件模块集成测试
 * 测试上传文件与获取 URL（需配置 file.upload.path 或使用默认 upload 目录）。
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("文件模块集成测试")
class FileIntegrationTest {

    @Autowired
    private FileService fileService;
    @Autowired
    private UserService userService;

    private static Long testUserId;

    @Test
    @Order(1)
    @DisplayName("集成测试 - 注册用户用于上传")
    void testRegisterUserForUpload() {
        String username = "file_test_" + "1";
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername(username);
        dto.setPassword("Test@123456");
        dto.setNickname("File Test User");
        dto.setPhone("13900139100");
        dto.setEmail("filetest@example.com");
        UserVO vo = userService.register(dto);
        assertThat(vo).isNotNull();
        testUserId = vo.getId();
    }

    @Test
    @Order(2)
    @DisplayName("集成测试 - 上传文件并获取URL")
    void testUploadAndGetUrl() {
        Assumptions.assumeTrue(testUserId != null, "需要先注册用户");
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "hello.txt",
            "text/plain",
            "hello world".getBytes(StandardCharsets.UTF_8)
        );


        String url = fileService.uploadFile(testUserId, file);
        assertThat(url).isNotBlank();
        assertThat(url).contains("file");
    }
}
