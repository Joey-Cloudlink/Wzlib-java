package wzlib;

/**
 * Debug: try clean v113 client files with detailed error info.
 */
public class WzDebugTest2 {

    static final String DIR = "C:/Users/Joey/Documents/楓之谷/MapleStory客戶端/乾淨主程式v113";

    public static void main(String[] args) throws Exception {
        String[] files = {"Item.wz", "String.wz", "Etc.wz", "TamingMob.wz", "TEST.wz"};

        for (String fname : files) {
            String path = DIR + "/" + fname;
            System.out.println("=== " + fname + " ===");
            for (WzMapleVersion ver : WzMapleVersion.values()) {
                System.out.print("  " + ver + ": ");
                try (WzFile wz = new WzFile(path, ver)) {
                    wz.parseWzFile();
                    WzDirectory root = wz.getRoot();
                    int dirs = root.getWzDirectories().size();
                    int imgs = root.countImages();

                    // Check first name
                    String firstName = "(empty)";
                    if (!root.getWzDirectories().isEmpty())
                        firstName = "dir:" + root.getWzDirectories().get(0).getName();
                    else if (!root.getWzImages().isEmpty())
                        firstName = "img:" + root.getWzImages().get(0).getName();

                    // Try parse first image
                    String parseResult = "";
                    WzImage firstImg = null;
                    if (!root.getWzImages().isEmpty())
                        firstImg = root.getWzImages().get(0);
                    else if (!root.getWzDirectories().isEmpty() && !root.getWzDirectories().get(0).getWzImages().isEmpty())
                        firstImg = root.getWzDirectories().get(0).getWzImages().get(0);

                    if (firstImg != null) {
                        try {
                            int props = firstImg.getProperties().size();
                            parseResult = " props=" + props;
                        } catch (Exception e) {
                            parseResult = " parseErr=" + e.getMessage();
                        }
                    }

                    System.out.println("v" + wz.getVersion() + " dirs=" + dirs + " imgs=" + imgs + " " + firstName + parseResult);
                } catch (Exception e) {
                    System.out.println("ERR: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
            System.out.println();
        }
    }
}
