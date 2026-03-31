package wzlib.util;

import wzlib.*;
import wzlib.property.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * 將 WZ 物件匯出為 XML 格式。
 * Exports WZ objects to XML format.
 *
 * <p>Ported from: WzSerializer.cs + WzClassicXmlSerializer.cs</p>
 */
public final class WzXmlSerializer {

    private WzXmlSerializer() {}

    /**
     * 匯出 WzImage 為 XML 檔案。
     * Export a WzImage to an XML file.
     */
    public static void serializeImage(WzImage image, File outputFile) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
            writer.newLine();
            serializeImage(image, writer);
        }
    }

    /**
     * Export a WzImage to a writer.
     */
    public static void serializeImage(WzImage image, Writer writer) throws IOException {
        List<WzImageProperty> props = image.getProperties();
        writer.write("<imgdir name=\"" + sanitize(image.getName()) + "\">");
        writer.write(System.lineSeparator());
        for (WzImageProperty prop : props) {
            writePropertyToXml(writer, prop, 1);
        }
        writer.write("</imgdir>");
        writer.write(System.lineSeparator());
    }

    /**
     * Export entire WzFile directory structure.
     */
    public static void serializeDirectory(WzDirectory dir, File outputDir) throws IOException {
        if (!outputDir.exists()) outputDir.mkdirs();

        // Export images in this directory
        for (WzImage img : dir.getWzImages()) {
            File xmlFile = new File(outputDir, img.getName() + ".xml");
            serializeImage(img, xmlFile);
        }

        // Recurse into subdirectories
        for (WzDirectory subDir : dir.getWzDirectories()) {
            File subOutputDir = new File(outputDir, subDir.getName());
            serializeDirectory(subDir, subOutputDir);
        }
    }

    /**
     * 匯出整個 WzFile 到指定目錄。
     * Export an entire WzFile to the specified directory.
     */
    public static void serializeFile(WzFile wzFile, File outputDir) throws IOException {
        serializeDirectory(wzFile.getRoot(), outputDir);
    }

    /**
     * Write a single property to XML.
     * Ported from: WzSerializer.WritePropertyToXML()
     */
    public static void writePropertyToXml(Writer writer, WzImageProperty prop, int level) throws IOException {
        String indent = indent(level);

        if (prop instanceof WzSubProperty) {
            WzSubProperty sub = (WzSubProperty) prop;
            writer.write(indent + "<imgdir name=\"" + sanitize(prop.getName()) + "\">");
            writer.write(System.lineSeparator());
            for (WzImageProperty child : sub.getProperties()) {
                writePropertyToXml(writer, child, level + 1);
            }
            writer.write(indent + "</imgdir>");
            writer.write(System.lineSeparator());

        } else if (prop instanceof WzCanvasProperty) {
            WzCanvasProperty canvas = (WzCanvasProperty) prop;
            WzPngProperty png = canvas.getPngProperty();
            String attrs = "name=\"" + sanitize(prop.getName()) + "\"";
            if (png != null) {
                attrs += " width=\"" + png.getWidth() + "\" height=\"" + png.getHeight() + "\"";
                // Include base64 PNG data
                try {
                    byte[] compressed = png.getCompressedBytes(false);
                    if (compressed != null) {
                        attrs += " basedata=\"" + Base64.getEncoder().encodeToString(compressed) + "\"";
                    }
                } catch (Exception e) { /* skip base data */ }
            }

            List<WzImageProperty> children = canvas.getProperties();
            if (children != null && !children.isEmpty()) {
                writer.write(indent + "<canvas " + attrs + ">");
                writer.write(System.lineSeparator());
                for (WzImageProperty child : children) {
                    writePropertyToXml(writer, child, level + 1);
                }
                writer.write(indent + "</canvas>");
            } else {
                writer.write(indent + "<canvas " + attrs + "/>");
            }
            writer.write(System.lineSeparator());

        } else if (prop instanceof WzIntProperty) {
            writer.write(indent + "<int name=\"" + sanitize(prop.getName()) + "\" value=\"" + ((WzIntProperty) prop).getInt() + "\"/>");
            writer.write(System.lineSeparator());

        } else if (prop instanceof WzShortProperty) {
            writer.write(indent + "<short name=\"" + sanitize(prop.getName()) + "\" value=\"" + ((WzShortProperty) prop).getShort() + "\"/>");
            writer.write(System.lineSeparator());

        } else if (prop instanceof WzLongProperty) {
            writer.write(indent + "<long name=\"" + sanitize(prop.getName()) + "\" value=\"" + ((WzLongProperty) prop).getLong() + "\"/>");
            writer.write(System.lineSeparator());

        } else if (prop instanceof WzFloatProperty) {
            writer.write(indent + "<float name=\"" + sanitize(prop.getName()) + "\" value=\"" + ((WzFloatProperty) prop).getFloat() + "\"/>");
            writer.write(System.lineSeparator());

        } else if (prop instanceof WzDoubleProperty) {
            writer.write(indent + "<double name=\"" + sanitize(prop.getName()) + "\" value=\"" + ((WzDoubleProperty) prop).getDouble() + "\"/>");
            writer.write(System.lineSeparator());

        } else if (prop instanceof WzStringProperty) {
            writer.write(indent + "<string name=\"" + sanitize(prop.getName()) + "\" value=\"" + sanitize(((WzStringProperty) prop).getString()) + "\"/>");
            writer.write(System.lineSeparator());

        } else if (prop instanceof WzNullProperty) {
            writer.write(indent + "<null name=\"" + sanitize(prop.getName()) + "\"/>");
            writer.write(System.lineSeparator());

        } else if (prop instanceof WzVectorProperty) {
            WzVectorProperty vec = (WzVectorProperty) prop;
            writer.write(indent + "<vector name=\"" + sanitize(prop.getName()) + "\" x=\"" + vec.getX().getInt() + "\" y=\"" + vec.getY().getInt() + "\"/>");
            writer.write(System.lineSeparator());

        } else if (prop instanceof WzUOLProperty) {
            writer.write(indent + "<uol name=\"" + sanitize(prop.getName()) + "\" value=\"" + sanitize(((WzUOLProperty) prop).getUOL()) + "\"/>");
            writer.write(System.lineSeparator());

        } else if (prop instanceof WzBinaryProperty) {
            WzBinaryProperty sound = (WzBinaryProperty) prop;
            String attrs = "name=\"" + sanitize(prop.getName()) + "\" length=\"" + sound.getSoundDataLen() + "\"";
            if (sound.getHeader() != null) {
                attrs += " basehead=\"" + Base64.getEncoder().encodeToString(sound.getHeader()) + "\"";
            }
            try {
                byte[] data = sound.getSoundBytes(false);
                if (data != null) {
                    attrs += " basedata=\"" + Base64.getEncoder().encodeToString(data) + "\"";
                }
            } catch (Exception e) { /* skip */ }
            writer.write(indent + "<sound " + attrs + "/>");
            writer.write(System.lineSeparator());

        } else if (prop instanceof WzConvexProperty) {
            WzConvexProperty convex = (WzConvexProperty) prop;
            writer.write(indent + "<extended name=\"" + sanitize(prop.getName()) + "\">");
            writer.write(System.lineSeparator());
            for (WzImageProperty child : convex.getProperties()) {
                writePropertyToXml(writer, child, level + 1);
            }
            writer.write(indent + "</extended>");
            writer.write(System.lineSeparator());

        } else if (prop instanceof WzRawDataProperty) {
            WzRawDataProperty raw = (WzRawDataProperty) prop;
            String attrs = "name=\"" + sanitize(prop.getName()) + "\"";
            byte[] data = raw.getBytes(false);
            if (data != null) {
                attrs += " basedata=\"" + Base64.getEncoder().encodeToString(data) + "\"";
            }
            writer.write(indent + "<rawdata " + attrs + "/>");
            writer.write(System.lineSeparator());

        } else if (prop instanceof WzLuaProperty) {
            WzLuaProperty lua = (WzLuaProperty) prop;
            writer.write(indent + "<lua name=\"" + sanitize(prop.getName()) + "\" value=\"" + sanitize(lua.getString()) + "\"/>");
            writer.write(System.lineSeparator());
        }
    }

    // ---- Helpers ----

    /** Sanitize text for XML. Ported from: XmlUtil.SanitizeText() */
    public static String sanitize(String text) {
        if (text == null) return "";
        return text
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    private static String indent(int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) sb.append("  ");
        return sb.toString();
    }
}
