package wzlib;

import wzlib.property.*;
import wzlib.util.WzBinaryReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class WzImageProperty extends WzObject {

    public abstract WzPropertyType getPropertyType();
    public abstract Object getValue();
    public abstract void setValue(Object value);

    @Override
    public WzObjectType getObjectType() { return WzObjectType.Property; }

    // Child properties for container types (SubProperty, Canvas, etc.)
    public List<WzImageProperty> getProperties() { return null; }

    public WzImageProperty getPropertyByName(String name) {
        List<WzImageProperty> props = getProperties();
        if (props == null) return null;
        for (WzImageProperty p : props) {
            if (p.getName() != null && p.getName().equalsIgnoreCase(name)) return p;
        }
        return null;
    }

    @Override
    public WzObject getChild(String name) {
        return getPropertyByName(name);
    }

    /**
     * Parse property list from reader.
     * Ported from: WzImageProperty.ParsePropertyList()
     *
     * C# logic:
     * - readCompressedInt for entryCount
     * - for each: readStringBlock(offset) for name, readByte() for ptype
     * - ptype switch: 0=Null, 2/11=Short, 3/19=Int, 20=Long, 4=Float, 5=Double, 8=String, 9=Extended
     */
    /**
     * Parse .lua property.
     * Ported from: WzImageProperty.ParseLuaProperty()
     */
    public static WzLuaProperty parseLuaProperty(long offset, WzBinaryReader reader, WzObject parent, WzImage parentImg) throws IOException {
        int length = reader.readCompressedInt();
        byte[] rawEncBytes = reader.readBytes(length);
        WzLuaProperty lua = new WzLuaProperty("Script", rawEncBytes);
        lua.setParent(parent);
        return lua;
    }

    public static List<WzImageProperty> parsePropertyList(long offset, WzBinaryReader reader, WzObject parent, WzImage parentImg) throws IOException {
        int entryCount = reader.readCompressedInt();
        List<WzImageProperty> properties = new ArrayList<>(entryCount);

        for (int i = 0; i < entryCount; i++) {
            String propName = reader.readStringBlock(offset);
            byte ptype = reader.readByte();

            switch (ptype & 0xFF) {
                case 0:
                    properties.add(withParent(new WzNullProperty(propName), parent));
                    break;
                case 11:
                case 2:
                    properties.add(withParent(new WzShortProperty(propName, reader.readInt16()), parent));
                    break;
                case 3:
                case 19:
                    properties.add(withParent(new WzIntProperty(propName, reader.readCompressedInt()), parent));
                    break;
                case 20:
                    properties.add(withParent(new WzLongProperty(propName, reader.readCompressedLong()), parent));
                    break;
                case 4: {
                    byte type = reader.readByte();
                    float val = (type & 0xFF) == 0x80 ? reader.readSingle() : 0f;
                    properties.add(withParent(new WzFloatProperty(propName, val), parent));
                    break;
                }
                case 5:
                    properties.add(withParent(new WzDoubleProperty(propName, reader.readDouble()), parent));
                    break;
                case 8:
                    properties.add(withParent(new WzStringProperty(propName, reader.readStringBlock(offset)), parent));
                    break;
                case 9: {
                    long eob = (reader.readUInt32()) + reader.getPosition();
                    WzImageProperty exProp = parseExtendedProp(reader, offset, eob, propName, parent, parentImg);
                    properties.add(exProp);
                    if (reader.getPosition() != eob) {
                        reader.setPosition(eob);
                    }
                    break;
                }
                default:
                    throw new WzException("Unknown property type at ParsePropertyList, ptype = " + (ptype & 0xFF));
            }
        }
        return properties;
    }

    /**
     * Parse extended property.
     * Ported from: WzImageProperty.ParseExtendedProp()
     */
    public static WzImageProperty parseExtendedProp(WzBinaryReader reader, long offset, long endOfBlock, String name, WzObject parent, WzImage imgParent) throws IOException {
        byte b = reader.readByte();
        switch (b & 0xFF) {
            case 0x01:
            case 0x1B: // WzImageHeaderByte_WithOffset
                return extractMore(reader, offset, endOfBlock, name, reader.readStringAtOffset(offset + reader.readInt32()), parent, imgParent);
            case 0x00:
            case 0x73: // WzImageHeaderByte_WithoutOffset
                return extractMore(reader, offset, endOfBlock, name, "", parent, imgParent);
            default:
                throw new WzException("Invalid byte read at ParseExtendedProp: " + (b & 0xFF));
        }
    }

    /**
     * Extract extended property by type name.
     * Ported from: WzImageProperty.ExtractMore()
     */
    private static WzImageProperty extractMore(WzBinaryReader reader, long offset, long eob, String name, String iname, WzObject parent, WzImage imgParent) throws IOException {
        if (iname.isEmpty()) {
            iname = reader.readString();
        }
        switch (iname) {
            case "Property": {
                WzSubProperty subProp = new WzSubProperty(name);
                subProp.setParent(parent);
                reader.skip(2); // Reserved
                subProp.addProperties(parsePropertyList(offset, reader, subProp, imgParent));
                return subProp;
            }
            case "Canvas": {
                WzCanvasProperty canvasProp = new WzCanvasProperty(name);
                canvasProp.setParent(parent);
                reader.skip(1);
                if ((reader.readByte() & 0xFF) == 1) {
                    reader.skip(2);
                    canvasProp.addProperties(parsePropertyList(offset, reader, canvasProp, imgParent));
                }
                canvasProp.setPngProperty(new WzPngProperty(reader, false));
                return canvasProp;
            }
            case "Shape2D#Vector2D": {
                WzVectorProperty vecProp = new WzVectorProperty(name);
                vecProp.setParent(parent);
                vecProp.setX(new WzIntProperty("X", reader.readCompressedInt()));
                vecProp.setY(new WzIntProperty("Y", reader.readCompressedInt()));
                return vecProp;
            }
            case "Shape2D#Convex2D": {
                WzConvexProperty convexProp = new WzConvexProperty(name);
                convexProp.setParent(parent);
                int count = reader.readCompressedInt();
                for (int i = 0; i < count; i++) {
                    convexProp.addProperty(parseExtendedProp(reader, offset, 0, name, convexProp, imgParent));
                }
                return convexProp;
            }
            case "Sound_DX8": {
                WzBinaryProperty soundProp = new WzBinaryProperty(name, reader, false);
                soundProp.setParent(parent);
                return soundProp;
            }
            case "UOL": {
                reader.skip(1);
                byte uolType = reader.readByte();
                String uolPath;
                if ((uolType & 0xFF) == 0) {
                    uolPath = reader.readString();
                } else {
                    uolPath = reader.readStringAtOffset(offset + reader.readInt32());
                }
                WzUOLProperty uol = new WzUOLProperty(name, uolPath);
                uol.setParent(parent);
                return uol;
            }
            case "RawData": {
                // GMS v220+ skeleton/raw binary data
                // Ported from: WzImageProperty.ExtractMore() RawData branch
                byte rawType = reader.readByte();
                WzRawDataProperty rawProp = new WzRawDataProperty(name, reader, rawType);
                rawProp.setParent(parent);
                // type 1: has sub-properties (like CanvasProperty)
                if (rawType == 1) {
                    if ((reader.readByte() & 0xFF) == 1) {
                        reader.skip(2); // Reserved
                        rawProp.addProperties(parsePropertyList(offset, reader, rawProp, imgParent));
                    }
                }
                // All types: parse the binary data
                rawProp.parse(false);
                return rawProp;
            }
            case "Canvas#Video": {
                // KMST v1181+ video property
                // Ported from: WzImageProperty.ExtractMore() Canvas#Video branch
                WzVideoProperty videoProp = new WzVideoProperty(name, reader);
                videoProp.setParent(parent);
                reader.skip(1);
                if ((reader.readByte() & 0xFF) == 1) {
                    reader.skip(2); // Reserved
                    videoProp.addProperties(parsePropertyList(offset, reader, videoProp, imgParent));
                }
                videoProp.parse(false);
                return videoProp;
            }
            default:
                throw new WzException("Unknown extended property type: " + iname);
        }
    }

    /**
     * Write a property list.
     * Ported from: WzImageProperty.WritePropertyList()
     */
    public static void writePropertyList(wzlib.util.WzBinaryWriter writer, java.util.List<WzImageProperty> properties) throws java.io.IOException {
        writer.writeUInt16(0); // reserved ushort 0
        writer.writeCompressedInt(properties.size());
        for (WzImageProperty prop : properties) {
            writer.writeStringValue(prop.getName(), 0x00, 0x01);
            if (prop instanceof wzlib.property.WzSubProperty || prop instanceof wzlib.property.WzCanvasProperty
                || prop instanceof wzlib.property.WzVectorProperty || prop instanceof wzlib.property.WzConvexProperty
                || prop instanceof wzlib.property.WzBinaryProperty || prop instanceof wzlib.property.WzUOLProperty) {
                writeExtendedValue(writer, prop);
            } else {
                prop.writeValue(writer);
            }
        }
    }

    /**
     * Write extended property wrapper (type byte 9 + length placeholder + value).
     * Ported from: WzImageProperty.WriteExtendedValue()
     */
    public static void writeExtendedValue(wzlib.util.WzBinaryWriter writer, WzImageProperty property) throws java.io.IOException {
        writer.writeByte((byte) 9);
        // We need to calculate the length. Write to a temp buffer first.
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        wzlib.util.WzBinaryWriter tempWriter = new wzlib.util.WzBinaryWriter(baos, writer.getWzKey().getIv());
        property.writeValue(tempWriter);
        tempWriter.flush();
        byte[] data = baos.toByteArray();
        writer.writeInt32(data.length);
        writer.writeBytes(data);
    }

    /** Deep clone this property and all children. */
    public abstract WzImageProperty deepClone();

    /** Override in each property subclass */
    public abstract void writeValue(wzlib.util.WzBinaryWriter writer) throws java.io.IOException;

    private static WzImageProperty withParent(WzImageProperty prop, WzObject parent) {
        prop.setParent(parent);
        return prop;
    }
}
