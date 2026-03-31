package wzlib.property;
import wzlib.WzImageProperty;
import wzlib.WzPropertyType;

public class WzNullProperty extends WzImageProperty {
    public WzNullProperty(String name) { this.name = name; }
    @Override public WzPropertyType getPropertyType() { return WzPropertyType.Null; }
    @Override public Object getValue() { return null; }
    @Override public void setValue(Object value) {}

    @Override
    public WzImageProperty deepClone() {
        return new WzNullProperty(name);
    }

    @Override
    public void writeValue(wzlib.util.WzBinaryWriter writer) throws java.io.IOException {
        writer.writeByte((byte) 0);
    }
}
