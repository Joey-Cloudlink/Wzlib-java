package wzlib;

import java.nio.file.*;

public class WzDeepDebug {
    public static void main(String[] args) throws Exception {
        // Test Item.wz with all version combinations
        Path itemWz = Paths.get("C:\\Users\\Joey\\Documents\\楓之谷\\MapleStory客戶端\\乾淨主程式v113\\Item.wz");

        System.out.println("=== Item.wz deep debug ===");
        System.out.println("Size: " + Files.size(itemWz));

        WzMapleVersion[] vers = WzMapleVersion.values();
        short[] patchVersions = {-1, 113, 112, 114, 83, 55, 95};

        for (WzMapleVersion ver : vers) {
            for (short pv : patchVersions) {
                try (WzFile wz = new WzFile(itemWz.toString(), pv, ver)) {
                    wz.parseWzFile();
                    WzDirectory root = wz.getRoot();
                    int imgs = root.countImages();
                    String firstName = "";
                    if (!root.getWzDirectories().isEmpty())
                        firstName = root.getWzDirectories().get(0).getName();
                    else if (!root.getWzImages().isEmpty())
                        firstName = root.getWzImages().get(0).getName();
                    boolean validName = isAscii(firstName);

                    if (validName && imgs > 0) {
                        System.out.printf("  %s pv=%d -> v%d imgs=%d first=%s%n", ver, pv, wz.getVersion(), imgs, firstName);

                        // Try parsing first image
                        WzImage img = findFirst(root);
                        if (img != null) {
                            try {
                                int props = img.getProperties().size();
                                System.out.printf("    Image: %s props=%d%n", img.getName(), props);
                            } catch (Exception e) {
                                System.out.printf("    Image parse fail: %s%n", e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    // skip
                }
            }
        }

        // Also test Etc.wz image parsing error
        System.out.println("\n=== Etc.wz image parse debug ===");
        Path etcWz = Paths.get("C:\\Users\\Joey\\Documents\\楓之谷\\MapleStory客戶端\\乾淨主程式v113\\Etc.wz");
        try (WzFile wz = new WzFile(etcWz.toString(), (short) 113, WzMapleVersion.EMS)) {
            wz.parseWzFile();
            for (WzImage img : wz.getRoot().getWzImages()) {
                try {
                    int props = img.getProperties().size();
                    System.out.printf("  %s: %d props OK%n", img.getName(), props);
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    System.out.printf("  %s: FAIL - %s: %s%n", img.getName(), cause.getClass().getSimpleName(), cause.getMessage());
                }
            }
        }
    }

    static boolean isAscii(String s) {
        if (s == null || s.isEmpty()) return false;
        for (char c : s.toCharArray()) if (c < 0x20 || c > 0x7E) return false;
        return true;
    }

    static WzImage findFirst(WzDirectory dir) {
        if (!dir.getWzImages().isEmpty()) return dir.getWzImages().get(0);
        for (WzDirectory sub : dir.getWzDirectories()) {
            WzImage img = findFirst(sub);
            if (img != null) return img;
        }
        return null;
    }
}
