package wzlib.crypto;

/**
 * Ported from: MapleLib/WzLib/Util/WzKeyGenerator.cs
 */
public final class WzKeyGenerator {

    private WzKeyGenerator() {}

    /**
     * Generate a WzMutableKey from IV bytes.
     * Uses the ACTIVE UserKey (which may be custom for private servers).
     * Ported from: WzKeyGenerator.GenerateWzKey(byte[] WzIv)
     * C#: new WzMutableKey(WzIv, MapleCryptoConstants.GetTrimmedUserKey(ref MapleCryptoConstants.UserKey_WzLib));
     */
    public static WzMutableKey generateWzKey(byte[] wzIv) {
        byte[] trimmedKey = WzCryptoConstants.getTrimmedUserKey(WzCryptoConstants.getActiveUserKey());
        return new WzMutableKey(wzIv, trimmedKey);
    }

    /**
     * Generate a WzMutableKey from IV bytes and a custom 128-byte AES UserKey.
     * Ported from: WzKeyGenerator.GenerateWzKey(byte[] WzIv, byte[] AesUserKey)
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
