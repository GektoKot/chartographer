package com.chartographer.controller;

import com.chartographer.domain.Charta;
import com.chartographer.repository.ChartaRepo;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
class ChartasControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChartaRepo chartaRepo;

    private final String FILE_NAME = "src/test/resources/chartas/img.bmp";

    @BeforeEach
    void before() throws IOException {
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 100; x++) {
            for (int y = 0; y < 100; y++) {
                img.setRGB(x, y, Color.BLACK.getRGB());
            }
        }
        ImageIO.write(img, "bmp", new File(FILE_NAME));
    }

    @AfterEach
    void after() throws IOException {
        Files.deleteIfExists(Path.of(FILE_NAME));
    }


    @Test
    void testReadAll() throws Exception {
        mockMvc.perform(get("/chartas"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void testRead() throws Exception {
        Charta charta = new Charta(10, 10);
        charta.setFileName(FILE_NAME);
        doReturn(Optional.of(charta)).when(this.chartaRepo).findById(1L);
        mockMvc.perform(get("/chartas/1?x=0&y=0&width=5&height=5"))
                .andExpect(status().isOk())
                .andExpect(view().name("charta"));
    }

    @Test
    void testUpdate() throws Exception {
        byte[] fileContent = Files.readAllBytes(Path.of("src/test/resources/chartas/uploadfile.bmp"));

        MockMultipartFile multipartFile = new MockMultipartFile("file", fileContent);

        Charta charta = new Charta(10, 10);
        charta.setFileName(FILE_NAME);
        doReturn(Optional.of(charta)).when(this.chartaRepo).findById(1L);
        mockMvc.perform(multipart("/chartas/1?x=0&y=0&width=1&height=1").file(multipartFile))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));

    }

    @Test
    void testCreate() throws Exception {
        Charta charta = new Charta(10, 10);
        charta.setId(1L);
        doReturn(charta).when(this.chartaRepo).save(any(Charta.class));
        mockMvc.perform(post("/chartas/?width=10&height=10"))
                .andExpect(status().isCreated())
                .andExpect(view().name("index"));
    }

    @Test
    void testDelete() throws Exception {
        Assertions.assertTrue(Files.exists(Path.of(FILE_NAME)));
        Charta charta = new Charta(10, 10);
        charta.setFileName(FILE_NAME);
        doReturn(Optional.of(charta)).when(this.chartaRepo).findById(1L);
        mockMvc.perform(delete("/chartas/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
        Assertions.assertFalse(Files.exists(Path.of(FILE_NAME)));
    }
}