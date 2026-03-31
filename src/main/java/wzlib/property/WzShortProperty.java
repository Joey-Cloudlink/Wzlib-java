package wzlib.property;
import wzlib.WzImageProperty;
import wzlib.WzPropertyType;

/**
 * 短整數屬性。
 * Short property.
 */
public class WzShortProperty extends WzImageProperty {
    private short value;
    public WzShortProperty(String name, short value) { this.name = name; this.value = value; }
    @Override public WzPropertyType getPropertyType() { return WzPropertyType.Short; }
    @Override public Object getValue() { return value; }
    @Override public void setValue(Object value) { this.value = ((Number)value).shortValue(); }
    public short getShort() { return value; }
    public void setShort(short v) { this.value = v; }

    @Override
    public WzImageProperty deepClone() {
        return new WzShortProperty(name, value);
    }

    @Override
    public void writeValue(wzlib.util.WzBinaryWriter writer) throws java.io.IOException {
        writer.writeByte((byte) 2);
        writer.writeInt16(value);
    }
}
