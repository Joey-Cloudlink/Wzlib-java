package wzlib;

import wzlib.util.WzBinaryReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Ported from: MapleLib/WzLib/WzImage.cs
 */
public class WzImage extends WzObject {

    /** Header byte for standard image without offset */
    public static final byte WZ_IMAGE_HEADER_BYTE_WITHOUT_OFFSET = 0x73;
    /** Header byte for image with offset */
    public static final byte WZ_IMAGE_HEADER_BYTE_WITH_OFFSET = 0x1B;

    /** Check if this image is a .lua file. Ported from: WzImage.IsLuaWzImage */
    public boolean isLuaWzImage() {
        return name != null && name.endsWith(".lua");
    }

    WzBinaryReader reader; // package-private (C# internal)
    private boolean parsed = false;
    private long offset;
    private int size;
    private int checksum;
    private boolean changed = false;
    private final List<WzImageProperty> properties = new ArrayList<>();

    // temp file positions for save workflow
    long tempFileStart;
    long tempFileEnd;

    public WzImage(String name, WzBinaryReader reader, int checksum) {
        this.name = name;
        this.reader = reader;
        this.checksum = checksum;
    }

    @Override
    public WzObjectType getObjectType() {
        return WzObjectType.Image;
    }

    public boolean isParsed() { return parsed; }
    public void setParsed(boolean parsed) { this.parsed = parsed; }

    public long getOffset() { return offset; }
    public void setOffset(long offset) { this.offset = offset; }

    public int getBlockSize() { return size; }
    public void setBlockSize(int size) { this.size = size; }

    public int getChecksum() { return checksum; }
    public void setChecksum(int checksum) { this.checksum = checksum; }

    public boolean isChanged() { return changed; }
    public void setChanged(boolean changed) { this.changed = changed; }

    /**
     * Parse image properties.
     * Ported from: WzImage.ParseImage()
     */
    public synchronized void parseImage() throws IOException {
        if (parsed) return;
        if (changed) { parsed = true; return; }

        synchronized (reader) {
            long originalPos = reader.getPosition();
            reader.setPosition(offset);

            byte b = reader.readByte();
            switch (b & 0xFF) {
                case 0x01: { // .lua image
                    // Ported from: WzImage.ParseImage() case 0x1
                    if (isLuaWzImage()) {
                        WzImageProperty lua = WzImageProperty.parseLuaProperty(offset, reader, this, this);
                        this.properties.add(lua);
                        parsed = true;
                        return;
                    }
                    reader.setPosition(originalPos);
                    return;
                }
                case 0x73: { // WzImageHeaderByte_WithoutOffset
                    String prop = reader.readString();
                    int val = reader.readUInt16();
                    if (!"Property".equals(prop) || val != 0) {
                        reader.setPosition(originalPos);
                        return;
                    }
                    break;
                }
                default:
                    reader.setPosition(originalPos);
                    return;
            }

            List<WzImageProperty> props = WzImageProperty.parsePropertyList(offset, reader, this, this);
            this.properties.addAll(props);
            parsed = true;
        }
    }

    public List<WzImageProperty> getProperties() {
        if (!parsed && reader != null) {
            try {
                parseImage();
            } catch (IOException e) {
                throw new WzException("Failed to parse image", e);
            }
        }
        return properties;
    }

    @Override
    public WzObject getChild(String name) {
        for (WzImageProperty p : getProperties()) {
            if (p.getName() != null && p.getName().equalsIgnoreCase(name)) return p;
        }
        return null;
    }

    /**
     * Serialize image to writer.
     * Ported from: WzImage.SaveImage()
     */
    public void saveImage(wzlib.util.WzBinaryWriter writer) throws java.io.IOException {
        if (changed) {
            // Parse if not yet parsed
            if (reader != null && !parsed) {
                parseImage();
            }

            // Create a WzSubProperty wrapper and write
            wzlib.property.WzSubProperty imgProp = new wzlib.property.WzSubProperty("");
            imgProp.addProperties(properties);

            long startPos = writer.getPosition();
            imgProp.writeValue(writer);
            writer.getStringCache().clear();

            size = (int) (writer.getPosition() - startPos);
        } else {
            // Unchanged: copy raw bytes from original reader
            // This path is handled by WzDirectory.saveImages()
        }
    }

    /**
     * Calculate and set checksum from serialized bytes.
     * Ported from: WzImage.CalculateAndSetImageChecksum()
     */
    public void calculateAndSetChecksum(byte[] bytes) {
        int sum = 0;
        for (byte b : bytes) {
            sum += b & 0xFF;
        }
        this.checksum = sum;
    }

    @Override
    public String toString() {
        return name;
    }
}
