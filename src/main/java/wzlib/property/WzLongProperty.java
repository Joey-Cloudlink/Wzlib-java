package wzlib.property;
import wzlib.WzImageProperty;
import wzlib.WzPropertyType;

/**
 * 長整數屬性。
 * Long property.
 */
public class WzLongProperty extends WzImageProperty {
    private long value;
    public WzLongProperty(String name, long value) { this.name = name; this.value = value; }
    @Override public WzPropertyType getPropertyType() { return WzPropertyType.Long; }
    @Override public Object getValue() { return value; }
    @Override public void setValue(Object value) { this.value = ((Number)value).longValue(); }
    public long getLong() { return value; }
    public void setLong(long v) { this.value = v; }

    @Override
    public WzImageProperty deepClone() {
        return new WzLongProperty(name, value);
    }

    @Override
    public void writeValue(wzlib.util.WzBinaryWriter writer) throws java.io.IOException {
        writer.writeByte((byte) 20);
        writer.writeCompressedLong(value);
    }
}
