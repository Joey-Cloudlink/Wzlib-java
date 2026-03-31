package wzlib;

import java.io.File;
import java.nio.file.*;

public class WzCleanClientTest {
    public static void main(String[] args) throws Exception {
        Path clientDir = Paths.get("C:\\Users\\Joey\\Documents\\楓之谷\\MapleStory客戶端\\乾淨主程式v113");

        System.out.println("Client dir exists: " + Files.exists(clientDir));

        // Test with known version 113 (no auto-detect)
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(clientDir, "*.wz")) {
            for (Path p : stream) {
                String fname = p.getFileName().toString();
                if (fname.equals("List.wz")) continue;
                long size = Files.size(p);

                System.out.printf("%-20s (%,d bytes) ", fname, size);

                // Try known version 113 + EMS
                try (WzFile wz = new WzFile(p.toString(), (short) 113, WzMapleVersion.EMS)) {
                    wz.parseWzFile();
                    WzDirectory root = wz.getRoot();
                    int dirs = root.getWzDirectories().size();
                    int imgs = root.countImages();

                    // Parse first image
                    WzImage firstImg = findFirstImage(root);
                    String imgInfo = "";
                    if (firstImg != null) {
                        try {
                            int props = firstImg.getProperties().size();
                            imgInfo = firstImg.getName() + "(" + props + " props)";
                        } catch (Exception e) {
                            imgInfo = "parseErr: " + e.getMessage();
                        }
                    }

                    System.out.printf("PASS dirs=%d imgs=%d %s%n", dirs, imgs, imgInfo);
                } catch (Exception e) {
                    // Fallback: try auto-detect
                    try (WzFile wz = new WzFile(p.toString(), WzMapleVersion.EMS)) {
                        wz.parseWzFile();
                        int imgs = wz.getRoot().countImages();
                        System.out.printf("PASS(auto) v%d imgs=%d%n", wz.getVersion(), imgs);
                    } catch (Exception e2) {
                        System.out.printf("FAIL %s%n", e.getMessage());
                    }
                }
            }
        }
    }

    static WzImage findFirstImage(WzDirectory dir) {
        if (!dir.getWzImages().isEmpty()) return dir.getWzImages().get(0);
        for (WzDirectory sub : dir.getWzDirectories()) {
            WzImage img = findFirstImage(sub);
            if (img != null) return img;
        }
        return null;
    }
}
