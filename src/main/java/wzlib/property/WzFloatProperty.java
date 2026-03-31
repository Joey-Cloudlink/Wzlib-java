package wzlib.property;
import wzlib.WzImageProperty;
import wzlib.WzPropertyType;

public class WzFloatProperty extends WzImageProperty {
    private float value;
    public WzFloatProperty(String name, float value) { this.name = name; this.value = value; }
    @Override public WzPropertyType getPropertyType() { return WzPropertyType.Float; }
    @Override public Object getValue() { return value; }
    @Override public void setValue(Object value) { this.value = ((Number)value).floatValue(); }
    public float getFloat() { return value; }
    public void setFloat(float v) { this.value = v; }

    @Override
    public WzImageProperty deepClone() {
        return new WzFloatProperty(name, value);
    }

    @Override
    public void writeValue(wzlib.util.WzBinaryWriter writer) throws java.io.IOException {
        writer.writeByte((byte) 4);
        if (value == 0f) {
            writer.writeByte((byte) 0);
        } else {
            writer.writeByte((byte) 0x80);
            writer.writeSingle(value);
        }
    }
}
