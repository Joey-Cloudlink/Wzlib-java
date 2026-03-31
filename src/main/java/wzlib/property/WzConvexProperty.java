package wzlib.property;
import wzlib.WzImageProperty;
import wzlib.WzPropertyType;
import java.util.ArrayList;
import java.util.List;

public class WzConvexProperty extends WzImageProperty {
    private final List<WzImageProperty> properties = new ArrayList<>();

    public WzConvexProperty(String name) { this.name = name; }

    @Override public WzPropertyType getPropertyType() { return WzPropertyType.Convex; }
    @Override public Object getValue() { return properties; }
    @Override public void setValue(Object value) {}
    @Override public List<WzImageProperty> getProperties() { return properties; }

    public void addProperty(WzImageProperty prop) {
        prop.setParent(this);
        properties.add(prop);
    }

    @Override
    public WzImageProperty deepClone() {
        WzConvexProperty clone = new WzConvexProperty(name);
        for (WzImageProperty prop : properties) {
            clone.addProperty(prop.deepClone());
        }
        return clone;
    }

    @Override
    public void writeValue(wzlib.util.WzBinaryWriter writer) throws java.io.IOException {
        writer.writeStringValue("Shape2D#Convex2D", (int) 0x73, (int) 0x1B);
        writer.writeCompressedInt(properties.size());
        for (wzlib.WzImageProperty prop : properties) {
            prop.writeValue(writer);
        }
    }
}
