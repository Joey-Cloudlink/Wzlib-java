package wzlib;

import java.io.File;
import java.nio.file.*;

public class WzPathTest {
    public static void main(String[] args) throws Exception {
        // Method 1: String literal
        String dir1 = "C:/Users/Joey/Documents/楓之谷/MapleStory客戶端/乾淨主程式v113";
        // Method 2: Path from NIO
        Path dir2 = Paths.get("C:\\Users\\Joey\\Documents\\楓之谷\\MapleStory客戶端\\乾淨主程式v113");

        System.out.println("Method 1 (literal): " + dir1);
        System.out.println("Method 2 (NIO):     " + dir2);
        System.out.println("Method 2 exists:    " + Files.exists(dir2));

        // List files via NIO
        System.out.println("\nFiles via NIO:");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir2, "*.wz")) {
            for (Path p : stream) {
                System.out.println("  " + p.getFileName() + " -> " + Files.size(p) + " bytes");

                // Quick parse test
                try (WzFile wz = new WzFile(p.toString(), WzMapleVersion.EMS)) {
                    wz.parseWzFile();
                    System.out.println("    PASS: v" + wz.getVersion() + " imgs=" + wz.getRoot().countImages());
                } catch (Exception e) {
                    System.out.println("    FAIL: " + e.getMessage());
                }
            }
        }
    }
}
