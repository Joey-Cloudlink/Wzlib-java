package wzlib;

import java.nio.file.*;

/**
 * Precise test: test each clean v113 file individually with full error trace.
 */
public class WzFixTest {
    static final String DIR = "C:\\Users\\Joey\\Documents\\楓之谷\\MapleStory客戶端\\乾淨主程式v113";

    public static void main(String[] args) throws Exception {
        String[] files = {"Base.wz", "Item.wz", "Map.wz", "Mob.wz", "String.wz",
                          "Etc.wz", "Character.wz", "Skill.wz", "Npc.wz", "Effect.wz",
                          "UI.wz", "Sound.wz", "Quest.wz", "Reactor.wz", "Morph.wz",
                          "TamingMob.wz", "TEST.wz"};

        for (String fname : files) {
            Path p = Paths.get(DIR, fname);
            if (!Files.exists(p)) { System.out.println(fname + ": NOT FOUND"); continue; }

            System.out.printf("%-20s ", fname);

            // Test 1: try known version 113 first, then auto-detect
            try (WzFile wz = new WzFile(p.toString(), (short) 113, WzMapleVersion.EMS)) {
                wz.parseWzFile();
                int imgs = wz.getRoot().countImages();
                int dirs = wz.getRoot().getWzDirectories().size();

                // Test parsing first image
                WzImage firstImg = findFirst(wz.getRoot());
                String imgInfo = "";
                if (firstImg != null) {
                    try {
                        int props = firstImg.getProperties().size();
                        imgInfo = " | " + firstImg.getName() + "=" + props + "props";
                    } catch (Exception e) {
                        imgInfo = " | parse:" + e.getCause().getClass().getSimpleName();
                    }
                }
                System.out.printf("PASS v%d dirs=%d imgs=%d%s%n", wz.getVersion(), dirs, imgs, imgInfo);
            } catch (Exception e) {
                System.out.printf("FAIL %s%n", e.getMessage());
                if (fname.equals("Map.wz") || fname.equals("Mob.wz") || fname.equals("Item.wz")) {
                    e.printStackTrace(System.out);
                }
            }
        }
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
