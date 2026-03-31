package wzlib.property;
import wzlib.WzImageProperty;
import wzlib.WzPropertyType;
import java.util.ArrayList;
import java.util.List;

public class WzCanvasProperty extends WzImageProperty {
    private final List<WzImageProperty> properties = new ArrayList<>();
    private WzPngProperty pngProperty;

    public WzCanvasProperty(String name) { this.name = name; }

    @Override public WzPropertyType getPropertyType() { return WzPropertyType.Canvas; }
    @Override public Object getValue() { return pngProperty; }
    @Override public void setValue(Object value) {}
    @Override public List<WzImageProperty> getProperties() { return properties; }

    public WzPngProperty getPngProperty() { return pngProperty; }
    public void setPngProperty(WzPngProperty png) {
        this.pngProperty = png;
        if (png != null) png.setParent(this);
    }

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
        WzCanvasProperty clone = new WzCanvasProperty(name);
        for (WzImageProperty prop : properties) {
            clone.addProperty(prop.deepClone());
        }
        if (pngProperty != null) {
            clone.setPngProperty((WzPngProperty) pngProperty.deepClone());
        }
        return clone;
    }

    @Override
    public void writeValue(wzlib.util.WzBinaryWriter writer) throws java.io.IOException {
        writer.writeStringValue("Canvas", (int) 0x73, (int) 0x1B);
        writer.writeByte((byte) 0);
        if (properties.size() > 0) {
            writer.writeByte((byte) 1);
            wzlib.WzImageProperty.writePropertyList(writer, properties);
        } else {
            writer.writeByte((byte) 0);
        }
        // PNG info
        writer.writeCompressedInt(pngProperty.getWidth());
        writer.writeCompressedInt(pngProperty.getHeight());
        int formatValue = pngProperty.getFormat();
        writer.writeCompressedInt(formatValue & 0xFF);
        writer.writeCompressedInt(formatValue >> 8);
        writer.writeInt32(0); // reserved
        // PNG data
        byte[] bytes = pngProperty.getCompressedBytes(false);
        writer.writeInt32(bytes.length + 1);
        writer.writeByte((byte) 0);
        writer.writeBytes(bytes);
    }
}
