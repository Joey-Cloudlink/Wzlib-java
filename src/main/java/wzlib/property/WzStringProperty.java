package wzlib.property;
import wzlib.WzImageProperty;
import wzlib.WzPropertyType;

/**
 * 字串屬性。
 * String property.
 */
public class WzStringProperty extends WzImageProperty {
    private String value;
    public WzStringProperty(String name, String value) { this.name = name; this.value = value; }
    @Override public WzPropertyType getPropertyType() { return WzPropertyType.String; }
    @Override public Object getValue() { return value; }
    @Override public void setValue(Object value) { this.value = (String) value; }
    public String getString() { return value; }
    public void setString(String v) { this.value = v; }

    @Override
    public WzImageProperty deepClone() {
        return new WzStringProperty(name, value);
    }

    @Override
    public void writeValue(wzlib.util.WzBinaryWriter writer) throws java.io.IOException {
        writer.writeByte((byte) 8);
        writer.writeStringValue(value, 0, 1);
    }
}
