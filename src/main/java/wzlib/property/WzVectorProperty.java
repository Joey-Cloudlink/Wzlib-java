package wzlib.property;
import wzlib.WzImageProperty;
import wzlib.WzPropertyType;

/**
 * 向量座標。
 * Vector coordinates.
 */
public class WzVectorProperty extends WzImageProperty {
    private WzIntProperty x;
    private WzIntProperty y;

    public WzVectorProperty(String name) { this.name = name; }

    @Override public WzPropertyType getPropertyType() { return WzPropertyType.Vector; }
    @Override public Object getValue() { return null; }
    @Override public void setValue(Object value) {}

    public WzIntProperty getX() { return x; }
    public void setX(WzIntProperty x) { this.x = x; }
    public WzIntProperty getY() { return y; }
    public void setY(WzIntProperty y) { this.y = y; }

    @Override
    public WzImageProperty deepClone() {
        WzVectorProperty clone = new WzVectorProperty(name);
        if (x != null) clone.setX(new WzIntProperty("X", x.getInt()));
        if (y != null) clone.setY(new WzIntProperty("Y", y.getInt()));
        return clone;
    }

    @Override
    public void writeValue(wzlib.util.WzBinaryWriter writer) throws java.io.IOException {
        writer.writeStringValue("Shape2D#Vector2D", (int) 0x73, (int) 0x1B);
        writer.writeCompressedInt(x.getInt());
        writer.writeCompressedInt(y.getInt());
    }
}
