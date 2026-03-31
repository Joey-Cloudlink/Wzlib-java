package wzlib;

import java.io.File;
import java.util.List;

/**
 * Phase 5 test: Read → Save → Re-read → Compare.
 */
public class WzSaveTest {

    public static void main(String[] args) throws Exception {
        String inputPath = args.length > 0 ? args[0] : "C:/Users/Joey/Desktop/item.wz";
        String outputPath = "C:/Users/Joey/Desktop/item_saved.wz";
        WzMapleVersion ver = args.length > 1 ? WzMapleVersion.valueOf(args[1]) : WzMapleVersion.EMS;

        System.out.println("=== Phase 5: Round-trip Save Test ===");
        System.out.println("Input:  " + inputPath);
        System.out.println("Output: " + outputPath);
        System.out.println();

        // Step 1: Read original
        int origDirs, origImages;
        short origVersion;
        String[] origImgNames;
        try (WzFile wz = new WzFile(inputPath, ver)) {
            wz.parseWzFile();
            origVersion = wz.getVersion();
            origDirs = countDirs(wz.getRoot());
            origImages = wz.getRoot().countImages();
            origImgNames = getFirstImageNames(wz.getRoot());

            System.out.println("Original: version=" + origVersion + ", dirs=" + origDirs + ", images=" + origImages);

            // Step 2: Save to new file
            System.out.println("Saving...");
            wz.saveToDisk(outputPath);
        }

        long origSize = new File(inputPath).length();
        long savedSize = new File(outputPath).length();
        System.out.println("Original size: " + origSize + " bytes");
        System.out.println("Saved size:    " + savedSize + " bytes");
        System.out.println();

        // Step 3: Re-read saved file
        System.out.println("Re-reading saved file...");
        int savedDirs, savedImages;
        short savedVersion;
        String[] savedImgNames;
        try (WzFile wz2 = new WzFile(outputPath, ver)) {
            wz2.parseWzFile();
            savedVersion = wz2.getVersion();
            savedDirs = countDirs(wz2.getRoot());
            savedImages = wz2.getRoot().countImages();
            savedImgNames = getFirstImageNames(wz2.getRoot());

            System.out.println("Saved:    version=" + savedVersion + ", dirs=" + savedDirs + ", images=" + savedImages);
            System.out.println();

            // Step 4: Compare
            boolean pass = true;
            if (origDirs != savedDirs) { System.out.println("FAIL: dir count mismatch"); pass = false; }
            if (origImages != savedImages) { System.out.println("FAIL: image count mismatch"); pass = false; }

            for (int i = 0; i < origImgNames.length && i < savedImgNames.length; i++) {
                if (!origImgNames[i].equals(savedImgNames[i])) {
                    System.out.println("FAIL: image name mismatch at " + i + ": " + origImgNames[i] + " vs " + savedImgNames[i]);
                    pass = false;
                }
            }

            // Try parsing an image from the saved file
            WzDirectory firstDir = wz2.getRoot().getWzDirectories().get(0);
            WzImage firstImg = firstDir.getWzImages().get(0);
            System.out.println("Parsing image from saved file: " + firstDir.getName() + "/" + firstImg.getName());
            List<WzImageProperty> props = firstImg.getProperties();
            System.out.println("  Properties count: " + props.size());
            if (props.isEmpty()) { System.out.println("FAIL: no properties in saved image"); pass = false; }

            if (pass) {
                System.out.println("\n=== ALL CHECKS PASSED ===");
            } else {
                System.out.println("\n=== SOME CHECKS FAILED ===");
            }
        }

        // Cleanup
        new File(outputPath).delete();
        new File(outputPath + ".TEMP").delete();
    }

    static int countDirs(WzDirectory dir) {
        int count = dir.getWzDirectories().size();
        for (WzDirectory sub : dir.getWzDirectories()) count += countDirs(sub);
        return count;
    }

    static String[] getFirstImageNames(WzDirectory root) {
        String[] names = new String[root.getWzDirectories().size()];
        for (int i = 0; i < names.length; i++) {
            WzDirectory sub = root.getWzDirectories().get(i);
            names[i] = sub.getName() + "/" + (sub.getWzImages().isEmpty() ? "(empty)" : sub.getWzImages().get(0).getName());
        }
        return names;
    }
}
