package com.chartographer.controller;

import com.chartographer.domain.Charta;
import com.chartographer.repository.ChartaRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;

@Controller
@RequestMapping("/chartas")
public class ChartasController {

    @Autowired
    private ChartaRepo chartaRepo;

    @GetMapping
    public String readAll(Model model) {
        model.addAttribute("chartas", chartaRepo.findAll());
        return "index";
    }


    /**
     * Получить восстановленную часть изображения размера {width} x {height} с координатами ({x};{y}),
     * где {width} и {height} — положительные целые числа, не превосходящие 5 000.
     * Под координатами подразумевается положение левого верхнего угла фрагмента
     * относительно левого верхнего угла основного изображения. Восстановленая часть возвращается в Модели.
     *
     * @param id
     * @param x
     * @param y
     * @param width
     * @param height
     * @param model
     * @param response
     * @return "charta" тэмплэйт
     * @throws IOException
     */
    @GetMapping("/{id}")
    public String read(@PathVariable Long id,
                       @RequestParam Integer x, @RequestParam Integer y,
                       @RequestParam Integer width, @RequestParam Integer height,
                       Model model, HttpServletResponse response) throws IOException {
        Optional<Charta> chartaOptional = chartaRepo.findById(id);
        if (chartaOptional.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("chartas", chartaRepo.findAll());
            return "index";
        }
        Charta charta = chartaOptional.get();
        if (width > 5000 || height > 5000
                || width <= 0 || height <= 0
                || x < 0 || y < 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            model.addAttribute("chartas", chartaRepo.findAll());
            return "index";
        }

        BufferedImage chartaImg = ImageIO.read(new File(charta.getFileName()));
        BufferedImage pieceOfCharta = newCharta(width, height);

        if (x + width > chartaImg.getWidth()) {
            width = chartaImg.getWidth() - x;
        }
        if (y + height > chartaImg.getHeight()) {
            height = chartaImg.getHeight() - y;
        }
        for (int xLoop = 0; xLoop < width; xLoop++) {
            for (int yLoop = 0; yLoop < height; yLoop++) {
                pieceOfCharta.setRGB(xLoop, yLoop, chartaImg.getRGB(xLoop + x, yLoop + y));
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(pieceOfCharta, "bmp", outputStream);
        model.addAttribute("charta", Base64.getEncoder().encodeToString(outputStream.toByteArray()));
        return "charta";
    }

    /**
     * Сохранить восстановленный фрагмент изображения размера {width} x {height} с координатами ({x};{y}).
     * Под координатами подразумевается положение левого верхнего угла фрагмента относительно левого
     * верхнего угла всего изображения.
     *
     * @param id
     * @param multipartFile загружаемый файл
     * @param x
     * @param y
     * @param width
     * @param height
     * @param model
     * @param response
     * @return "index" template
     * @throws IOException
     */
    @PostMapping("/{id}")
    public String update(@PathVariable("id") Long id, @RequestParam("file") MultipartFile multipartFile,
                         @RequestParam Integer x, @RequestParam Integer y,
                         @RequestParam Integer width, @RequestParam Integer height,
                         Model model, HttpServletResponse response) throws IOException {
        Optional<Charta> chartaOptional = chartaRepo.findById(id);
        if (chartaOptional.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("chartas", chartaRepo.findAll());
            return "index";
        }
        Charta charta = chartaOptional.get();
        if (x > charta.getWidth() || y > charta.getHeight()
                || x < 0 || y < 0
                || width <= 0 || height <= 0
        ) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            model.addAttribute("chartas", chartaRepo.findAll());
            return "index";
        }
        File chartaFile = new File(charta.getFileName());


        ByteArrayInputStream bais = new ByteArrayInputStream(multipartFile.getBytes());
        BufferedImage base = ImageIO.read(chartaFile);
        BufferedImage source = ImageIO.read(bais);

        if (x + width > base.getWidth()) {
            width = base.getWidth() - x;
        }
        if (y + height > base.getHeight()) {
            height = base.getHeight() - y;
        }
        for (int xLoop = 0; xLoop < width; xLoop++) {
            for (int yLoop = 0; yLoop < height; yLoop++) {
                base.setRGB(x + xLoop, y + yLoop, source.getRGB(xLoop, yLoop));
            }
        }

        ImageIO.write(base, "bmp", chartaFile);
        model.addAttribute("chartas", chartaRepo.findAll());
        return "index";
    }

    private void fillImage(Integer x, Integer y, Integer width, Integer height, BufferedImage target, BufferedImage source) {

    }

    /**
     * Создать новое изображение харты заданного размера, где {width} и {height} —
     * положительные целые числа, не превосходящие 20 000 и 50 000, соответственно.
     *
     * @param width
     * @param height
     * @param model
     * @param response
     * @return "index" template
     * @throws IOException
     */
    @PostMapping
    public String create(@RequestParam Integer width, @RequestParam Integer height,
                         Model model, HttpServletResponse response) throws IOException {
        if (width > 20_000 || height > 50_000
                || width <= 0 || height <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            model.addAttribute("chartas", chartaRepo.findAll());
            return "index";
        }

        Charta charta = chartaRepo.save(new Charta(width, height));
        String path = "src/main/resources/chartas/" +
                charta.getId() + "_" + charta.getWidth() + "_" + charta.getHeight() + ".bmp";
        charta.setFileName(path);
        chartaRepo.save(charta);

        BufferedImage img = newCharta(width, height);
        ImageIO.write(img, "bmp", new File(path));

        response.setStatus(HttpServletResponse.SC_CREATED);
        model.addAttribute("chartas", chartaRepo.findAll());
        return "index";
    }

    /**
     * Удаляет изображение с идентификатором {id}.
     *
     * @param id
     * @param model
     * @return "index" template
     */
    @DeleteMapping("/{id}")
    public String delete(@PathVariable("id") Long id, Model model) {
        chartaRepo.findById(id).ifPresent(charta -> {
            try {
                Files.deleteIfExists(Path.of(charta.getFileName()));
                chartaRepo.deleteById(id);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        model.addAttribute("chartas", chartaRepo.findAll());
        return "index";
    }

    /**
     * Вспомогательный метод, рисует новую харту.
     *
     * @param width
     * @param height
     * @return BufferedImage
     */
    // создание новой харты
    private BufferedImage newCharta(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                img.setRGB(x, y, Color.BLACK.getRGB());
            }
        }
        return img;
    }

}
