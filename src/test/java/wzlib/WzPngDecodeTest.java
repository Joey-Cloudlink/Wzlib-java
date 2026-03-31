package wzlib;

import wzlib.property.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Phase 4 test: decode PNG images from .wz file and save to disk.
 */
public class WzPngDecodeTest {

    public static void main(String[] args) throws Exception {
        String wzPath = args.length > 0 ? args[0] : "C:/Users/Joey/Desktop/item.wz";
        WzMapleVersion ver = args.length > 1 ? WzMapleVersion.valueOf(args[1]) : WzMapleVersion.EMS;
        String outDir = "C:/Users/Joey/Desktop/wzlib-test-output";
        new File(outDir).mkdirs();

        System.out.println("Opening: " + wzPath);

        try (WzFile wz = new WzFile(wzPath, ver)) {
            wz.parseWzFile();

            // Get first image from Cash directory: 0526.img
            WzDirectory cashDir = null;
            for (WzDirectory dir : wz.getRoot().getWzDirectories()) {
                if ("Cash".equals(dir.getName())) { cashDir = dir; break; }
            }
            if (cashDir == null) { System.out.println("Cash dir not found!"); return; }

            WzImage img = cashDir.getWzImages().get(0);
            System.out.println("Parsing image: " + img.getName());
            List<WzImageProperty> props = img.getProperties();

            // Find all canvas properties and decode them
            int decoded = 0;
            int failed = 0;
            decodeCanvasProperties(props, outDir, img.getName(), 0);

            // Count results
            File[] pngFiles = new File(outDir).listFiles((d, n) -> n.endsWith(".png"));
            System.out.println("\nSaved " + (pngFiles != null ? pngFiles.length : 0) + " PNG files to " + outDir);
        }
    }

    static void decodeCanvasProperties(List<WzImageProperty> props, String outDir, String prefix, int depth) {
        if (props == null) return;
        for (WzImageProperty p : props) {
            if (p instanceof WzCanvasProperty) {
                WzCanvasProperty canvas = (WzCanvasProperty) p;
                WzPngProperty png = canvas.getPngProperty();
                if (png != null) {
                    String fileName = prefix + "_" + p.getName() + "_" + png.getWidth() + "x" + png.getHeight();
                    try {
                        BufferedImage image = png.getImage(false);
                        if (image != null) {
                            File outFile = new File(outDir, fileName + ".png");
                            ImageIO.write(image, "PNG", outFile);
                            System.out.println("  OK: " + fileName + " (" + png.getWidth() + "x" + png.getHeight() + " fmt=" + png.getFormat() + ")");
                        } else {
                            System.out.println("  NULL: " + fileName);
                        }
                    } catch (Exception e) {
                        System.out.println("  FAIL: " + fileName + " - " + e.getMessage());
                    }
                }
                // Recurse into canvas sub-properties
                decodeCanvasProperties(canvas.getProperties(), outDir, prefix + "_" + p.getName(), depth + 1);
            } else if (p instanceof WzSubProperty) {
                decodeCanvasProperties(((WzSubProperty) p).getProperties(), outDir, prefix + "_" + p.getName(), depth + 1);
            }
        }
    }
}
