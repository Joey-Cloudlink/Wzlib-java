package wzlib.crypto;

/**
 * Ported from:
 *   MapleLib/WzLib/WzAESConstant.cs
 *   MapleLib/MapleCryptoLib/MapleCryptoConstants.cs
 *
 * All constants copied verbatim from C# source.
 */
public final class WzCryptoConstants {

    private WzCryptoConstants() {}

    // ---- From WzAESConstant.cs ----

    /** IV for GMS */
    public static final byte[] WZ_GMSIV = {0x4D, 0x23, (byte) 0xC7, 0x2B};

    /** IV for MSEA/EMS/KMS */
    public static final byte[] WZ_MSEAIV = {(byte) 0xB9, 0x7D, 0x63, (byte) 0xE9};

    /** IV for BMS/Classic (no encryption) */
    public static final byte[] WZ_BMSCLASSIC = {0x00, 0x00, 0x00, 0x00};

    /** Constant used in WZ offset encryption */
    public static final long WZ_OffsetConstant = 0x581C3F6DL;

    // ---- From MapleCryptoConstants.cs ----

    /**
     * Default AES UserKey used by MapleStory (128 bytes = 32 DWORDs).
     * Copied verbatim from C#: MapleCryptoConstants.MAPLESTORY_USERKEY_DEFAULT
     */
    public static final byte[] MAPLESTORY_USERKEY_DEFAULT = {
        0x13, 0x00, 0x00, 0x00, 0x52, 0x00, 0x00, 0x00, 0x2A, 0x00, 0x00, 0x00, 0x5B, 0x00, 0x00, 0x00,
        0x08, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x10, 0x00, 0x00, 0x00, 0x60, 0x00, 0x00, 0x00,
        0x06, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x43, 0x00, 0x00, 0x00, 0x0F, 0x00, 0x00, 0x00,
        (byte) 0xB4, 0x00, 0x00, 0x00, 0x4B, 0x00, 0x00, 0x00, 0x35, 0x00, 0x00, 0x00, 0x05, 0x00, 0x00, 0x00,
        0x1B, 0x00, 0x00, 0x00, 0x0A, 0x00, 0x00, 0x00, 0x5F, 0x00, 0x00, 0x00, 0x09, 0x00, 0x00, 0x00,
        0x0F, 0x00, 0x00, 0x00, 0x50, 0x00, 0x00, 0x00, 0x0C, 0x00, 0x00, 0x00, 0x1B, 0x00, 0x00, 0x00,
        0x33, 0x00, 0x00, 0x00, 0x55, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x09, 0x00, 0x00, 0x00,
        0x52, 0x00, 0x00, 0x00, (byte) 0xDE, 0x00, 0x00, 0x00, (byte) 0xC7, 0x00, 0x00, 0x00, 0x1E, 0x00, 0x00, 0x00
    };

    /**
     * The active AES UserKey used by wzlib.
     * Defaults to MAPLESTORY_USERKEY_DEFAULT.
     * Can be replaced for private servers (same as C#'s MapleCryptoConstants.UserKey_WzLib).
     * Ported from: MapleCryptoConstants.UserKey_WzLib
     */
    private static byte[] activeUserKey = MAPLESTORY_USERKEY_DEFAULT.clone();

    /** Get the current active UserKey (128 bytes). */
    public static byte[] getActiveUserKey() {
        return activeUserKey.clone();
    }

    /**
     * Set a custom UserKey for private servers.
     * Ported from: C# setting MapleCryptoConstants.UserKey_WzLib
     */
    public static void setActiveUserKey(byte[] customUserKey) {
        if (customUserKey.length != 128) {
            throw new IllegalArgumentException("UserKey expects 128 bytes, got " + customUserKey.length);
        }
        activeUserKey = customUserKey.clone();
    }

    /** Reset to default MapleStory UserKey. */
    public static void resetActiveUserKey() {
        activeUserKey = MAPLESTORY_USERKEY_DEFAULT.clone();
    }

    /** Check if the active UserKey is the default one. */
    public static boolean isDefaultUserKey() {
        return java.util.Arrays.equals(activeUserKey, MAPLESTORY_USERKEY_DEFAULT);
    }

    /**
     * Trims the AES UserKey (128 bytes -> 32 bytes) for use as AES key.
     * Takes every 16th byte (i.e. first byte of each DWORD).
     * Ported from: MapleCryptoConstants.GetTrimmedUserKey()
     */
    public static byte[] getTrimmedUserKey(byte[] userKey) {
        if (userKey.length != 128) {
            throw new IllegalArgumentException("UserKey expects 128 bytes, got " + userKey.length);
        }
        byte[] key = new byte[32];
        for (int i = 0; i < 128; i += 16) {
            key[i / 4] = userKey[i];
        }
        return key;
    }
}
