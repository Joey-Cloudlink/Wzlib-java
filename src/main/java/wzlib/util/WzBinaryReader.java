package wzlib.util;

import wzlib.WzException;
import wzlib.WzHeader;
import wzlib.crypto.WzCryptoConstants;
import wzlib.crypto.WzKeyGenerator;
import wzlib.crypto.WzMutableKey;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Ported from: MapleLib/WzLib/Util/WzBinaryReader.cs
 *
 * Little-endian binary reader with WZ-specific features:
 * - Compressed int/long
 * - Encrypted WZ strings (ASCII + Unicode)
 * - Encrypted offsets
 * - String block reading
 */
public class WzBinaryReader implements Closeable {

    private final DataInputStream dis;
    private final RandomAccessFile raf;
    private final WzMutableKey wzKey;
    private final long startOffset;
    private long position;

    // References set after construction (matching C# pattern)
    private long hash;
    private WzHeader header;

    /**
     * Constructor for file-based reader.
     * Ported from: WzBinaryReader(Stream input, byte[] WzIv, long startOffset)
     */
    public WzBinaryReader(File file, byte[] wzIv) throws IOException {
        this(file, wzIv, 0);
    }

    public WzBinaryReader(File file, byte[] wzIv, long startOffset) throws IOException {
        this.raf = new RandomAccessFile(file, "r");
        this.dis = null;
        this.wzKey = WzKeyGenerator.generateWzKey(wzIv);
        this.startOffset = startOffset;
        this.position = 0;
    }

    /**
     * Constructor for memory-based reader (used by createReaderForSection).
     */
    public WzBinaryReader(byte[] data, WzMutableKey wzKey, long startOffset) {
        this.raf = null;
        this.dis = new DataInputStream(new ByteArrayInputStream(data));
        this.wzKey = wzKey;
        this.startOffset = startOffset;
        this.position = 0;
    }

    // ---- Properties (matching C#) ----

    public WzMutableKey getWzKey() { return wzKey; }

    public long getHash() { return hash; }
    public void setHash(long hash) { this.hash = hash; }

    public WzHeader getHeader() { return header; }
    public void setHeader(WzHeader header) { this.header = header; }

    public long getPosition() throws IOException {
        if (raf != null) {
            return raf.getFilePointer() + startOffset;
        }
        return position + startOffset;
    }

    public void setPosition(long pos) throws IOException {
        long actual = pos - startOffset;
        if (raf != null) {
            raf.seek(actual);
        } else {
            throw new WzException("Cannot seek on memory stream to arbitrary position");
        }
        this.position = actual;
    }

    public long available() throws IOException {
        if (raf != null) {
            return raf.length() - raf.getFilePointer();
        }
        return dis.available();
    }

    // ---- Primitive reads (little-endian) ----

    public byte readByte() throws IOException {
        int b;
        if (raf != null) {
            b = raf.read();
        } else {
            b = dis.read();
        }
        if (b < 0) throw new EOFException("Unexpected end of stream");
        position++;
        return (byte) b;
    }

    /** Read signed byte (sbyte in C#) */
    public byte readSByte() throws IOException {
        return readByte();
    }

    public short readInt16() throws IOException {
        byte[] buf = readBytes(2);
        return ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }

    /** Read unsigned short (ushort in C#) */
    public int readUInt16() throws IOException {
        return readInt16() & 0xFFFF;
    }

    public int readInt32() throws IOException {
        byte[] buf = readBytes(4);
        return ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    /** Read unsigned int (uint in C#), returned as long */
    public long readUInt32() throws IOException {
        return readInt32() & 0xFFFFFFFFL;
    }

    public long readInt64() throws IOException {
        byte[] buf = readBytes(8);
        return ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    public float readSingle() throws IOException {
        byte[] buf = readBytes(4);
        return ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    public double readDouble() throws IOException {
        byte[] buf = readBytes(8);
        return ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).getDouble();
    }

    public byte[] readBytes(int count) throws IOException {
        byte[] buf = new byte[count];
        int read = 0;
        while (read < count) {
            int r;
            if (raf != null) {
                r = raf.read(buf, read, count - read);
            } else {
                r = dis.read(buf, read, count - read);
            }
            if (r < 0) throw new EOFException("Unexpected end of stream, needed " + count + " bytes");
            read += r;
        }
        position += count;
        return buf;
    }

    public void skip(int count) throws IOException {
        if (raf != null) {
            raf.seek(raf.getFilePointer() + count);
        } else {
            long skipped = dis.skip(count);
            if (skipped < count) {
                // try reading the rest
                readBytes((int) (count - skipped));
                return;
            }
        }
        position += count;
    }

    // ---- WZ-specific reads ----

    /**
     * Ported from: WzBinaryReader.ReadCompressedInt()
     *
     * sbyte == MinValue (-128) → read full int32
     * otherwise sbyte IS the value
     */
    public int readCompressedInt() throws IOException {
        byte sb = readSByte();
        if (sb == -128) { // sbyte.MinValue
            return readInt32();
        }
        return sb; // auto sign-extends
    }

    /**
     * Ported from: WzBinaryReader.ReadLong()
     *
     * sbyte == MinValue (-128) → read full int64
     * otherwise sbyte IS the value
     */
    public long readCompressedLong() throws IOException {
        byte sb = readSByte();
        if (sb == -128) {
            return readInt64();
        }
        return sb;
    }

    /**
     * Read WZ encrypted string.
     * Ported from: WzBinaryReader.ReadString() override
     */
    public String readString() throws IOException {
        byte smallLength = readSByte();
        if (smallLength == 0) {
            return "";
        }

        int length;
        if (smallLength > 0) { // Unicode
            length = (smallLength == 127) ? readInt32() : smallLength; // 127 = sbyte.MaxValue
        } else { // ASCII
            length = (smallLength == -128) ? readInt32() : -smallLength; // -128 = sbyte.MinValue
        }

        if (length <= 0) {
            return "";
        }

        if (smallLength > 0) {
            return decodeUnicode(length);
        } else {
            return decodeAscii(length);
        }
    }

    /**
     * Ported from: WzBinaryReader.DecodeUnicode(int)
     */
    private String decodeUnicode(int length) throws IOException {
        char[] chars = new char[length];
        int mask = 0xAAAA;

        for (int i = 0; i < length; i++) {
            int encryptedChar = readUInt16();
            encryptedChar ^= mask;
            encryptedChar ^= ((wzKey.at(i * 2 + 1) & 0xFF) << 8) + (wzKey.at(i * 2) & 0xFF);
            chars[i] = (char) encryptedChar;
            mask++;
        }
        return new String(chars);
    }

    /**
     * Ported from: WzBinaryReader.DecodeAscii(int)
     */
    private String decodeAscii(int length) throws IOException {
        byte[] bytes = new byte[length];
        int mask = 0xAA;

        for (int i = 0; i < length; i++) {
            int encryptedChar = readByte() & 0xFF;
            encryptedChar ^= mask;
            encryptedChar ^= (wzKey.at(i) & 0xFF);
            bytes[i] = (byte) encryptedChar;
            mask++;
        }
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    /**
     * Read an ASCII string of fixed length, without decryption.
     * Ported from: WzBinaryReader.ReadString(int length)
     */
    public String readString(int length) throws IOException {
        byte[] buf = readBytes(length);
        return new String(buf, StandardCharsets.US_ASCII);
    }

    /**
     * Read null-terminated string (UTF-8).
     * Ported from: WzBinaryReader.ReadNullTerminatedString()
     */
    public String readNullTerminatedString() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
        byte b;
        while ((b = readByte()) != 0) {
            baos.write(b);
        }
        return baos.toString("UTF-8");
    }

    /**
     * Read string at a specific offset, then restore position.
     * Ported from: WzBinaryReader.ReadStringAtOffset(long, bool)
     */
    public String readStringAtOffset(long offset) throws IOException {
        return readStringAtOffset(offset, false);
    }

    public String readStringAtOffset(long offset, boolean readByteFirst) throws IOException {
        long currentOffset = getPosition();
        setPosition(offset);
        if (readByteFirst) {
            readByte();
        }
        String result = readString();
        setPosition(currentOffset);
        return result;
    }

    /**
     * Sets position to Header.FStart + offset.
     * Ported from: WzBinaryReader.SetOffsetFromFStartToPosition(int)
     */
    public void setOffsetFromFStartToPosition(int offset) throws IOException {
        setPosition((header.getFStart() + offset));
    }

    /**
     * Rollback stream position by byOffset bytes.
     * Ported from: WzBinaryReader.RollbackStreamPosition(int)
     */
    public void rollbackStreamPosition(int byOffset) throws IOException {
        long current = position;
        if (current < byOffset) {
            throw new WzException("Can't rollback stream position below 0");
        }
        setPosition(getPosition() - byOffset);
    }

    /**
     * Read encrypted offset.
     * Ported from: WzBinaryReader.ReadOffset()
     *
     * C# code:
     *   uint offset = (uint)BaseStream.Position;
     *   offset = (offset - Header.FStart) ^ uint.MaxValue;
     *   offset *= Hash;
     *   offset -= WZ_OffsetConstant;
     *   offset = RotateLeft(offset, (byte)(offset & 0x1F));
     *   uint encryptedOffset = ReadUInt32();
     *   offset ^= encryptedOffset;
     *   offset += Header.FStart * 2;
     */
    public long readOffset() throws IOException {
        // Use actual file position to match C# BaseStream.Position exactly
        // All arithmetic emulates C# uint (32-bit unsigned) via int with >>> for shifts
        long actualPos = (raf != null) ? raf.getFilePointer() : (position + startOffset);

        // C#: uint offset = (uint)BaseStream.Position;
        int offset = (int) actualPos;
        // C#: offset = (offset - Header.FStart) ^ uint.MaxValue;
        offset = (offset - (int) header.getFStart()) ^ 0xFFFFFFFF;
        // C#: offset *= Hash;  (uint * uint, wraps at 32 bits)
        offset *= (int) hash;
        // C#: offset -= WZ_OffsetConstant;
        offset -= (int) WzCryptoConstants.WZ_OffsetConstant;
        // C#: offset = RotateLeft(offset, (byte)(offset & 0x1F));
        int n = offset & 0x1F;
        offset = (offset << n) | (offset >>> (32 - n));
        // C#: uint encryptedOffset = ReadUInt32();
        int encryptedOffset = readInt32();
        // C#: offset ^= encryptedOffset;
        offset ^= encryptedOffset;
        // C#: offset += Header.FStart * 2;
        offset += (int) header.getFStart() * 2;

        // Return as unsigned long
        return (offset & 0xFFFFFFFFL) + startOffset;
    }

    /**
     * Read string block (inline or offset reference).
     * Ported from: WzBinaryReader.ReadStringBlock(long)
     *
     * C#:
     *   ReadByte() switch {
     *     0 or 0x73 => ReadString(),
     *     1 or 0x1B => ReadStringAtOffset(offset + ReadInt32()),
     *   }
     */
    public String readStringBlock(long offset) throws IOException {
        int blockType = readByte() & 0xFF;
        switch (blockType) {
            case 0x00:
            case 0x73:
                return readString();
            case 0x01:
            case 0x1B:
                return readStringAtOffset(offset + readInt32());
            default:
                return "";
        }
    }

    /**
     * Decrypt a List.wz unicode string (no mask).
     * Ported from: WzBinaryReader.DecryptString(ReadOnlySpan<char>)
     */
    public String decryptString(String stringToDecrypt) {
        char[] output = new char[stringToDecrypt.length()];
        for (int i = 0; i < stringToDecrypt.length(); i++) {
            output[i] = (char) (stringToDecrypt.charAt(i) ^
                    (char) (((wzKey.at(i * 2 + 1) & 0xFF) << 8) + (wzKey.at(i * 2) & 0xFF)));
        }
        return new String(output);
    }

    /**
     * Decrypt a non-unicode string (no mask).
     * Ported from: WzBinaryReader.DecryptNonUnicodeString(ReadOnlySpan<char>)
     */
    public String decryptNonUnicodeString(String stringToDecrypt) {
        char[] output = new char[stringToDecrypt.length()];
        for (int i = 0; i < stringToDecrypt.length(); i++) {
            output[i] = (char) (stringToDecrypt.charAt(i) ^ (wzKey.at(i) & 0xFF));
        }
        return new String(output);
    }

    /**
     * Create a sub-reader for a section of the file (for concurrent reading).
     * Ported from: WzBinaryReader.CreateReaderForSection(long, int)
     */
    public WzBinaryReader createReaderForSection(long start, int length) throws IOException {
        if (raf == null) {
            throw new WzException("createReaderForSection requires file-based reader");
        }
        byte[] buffer;
        synchronized (this) {
            long savedPos = raf.getFilePointer();
            raf.seek(start);
            buffer = new byte[length];
            int read = 0;
            while (read < length) {
                int r = raf.read(buffer, read, length - read);
                if (r < 0) throw new EOFException();
                read += r;
            }
            raf.seek(savedPos);
        }

        WzBinaryReader reader = new WzBinaryReader(buffer, this.wzKey, start);
        reader.setHash(this.hash);
        reader.setHeader(this.header);
        return reader;
    }

    @Override
    public void close() throws IOException {
        if (raf != null) {
            raf.close();
        }
        if (dis != null) {
            dis.close();
        }
    }
}
