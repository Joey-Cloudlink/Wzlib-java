package wzlib.util;

import wzlib.WzHeader;
import wzlib.crypto.WzCryptoConstants;
import wzlib.crypto.WzKeyGenerator;
import wzlib.crypto.WzMutableKey;

import java.io.*;
import java.nio.file.Files;

/**
 * Round-trip tests for WzBinaryReader/Writer.
 */
public class WzBinaryRWTest {

    public static void main(String[] args) throws Exception {
        testCompressedIntRoundTrip();
        testCompressedLongRoundTrip();
        testStringRoundTrip();
        testPrimitiveRoundTrip();
        System.out.println("\nAll BinaryRW tests passed!");
    }

    static void testCompressedIntRoundTrip() throws Exception {
        System.out.print("testCompressedIntRoundTrip... ");
        int[] values = {0, 1, -1, 127, -127, 128, -128, -129, 256, 100000, -100000};

        File tmp = File.createTempFile("wztest", ".bin");
        tmp.deleteOnExit();
        byte[] iv = WzCryptoConstants.WZ_BMSCLASSIC; // no encryption for simplicity

        // Write
        try (FileOutputStream fos = new FileOutputStream(tmp);
             WzBinaryWriter writer = new WzBinaryWriter(fos, iv)) {
            for (int v : values) {
                writer.writeCompressedInt(v);
            }
        }

        // Read
        try (WzBinaryReader reader = new WzBinaryReader(tmp, iv)) {
            for (int v : values) {
                int read = reader.readCompressedInt();
                assert read == v : "CompressedInt: expected " + v + " got " + read;
            }
        }

        System.out.println("OK");
    }

    static void testCompressedLongRoundTrip() throws Exception {
        System.out.print("testCompressedLongRoundTrip... ");
        long[] values = {0, 1, -1, 127, -127, 128, -128, 1000000000L, -1000000000L};

        File tmp = File.createTempFile("wztest", ".bin");
        tmp.deleteOnExit();
        byte[] iv = WzCryptoConstants.WZ_BMSCLASSIC;

        try (FileOutputStream fos = new FileOutputStream(tmp);
             WzBinaryWriter writer = new WzBinaryWriter(fos, iv)) {
            for (long v : values) {
                writer.writeCompressedLong(v);
            }
        }

        try (WzBinaryReader reader = new WzBinaryReader(tmp, iv)) {
            for (long v : values) {
                long read = reader.readCompressedLong();
                assert read == v : "CompressedLong: expected " + v + " got " + read;
            }
        }

        System.out.println("OK");
    }

    static void testStringRoundTrip() throws Exception {
        System.out.print("testStringRoundTrip... ");
        String[] values = {
            "",
            "hello",
            "test123",
            "MapleStory",
            "a",                  // single char
            "ABCDEFGHIJKLMNOP",   // 16 chars
        };

        // Test with BMS IV (no encryption) — simpler case
        File tmp = File.createTempFile("wztest", ".bin");
        tmp.deleteOnExit();
        byte[] iv = WzCryptoConstants.WZ_BMSCLASSIC;

        try (FileOutputStream fos = new FileOutputStream(tmp);
             WzBinaryWriter writer = new WzBinaryWriter(fos, iv)) {
            for (String v : values) {
                writer.writeString(v);
            }
        }

        try (WzBinaryReader reader = new WzBinaryReader(tmp, iv)) {
            for (String v : values) {
                String read = reader.readString();
                assert read.equals(v) : "String: expected '" + v + "' got '" + read + "'";
            }
        }

        // Now test with GMS IV (with encryption)
        File tmp2 = File.createTempFile("wztest_gms", ".bin");
        tmp2.deleteOnExit();
        byte[] gmsIv = WzCryptoConstants.WZ_GMSIV;

        try (FileOutputStream fos = new FileOutputStream(tmp2);
             WzBinaryWriter writer = new WzBinaryWriter(fos, gmsIv)) {
            for (String v : values) {
                writer.writeString(v);
            }
        }

        try (WzBinaryReader reader = new WzBinaryReader(tmp2, gmsIv)) {
            for (String v : values) {
                String read = reader.readString();
                assert read.equals(v) : "String (GMS): expected '" + v + "' got '" + read + "'";
            }
        }

        System.out.println("OK");
    }

    static void testPrimitiveRoundTrip() throws Exception {
        System.out.print("testPrimitiveRoundTrip... ");
        File tmp = File.createTempFile("wztest", ".bin");
        tmp.deleteOnExit();
        byte[] iv = WzCryptoConstants.WZ_BMSCLASSIC;

        try (FileOutputStream fos = new FileOutputStream(tmp);
             WzBinaryWriter writer = new WzBinaryWriter(fos, iv)) {
            writer.writeByte((byte) 0x42);
            writer.writeInt16((short) -1234);
            writer.writeInt32(123456789);
            writer.writeInt64(9876543210L);
            writer.writeSingle(3.14f);
            writer.writeDouble(2.71828);
        }

        try (WzBinaryReader reader = new WzBinaryReader(tmp, iv)) {
            assert reader.readByte() == 0x42 : "byte mismatch";
            assert reader.readInt16() == -1234 : "int16 mismatch";
            assert reader.readInt32() == 123456789 : "int32 mismatch";
            assert reader.readInt64() == 9876543210L : "int64 mismatch";
            assert reader.readSingle() == 3.14f : "float mismatch";
            assert reader.readDouble() == 2.71828 : "double mismatch";
        }

        System.out.println("OK");
    }
}
