package wzlib.property;
import wzlib.WzImageProperty;
import wzlib.WzPropertyType;

/**
 * 整數屬性。
 * Integer property.
 */
public class WzIntProperty extends WzImageProperty {
    private int value;
    public WzIntProperty(String name, int value) { this.name = name; this.value = value; }
    @Override public WzPropertyType getPropertyType() { return WzPropertyType.Int; }
    @Override public Object getValue() { return value; }
    @Override public void setValue(Object value) { this.value = ((Number)value).intValue(); }
    public int getInt() { return value; }
    public void setInt(int v) { this.value = v; }

    @Override
    public WzImageProperty deepClone() {
        return new WzIntProperty(name, value);
    }

    @Override
    public void writeValue(wzlib.util.WzBinaryWriter writer) throws java.io.IOException {
        writer.writeByte((byte) 3);
        writer.writeCompressedInt(value);
    }
}
