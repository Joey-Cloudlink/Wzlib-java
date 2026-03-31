package wzlib;

import wzlib.property.*;
import wzlib.util.*;

import java.io.*;
import java.nio.file.*;
import java.util.List;

/**
 * Test XML serialization: WZ → XML → parse back → compare.
 */
public class WzXmlTest {

    static final String CLIENT_DIR = "C:\\Users\\Joey\\Documents\\楓之谷\\MapleStory客戶端\\乾淨主程式v113";
    static int pass = 0, fail = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== XML Serialization Test ===\n");

        Path itemWz = Paths.get(CLIENT_DIR, "Item.wz");
        String xmlDir = "C:/Users/Joey/Desktop/wzlib-xml-test";
        new File(xmlDir).mkdirs();

        try (WzFile wz = new WzFile(itemWz.toString(), (short) 113, WzMapleVersion.EMS)) {
            wz.parseWzFile();

            // Get 0526.img
            WzImage img0526 = (WzImage) wz.getRoot().getFromPath("Cash/0526.img");

            // Step 1: Serialize to XML
            System.out.println("--- Step 1: Serialize to XML ---");
            File xmlFile = new File(xmlDir, "0526.img.xml");
            WzXmlSerializer.serializeImage(img0526, xmlFile);
            System.out.println("  Written: " + xmlFile.getAbsolutePath() + " (" + xmlFile.length() + " bytes)");
            check("XML file created", xmlFile.exists() && xmlFile.length() > 0);

            // Print first few lines
            try (BufferedReader br = new BufferedReader(new FileReader(xmlFile))) {
                System.out.println("  Preview:");
                for (int i = 0; i < 15; i++) {
                    String line = br.readLine();
                    if (line == null) break;
                    System.out.println("    " + line);
                }
                System.out.println("    ...");
            }

            // Step 2: Parse XML back
            System.out.println("\n--- Step 2: Parse XML back ---");
            List<WzImageProperty> parsed = WzXmlDeserializer.parseXml(xmlFile);
            check("XML parsed", parsed != null && !parsed.isEmpty());
            System.out.println("  Parsed " + parsed.size() + " top-level properties");

            // Step 3: Compare
            System.out.println("\n--- Step 3: Compare ---");
            List<WzImageProperty> original = img0526.getProperties();
            check("Same top-level count (" + original.size() + " vs " + parsed.size() + ")",
                original.size() == parsed.size());

            // Compare first item's structure
            if (!original.isEmpty() && !parsed.isEmpty()) {
                WzSubProperty origFirst = (WzSubProperty) original.get(0);
                WzSubProperty parsedFirst = (WzSubProperty) parsed.get(0);
                check("First prop name match: " + origFirst.getName(),
                    origFirst.getName().equals(parsedFirst.getName()));

                // Deep compare child counts
                int origCount = countAll(origFirst.getProperties());
                int parsedCount = countAll(parsedFirst.getProperties());
                check("First prop total children (" + origCount + " vs " + parsedCount + ")",
                    origCount == parsedCount);

                // Compare specific values
                WzImageProperty origSlot = findDeep(origFirst.getProperties(), "slotMax");
                WzImageProperty parsedSlot = findDeep(parsedFirst.getProperties(), "slotMax");
                if (origSlot != null && parsedSlot != null) {
                    check("slotMax value match",
                        ((WzIntProperty) origSlot).getInt() == ((WzIntProperty) parsedSlot).getInt());
                }

                WzImageProperty origCash = findDeep(origFirst.getProperties(), "cash");
                WzImageProperty parsedCash = findDeep(parsedFirst.getProperties(), "cash");
                if (origCash != null && parsedCash != null) {
                    check("cash value match",
                        ((WzIntProperty) origCash).getInt() == ((WzIntProperty) parsedCash).getInt());
                }

                // Check vector
                WzImageProperty origOrigin = findDeep(origFirst.getProperties(), "origin");
                WzImageProperty parsedOrigin = findDeep(parsedFirst.getProperties(), "origin");
                if (origOrigin instanceof WzVectorProperty && parsedOrigin instanceof WzVectorProperty) {
                    WzVectorProperty ov = (WzVectorProperty) origOrigin;
                    WzVectorProperty pv = (WzVectorProperty) parsedOrigin;
                    check("origin vector match (" + ov.getX().getInt() + "," + ov.getY().getInt() + ")",
                        ov.getX().getInt() == pv.getX().getInt() && ov.getY().getInt() == pv.getY().getInt());
                }

                // Check canvas exists after parse
                WzImageProperty parsedIcon = findDeep(parsedFirst.getProperties(), "icon");
                check("Canvas 'icon' preserved", parsedIcon instanceof WzCanvasProperty);
                if (parsedIcon instanceof WzCanvasProperty) {
                    WzCanvasProperty pc = (WzCanvasProperty) parsedIcon;
                    check("Canvas has PNG", pc.getPngProperty() != null);
                    if (pc.getPngProperty() != null) {
                        check("Canvas size 27x27",
                            pc.getPngProperty().getWidth() == 27 && pc.getPngProperty().getHeight() == 27);
                    }
                }
            }

            // Step 4: Full directory export test
            System.out.println("\n--- Step 4: Directory export ---");
            File fullExportDir = new File(xmlDir, "Cash");
            WzDirectory cashDir = (WzDirectory) wz.getRoot().getChild("Cash");
            int imgCount = cashDir.getWzImages().size();
            WzXmlSerializer.serializeDirectory(cashDir, fullExportDir);
            File[] xmlFiles = fullExportDir.listFiles((d, n) -> n.endsWith(".xml"));
            int exported = xmlFiles != null ? xmlFiles.length : 0;
            check("Exported " + exported + "/" + imgCount + " images as XML", exported == imgCount);
        }

        // Cleanup
        deleteRecursive(new File(xmlDir));

        System.out.printf("\n=== Results: %d/%d PASS ===%n", pass, pass + fail);
    }

    static void check(String name, boolean ok) {
        System.out.println("  " + (ok ? "PASS" : "FAIL") + ": " + name);
        if (ok) pass++; else fail++;
    }

    static int countAll(List<WzImageProperty> props) {
        if (props == null) return 0;
        int c = props.size();
        for (WzImageProperty p : props) c += countAll(p.getProperties());
        return c;
    }

    static WzImageProperty findDeep(List<WzImageProperty> props, String name) {
        if (props == null) return null;
        for (WzImageProperty p : props) {
            if (name.equals(p.getName())) return p;
            WzImageProperty found = findDeep(p.getProperties(), name);
            if (found != null) return found;
        }
        return null;
    }

    static void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) for (File c : children) deleteRecursive(c);
        }
        f.delete();
    }
}
