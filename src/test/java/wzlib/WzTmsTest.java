package wzlib;

import wzlib.property.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Test high-version TMS UI .wz files comprehensively.
 */
public class WzTmsTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=== TMS High-Version UI Test ===\n");

        String path = "C:/Users/Joey/Desktop/同步UI/UI/UI_000.wz";
        int pass = 0, fail = 0;

        try (WzFile wz = new WzFile(path, WzMapleVersion.BMS)) {
            wz.parseWzFile();
            WzDirectory root = wz.getRoot();

            System.out.println("Version: " + wz.getVersion());
            System.out.println("64-bit: " + wz.is64BitWzFile());
            System.out.println("Images: " + root.countImages());
            System.out.println();

            // Parse ALL images and count successes/failures
            int parseOk = 0, parseFail = 0;
            int canvasOk = 0, canvasFail = 0;
            int totalProps = 0;

            List<WzImage> images = root.getWzImages();
            for (WzImage img : images) {
                try {
                    List<WzImageProperty> props = img.getProperties();
                    totalProps += props.size();
                    parseOk++;

                    // Try decode first canvas
                    WzCanvasProperty canvas = findCanvas(props);
                    if (canvas != null && canvas.getPngProperty() != null) {
                        try {
                            BufferedImage bi = canvas.getPngProperty().getImage(false);
                            if (bi != null) canvasOk++;
                            else canvasFail++;
                        } catch (Exception e) {
                            canvasFail++;
                        }
                    }
                } catch (Exception e) {
                    parseFail++;
                    if (parseFail <= 5) {
                        Throwable cause = e.getCause() != null ? e.getCause() : e;
                        System.out.println("  FAIL: " + img.getName() + " - " +
                            cause.getClass().getSimpleName() + ": " + cause.getMessage());
                    }
                }
            }

            System.out.println("\n--- Results ---");
            System.out.printf("Images parsed:  %d/%d OK%n", parseOk, images.size());
            System.out.printf("Canvas decoded: %d OK, %d FAIL%n", canvasOk, canvasFail);
            System.out.printf("Total props:    %d%n", totalProps);

            if (parseFail > 5) {
                System.out.println("(Showing first 5 failures only)");
            }

            // Print a few image details
            System.out.println("\n--- Sample Images ---");
            int shown = 0;
            for (WzImage img : images) {
                if (shown >= 10) break;
                try {
                    List<WzImageProperty> props = img.getProperties();
                    if (props.size() > 0) {
                        System.out.printf("  %-30s %d props", img.getName(), props.size());
                        // Show property types
                        StringBuilder types = new StringBuilder();
                        for (WzImageProperty p : props) {
                            if (types.length() > 0) types.append(", ");
                            types.append(p.getName()).append("[").append(p.getPropertyType()).append("]");
                            if (types.length() > 60) { types.append("..."); break; }
                        }
                        System.out.println(" | " + types);
                        shown++;
                    }
                } catch (Exception e) { /* skip */ }
            }

            pass = parseOk;
            fail = parseFail;
        }

        System.out.printf("\n=== %d/%d images parsed successfully ===%n", pass, pass + fail);
    }

    static WzCanvasProperty findCanvas(List<WzImageProperty> props) {
        if (props == null) return null;
        for (WzImageProperty p : props) {
            if (p instanceof WzCanvasProperty) return (WzCanvasProperty) p;
            WzCanvasProperty found = findCanvas(p.getProperties());
            if (found != null) return found;
        }
        return null;
    }
}
