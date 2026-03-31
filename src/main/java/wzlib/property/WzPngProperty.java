package wzlib.property;
import wzlib.WzImageProperty;
import wzlib.WzPropertyType;
import wzlib.util.WzBinaryReader;
import wzlib.util.WzPngCodec;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * PNG 圖片資料，儲存壓縮像素與中繼資料。
 * PNG image data, stores compressed pixels and metadata.
 *
 * <p>Ported from: MapleLib/WzLib/WzProperties/WzPngProperty.cs</p>
 */
public class WzPngProperty extends WzImageProperty {
    private int width;
    private int height;
    private int format;
    private byte[] compressedImageBytes;

    // For lazy loading from reader
    private WzBinaryReader wzReader;
    private long dataOffset; // offset to the length int in the file
    private int compressedLength;

    public WzPngProperty() { this.name = "PNG"; }

    /**
     * Constructor that reads from WzBinaryReader.
     * Ported from: WzPngProperty(WzBinaryReader reader, bool parseNow)
     */
    public WzPngProperty(WzBinaryReader reader, boolean parseNow) throws IOException {
        this.name = "PNG";
        this.wzReader = reader;

        // Read header
        this.width = reader.readCompressedInt();
        this.height = reader.readCompressedInt();

        int format1 = reader.readCompressedInt();
        int format2 = reader.readCompressedInt();
        this.format = format1 + (format2 << 8);

        reader.skip(4); // reserved
        this.dataOffset = reader.getPosition();
        int len = reader.readInt32() - 1;
        reader.skip(1); // skip 1 byte after length

        if (len > 0) {
            if (parseNow) {
                compressedImageBytes = reader.readBytes(len);
            } else {
                reader.skip(len);
            }
        }
        this.compressedLength = len;
    }

    @Override public WzPropertyType getPropertyType() { return WzPropertyType.PNG; }
    @Override public Object getValue() { return getImage(true); }
    @Override public void setValue(Object value) {}

    /**
     * 解碼 PNG 資料為 BufferedImage。
     * Decode the PNG data into a BufferedImage.
     */
    public BufferedImage getImage(boolean saveInMemory) {
        try {
            byte[] data = getCompressedBytes(saveInMemory);
            if (data == null) return null;
            return WzPngCodec.decode(data, width, height, format,
                    wzReader != null ? wzReader.getWzKey() : null);
        } catch (IOException e) {
            throw new wzlib.WzException("Failed to decode PNG", e);
        }
    }

    /** 取得圖片寬度。/ Get image width. */
    public int getWidth() { return width; }
    /** 取得圖片高度。/ Get image height. */
    public int getHeight() { return height; }
    public int getFormat() { return format; }

    public byte[] getCompressedImageBytes() { return compressedImageBytes; }
    public void setCompressedImageBytes(byte[] data) { this.compressedImageBytes = data; }

    @Override
    public WzImageProperty deepClone() {
        WzPngProperty clone = new WzPngProperty();
        clone.width = this.width;
        clone.height = this.height;
        clone.format = this.format;
        if (this.compressedImageBytes != null) {
            clone.compressedImageBytes = this.compressedImageBytes.clone();
        } else if (this.wzReader != null) {
            try {
                byte[] bytes = getCompressedBytes(false);
                if (bytes != null) clone.compressedImageBytes = bytes.clone();
            } catch (Exception e) {
                // fallback: no data
            }
        }
        return clone;
    }

    @Override
    public void writeValue(wzlib.util.WzBinaryWriter writer) throws java.io.IOException {
        throw new UnsupportedOperationException("Cannot write a PngProperty directly");
    }

    /** Get compressed bytes, reading from file if needed (lazy load). */
    public byte[] getCompressedBytes(boolean saveInMemory) throws IOException {
        if (compressedImageBytes != null) return compressedImageBytes;
        if (wzReader == null || compressedLength <= 0) return null;

        synchronized (wzReader) {
            long savedPos = wzReader.getPosition();
            wzReader.setPosition(dataOffset);
            int len = wzReader.readInt32() - 1;
            wzReader.skip(1);
            byte[] data = wzReader.readBytes(len);
            wzReader.setPosition(savedPos);

            if (saveInMemory) {
                compressedImageBytes = data;
            }
            return data;
        }
    }
}
