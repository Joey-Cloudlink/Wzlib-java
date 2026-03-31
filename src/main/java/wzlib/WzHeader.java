package wzlib;

/**
 * Ported from: MapleLib/WzLib/WzHeader.cs
 */
public class WzHeader {

    private static final String DEFAULT_WZ_HEADER_COPYRIGHT = "Package file v1.0 Copyright 2002 Wizet, ZMS";

    private String ident;      // "PKG1"
    private long fSize;        // uint64 file size
    private long fStart;       // uint32 offset where data begins
    private String copyright;

    public String getIdent() { return ident; }
    public void setIdent(String ident) { this.ident = ident; }

    public long getFSize() { return fSize; }
    public void setFSize(long fSize) { this.fSize = fSize; }

    public long getFStart() { return fStart; }
    public void setFStart(long fStart) { this.fStart = fStart; }

    public String getCopyright() { return copyright; }
    public void setCopyright(String copyright) { this.copyright = copyright; }

    /**
     * Recalculate FStart based on header content size.
     * Ported from: WzHeader.RecalculateFileStart()
     */
    public void recalculateFileStart() {
        fStart = ident.length() + 8 + 4 + copyright.length() + 1;
    }

    /**
     * Get a default WZ header.
     * Ported from: WzHeader.GetDefault()
     */
    public static WzHeader getDefault() {
        WzHeader header = new WzHeader();
        header.ident = "PKG1";
        header.copyright = DEFAULT_WZ_HEADER_COPYRIGHT;
        header.fStart = 60;
        header.fSize = 0;
        return header;
    }
}
