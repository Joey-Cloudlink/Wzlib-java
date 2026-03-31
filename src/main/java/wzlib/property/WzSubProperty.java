package wzlib.property;
import wzlib.WzImageProperty;
import wzlib.WzPropertyType;
import wzlib.WzObject;
import java.util.ArrayList;
import java.util.List;

/**
 * 子屬性容器。
 * Sub-property container.
 */
public class WzSubProperty extends WzImageProperty {
    private final List<WzImageProperty> properties = new ArrayList<>();

    public WzSubProperty(String name) { this.name = name; }

    @Override public WzPropertyType getPropertyType() { return WzPropertyType.SubProperty; }
    @Override public Object getValue() { return properties; }
    @Override public void setValue(Object value) {}
    @Override public List<WzImageProperty> getProperties() { return properties; }

    public void addProperty(WzImageProperty prop) {
        prop.setParent(this);
        properties.add(prop);
    }
    public void addProperties(List<WzImageProperty> props) {
        for (WzImageProperty p : props) {
            p.setParent(this);
            properties.add(p);
        }
    }

    @Override
    public WzImageProperty deepClone() {
        WzSubProperty clone = new WzSubProperty(name);
        for (WzImageProperty prop : properties) {
            clone.addProperty(prop.deepClone());
        }
        return clone;
    }

    @Override
    public void writeValue(wzlib.util.WzBinaryWriter writer) throws java.io.IOException {
        writer.writeStringValue("Property", (int) 0x73, (int) 0x1B);
        wzlib.WzImageProperty.writePropertyList(writer, properties);
    }
}
