package wzlib;

import wzlib.property.*;

import java.nio.file.*;
import java.util.List;

/**
 * Test DeepClone + PathCache.
 */
public class WzClonePathTest {

    static final String CLIENT_DIR = "C:\\Users\\Joey\\Documents\\楓之谷\\MapleStory客戶端\\乾淨主程式v113";
    static int pass = 0, fail = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== DeepClone + PathCache Test ===\n");

        Path itemWz = Paths.get(CLIENT_DIR, "Item.wz");
        try (WzFile wz = new WzFile(itemWz.toString(), (short) 113, WzMapleVersion.EMS)) {
            wz.parseWzFile();

            // ---- PathCache Tests ----
            System.out.println("--- PathCache Tests ---");

            // Test basic path lookup
            WzObject cashDir = wz.getRoot().getFromPath("Cash");
            check("Path 'Cash'", cashDir != null && "Cash".equals(cashDir.getName()));

            WzObject img0526 = wz.getRoot().getFromPath("Cash/0526.img");
            check("Path 'Cash/0526.img'", img0526 != null && "0526.img".equals(img0526.getName()));

            // Test cached path lookup on WzFile
            WzObject cached1 = wz.getFromPathCached("Cash");
            WzObject cached2 = wz.getFromPathCached("Cash");
            check("PathCache returns same ref", cached1 == cached2);

            // Test deep path through image
            WzImage img = (WzImage) img0526;
            WzObject slotMax = img.getFromPath("05260000/info/slotMax");
            check("Deep path '05260000/info/slotMax'", slotMax instanceof WzIntProperty);
            if (slotMax instanceof WzIntProperty) {
                check("  slotMax value = 1", ((WzIntProperty) slotMax).getInt() == 1);
            }

            WzObject icon = img.getFromPath("05260000/info/icon");
            check("Deep path to Canvas", icon instanceof WzCanvasProperty);

            // Non-existent path
            WzObject missing = img.getFromPath("nonexistent/path");
            check("Missing path returns null", missing == null);

            // ---- DeepClone Tests ----
            System.out.println("\n--- DeepClone Tests ---");

            // Clone an int property
            WzIntProperty origInt = (WzIntProperty) slotMax;
            WzIntProperty cloneInt = (WzIntProperty) origInt.deepClone();
            check("Clone Int: same value", cloneInt.getInt() == origInt.getInt());
            check("Clone Int: same name", cloneInt.getName().equals(origInt.getName()));
            cloneInt.setInt(999);
            check("Clone Int: modify clone doesn't affect original", origInt.getInt() == 1);

            // Clone a SubProperty (with children)
            WzSubProperty origSub = (WzSubProperty) img.getFromPath("05260000");
            WzSubProperty cloneSub = (WzSubProperty) origSub.deepClone();
            check("Clone Sub: same name", cloneSub.getName().equals(origSub.getName()));
            check("Clone Sub: same child count", cloneSub.getProperties().size() == origSub.getProperties().size());
            check("Clone Sub: different object ref", cloneSub != origSub);

            // Verify deep children are also cloned
            WzSubProperty origInfo = (WzSubProperty) findProp(origSub.getProperties(), "info");
            WzSubProperty cloneInfo = (WzSubProperty) findProp(cloneSub.getProperties(), "info");
            check("Clone deep: info exists", cloneInfo != null);
            check("Clone deep: different ref", cloneInfo != origInfo);
            check("Clone deep: same child count", cloneInfo.getProperties().size() == origInfo.getProperties().size());

            // Clone a Canvas property (with PNG)
            WzCanvasProperty origCanvas = (WzCanvasProperty) findProp(origInfo.getProperties(), "icon");
            WzCanvasProperty cloneCanvas = (WzCanvasProperty) origCanvas.deepClone();
            check("Clone Canvas: same name", cloneCanvas.getName().equals(origCanvas.getName()));
            check("Clone Canvas: has PNG", cloneCanvas.getPngProperty() != null);
            check("Clone Canvas: PNG different ref", cloneCanvas.getPngProperty() != origCanvas.getPngProperty());
            check("Clone Canvas: same dimensions",
                cloneCanvas.getPngProperty().getWidth() == origCanvas.getPngProperty().getWidth() &&
                cloneCanvas.getPngProperty().getHeight() == origCanvas.getPngProperty().getHeight());

            // Clone a Vector property
            WzVectorProperty origVec = null;
            for (WzImageProperty p : origCanvas.getProperties()) {
                if (p instanceof WzVectorProperty) { origVec = (WzVectorProperty) p; break; }
            }
            if (origVec != null) {
                WzVectorProperty cloneVec = (WzVectorProperty) origVec.deepClone();
                check("Clone Vector: same X", cloneVec.getX().getInt() == origVec.getX().getInt());
                check("Clone Vector: same Y", cloneVec.getY().getInt() == origVec.getY().getInt());
                cloneVec.getX().setInt(999);
                check("Clone Vector: modify doesn't affect original", origVec.getX().getInt() != 999);
            }

            // Clone String property
            WzObject strResult = wz.getRoot().getFromPath("Etc");
            if (strResult == null) {
                // Try finding a string property somewhere
                System.out.println("  (Skipping String clone - no Etc dir in Item.wz)");
            }

            // Clone entire image
            System.out.println("\n--- Full Image Clone ---");
            WzSubProperty fullClone = (WzSubProperty) origSub.deepClone();
            int origProps = countAllProps(origSub.getProperties());
            int cloneProps = countAllProps(fullClone.getProperties());
            check("Full clone: same total props (" + origProps + " vs " + cloneProps + ")",
                origProps == cloneProps);
        }

        System.out.printf("\n=== Results: %d/%d PASS ===%n", pass, pass + fail);
    }

    static void check(String name, boolean ok) {
        System.out.println("  " + (ok ? "PASS" : "FAIL") + ": " + name);
        if (ok) pass++; else fail++;
    }

    static WzImageProperty findProp(List<WzImageProperty> props, String name) {
        for (WzImageProperty p : props) if (name.equals(p.getName())) return p;
        return null;
    }

    static int countAllProps(List<WzImageProperty> props) {
        if (props == null) return 0;
        int c = props.size();
        for (WzImageProperty p : props) c += countAllProps(p.getProperties());
        return c;
    }
}
