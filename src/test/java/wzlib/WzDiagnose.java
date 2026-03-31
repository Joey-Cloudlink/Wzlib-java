package wzlib;

import wzlib.util.WzBinaryReader;
import java.io.File;
import java.nio.file.*;

/**
 * Diagnose high-version TMS .wz files — check header, format, encryption.
 */
public class WzDiagnose {

    public static void main(String[] args) throws Exception {
        String[] files = {
            "C:/Users/Joey/Desktop/同步UI/UI/UI.wz",
            "C:/Users/Joey/Desktop/同步UI/UI/UI_000.wz",
            // UI_TOTAL.wz is 4.1GB, skip for now
        };

        for (String path : files) {
            System.out.println("=== " + Paths.get(path).getFileName() + " (" + Files.size(Paths.get(path)) + " bytes) ===");
            diagnoseHeader(path);

            // Try all versions
            System.out.println("  Version detection:");
            WzMapleVersion[] vers = {WzMapleVersion.BMS, WzMapleVersion.EMS, WzMapleVersion.GMS};
            for (WzMapleVersion ver : vers) {
                // Auto-detect
                try (WzFile wz = new WzFile(path, ver)) {
                    wz.parseWzFile();
                    WzDirectory root = wz.getRoot();
                    System.out.printf("    %s auto: v%d 64bit=%s dirs=%d imgs=%d%n",
                        ver, wz.getVersion(), wz.is64BitWzFile(),
                        root.getWzDirectories().size(), root.countImages());

                    // Try parsing first image
                    WzImage firstImg = findFirst(root);
                    if (firstImg != null) {
                        try {
                            int props = firstImg.getProperties().size();
                            System.out.printf("      First image: %s (%d props)%n", firstImg.getName(), props);
                        } catch (Exception e) {
                            Throwable cause = e.getCause() != null ? e.getCause() : e;
                            System.out.printf("      First image: %s FAIL: %s: %s%n",
                                firstImg.getName(), cause.getClass().getSimpleName(), cause.getMessage());
                        }
                    }
                    break; // found working version
                } catch (Exception e) {
                    System.out.printf("    %s auto: FAIL (%s)%n", ver, e.getMessage());
                }
            }
            System.out.println();
        }
    }

    static void diagnoseHeader(String path) throws Exception {
        // Read raw header bytes
        byte[] raw = new byte[80];
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(path, "r")) {
            raf.readFully(raw, 0, (int) Math.min(80, raf.length()));
        }

        // Print hex
        System.out.print("  Header hex: ");
        for (int i = 0; i < Math.min(20, raw.length); i++) System.out.printf("%02X ", raw[i] & 0xFF);
        System.out.println();

        // Parse header fields
        String ident = new String(raw, 0, 4);
        long fSize = readLE8(raw, 4);
        int fStart = readLE4(raw, 12);
        System.out.printf("  Ident=%s FSize=%d FStart=%d%n", ident, fSize, fStart);

        if (raw.length > fStart + 1) {
            int encVer = (raw[fStart] & 0xFF) | ((raw[fStart + 1] & 0xFF) << 8);
            System.out.printf("  encVer at %d: %d (0x%04X) → %s%n", fStart, encVer, encVer,
                encVer > 0xFF ? "64-bit (no encVer)" : "32-bit");
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

    static int readLE4(byte[] d, int o) { return (d[o]&0xFF)|((d[o+1]&0xFF)<<8)|((d[o+2]&0xFF)<<16)|((d[o+3]&0xFF)<<24); }
    static long readLE8(byte[] d, int o) { return (readLE4(d,o)&0xFFFFFFFFL)|((long)readLE4(d,o+4)<<32); }
}
