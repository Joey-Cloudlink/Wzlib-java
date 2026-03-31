package wzlib;

/**
 * Ported from: MapleLib/WzLib/WzMapleVersion.cs
 * + MapleLib/WzLib/WzAESConstant.cs (IV values)
 */
public enum WzMapleVersion {
    GMS(new byte[]{0x4D, 0x23, (byte) 0xC7, 0x2B}),
    EMS(new byte[]{(byte) 0xB9, 0x7D, 0x63, (byte) 0xE9}),
    BMS(new byte[]{0x00, 0x00, 0x00, 0x00}),
    CLASSIC(new byte[]{0x00, 0x00, 0x00, 0x00}),
    GENERATE(new byte[]{0x00, 0x00, 0x00, 0x00}),
    GETFROMZLZ(null),   // IV read from ZLZ.dll at runtime
    CUSTOM(null);        // IV provided by user (private server)

    private final byte[] iv;

    WzMapleVersion(byte[] iv) {
        this.iv = iv;
    }

    public byte[] getIv() {
        return iv != null ? iv.clone() : null;
    }
}
