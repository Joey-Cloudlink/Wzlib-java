package wzlib.util;

import wzlib.crypto.WzCryptoConstants;
import wzlib.crypto.WzKeyGenerator;
import wzlib.crypto.WzMutableKey;
import wzlib.WzMapleVersion;

import java.util.HashMap;
import java.util.Map;

/**
 * Ported from: MapleLib/WzLib/Util/WzTool.cs
 */
public final class WzTool {

    private WzTool() {}

    /** "PKG1" as int representation (little-endian) */
    public static final int WZ_HEADER = 0x31474B50;

    public static Map<String, Integer> stringCache = new HashMap<>();

    /**
     * Ported from: WzTool.RotateLeft(UInt32, byte)
     * Java note: uses long to emulate uint32
     */
    public static long rotateLeft(long x, byte n) {
        x &= 0xFFFFFFFFL; // ensure uint32
        return ((x << n) | (x >>> (32 - n))) & 0xFFFFFFFFL;
    }

    /**
     * Ported from: WzTool.RotateRight(UInt32, byte)
     */
    public static long rotateRight(long x, byte n) {
        x &= 0xFFFFFFFFL;
        return ((x >>> n) | (x << (32 - n))) & 0xFFFFFFFFL;
    }

    /**
     * Ported from: WzTool.GetCompressedIntLength(int)
     */
    public static int getCompressedIntLength(int i) {
        if (i > 127 || i < -127) {
            return 5;
        }
        return 1;
    }

    /**
     * Ported from: WzTool.GetEncodedStringLength(string)
     */
    public static int getEncodedStringLength(String s) {
        if (s == null || s.isEmpty()) {
            return 1;
        }

        boolean unicode = false;
        int length = s.length();

        for (char c : s.toCharArray()) {
            if (c > 255) {
                unicode = true;
                break;
            }
        }
        int prefixLength = length > (unicode ? 126 : 127) ? 5 : 1;
        int encodedLength = unicode ? length * 2 : length;

        return prefixLength + encodedLength;
    }

    /**
     * Ported from: WzTool.GetWzObjectValueLength(string, byte)
     */
    public static int getWzObjectValueLength(String s, byte type) {
        String storeName = type + "_" + s;
        if (s.length() > 4 && stringCache.containsKey(storeName)) {
            return 5;
        } else {
            stringCache.put(storeName, 1);
            return 1 + getEncodedStringLength(s);
        }
    }

    /**
     * Get WZ encryption IV from maple version.
     * Ported from: WzTool.GetIvByMapleVersion(WzMapleVersion)
     */
    /**
     * Get WZ encryption IV from maple version.
     * Ported from: WzTool.GetIvByMapleVersion(WzMapleVersion)
     */
    public static byte[] getIvByMapleVersion(WzMapleVersion ver) {
        switch (ver) {
            case GMS:
                return WzCryptoConstants.WZ_GMSIV.clone();
            case EMS:
                return WzCryptoConstants.WZ_MSEAIV.clone();
            case GENERATE:
                return new byte[4]; // empty IV for generating new files
            case CUSTOM:
            case GETFROMZLZ:
                return null; // handled by WzFile constructor
            case BMS:
            case CLASSIC:
            default:
                return WzCryptoConstants.WZ_BMSCLASSIC.clone();
        }
    }
}
