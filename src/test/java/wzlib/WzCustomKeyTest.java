package wzlib;

import wzlib.crypto.WzCryptoConstants;
import wzlib.crypto.WzKeyGenerator;
import wzlib.crypto.WzMutableKey;

import java.nio.file.*;
import java.util.Arrays;

/**
 * Test private server support:
 * 1. Custom IV constructor
 * 2. Custom UserKey (global setting)
 * 3. Verify standard files still work
 */
public class WzCustomKeyTest {

    static final String CLIENT_DIR = "C:\\Users\\Joey\\Documents\\楓之谷\\MapleStory客戶端\\乾淨主程式v113";
    static int pass = 0, fail = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Private Server Support Test ===\n");

        // Test 1: Custom IV constructor (use EMS IV manually)
        System.out.println("--- Test 1: Custom IV constructor ---");
        byte[] emsIv = {(byte) 0xB9, 0x7D, 0x63, (byte) 0xE9};
        Path itemWz = Paths.get(CLIENT_DIR, "Item.wz");
        try (WzFile wz = new WzFile(itemWz.toString(), (short) 113, emsIv)) {
            wz.parseWzFile();
            WzDirectory root = wz.getRoot();
            check("Custom IV: parse OK", root.countImages() == 183);
            check("Custom IV: correct dir name",
                !root.getWzDirectories().isEmpty() && "Cash".equals(root.getWzDirectories().get(0).getName()));
        } catch (Exception e) {
            check("Custom IV: " + e.getMessage(), false);
        }

        // Test 2: Custom IV auto-detect
        System.out.println("\n--- Test 2: Custom IV auto-detect ---");
        try (WzFile wz = new WzFile(itemWz.toString(), emsIv)) {
            wz.parseWzFile();
            check("Custom IV auto-detect: v" + wz.getVersion() + " imgs=" + wz.getRoot().countImages(),
                wz.getRoot().countImages() == 183);
        } catch (Exception e) {
            check("Custom IV auto-detect: " + e.getMessage(), false);
        }

        // Test 3: Wrong IV should fail or produce garbled names
        System.out.println("\n--- Test 3: Wrong IV produces garbled names ---");
        byte[] wrongIv = {0x11, 0x22, 0x33, 0x44};
        try (WzFile wz = new WzFile(itemWz.toString(), (short) 113, wrongIv)) {
            wz.parseWzFile();
            String dirName = wz.getRoot().getWzDirectories().isEmpty() ? "" :
                wz.getRoot().getWzDirectories().get(0).getName();
            boolean garbled = !dirName.equals("Cash"); // Should NOT be "Cash" with wrong IV
            check("Wrong IV: garbled names (got '" + dirName + "')", garbled);
        } catch (Exception e) {
            check("Wrong IV: exception (expected)", true);
        }

        // Test 4: Custom UserKey
        System.out.println("\n--- Test 4: Custom UserKey ---");
        check("Default UserKey is active", WzCryptoConstants.isDefaultUserKey());

        // Set a custom UserKey (just modify one byte)
        byte[] customKey = WzCryptoConstants.MAPLESTORY_USERKEY_DEFAULT.clone();
        customKey[0] = 0x42; // Change first byte
        WzCryptoConstants.setActiveUserKey(customKey);
        check("Custom UserKey is now active", !WzCryptoConstants.isDefaultUserKey());

        // Try to open with custom key — should produce garbled names since the file uses default key
        try (WzFile wz = new WzFile(itemWz.toString(), (short) 113, WzMapleVersion.EMS)) {
            wz.parseWzFile();
            String dirName = wz.getRoot().getWzDirectories().isEmpty() ? "" :
                wz.getRoot().getWzDirectories().get(0).getName();
            boolean garbled = !dirName.equals("Cash");
            check("Custom UserKey: different decryption (got '" + dirName + "')", garbled);
        } catch (Exception e) {
            check("Custom UserKey: exception (expected)", true);
        }

        // Reset to default
        WzCryptoConstants.resetActiveUserKey();
        check("Reset to default UserKey", WzCryptoConstants.isDefaultUserKey());

        // Test 5: Verify standard files still work after reset
        System.out.println("\n--- Test 5: Standard files still work ---");
        try (WzFile wz = new WzFile(itemWz.toString(), (short) 113, WzMapleVersion.EMS)) {
            wz.parseWzFile();
            check("Standard after reset: imgs=" + wz.getRoot().countImages(),
                wz.getRoot().countImages() == 183);
        } catch (Exception e) {
            check("Standard after reset: " + e.getMessage(), false);
        }

        // Test 6: WzKeyGenerator with custom 128-byte UserKey
        System.out.println("\n--- Test 6: WzKeyGenerator custom key ---");
        byte[] customUserKey128 = new byte[128];
        Arrays.fill(customUserKey128, (byte) 0x42);
        WzMutableKey key1 = WzKeyGenerator.generateWzKey(emsIv, customUserKey128);
        WzMutableKey key2 = WzKeyGenerator.generateWzKey(emsIv); // uses active (default)
        key1.ensureKeySize(16);
        key2.ensureKeySize(16);
        check("Different keys produce different results", !Arrays.equals(key1.getKeys(), key2.getKeys()));

        // Test 7: WzMapleVersion enum completeness
        System.out.println("\n--- Test 7: WzMapleVersion enum ---");
        for (WzMapleVersion v : WzMapleVersion.values()) {
            System.out.println("  " + v.name() + " iv=" +
                (v.getIv() != null ? hex(v.getIv()) : "null"));
        }
        check("All versions defined", WzMapleVersion.values().length == 7);

        System.out.printf("\n=== Results: %d/%d PASS ===%n", pass, pass + fail);
    }

    static void check(String name, boolean ok) {
        System.out.println("  " + (ok ? "PASS" : "FAIL") + ": " + name);
        if (ok) pass++; else fail++;
    }

    static String hex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) sb.append(String.format("%02X", b & 0xFF));
        return sb.toString();
    }
}
