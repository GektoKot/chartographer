package com.chartographer;

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

@Controller
@RequestMapping("/chartas")
public class FirstController {

    @Autowired
    private ChartaRepo chartaRepo;

    @GetMapping
    public String getAll(Model model) {
        model.addAttribute("chartas", chartaRepo.findAll());
        return "index";
    }

    @GetMapping("/{id}")
    public String getImg(@PathVariable Long id,
                         @RequestParam Integer xCoord, @RequestParam Integer yCoord,
                         @RequestParam Integer width, @RequestParam Integer height,
                         Model model, HttpServletResponse response) throws IOException {
        if (width > 5000 || height > 5000) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "index";
        }
        Charta charta = chartaRepo.findById(id).get();

        BufferedImage chartaImg = ImageIO.read(new File(charta.getFileName()));
        BufferedImage pieceOfCharta = newCharta(width, height);


        if (xCoord + width > chartaImg.getWidth()) {
            width = chartaImg.getWidth() - xCoord;
        }
        if (yCoord + height > chartaImg.getHeight()) {
            height = chartaImg.getHeight() - yCoord;
        }

        paint(width, height, pieceOfCharta, xCoord, yCoord, chartaImg);


//        String path = "src/main/resources/tempchartas/" + "temp_" + charta.getId() + ".bmp";
//        File file = new File(path);
//        ImageIO.write(pieceOfCharta, "bmp", file);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        ImageIO.write(pieceOfCharta, "bmp", outputStream);
//        model.addAttribute("charta", outputStream);

        model.addAttribute("charta", Base64.getEncoder().encodeToString(outputStream.toByteArray()));


//        model.addAttribute("charta", path);

//        file.deleteOnExit();



        return "charta";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable("id") Long id, @RequestParam("file") MultipartFile multipartFile,
                         @RequestParam Integer xCoord, @RequestParam Integer yCoord,
                         @RequestParam Integer width, @RequestParam Integer height,
                         Model model, HttpServletResponse response) throws IOException {
        Charta charta = chartaRepo.findById(id).get();

        String path = "src/main/resources/chartas/" +
                charta.getId() + "_" + charta.getWidth() + "_" + charta.getHeight() + ".bmp";


        ByteArrayInputStream bais = new ByteArrayInputStream(multipartFile.getBytes());


        BufferedImage base = ImageIO.read(new File(path));
        BufferedImage source = ImageIO.read(bais);


        if (xCoord + width > base.getWidth()) {
            width = base.getWidth() - xCoord;
        }
        if (yCoord + height > base.getHeight()) {
            height = base.getHeight() - yCoord;
        }

        paint(width, height, base, xCoord, yCoord, source);

        ImageIO.write(base, "bmp", new File(path));

        model.addAttribute("chartas", chartaRepo.findAll());

        return "index";
    }

    @PostMapping
    public String create(@RequestParam int width, @RequestParam int height, Model model, HttpServletResponse response) {


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

        try {
            ImageIO.write(img, "bmp", new File(path));
        } catch (IOException e) {
            e.printStackTrace();
        }

        model.addAttribute("chartas", chartaRepo.findAll());

        response.addIntHeader("id", charta.getId().intValue());
        response.setStatus(HttpServletResponse.SC_CREATED);
        return "index";
    }


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


    private void paint(Integer width, Integer height, BufferedImage base, Integer xCoord, Integer yCoord, BufferedImage source) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                base.setRGB(xCoord + x, yCoord + y, source.getRGB(x, y));
            }
        }
    }

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
