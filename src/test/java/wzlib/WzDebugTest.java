package wzlib;

/**
 * Debug test: try opening a specific .wz with all versions and print errors.
 */
public class WzDebugTest {

    public static void main(String[] args) throws Exception {
        String path = "C:/Users/Joey/Documents/楓之谷/MapleStory客戶端/乾淨主程式v113/Item.wz";

        for (WzMapleVersion ver : WzMapleVersion.values()) {
            System.out.println("--- Trying " + ver + " ---");
            try (WzFile wz = new WzFile(path, ver)) {
                wz.parseWzFile();
                WzDirectory root = wz.getRoot();
                System.out.println("  Version: " + wz.getVersion());
                System.out.println("  64-bit: " + wz.is64BitWzFile());
                System.out.println("  Dirs: " + root.getWzDirectories().size());
                System.out.println("  Images: " + root.countImages());

                if (!root.getWzDirectories().isEmpty()) {
                    WzDirectory first = root.getWzDirectories().get(0);
                    System.out.println("  First dir: " + first.getName());
                    if (!first.getWzImages().isEmpty()) {
                        WzImage img = first.getWzImages().get(0);
                        System.out.println("  First img: " + img.getName());
                        try {
                            int props = img.getProperties().size();
                            System.out.println("  Props: " + props);
                        } catch (Exception e) {
                            System.out.println("  Parse img error: " + e.getMessage());
                        }
                    }
                } else if (!root.getWzImages().isEmpty()) {
                    WzImage img = root.getWzImages().get(0);
                    System.out.println("  First img: " + img.getName());
                }
            } catch (Exception e) {
                System.out.println("  ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }
}
