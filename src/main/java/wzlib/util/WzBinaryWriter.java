package wzlib.util;

import wzlib.WzHeader;
import wzlib.crypto.WzCryptoConstants;
import wzlib.crypto.WzKeyGenerator;
import wzlib.crypto.WzMutableKey;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Ported from: MapleLib/WzLib/Util/WzBinaryWriter.cs
 *
 * Little-endian binary writer with WZ-specific features:
 * - Compressed int/long
 * - Encrypted WZ strings (ASCII + Unicode)
 * - Encrypted offsets
 * - String caching for offset-based references
 */
public class WzBinaryWriter implements Closeable {

    private final OutputStream out;
    private final BufferedOutputStream bos;
    private final WzMutableKey wzKey;
    private long position;
    private final Map<String, Integer> stringCache;
    private long hash;
    private WzHeader header;
    private final boolean leaveOpen;

    public WzBinaryWriter(OutputStream output, byte[] wzIv) {
        this(output, wzIv, false);
    }

    public WzBinaryWriter(OutputStream output, byte[] wzIv, long hash) {
        this(output, wzIv, false);
        this.hash = hash;
    }

    /**
     * Ported from: WzBinaryWriter(Stream output, byte[] WzIv, bool leaveOpen)
     */
    public WzBinaryWriter(OutputStream output, byte[] wzIv, boolean leaveOpen) {
        this.out = output;
        this.bos = (output instanceof BufferedOutputStream) ? (BufferedOutputStream) output : new BufferedOutputStream(output);
        this.wzKey = WzKeyGenerator.generateWzKey(wzIv);
        this.stringCache = new HashMap<>();
        this.position = 0;
        this.hash = 0;
        this.leaveOpen = leaveOpen;
    }

    // ---- Properties ----

    public WzMutableKey getWzKey() { return wzKey; }
    public long getHash() { return hash; }
    public void setHash(long hash) { this.hash = hash; }
    public WzHeader getHeader() { return header; }
    public void setHeader(WzHeader header) { this.header = header; }
    public Map<String, Integer> getStringCache() { return stringCache; }
    public long getPosition() { return position; }

    // ---- Primitive writes (little-endian) ----

    public void writeByte(byte v) throws IOException {
        bos.write(v & 0xFF);
        position++;
    }

    public void writeInt16(short v) throws IOException {
        byte[] buf = new byte[2];
        ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).putShort(v);
        bos.write(buf);
        position += 2;
    }

    public void writeUInt16(int v) throws IOException {
        writeInt16((short) v);
    }

    public void writeInt32(int v) throws IOException {
        byte[] buf = new byte[4];
        ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).putInt(v);
        bos.write(buf);
        position += 4;
    }

    public void writeUInt32(long v) throws IOException {
        writeInt32((int) v);
    }

    public void writeInt64(long v) throws IOException {
        byte[] buf = new byte[8];
        ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).putLong(v);
        bos.write(buf);
        position += 8;
    }

    public void writeSingle(float v) throws IOException {
        byte[] buf = new byte[4];
        ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).putFloat(v);
        bos.write(buf);
        position += 4;
    }

    public void writeDouble(double v) throws IOException {
        byte[] buf = new byte[8];
        ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).putDouble(v);
        bos.write(buf);
        position += 8;
    }

    public void writeBytes(byte[] data) throws IOException {
        bos.write(data);
        position += data.length;
    }

    // ---- WZ-specific writes ----

    /**
     * Write WZ encrypted string (auto-detect Unicode vs ASCII).
     * Ported from: WzBinaryWriter.Write(string value)
     */
    public void writeString(String value) throws IOException {
        if (value.isEmpty()) {
            writeByte((byte) 0);
            return;
        }

        boolean unicode = false;
        for (char c : value.toCharArray()) {
            if (c > 127) { // sbyte.MaxValue in C#
                unicode = true;
                break;
            }
        }

        if (unicode) {
            writeUnicodeString(value);
        } else {
            writeAsciiString(value);
        }
    }

    /**
     * Ported from: WzBinaryWriter.WriteUnicodeString(string)
     */
    private void writeUnicodeString(String value) throws IOException {
        if (value.length() >= 127) { // >= sbyte.MaxValue
            writeByte((byte) 127); // sbyte.MaxValue
            writeInt32(value.length());
        } else {
            writeByte((byte) value.length());
        }

        int mask = 0xAAAA;
        for (int i = 0; i < value.length(); i++) {
            int encryptedChar = value.charAt(i);
            encryptedChar ^= ((wzKey.at(i * 2 + 1) & 0xFF) << 8) + (wzKey.at(i * 2) & 0xFF);
            encryptedChar ^= mask;
            mask++;
            writeUInt16(encryptedChar);
        }
    }

    /**
     * Ported from: WzBinaryWriter.WriteAsciiString(string)
     */
    private void writeAsciiString(String value) throws IOException {
        if (value.length() > 127) { // > sbyte.MaxValue (note: no >= here, per C# comment about 2's complement)
            writeByte((byte) -128); // sbyte.MinValue
            writeInt32(value.length());
        } else {
            writeByte((byte) (-value.length()));
        }

        int mask = 0xAA;
        for (int i = 0; i < value.length(); i++) {
            int encryptedChar = value.charAt(i) & 0xFF;
            encryptedChar ^= (wzKey.at(i) & 0xFF);
            encryptedChar ^= mask;
            mask++;
            writeByte((byte) encryptedChar);
        }
    }

    /**
     * Write string value with offset caching.
     * Ported from: WzBinaryWriter.WriteStringValue(string, int, int)
     */
    public void writeStringValue(String str, int withoutOffset, int withOffset) throws IOException {
        if (str.length() > 4 && stringCache.containsKey(str)) {
            writeByte((byte) withOffset);
            writeInt32(stringCache.get(str));
        } else {
            writeByte((byte) withoutOffset);
            int sOffset = (int) position;
            writeString(str);
            if (!stringCache.containsKey(str)) {
                stringCache.put(str, sOffset);
            }
        }
    }

    /**
     * Write WZ object value (directory entry name) with type-prefixed caching.
     * Ported from: WzBinaryWriter.WriteWzObjectValue(string, WzDirectoryType)
     *
     * @param stringObjectValue the string value
     * @param type directory entry type byte
     * @return true if written as offset reference
     */
    public boolean writeWzObjectValue(String stringObjectValue, byte type) throws IOException {
        String storeName = type + "_" + stringObjectValue;

        if (stringObjectValue.length() > 4 && stringCache.containsKey(storeName)) {
            writeByte((byte) 2); // WzDirectoryType.RetrieveStringFromOffset_2
            writeInt32(stringCache.get(storeName));
            return true;
        } else {
            int sOffset = (int) (position - header.getFStart());
            writeByte(type);
            writeString(stringObjectValue);
            if (!stringCache.containsKey(storeName)) {
                stringCache.put(storeName, sOffset);
            }
        }
        return false;
    }

    /**
     * Ported from: WzBinaryWriter.WriteCompressedInt(int)
     */
    public void writeCompressedInt(int value) throws IOException {
        if (value > 127 || value <= -128) { // > sbyte.MaxValue || <= sbyte.MinValue
            writeByte((byte) -128); // sbyte.MinValue
            writeInt32(value);
        } else {
            writeByte((byte) value);
        }
    }

    /**
     * Ported from: WzBinaryWriter.WriteCompressedLong(long)
     */
    public void writeCompressedLong(long value) throws IOException {
        if (value > 127 || value <= -128) {
            writeByte((byte) -128);
            writeInt64(value);
        } else {
            writeByte((byte) value);
        }
    }

    /**
     * Write encrypted offset.
     * Ported from: WzBinaryWriter.WriteOffset(long)
     *
     * C#:
     *   uint encOffset = (uint)BaseStream.Position;
     *   encOffset = (encOffset - Header.FStart) ^ 0xFFFFFFFF;
     *   encOffset *= Hash;
     *   encOffset -= WZ_OffsetConstant;
     *   encOffset = RotateLeft(encOffset, (byte)(encOffset & 0x1F));
     *   uint writeOffset = encOffset ^ ((uint)value - (Header.FStart * 2));
     *   Write(writeOffset);
     */
    public void writeOffset(long value) throws IOException {
        // Use int arithmetic to match C# uint overflow behavior (same fix as readOffset)
        int encOffset = (int) position;
        encOffset = (encOffset - (int) header.getFStart()) ^ 0xFFFFFFFF;
        encOffset *= (int) hash;
        encOffset -= (int) WzCryptoConstants.WZ_OffsetConstant;
        int n = encOffset & 0x1F;
        encOffset = (encOffset << n) | (encOffset >>> (32 - n));
        int writeOff = encOffset ^ ((int) value - (int) header.getFStart() * 2);
        writeInt32(writeOff);
    }

    public void writeNullTerminatedString(String value) throws IOException {
        writeBytes(value.getBytes(StandardCharsets.UTF_8));
        writeByte((byte) 0);
    }

    public void flush() throws IOException {
        bos.flush();
    }

    @Override
    public void close() throws IOException {
        if (!leaveOpen) {
            bos.flush();
            bos.close();
        }
    }
}
