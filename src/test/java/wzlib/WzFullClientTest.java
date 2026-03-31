package wzlib;

import wzlib.property.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Full client test: test ALL .wz files from clean v113 client.
 * Read-only — no files will be modified.
 */
public class WzFullClientTest {

    static final String CLIENT_DIR = "C:/Users/Joey/Documents/楓之谷/MapleStory客戶端/乾淨主程式v113";
    static int totalPass = 0, totalFail = 0, totalSkip = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== wzlib-java Full Client Test (v113) ===");
        System.out.println("Client: " + CLIENT_DIR + "\n");

        File dir = new File(CLIENT_DIR);
        File[] wzFiles = dir.listFiles((d, n) -> n.endsWith(".wz"));
        if (wzFiles == null || wzFiles.length == 0) {
            System.out.println("No .wz files found!"); return;
        }

        java.util.Arrays.sort(wzFiles);

        // Try EMS first (TWMS v113 uses MSEA IV)
        WzMapleVersion[] tryVersions = { WzMapleVersion.EMS, WzMapleVersion.GMS, WzMapleVersion.BMS };

        for (File wzFile : wzFiles) {
            testWzFile(wzFile, tryVersions);
        }

        System.out.println("\n========================================");
        System.out.println("Total: " + (totalPass + totalFail + totalSkip) + " tests");
        System.out.println("  PASS: " + totalPass);
        System.out.println("  FAIL: " + totalFail);
        System.out.println("  SKIP: " + totalSkip);
        System.out.println(totalFail == 0 ? "\nALL TESTS PASSED!" : "\nSOME TESTS FAILED!");
    }

    static void testWzFile(File wzFile, WzMapleVersion[] versions) {
        String name = wzFile.getName();
        long size = wzFile.length();
        System.out.printf("%-20s (%,d bytes) ", name, size);

        // Skip List.wz (different format)
        if (name.equals("List.wz")) {
            System.out.println("SKIP (list format)");
            totalSkip++;
            return;
        }

        for (WzMapleVersion ver : versions) {
            try (WzFile wz = new WzFile(wzFile.getAbsolutePath(), ver)) {
                wz.parseWzFile();

                WzDirectory root = wz.getRoot();
                int dirs = countDirs(root);
                int imgs = root.countImages();

                if (imgs == 0 && dirs == 0) {
                    continue; // try next version
                }

                // Verify names are readable ASCII/valid text (not garbled)
                WzImage firstImg = findFirstImage(root);
                if (firstImg != null && !isValidName(firstImg.getName())) {
                    continue; // garbled name = wrong version
                }
                // Also check directory names
                if (!root.getWzDirectories().isEmpty() && !isValidName(root.getWzDirectories().get(0).getName())) {
                    continue;
                }

                int propCount = 0;
                String imgName = "(none)";
                if (firstImg != null) {
                    imgName = firstImg.getName();
                    try {
                        propCount = firstImg.getProperties().size();
                    } catch (Exception e) {
                        continue; // wrong version, try next
                    }
                }

                // Try decoding a canvas
                String canvasInfo = "";
                if (firstImg != null) {
                    WzCanvasProperty canvas = findFirstCanvas(firstImg.getProperties());
                    if (canvas != null && canvas.getPngProperty() != null) {
                        try {
                            WzPngProperty png = canvas.getPngProperty();
                            BufferedImage img = png.getImage(false);
                            if (img != null) {
                                canvasInfo = " | PNG:" + png.getWidth() + "x" + png.getHeight() + " OK";
                            }
                        } catch (Exception e) {
                            canvasInfo = " | PNG:FAIL(" + e.getMessage() + ")";
                        }
                    }
                }

                System.out.printf("PASS [%s] dirs=%d imgs=%d | %s(%d props)%s%n",
                        ver, dirs, imgs, imgName, propCount, canvasInfo);
                totalPass++;
                return;

            } catch (Exception e) {
                System.out.print("[" + ver + ":" + e.getClass().getSimpleName() + "] ");
                // try next version
            }
        }

        System.out.println("FAIL");
        totalFail++;
    }

    static int countDirs(WzDirectory dir) {
        int count = dir.getWzDirectories().size();
        for (WzDirectory sub : dir.getWzDirectories()) count += countDirs(sub);
        return count;
    }

    static WzImage findFirstImage(WzDirectory dir) {
        if (!dir.getWzImages().isEmpty()) return dir.getWzImages().get(0);
        for (WzDirectory sub : dir.getWzDirectories()) {
            WzImage img = findFirstImage(sub);
            if (img != null) return img;
        }
        return null;
    }

    /** Check if a name looks like valid text (printable ASCII, dots, digits) */
    static boolean isValidName(String name) {
        if (name == null || name.isEmpty()) return false;
        for (char c : name.toCharArray()) {
            if (c < 0x20 || c > 0x7E) return false;
        }
        return true;
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
