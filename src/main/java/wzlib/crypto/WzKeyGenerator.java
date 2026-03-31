package wzlib.crypto;

/**
 * WZ 金鑰產生器，從 IV 與 UserKey 產生 AES 解密金鑰。
 * WZ key generator that produces AES decryption keys from IV and UserKey.
 *
 * <p>Ported from: MapleLib/WzLib/Util/WzKeyGenerator.cs</p>
 */
public final class WzKeyGenerator {

    private WzKeyGenerator() {}

    /**
     * 從 IV 產生 WZ 金鑰（使用目前作用中的 UserKey）。
     * Generate a WZ key from IV bytes (uses the active UserKey).
     */
    public static WzMutableKey generateWzKey(byte[] wzIv) {
        byte[] trimmedKey = WzCryptoConstants.getTrimmedUserKey(WzCryptoConstants.getActiveUserKey());
        return new WzMutableKey(wzIv, trimmedKey);
    }

    /**
     * 從 IV 與自訂 128-byte UserKey 產生 WZ 金鑰。
     * Generate a WZ key from IV bytes and a custom 128-byte AES UserKey.
     */
    public static WzMutableKey generateWzKey(byte[] wzIv, byte[] aesUserKey) {
        if (aesUserKey.length != 128) {
            throw new IllegalArgumentException("AesUserKey expects 128 bytes, not " + aesUserKey.length);
        }
        byte[] trimmedKey = WzCryptoConstants.getTrimmedUserKey(aesUserKey);
        return new WzMutableKey(wzIv, trimmedKey);
    }

    /**
     * Generate the WZ Key for .Lua property.
     * Uses MSEA IV + default MapleStory UserKey.
     * Ported from: WzKeyGenerator.GenerateLuaWzKey()
     */
    public static WzMutableKey generateLuaWzKey() {
        byte[] trimmedKey = WzCryptoConstants.getTrimmedUserKey(WzCryptoConstants.MAPLESTORY_USERKEY_DEFAULT);
        return new WzMutableKey(WzCryptoConstants.WZ_MSEAIV, trimmedKey);
    }
}
