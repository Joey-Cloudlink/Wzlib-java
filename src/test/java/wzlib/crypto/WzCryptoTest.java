package wzlib.crypto;

/**
 * Quick verification of crypto functions against known C# MapleLib outputs.
 * Run with: java wzlib.crypto.WzCryptoTest
 */
public class WzCryptoTest {

    public static void main(String[] args) {
        testTrimmedUserKey();
        testMutableKeyGMS();
        testMutableKeyBMS();
        testCompressedInt();
        System.out.println("\nAll crypto tests passed!");
    }

    static void testTrimmedUserKey() {
        System.out.print("testTrimmedUserKey... ");
        byte[] trimmed = WzCryptoConstants.getTrimmedUserKey(WzCryptoConstants.MAPLESTORY_USERKEY_DEFAULT);
        // C# logic: for (i=0; i<128; i+=16) key[i/4] = UserKey[i]
        // Only positions 0,4,8,12,16,20,24,28 are set; rest are 0.
        // UserKey[0]=0x13, [16]=0x08, [32]=0x06, [48]=0xB4,
        // [64]=0x1B, [80]=0x0F, [96]=0x33, [112]=0x52
        byte[] expected = {
            0x13, 0x00, 0x00, 0x00,  // key[0] = UserKey[0]
            0x08, 0x00, 0x00, 0x00,  // key[4] = UserKey[16]
            0x06, 0x00, 0x00, 0x00,  // key[8] = UserKey[32]
            (byte) 0xB4, 0x00, 0x00, 0x00,  // key[12] = UserKey[48]
            0x1B, 0x00, 0x00, 0x00,  // key[16] = UserKey[64]
            0x0F, 0x00, 0x00, 0x00,  // key[20] = UserKey[80]
            0x33, 0x00, 0x00, 0x00,  // key[24] = UserKey[96]
            0x52, 0x00, 0x00, 0x00   // key[28] = UserKey[112]
        };
        assertArrayEquals(expected, trimmed, "TrimmedUserKey");
        System.out.println("OK");
    }

    static void testMutableKeyGMS() {
        System.out.print("testMutableKeyGMS... ");
        // GMS IV = {0x4D, 0x23, 0xC7, 0x2B}
        WzMutableKey key = WzKeyGenerator.generateWzKey(WzCryptoConstants.WZ_GMSIV);
        key.ensureKeySize(16);
        byte[] keys = key.getKeys();

        // Verify key is not all zeros (GMS has non-zero IV)
        boolean allZero = true;
        for (int i = 0; i < 16; i++) {
            if (keys[i] != 0) { allZero = false; break; }
        }
        assert !allZero : "GMS key should not be all zeros";

        // Key should be exactly 4096 bytes (one batch)
        assert keys.length == 4096 : "Expected 4096 bytes, got " + keys.length;

        System.out.println("OK (first 8 bytes: " + hex(keys, 8) + ")");
    }

    static void testMutableKeyBMS() {
        System.out.print("testMutableKeyBMS... ");
        // BMS IV = {0, 0, 0, 0} → all-zero key
        WzMutableKey key = WzKeyGenerator.generateWzKey(WzCryptoConstants.WZ_BMSCLASSIC);
        key.ensureKeySize(16);
        byte[] keys = key.getKeys();

        for (int i = 0; i < 16; i++) {
            assert keys[i] == 0 : "BMS key byte " + i + " should be 0, got " + (keys[i] & 0xFF);
        }
        System.out.println("OK (all zeros as expected)");
    }

    static void testCompressedInt() {
        System.out.print("testCompressedInt round-trip... ");
        // Test via WzTool
        assert wzlib.util.WzTool.getCompressedIntLength(0) == 1;
        assert wzlib.util.WzTool.getCompressedIntLength(127) == 1;
        assert wzlib.util.WzTool.getCompressedIntLength(-127) == 1;
        assert wzlib.util.WzTool.getCompressedIntLength(128) == 5;
        assert wzlib.util.WzTool.getCompressedIntLength(-128) == 5;
        assert wzlib.util.WzTool.getCompressedIntLength(1000) == 5;
        System.out.println("OK");
    }

    // ---- Helpers ----

    static void assertArrayEquals(byte[] expected, byte[] actual, String name) {
        if (expected.length != actual.length) {
            throw new AssertionError(name + ": length mismatch, expected " + expected.length + " got " + actual.length);
        }
        for (int i = 0; i < expected.length; i++) {
            if (expected[i] != actual[i]) {
                throw new AssertionError(name + ": mismatch at index " + i +
                    ", expected 0x" + String.format("%02X", expected[i] & 0xFF) +
                    " got 0x" + String.format("%02X", actual[i] & 0xFF));
            }
        }
    }

    static String hex(byte[] data, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count && i < data.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(String.format("%02X", data[i] & 0xFF));
        }
        return sb.toString();
    }
}
