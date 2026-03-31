package wzlib;

import wzlib.crypto.WzKeyGenerator;
import wzlib.crypto.WzMutableKey;
import wzlib.util.WzBinaryReader;
import wzlib.util.WzBinaryWriter;
import wzlib.util.WzTool;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * List.wz 檔案的解析與寫入工具 — 儲存 WZ 路徑字串清單的特殊格式。
 * Parser and writer for List.wz files — a special format storing WZ path strings.
 *
 * <p>Ported from: MapleLib/WzLib/WzListFile.cs (ListFileParser)</p>
 */
public final class WzListFile {

    private WzListFile() {}

    /**
     * 解析 List.wz 檔案為路徑字串清單。
     * Parse a List.wz file into a list of path strings.
     */
    public static List<String> parse(String filePath, WzMapleVersion version) throws IOException {
        return parse(filePath, WzTool.getIvByMapleVersion(version));
    }

    /**
     * Parse a List.wz file with custom IV.
     * Ported from: ListFileParser.ParseListFile(string, byte[])
     */
    public static List<String> parse(String filePath, byte[] wzIv) throws IOException {
        byte[] fileBytes = readAllBytes(filePath);
        WzMutableKey wzKey = WzKeyGenerator.generateWzKey(wzIv);

        List<String> entries = new ArrayList<>();
        int pos = 0;

        while (pos < fileBytes.length) {
            // Read int32 length (little-endian)
            if (pos + 4 > fileBytes.length) break;
            int len = (fileBytes[pos] & 0xFF)
                    | ((fileBytes[pos + 1] & 0xFF) << 8)
                    | ((fileBytes[pos + 2] & 0xFF) << 16)
                    | ((fileBytes[pos + 3] & 0xFF) << 24);
            pos += 4;

            // Read len * int16 chars (little-endian)
            char[] chars = new char[len];
            for (int i = 0; i < len; i++) {
                if (pos + 2 > fileBytes.length) break;
                chars[i] = (char) ((fileBytes[pos] & 0xFF) | ((fileBytes[pos + 1] & 0xFF) << 8));
                pos += 2;
            }

            // Read encrypted null (uint16)
            if (pos + 2 <= fileBytes.length) {
                pos += 2;
            }

            // Decrypt string using WzKey
            String decrypted = decryptListString(chars, wzKey);
            entries.add(decrypted);
        }

        // C# quirk: last entry's last char is changed from '/' to 'g'
        if (!entries.isEmpty()) {
            int lastIndex = entries.size() - 1;
            String lastEntry = entries.get(lastIndex);
            if (lastEntry.length() > 0) {
                entries.set(lastIndex, lastEntry.substring(0, lastEntry.length() - 1) + "g");
            }
        }

        return entries;
    }

    /**
     * 將路徑字串清單儲存為 List.wz 檔案。
     * Save a list of path strings to a List.wz file.
     */
    public static void save(String outputPath, WzMapleVersion version, List<String> entries) throws IOException {
        save(outputPath, WzTool.getIvByMapleVersion(version), entries);
    }

    public static void save(String outputPath, byte[] wzIv, List<String> entries) throws IOException {
        WzMutableKey wzKey = WzKeyGenerator.generateWzKey(wzIv);

        // C# quirk: change last entry's last char from 'g' to '/'
        List<String> writableEntries = new ArrayList<>(entries);
        if (!writableEntries.isEmpty()) {
            int lastIndex = writableEntries.size() - 1;
            String lastEntry = writableEntries.get(lastIndex);
            if (lastEntry.length() > 0) {
                writableEntries.set(lastIndex, lastEntry.substring(0, lastEntry.length() - 1) + "/");
            }
        }

        try (FileOutputStream fos = new FileOutputStream(outputPath);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            for (String entry : writableEntries) {
                // Write int32 length
                int len = entry.length();
                writeLE4(bos, len);

                // Encrypt and write chars (including null terminator)
                String withNull = entry + "\0";
                char[] encrypted = encryptListString(withNull, wzKey);
                for (char c : encrypted) {
                    bos.write(c & 0xFF);
                    bos.write((c >> 8) & 0xFF);
                }
            }
            bos.flush();
        }
    }

    /**
     * 檢查檔案是否為 List.wz 格式（非標準 PKG1 格式）。
     * Check if a file is a List.wz (not standard PKG1 format).
     */
    public static boolean isListFile(String filePath) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            if (raf.length() < 4) return true;
            int header = Integer.reverseBytes(raf.readInt()); // read as big-endian, reverse
            // Actually read as little-endian
            raf.seek(0);
            byte[] buf = new byte[4];
            raf.readFully(buf);
            int headerLE = (buf[0] & 0xFF) | ((buf[1] & 0xFF) << 8)
                    | ((buf[2] & 0xFF) << 16) | ((buf[3] & 0xFF) << 24);
            return headerLE != WzTool.WZ_HEADER; // not "PKG1"
        }
    }

    // ---- Internal helpers ----

    /**
     * Decrypt List.wz string (XOR with WzKey, no mask).
     * Ported from: WzBinaryReader.DecryptString()
     */
    private static String decryptListString(char[] chars, WzMutableKey wzKey) {
        char[] output = new char[chars.length];
        for (int i = 0; i < chars.length; i++) {
            output[i] = (char) (chars[i] ^ (char) (((wzKey.at(i * 2 + 1) & 0xFF) << 8) + (wzKey.at(i * 2) & 0xFF)));
        }
        return new String(output);
    }

    /**
     * Encrypt List.wz string (XOR with WzKey).
     * Ported from: WzBinaryWriter.EncryptString()
     */
    private static char[] encryptListString(String str, WzMutableKey wzKey) {
        char[] output = new char[str.length()];
        for (int i = 0; i < str.length(); i++) {
            output[i] = (char) (str.charAt(i) ^ (char) (((wzKey.at(i * 2 + 1) & 0xFF) << 8) + (wzKey.at(i * 2) & 0xFF)));
        }
        return output;
    }

    private static byte[] readAllBytes(String filePath) throws IOException {
        File file = new File(filePath);
        byte[] data = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            int read = 0;
            while (read < data.length) {
                int r = fis.read(data, read, data.length - read);
                if (r < 0) break;
                read += r;
            }
        }
        return data;
    }

    private static void writeLE4(OutputStream os, int val) throws IOException {
        os.write(val & 0xFF);
        os.write((val >> 8) & 0xFF);
        os.write((val >> 16) & 0xFF);
        os.write((val >> 24) & 0xFF);
    }
}
