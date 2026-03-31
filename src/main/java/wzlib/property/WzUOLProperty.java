package wzlib.property;
import wzlib.WzImageProperty;
import wzlib.WzPropertyType;

public class WzUOLProperty extends WzImageProperty {
    private String value; // link path

    public WzUOLProperty(String name, String value) { this.name = name; this.value = value; }

    @Override public WzPropertyType getPropertyType() { return WzPropertyType.UOL; }
    @Override public Object getValue() { return value; }
    @Override public void setValue(Object value) { this.value = (String) value; }
    public String getUOL() { return value; }

    @Override
    public WzImageProperty deepClone() {
        return new WzUOLProperty(name, value);
    }

    @Override
    public void writeValue(wzlib.util.WzBinaryWriter writer) throws java.io.IOException {
        writer.writeStringValue("UOL", (int) 0x73, (int) 0x1B);
        writer.writeByte((byte) 0);
        writer.writeStringValue(value, 0, 1);
    }
}
