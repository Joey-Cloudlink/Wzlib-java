package wzlib;

import wzlib.util.WzBinaryReader;
import wzlib.util.WzTool;
import java.io.File;
import java.nio.file.*;

/**
 * Debug Map.wz/Mob.wz directory parsing failures and EOF issues.
 */
public class WzFixDebug {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Debug Map.wz ===");
        debugFile("C:\\Users\\Joey\\Documents\\楓之谷\\MapleStory客戶端\\乾淨主程式v113\\Map.wz");

        System.out.println("\n=== Debug Mob.wz ===");
        debugFile("C:\\Users\\Joey\\Documents\\楓之谷\\MapleStory客戶端\\乾淨主程式v113\\Mob.wz");

        System.out.println("\n=== Debug String.wz (EOF issue) ===");
        debugImageParse("C:\\Users\\Joey\\Documents\\楓之谷\\MapleStory客戶端\\乾淨主程式v113\\String.wz");
    }

    static void debugFile(String path) throws Exception {
        Path p = Paths.get(path);
        if (!Files.exists(p)) { System.out.println("Not found"); return; }

        byte[] wzIv = WzMapleVersion.EMS.getIv();
        try (WzBinaryReader reader = new WzBinaryReader(p.toFile(), wzIv)) {
            // Read header
            String ident = reader.readString(4);
            long fSize = reader.readInt64();
            long fStart = reader.readUInt32();
            String copyright = reader.readString((int)(fStart - 17));
            reader.readByte(); // unk
            long curPos = reader.getPosition();
            int remaining = (int)(fStart - curPos);
            if (remaining > 0) reader.readBytes(remaining);

            WzHeader header = new WzHeader();
            header.setIdent(ident);
            header.setFSize(fSize);
            header.setFStart(fStart);
            header.setCopyright(copyright);
            reader.setHeader(header);

            System.out.println("Ident: " + ident + ", FSize: " + fSize + ", FStart: " + fStart);

            // Read encVer
            reader.setPosition(fStart);
            int encVer = reader.readUInt16();
            System.out.println("encVer: " + encVer);

            // Compute hash for v113
            long versionHash = 0;
            for (char ch : "113".toCharArray()) {
                versionHash = ((versionHash * 32) + (ch & 0xFF) + 1) & 0xFFFFFFFFL;
            }
            reader.setHash(versionHash);
            System.out.println("versionHash for 113: " + versionHash);

            // Try reading first few directory entries
            long dirStart = reader.getPosition();
            System.out.println("Directory starts at: " + dirStart);

            try {
                int entryCount = reader.readCompressedInt();
                System.out.println("Entry count: " + entryCount);

                for (int i = 0; i < Math.min(entryCount, 5); i++) {
                    long entryPos = reader.getPosition();
                    byte type = reader.readByte();
                    System.out.printf("  Entry %d at pos %d: type=%d (0x%02X)%n", i, entryPos, type & 0xFF, type & 0xFF);

                    if ((type & 0xFF) == 3 || (type & 0xFF) == 4) {
                        String fname = reader.readString();
                        System.out.println("    name: " + fname);
                        int fsize = reader.readCompressedInt();
                        int checksum = reader.readCompressedInt();
                        long offset = reader.readOffset();
                        System.out.printf("    size=%d, checksum=%d, offset=%d%n", fsize, checksum, offset);
                    } else if ((type & 0xFF) == 2) {
                        int strOffset = reader.readInt32();
                        long remPos = reader.getPosition();
                        reader.setPosition(header.getFStart() + strOffset);
                        byte realType = reader.readByte();
                        String fname = reader.readString();
                        reader.setPosition(remPos);
                        System.out.printf("    strOffset=%d, realType=%d, name=%s%n", strOffset, realType & 0xFF, fname);
                        int fsize = reader.readCompressedInt();
                        int checksum = reader.readCompressedInt();
                        long offset = reader.readOffset();
                        System.out.printf("    size=%d, checksum=%d, offset=%d%n", fsize, checksum, offset);
                    } else if ((type & 0xFF) == 1) {
                        int unk = reader.readInt32();
                        reader.readInt16();
                        long offs = reader.readOffset();
                        System.out.printf("    type1: unk=%d, offset=%d%n", unk, offs);
                    } else {
                        System.out.println("    UNKNOWN TYPE - next 10 bytes:");
                        byte[] next = reader.readBytes(10);
                        StringBuilder sb = new StringBuilder();
                        for (byte b : next) sb.append(String.format("%02X ", b & 0xFF));
                        System.out.println("    " + sb);
                        break;
                    }
                }
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    static void debugImageParse(String path) throws Exception {
        Path p = Paths.get(path);
        try (WzFile wz = new WzFile(p.toString(), (short)113, WzMapleVersion.EMS)) {
            wz.parseWzFile();
            System.out.println("Images: " + wz.getRoot().countImages());
            for (WzImage img : wz.getRoot().getWzImages()) {
                System.out.printf("  %s offset=%d size=%d ", img.getName(), img.getOffset(), img.getBlockSize());
                try {
                    int props = img.getProperties().size();
                    System.out.println("props=" + props);
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    System.out.println("FAIL: " + cause.getClass().getSimpleName() + " at " + cause.getStackTrace()[0]);
                }
            }
        }
    }
}
