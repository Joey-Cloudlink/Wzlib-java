package wzlib.property;

import wzlib.WzImageProperty;
import wzlib.WzPropertyType;
import wzlib.util.WzBinaryReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 原始資料。
 * Raw binary data.
 *
 * <p>Ported from: MapleLib/WzLib/WzProperties/WzRawDataProperty.cs</p>
 */
public class WzRawDataProperty extends WzImageProperty {

    public static final String RAW_DATA_HEADER = "RawData";

    private byte type;
    private int length;
    private long rawDataOffset;
    private byte[] bytes;
    private WzBinaryReader wzReader;
    private final List<WzImageProperty> properties = new ArrayList<>();

    public WzRawDataProperty(String name, WzBinaryReader reader, byte type) {
        this.name = name;
        this.wzReader = reader;
        this.type = type;
    }

    @Override public WzPropertyType getPropertyType() { return WzPropertyType.Raw; }
    @Override public Object getValue() { return getBytes(false); }
    @Override public void setValue(Object value) {}
    @Override public List<WzImageProperty> getProperties() { return properties; }

    public byte getType() { return type; }
    public int getLength() { return length; }

    /**
     * Parse the raw data from the reader.
     * Ported from: WzRawDataProperty.Parse(bool)
     */
    public void parse(boolean parseNow) throws IOException {
        length = wzReader.readCompressedInt();
        rawDataOffset = wzReader.getPosition();
        if (parseNow) {
            getBytes(true);
        } else {
            wzReader.setPosition(rawDataOffset + length);
        }
    }

    /**
     * Get raw bytes, reading from file if needed.
     * Ported from: WzRawDataProperty.GetBytes(bool)
     */
    public byte[] getBytes(boolean saveInMemory) {
        if (this.bytes != null) return this.bytes;
        if (this.wzReader == null) return null;

        try {
            synchronized (wzReader) {
                long currentPos = wzReader.getPosition();
                wzReader.setPosition(rawDataOffset);
                byte[] data = wzReader.readBytes(length);
                wzReader.setPosition(currentPos);

                if (saveInMemory) {
                    this.bytes = data;
                }
                return data;
            }
        } catch (IOException e) {
            throw new wzlib.WzException("Failed to read raw data", e);
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

    @Override
    public WzImageProperty deepClone() {
        WzRawDataProperty clone = new WzRawDataProperty(name, null, type);
        for (WzImageProperty prop : properties) {
            clone.addProperty(prop.deepClone());
        }
        clone.length = this.length;
        byte[] data = getBytes(false);
        if (data != null) clone.bytes = data.clone();
        return clone;
    }

    /**
     * Ported from: WzRawDataProperty.WriteValue()
     */
    @Override
    public void writeValue(wzlib.util.WzBinaryWriter writer) throws IOException {
        byte[] data = getBytes(false);
        writer.writeStringValue(RAW_DATA_HEADER, 0x73, 0x1B);
        writer.writeByte(type);
        if (type == 1) {
            if (properties.size() > 0) {
                writer.writeByte((byte) 1);
                WzImageProperty.writePropertyList(writer, properties);
            } else {
                writer.writeByte((byte) 0);
            }
        }
        writer.writeCompressedInt(data != null ? data.length : 0);
        if (data != null) writer.writeBytes(data);
    }
}
