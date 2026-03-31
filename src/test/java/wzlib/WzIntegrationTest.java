package wzlib;

import wzlib.property.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Phase 6: Integration test with multiple .wz files.
 */
public class WzIntegrationTest {

    static int totalPass = 0;
    static int totalFail = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== wzlib-java Integration Test Suite ===\n");

        // Test multiple .wz files
        testFile("C:/Users/Joey/Desktop/item.wz", WzMapleVersion.EMS);
        testFile("C:/Users/Joey/Desktop/超六轉要用到的檔案/String.wz", WzMapleVersion.EMS);
        testFile("C:/Users/Joey/Desktop/超六轉要用到的檔案/Skill.wz", WzMapleVersion.EMS);
        testFile("C:/Users/Joey/Desktop/楓之谷圖片/Effect.wz", WzMapleVersion.EMS);

        System.out.println("\n=== Final Results ===");
        System.out.println("Passed: " + totalPass);
        System.out.println("Failed: " + totalFail);
        System.out.println(totalFail == 0 ? "ALL TESTS PASSED!" : "SOME TESTS FAILED!");
    }

    static void testFile(String path, WzMapleVersion ver) {
        System.out.println("--- Testing: " + path + " ---");
        File f = new File(path);
        if (!f.exists()) {
            System.out.println("  SKIP: file not found\n");
            return;
        }

        try (WzFile wz = new WzFile(path, ver)) {
            wz.parseWzFile();

            // Test 1: Header valid
            check("Header is PKG1", "PKG1".equals(wz.getHeader().getIdent()));

            // Test 2: Has directories or images
            WzDirectory root = wz.getRoot();
            int dirs = countDirs(root);
            int imgs = root.countImages();
            check("Has content (dirs=" + dirs + ", imgs=" + imgs + ")", dirs > 0 || imgs > 0);

            // Test 3: Parse first image from first directory
            WzDirectory firstDir = root.getWzDirectories().isEmpty() ? root : root.getWzDirectories().get(0);
            if (!firstDir.getWzImages().isEmpty()) {
                WzImage firstImg = firstDir.getWzImages().get(0);
                List<WzImageProperty> props = firstImg.getProperties();
                check("Image '" + firstImg.getName() + "' has properties (" + props.size() + ")", !props.isEmpty());

                // Test 4: Count property types
                int[] typeCounts = new int[WzPropertyType.values().length];
                countPropertyTypes(props, typeCounts);
                StringBuilder sb = new StringBuilder("  Property types: ");
                for (WzPropertyType t : WzPropertyType.values()) {
                    int c = typeCounts[t.ordinal()];
                    if (c > 0) sb.append(t.name()).append("=").append(c).append(" ");
                }
                System.out.println(sb);

                // Test 5: Decode first canvas if any
                WzCanvasProperty canvas = findFirstCanvas(props);
                if (canvas != null && canvas.getPngProperty() != null) {
                    WzPngProperty png = canvas.getPngProperty();
                    try {
                        BufferedImage img = png.getImage(false);
                        check("Canvas decode '" + canvas.getName() + "' " + png.getWidth() + "x" + png.getHeight(), img != null);
                    } catch (Exception e) {
                        check("Canvas decode '" + canvas.getName() + "' (error: " + e.getMessage() + ")", false);
                    }
                }
            }

            // Test 6: Round-trip save
            String savedPath = path + ".roundtrip.wz";
            try {
                wz.saveToDisk(savedPath);
                try (WzFile wz2 = new WzFile(savedPath, ver)) {
                    wz2.parseWzFile();
                    int savedImgs = wz2.getRoot().countImages();
                    check("Round-trip: images match (" + imgs + " -> " + savedImgs + ")", imgs == savedImgs);
                }
            } catch (Exception e) {
                check("Round-trip save (error: " + e.getMessage() + ")", false);
            } finally {
                new File(savedPath).delete();
                new File(savedPath + ".TEMP").delete();
            }

        } catch (Exception e) {
            check("Open and parse (error: " + e.getMessage() + ")", false);
            e.printStackTrace();
        }
        System.out.println();
    }

    static void check(String name, boolean pass) {
        System.out.println("  " + (pass ? "PASS" : "FAIL") + ": " + name);
        if (pass) totalPass++; else totalFail++;
    }

    static int countDirs(WzDirectory dir) {
        int count = dir.getWzDirectories().size();
        for (WzDirectory sub : dir.getWzDirectories()) count += countDirs(sub);
        return count;
    }

    static void countPropertyTypes(List<WzImageProperty> props, int[] counts) {
        if (props == null) return;
        for (WzImageProperty p : props) {
            counts[p.getPropertyType().ordinal()]++;
            if (p.getProperties() != null) countPropertyTypes(p.getProperties(), counts);
        }
    }

    static WzCanvasProperty findFirstCanvas(List<WzImageProperty> props) {
        if (props == null) return null;
        for (WzImageProperty p : props) {
            if (p instanceof WzCanvasProperty) return (WzCanvasProperty) p;
            WzCanvasProperty found = findFirstCanvas(p.getProperties());
            if (found != null) return found;
        }
        return null;
    }
}
