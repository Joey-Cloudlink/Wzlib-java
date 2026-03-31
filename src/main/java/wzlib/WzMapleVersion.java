package wzlib;

/**
 * MapleStory 版本列舉，決定 WZ 加密用的 IV 向量。
 * MapleStory version enum that determines the AES IV used for WZ encryption.
 *
 * <p>Ported from: MapleLib/WzLib/WzMapleVersion.cs + WzAESConstant.cs</p>
 */
public enum WzMapleVersion {
    /** GMS — Global MapleStory 專用 IV。/ IV for Global MapleStory. */
    GMS(new byte[]{0x4D, 0x23, (byte) 0xC7, 0x2B}),
    /** EMS — 歐洲/韓國/東南亞版本 IV。/ IV for EMS/KMS/MSEA. */
    EMS(new byte[]{(byte) 0xB9, 0x7D, 0x63, (byte) 0xE9}),
    /** BMS — 巴西版本，無加密。/ IV for BMS (no encryption). */
    BMS(new byte[]{0x00, 0x00, 0x00, 0x00}),
    /** CLASSIC — 舊版格式，無加密。/ Classic format (no encryption). */
    CLASSIC(new byte[]{0x00, 0x00, 0x00, 0x00}),
    /** GENERATE — 產生新檔案用，無加密。/ For generating new files (no encryption). */
    GENERATE(new byte[]{0x00, 0x00, 0x00, 0x00}),
    /** GETFROMZLZ — 從 ZLZ.dll 讀取 IV。/ IV read from ZLZ.dll at runtime. */
    GETFROMZLZ(null),
    /** CUSTOM — 使用者自訂 IV（私服用）。/ User-provided IV (for private servers). */
    CUSTOM(null);

    private final byte[] iv;

    WzMapleVersion(byte[] iv) {
        this.iv = iv;
    }

    public byte[] getIv() {
        return iv != null ? iv.clone() : null;
    }
}
