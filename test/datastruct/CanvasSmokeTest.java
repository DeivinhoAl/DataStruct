package datastruct;

import datastruct.core.Simulator;
import datastruct.core.StructureType;
import datastruct.ui.StructureCanvas;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Paints every structure without a desktop, catching drawing errors. */
public final class CanvasSmokeTest {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Path output = args.length > 0 ? Path.of(args[0]) : null;
        if (output != null) Files.createDirectories(output);
        Simulator simulator = new Simulator();
        for (StructureType type : StructureType.values()) {
            simulator.example(type);
            StructureCanvas canvas = new StructureCanvas();
            canvas.show(type, simulator.current(type));
            canvas.setSize(canvas.getPreferredSize());
            BufferedImage image = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            canvas.paint(graphics);
            graphics.dispose();
            int darkPixels = 0;
            for (int y = 0; y < image.getHeight(); y += 3)
                for (int x = 0; x < image.getWidth(); x += 3) {
                    int rgb = image.getRGB(x, y);
                    if (((rgb >> 16) & 255) < 170 && ((rgb >> 8) & 255) < 170 && (rgb & 255) < 190)
                        darkPixels++;
                }
            if (darkPixels < 15) throw new AssertionError("Canvas appears blank for " + type);
            if (output != null) ImageIO.write(image, "png", output.resolve(type.name().toLowerCase() + ".png").toFile());
        }
        System.out.println("OK: seis canvas renderizados.");
    }
}
