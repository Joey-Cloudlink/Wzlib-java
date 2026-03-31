package wzlib.property;
import wzlib.WzImageProperty;
import wzlib.WzPropertyType;

public class WzDoubleProperty extends WzImageProperty {
    private double value;
    public WzDoubleProperty(String name, double value) { this.name = name; this.value = value; }
    @Override public WzPropertyType getPropertyType() { return WzPropertyType.Double; }
    @Override public Object getValue() { return value; }
    @Override public void setValue(Object value) { this.value = ((Number)value).doubleValue(); }
    public double getDouble() { return value; }
    public void setDouble(double v) { this.value = v; }

    @Override
    public WzImageProperty deepClone() {
        return new WzDoubleProperty(name, value);
    }

    @Override
    public void writeValue(wzlib.util.WzBinaryWriter writer) throws java.io.IOException {
        writer.writeByte((byte) 5);
        writer.writeDouble(value);
    }
}
