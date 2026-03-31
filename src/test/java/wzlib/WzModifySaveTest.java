package wzlib;

import wzlib.property.*;

import java.nio.file.*;
import java.util.List;

/**
 * Verify: Read → Modify → Save → Re-read → Confirm changes persisted.
 * Uses Item.wz from clean v113 client.
 */
public class WzModifySaveTest {

    static final String CLIENT_DIR = "C:\\Users\\Joey\\Documents\\楓之谷\\MapleStory客戶端\\乾淨主程式v113";

    public static void main(String[] args) throws Exception {
        Path inputPath = Paths.get(CLIENT_DIR, "Item.wz");
        String outputPath = "C:/Users/Joey/Desktop/Item_modified.wz";

        System.out.println("=== Modify + Save Test ===\n");

        // Step 1: Read original and find a value to modify
        System.out.println("Step 1: Read original Item.wz");
        int originalSlotMax;
        try (WzFile wz = new WzFile(inputPath.toString(), (short) 113, WzMapleVersion.EMS)) {
            wz.parseWzFile();
            System.out.println("  Images: " + wz.getRoot().countImages());

            // Navigate to Cash/0526.img/05260000/info/slotMax
            WzDirectory cashDir = (WzDirectory) wz.getRoot().getChild("Cash");
            WzImage img0526 = (WzImage) cashDir.getChild("0526.img");
            List<WzImageProperty> props = img0526.getProperties();

            WzSubProperty item0 = (WzSubProperty) findProp(props, "05260000");
            WzSubProperty info = (WzSubProperty) findProp(item0.getProperties(), "info");
            WzIntProperty slotMax = (WzIntProperty) findProp(info.getProperties(), "slotMax");

            originalSlotMax = slotMax.getInt();
            System.out.println("  Found: Cash/0526.img/05260000/info/slotMax = " + originalSlotMax);

            // Step 2: Modify the value
            int newValue = originalSlotMax == 1 ? 999 : 1;
            System.out.println("\nStep 2: Modify slotMax " + originalSlotMax + " -> " + newValue);
            slotMax.setInt(newValue);
            img0526.setChanged(true);

            // Step 3: Save
            System.out.println("\nStep 3: Save to " + outputPath);
            wz.saveToDisk(outputPath);
            System.out.println("  Saved! Size: " + Files.size(Paths.get(outputPath)) + " bytes");
        }

        // Step 4: Re-read the saved file and verify
        System.out.println("\nStep 4: Re-read saved file and verify");
        try (WzFile wz2 = new WzFile(outputPath, (short) 113, WzMapleVersion.EMS)) {
            wz2.parseWzFile();
            System.out.println("  Images: " + wz2.getRoot().countImages());

            WzDirectory cashDir2 = (WzDirectory) wz2.getRoot().getChild("Cash");
            WzImage img0526_2 = (WzImage) cashDir2.getChild("0526.img");
            List<WzImageProperty> props2 = img0526_2.getProperties();

            WzSubProperty item0_2 = (WzSubProperty) findProp(props2, "05260000");
            WzSubProperty info2 = (WzSubProperty) findProp(item0_2.getProperties(), "info");
            WzIntProperty slotMax2 = (WzIntProperty) findProp(info2.getProperties(), "slotMax");

            int savedValue = slotMax2.getInt();
            System.out.println("  Read back: Cash/0526.img/05260000/info/slotMax = " + savedValue);

            int expectedValue = originalSlotMax == 1 ? 999 : 1;
            if (savedValue == expectedValue) {
                System.out.println("\n=== SUCCESS: Value was modified and persisted! ===");
            } else {
                System.out.println("\n=== FAIL: Expected " + expectedValue + " but got " + savedValue + " ===");
            }

            // Also verify other properties are intact
            WzIntProperty cash2 = (WzIntProperty) findProp(info2.getProperties(), "cash");
            System.out.println("  Verify unchanged: cash = " + cash2.getInt() + " (should be 1)");

            // Check canvas is still valid
            WzCanvasProperty icon2 = (WzCanvasProperty) findProp(info2.getProperties(), "icon");
            if (icon2 != null && icon2.getPngProperty() != null) {
                System.out.println("  Verify canvas: icon " +
                    icon2.getPngProperty().getWidth() + "x" + icon2.getPngProperty().getHeight() +
                    " (should be 27x27 or 27x30)");
            }

            // Count total properties to make sure nothing was lost
            int totalProps = countAllProps(props2);
            System.out.println("  Total properties in 0526.img: " + totalProps);
        }

        // Cleanup
        Files.deleteIfExists(Paths.get(outputPath));
        Files.deleteIfExists(Paths.get(outputPath + ".TEMP"));
    }

    static WzImageProperty findProp(List<WzImageProperty> props, String name) {
        for (WzImageProperty p : props) {
            if (name.equals(p.getName())) return p;
        }
        return null;
    }

    static int countAllProps(List<WzImageProperty> props) {
        if (props == null) return 0;
        int count = props.size();
        for (WzImageProperty p : props) {
            count += countAllProps(p.getProperties());
        }
        return count;
    }
}
