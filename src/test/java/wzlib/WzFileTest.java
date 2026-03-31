package wzlib;

import java.io.File;

/**
 * Phase 2 test: open a real .wz file and print the directory tree.
 * Usage: java wzlib.WzFileTest <path_to_wz_file> [BMS|GMS|EMS]
 */
public class WzFileTest {

    public static void main(String[] args) throws Exception {
        String wzPath = args.length > 0 ? args[0] : "C:/Users/Joey/Desktop/item.wz";
        WzMapleVersion ver = WzMapleVersion.BMS;
        if (args.length > 1) {
            ver = WzMapleVersion.valueOf(args[1]);
        }

        System.out.println("Opening: " + wzPath);
        System.out.println("Version: " + ver);
        System.out.println();

        try (WzFile wz = new WzFile(wzPath, ver)) {
            wz.parseWzFile();

            System.out.println("=== WZ File Info ===");
            System.out.println("Name: " + wz.getName());
            System.out.println("Header Ident: " + wz.getHeader().getIdent());
            System.out.println("Header FSize: " + wz.getHeader().getFSize());
            System.out.println("Header FStart: " + wz.getHeader().getFStart());
            System.out.println("Header Copyright: " + wz.getHeader().getCopyright());
            System.out.println("Detected Version: " + wz.getVersion());
            System.out.println("64-bit: " + wz.is64BitWzFile());
            System.out.println();

            // Print directory tree
            WzDirectory root = wz.getRoot();
            if (root != null) {
                System.out.println("=== Directory Tree ===");
                printTree(root, 0);

                System.out.println();
                System.out.println("Total directories: " + countDirs(root));
                System.out.println("Total images: " + root.countImages());
            } else {
                System.out.println("ERROR: Root directory is null!");
            }
        }
    }

    static void printTree(WzDirectory dir, int indent) {
        String pad = " ".repeat(indent);

        // Print images (limit to first 20 at each level to avoid flooding)
        int imgCount = 0;
        for (WzImage img : dir.getWzImages()) {
            if (imgCount < 20) {
                System.out.println(pad + "[IMG] " + img.getName() +
                    " (offset=" + img.getOffset() + ", size=" + img.getBlockSize() + ")");
            }
            imgCount++;
        }
        if (imgCount > 20) {
            System.out.println(pad + "  ... and " + (imgCount - 20) + " more images");
        }

        // Print subdirectories
        for (WzDirectory sub : dir.getWzDirectories()) {
            System.out.println(pad + "[DIR] " + sub.getName());
            printTree(sub, indent + 2);
        }
    }

    static int countDirs(WzDirectory dir) {
        int count = dir.getWzDirectories().size();
        for (WzDirectory sub : dir.getWzDirectories()) {
            count += countDirs(sub);
        }
        return count;
    }
}
