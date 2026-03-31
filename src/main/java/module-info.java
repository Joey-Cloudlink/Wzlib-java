module wzlib {
    requires java.desktop;  // java.awt for BufferedImage
    requires java.xml;      // javax.xml for XML parsing

    exports wzlib;
    exports wzlib.property;
    exports wzlib.crypto;
    exports wzlib.util;
}
