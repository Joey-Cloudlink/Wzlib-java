package wzlib.util;

import wzlib.*;
import wzlib.property.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 從 XML 格式匯入 WZ 物件。
 * Imports WZ objects from XML format.
 *
 * <p>Ported from: MapleLib/WzLib/Serializer/WzXmlDeserializer.cs</p>
 */
public final class WzXmlDeserializer {

    private WzXmlDeserializer() {}

    /**
     * 解析 XML 檔案為 WzImageProperty 列表。
     * Parse an XML file into a list of WzImageProperty.
     */
    public static List<WzImageProperty> parseXml(File xmlFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);
        doc.getDocumentElement().normalize();

        Element root = doc.getDocumentElement();
        return parseChildren(root);
    }

    /**
     * Parse XML string into properties.
     */
    public static List<WzImageProperty> parseXmlString(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new org.xml.sax.InputSource(new StringReader(xml)));
        doc.getDocumentElement().normalize();

        Element root = doc.getDocumentElement();
        return parseChildren(root);
    }

    /**
     * 解析 XML 為 WzImage 物件。
     * Parse XML into a WzImage object.
     */
    public static WzImage parseXmlImage(File xmlFile, String imageName) throws Exception {
        List<WzImageProperty> props = parseXml(xmlFile);
        WzImage image = new WzImage(imageName, null, 0);
        for (WzImageProperty prop : props) {
            prop.setParent(image);
        }
        // Access internal properties list
        image.getProperties().addAll(props);
        image.setChanged(true);
        return image;
    }

    /**
     * Parse child elements of a parent XML element.
     */
    private static List<WzImageProperty> parseChildren(Element parent) {
        List<WzImageProperty> properties = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;
            Element elem = (Element) node;
            WzImageProperty prop = parsePropertyFromXml(elem);
            if (prop != null) {
                properties.add(prop);
            }
        }
        return properties;
    }

    /**
     * Parse a single XML element into a WzImageProperty.
     * Ported from: WzXmlDeserializer.ParsePropertyFromXMLElement()
     */
    private static WzImageProperty parsePropertyFromXml(Element elem) {
        String tag = elem.getTagName();
        String name = elem.getAttribute("name");

        switch (tag) {
            case "imgdir": {
                WzSubProperty sub = new WzSubProperty(name);
                for (WzImageProperty child : parseChildren(elem)) {
                    sub.addProperty(child);
                }
                return sub;
            }
            case "canvas": {
                WzCanvasProperty canvas = new WzCanvasProperty(name);
                // Parse child properties
                for (WzImageProperty child : parseChildren(elem)) {
                    canvas.addProperty(child);
                }
                // Parse PNG data
                int width = getIntAttr(elem, "width", 0);
                int height = getIntAttr(elem, "height", 0);
                String basedata = elem.getAttribute("basedata");
                if (!basedata.isEmpty() && width > 0 && height > 0) {
                    byte[] compressed = Base64.getDecoder().decode(basedata);
                    WzPngProperty png = new WzPngProperty();
                    png.setCompressedImageBytes(compressed);
                    // We need to set width/height/format on PngProperty
                    // Since fields are private, use a factory method approach
                    setPngDimensions(png, width, height);
                    canvas.setPngProperty(png);
                }
                return canvas;
            }
            case "int":
                return new WzIntProperty(name, getIntAttr(elem, "value", 0));
            case "short":
                return new WzShortProperty(name, (short) getIntAttr(elem, "value", 0));
            case "long":
                return new WzLongProperty(name, getLongAttr(elem, "value", 0));
            case "float":
                return new WzFloatProperty(name, getFloatAttr(elem, "value", 0));
            case "double":
                return new WzDoubleProperty(name, getDoubleAttr(elem, "value", 0));
            case "string":
                return new WzStringProperty(name, elem.getAttribute("value"));
            case "null":
                return new WzNullProperty(name);
            case "vector": {
                WzVectorProperty vec = new WzVectorProperty(name);
                vec.setX(new WzIntProperty("X", getIntAttr(elem, "x", 0)));
                vec.setY(new WzIntProperty("Y", getIntAttr(elem, "y", 0)));
                return vec;
            }
            case "uol":
                return new WzUOLProperty(name, elem.getAttribute("value"));
            case "sound": {
                // Sound from XML with base64 data
                String basehead = elem.getAttribute("basehead");
                String basedata = elem.getAttribute("basedata");
                int length = getIntAttr(elem, "length", 0);
                // Create a minimal WzBinaryProperty from XML data
                // This requires the XML to contain the full sound data
                return createSoundFromXml(name, basehead, basedata, length);
            }
            case "extended": {
                WzConvexProperty convex = new WzConvexProperty(name);
                for (WzImageProperty child : parseChildren(elem)) {
                    convex.addProperty(child);
                }
                return convex;
            }
            case "rawdata": {
                String basedata = elem.getAttribute("basedata");
                // Create minimal RawData property
                WzRawDataProperty raw = new WzRawDataProperty(name, null, (byte) 0);
                // If data is available, store it
                return raw;
            }
            case "lua": {
                String value = elem.getAttribute("value");
                // Lua stores encrypted bytes; from XML we get the decrypted string
                // Re-encrypt it for storage
                WzLuaProperty lua = new WzLuaProperty(name, new byte[0]);
                byte[] encrypted = lua.encodeDecode(value.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
                lua.setValue(encrypted);
                return lua;
            }
            default:
                return null;
        }
    }

    /**
     * Set dimensions on WzPngProperty via reflection or public setter.
     */
    private static void setPngDimensions(WzPngProperty png, int width, int height) {
        try {
            java.lang.reflect.Field wField = WzPngProperty.class.getDeclaredField("width");
            java.lang.reflect.Field hField = WzPngProperty.class.getDeclaredField("height");
            java.lang.reflect.Field fField = WzPngProperty.class.getDeclaredField("format");
            wField.setAccessible(true);
            hField.setAccessible(true);
            fField.setAccessible(true);
            wField.setInt(png, width);
            hField.setInt(png, height);
            fField.setInt(png, 2); // default BGRA8888
        } catch (Exception e) {
            throw new WzException("Failed to set PNG dimensions", e);
        }
    }

    /**
     * Create a WzBinaryProperty from XML sound data.
     */
    private static WzImageProperty createSoundFromXml(String name, String basehead, String basedata, int length) {
        // We need a constructor that doesn't read from a reader
        // Use reflection or create a factory
        try {
            java.lang.reflect.Constructor<WzBinaryProperty> ctor =
                WzBinaryProperty.class.getDeclaredConstructor(String.class);
            ctor.setAccessible(true);
            WzBinaryProperty sound = ctor.newInstance(name);

            // Set fields via reflection
            if (!basehead.isEmpty()) {
                java.lang.reflect.Field headerField = WzBinaryProperty.class.getDeclaredField("header");
                headerField.setAccessible(true);
                headerField.set(sound, Base64.getDecoder().decode(basehead));
            }
            if (!basedata.isEmpty()) {
                java.lang.reflect.Field bytesField = WzBinaryProperty.class.getDeclaredField("fileBytes");
                bytesField.setAccessible(true);
                bytesField.set(sound, Base64.getDecoder().decode(basedata));
            }
            java.lang.reflect.Field lenField = WzBinaryProperty.class.getDeclaredField("soundDataLen");
            lenField.setAccessible(true);
            lenField.setInt(sound, length);

            return sound;
        } catch (Exception e) {
            // Fallback: return null property
            return new WzNullProperty(name);
        }
    }

    // ---- Attribute helpers ----

    private static int getIntAttr(Element elem, String attr, int def) {
        String val = elem.getAttribute(attr);
        if (val == null || val.isEmpty()) return def;
        try { return Integer.parseInt(val); } catch (NumberFormatException e) { return def; }
    }

    private static long getLongAttr(Element elem, String attr, long def) {
        String val = elem.getAttribute(attr);
        if (val == null || val.isEmpty()) return def;
        try { return Long.parseLong(val); } catch (NumberFormatException e) { return def; }
    }

    private static float getFloatAttr(Element elem, String attr, float def) {
        String val = elem.getAttribute(attr);
        if (val == null || val.isEmpty()) return def;
        try { return Float.parseFloat(val); } catch (NumberFormatException e) { return def; }
    }

    private static double getDoubleAttr(Element elem, String attr, double def) {
        String val = elem.getAttribute(attr);
        if (val == null || val.isEmpty()) return def;
        try { return Double.parseDouble(val); } catch (NumberFormatException e) { return def; }
    }
}
