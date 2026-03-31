package wzlib;

import wzlib.property.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.List;

/**
 * Final comprehensive test: both auto-detect and known version on all v113 files.
 */
public class WzFinalTest {
    static final String DIR = "C:\\Users\\Joey\\Documents\\楓之谷\\MapleStory客戶端\\乾淨主程式v113";
    static int pass = 0, fail = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== wzlib-java Final Test (v113 Clean Client) ===\n");

        String[] files = {"Base.wz", "Item.wz", "Map.wz", "Mob.wz", "String.wz",
                          "Etc.wz", "Character.wz", "Skill.wz", "Npc.wz", "Effect.wz",
                          "UI.wz", "Sound.wz", "Quest.wz", "Reactor.wz", "Morph.wz",
                          "TamingMob.wz"};

        for (String fname : files) {
            Path p = Paths.get(DIR, fname);
            if (!java.nio.file.Files.exists(p)) continue;

            System.out.printf("%-18s ", fname);

            // Test auto-detect
            boolean autoOk = false;
            int autoImgs = 0;
            short autoVer = -1;
            try (WzFile wz = new WzFile(p.toString(), WzMapleVersion.EMS)) {
                wz.parseWzFile();
                autoVer = wz.getVersion();
                autoImgs = wz.getRoot().countImages();
                autoOk = true;
            } catch (Exception e) {
                // auto-detect failed
            }

            // Test known version
            boolean knownOk = false;
            int knownImgs = 0;
            int knownProps = 0;
            String imgName = "";
            String canvasInfo = "";
            try (WzFile wz = new WzFile(p.toString(), (short) 113, WzMapleVersion.EMS)) {
                wz.parseWzFile();
                knownImgs = wz.getRoot().countImages();
                knownOk = true;

                WzImage firstImg = findFirst(wz.getRoot());
                if (firstImg != null) {
                    imgName = firstImg.getName();
                    try {
                        knownProps = firstImg.getProperties().size();

                        // Try decode canvas
                        WzCanvasProperty canvas = findCanvas(firstImg.getProperties());
                        if (canvas != null && canvas.getPngProperty() != null) {
                            WzPngProperty png = canvas.getPngProperty();
                            BufferedImage img = png.getImage(false);
                            if (img != null) canvasInfo = " PNG:" + png.getWidth() + "x" + png.getHeight();
                        }
                    } catch (Exception e) {
                        // parse error on specific image
                    }
                }
            } catch (Exception e) {
                // known version failed
            }

            String status = knownOk ? "PASS" : "FAIL";
            String autoStatus = autoOk ? "auto:v" + autoVer + "(" + autoImgs + ")" : "auto:FAIL";
            System.out.printf("%s imgs=%d %s(%d props)%s [%s]%n",
                    status, knownImgs, imgName, knownProps, canvasInfo, autoStatus);

            if (knownOk) pass++; else fail++;
        }

        System.out.printf("%n=== Results: %d/%d PASS ===%n", pass, pass + fail);
    }

    static WzImage findFirst(WzDirectory dir) {
        if (!dir.getWzImages().isEmpty()) return dir.getWzImages().get(0);
        for (WzDirectory sub : dir.getWzDirectories()) {
            WzImage img = findFirst(sub);
            if (img != null) return img;
        }
        return null;
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
