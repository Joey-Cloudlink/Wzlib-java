package wzlib;

import wzlib.property.*;

import java.util.List;

/**
 * Phase 3 test: parse images and print property trees.
 * Usage: java wzlib.WzImageParseTest [path_to_wz] [version]
 */
public class WzImageParseTest {

    public static void main(String[] args) throws Exception {
        String wzPath = args.length > 0 ? args[0] : "C:/Users/Joey/Desktop/item.wz";
        WzMapleVersion ver = args.length > 1 ? WzMapleVersion.valueOf(args[1]) : WzMapleVersion.EMS;

        System.out.println("Opening: " + wzPath + " (" + ver + ")");

        try (WzFile wz = new WzFile(wzPath, ver)) {
            wz.parseWzFile();

            WzDirectory root = wz.getRoot();
            System.out.println("Root: " + root.getName());
            System.out.println("Directories: " + root.getWzDirectories().size());
            System.out.println("Images: " + root.countImages());
            System.out.println();

            // Parse first image from each directory
            for (WzDirectory dir : root.getWzDirectories()) {
                System.out.println("=== DIR: " + dir.getName() + " ===");
                List<WzImage> images = dir.getWzImages();
                if (images.isEmpty()) continue;

                // Parse the first image
                WzImage img = images.get(0);
                System.out.println("Parsing image: " + img.getName());

                try {
                    List<WzImageProperty> props = img.getProperties();
                    System.out.println("  Properties count: " + props.size());
                    printProperties(props, 2);
                } catch (Exception e) {
                    System.out.println("  ERROR: " + e.getMessage());
                    e.printStackTrace();
                }
                System.out.println();
            }
        }
    }

    static void printProperties(List<WzImageProperty> props, int indent) {
        if (props == null) return;
        String pad = " ".repeat(indent);
        int count = 0;
        for (WzImageProperty p : props) {
            if (count >= 30) {
                System.out.println(pad + "... and " + (props.size() - 30) + " more");
                break;
            }
            String typeName = p.getPropertyType().name();
            String value = formatValue(p);
            System.out.println(pad + "[" + typeName + "] " + p.getName() + value);

            // Recurse into sub-properties
            List<WzImageProperty> subProps = p.getProperties();
            if (subProps != null && !subProps.isEmpty()) {
                printProperties(subProps, indent + 2);
            }
            count++;
        }
    }

    static String formatValue(WzImageProperty p) {
        switch (p.getPropertyType()) {
            case Null: return "";
            case Short: return " = " + ((WzShortProperty) p).getShort();
            case Int: return " = " + ((WzIntProperty) p).getInt();
            case Long: return " = " + ((WzLongProperty) p).getLong();
            case Float: return " = " + ((WzFloatProperty) p).getFloat();
            case Double: return " = " + ((WzDoubleProperty) p).getDouble();
            case String: return " = \"" + ((WzStringProperty) p).getString() + "\"";
            case UOL: return " -> " + ((WzUOLProperty) p).getUOL();
            case Vector: {
                WzVectorProperty v = (WzVectorProperty) p;
                return " = (" + v.getX().getInt() + ", " + v.getY().getInt() + ")";
            }
            case Canvas: {
                WzCanvasProperty c = (WzCanvasProperty) p;
                WzPngProperty png = c.getPngProperty();
                if (png != null) {
                    return " [" + png.getWidth() + "x" + png.getHeight() + " fmt=" + png.getFormat() + "]";
                }
                return " [canvas]";
            }
            case Sound: {
                WzBinaryProperty s = (WzBinaryProperty) p;
                return " [" + s.getLenMs() + "ms, " + s.getSoundDataLen() + " bytes]";
            }
            case SubProperty: return " {" + ((WzSubProperty) p).getProperties().size() + " props}";
            default: return "";
        }
    }
}
