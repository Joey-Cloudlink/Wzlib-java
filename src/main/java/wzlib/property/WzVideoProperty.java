package wzlib.property;

import wzlib.WzImageProperty;
import wzlib.WzPropertyType;
import wzlib.util.WzBinaryReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Ported from: MapleLib/WzLib/WzProperties/WzVideoProperty.cs
 *
 * KMST v1181+ Canvas#Video property — stores video data.
 */
public class WzVideoProperty extends WzImageProperty {

    public static final String CANVAS_VIDEO_HEADER = "Canvas#Video";

    private WzBinaryReader wzReader;
    private final List<WzImageProperty> properties = new ArrayList<>();
    private long dataOffset;
    private int length;
    private int type;
    private byte[] bytes;

    public WzVideoProperty(String name, WzBinaryReader reader) {
        this.name = name;
        this.wzReader = reader;
    }

    /** Constructor for cloning */
    private WzVideoProperty(String name) {
        this.name = name;
    }

    @Override public WzPropertyType getPropertyType() { return WzPropertyType.Raw; }
    @Override public Object getValue() { return getBytes(false); }
    @Override public void setValue(Object value) {}
    @Override public List<WzImageProperty> getProperties() { return properties; }

    public int getVideoType() { return type; }
    public int getLength() { return length; }

    /**
     * Parse the video data from the reader.
     * Ported from: WzVideoProperty.Parse(bool)
     */
    public void parse(boolean parseNow) throws IOException {
        type = wzReader.readByte() & 0xFF;
        length = wzReader.readCompressedInt();
        dataOffset = wzReader.getPosition();
        if (parseNow) {
            getBytes(true);
        } else {
            wzReader.setPosition(dataOffset + length);
        }
    }

    /**
     * Get video bytes, reading from file if needed.
     * Ported from: WzVideoProperty.GetBytes(bool)
     */
    public byte[] getBytes(boolean saveInMemory) {
        if (this.bytes != null) return this.bytes;
        if (this.wzReader == null) return null;

        try {
            synchronized (wzReader) {
                long currentPos = wzReader.getPosition();
                wzReader.setPosition(dataOffset);
                byte[] data = wzReader.readBytes(length);
                wzReader.setPosition(currentPos);

                if (saveInMemory) {
                    this.bytes = data;
                }
                return data;
            }
        } catch (IOException e) {
            throw new wzlib.WzException("Failed to read video data", e);
        }
    }

    public void addProperty(WzImageProperty prop) {
        prop.setParent(this);
        properties.add(prop);
    }

    public void addProperties(List<WzImageProperty> props) {
        for (WzImageProperty p : props) {
            p.setParent(this);
            properties.add(p);
        }
    }

    /**
     * Ported from: WzVideoProperty.WriteValue()
     */
    @Override
    public void writeValue(wzlib.util.WzBinaryWriter writer) throws IOException {
        byte[] data = getBytes(false);
        writer.writeStringValue(CANVAS_VIDEO_HEADER, 0x73, 0x1B);
        writer.writeByte((byte) 0);
        if (properties.size() > 0) {
            writer.writeByte((byte) 1);
            WzImageProperty.writePropertyList(writer, properties);
        } else {
            writer.writeByte((byte) 0);
        }
        writer.writeByte((byte) type);
        writer.writeCompressedInt(data != null ? data.length : 0);
        if (data != null) writer.writeBytes(data);
    }

    @Override
    public WzImageProperty deepClone() {
        WzVideoProperty clone = new WzVideoProperty(name);
        clone.type = this.type;
        clone.length = this.length;
        byte[] data = getBytes(false);
        if (data != null) clone.bytes = data.clone();
        for (WzImageProperty prop : properties) {
            clone.addProperty(prop.deepClone());
        }
        return clone;
    }
}
