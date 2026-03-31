package wzlib.property;
import wzlib.WzImageProperty;
import wzlib.WzPropertyType;
import wzlib.util.WzBinaryReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Ported from: MapleLib/WzLib/WzProperties/WzBinaryProperty.cs
 * Phase 3: stores metadata + raw sound bytes. Audio decoding in Phase 4.
 */
public class WzBinaryProperty extends WzImageProperty {

    public static final byte[] SOUND_HEADER = {
        0x02,
        (byte)0x83, (byte)0xEB, 0x36, (byte)0xE4, 0x4F, 0x52, (byte)0xCE, 0x11,
        (byte)0x9F, 0x53, 0x00, 0x20, (byte)0xAF, 0x0B, (byte)0xA7, 0x70,
        (byte)0x8B, (byte)0xEB, 0x36, (byte)0xE4, 0x4F, 0x52, (byte)0xCE, 0x11,
        (byte)0x9F, 0x53, 0x00, 0x20, (byte)0xAF, 0x0B, (byte)0xA7, 0x70,
        0x00,
        0x01,
        (byte)0x81, (byte)0x9F, 0x58, 0x05, 0x56, (byte)0xC3, (byte)0xCE, 0x11,
        (byte)0xBF, 0x01, 0x00, (byte)0xAA, 0x00, 0x55, 0x59, 0x5A
    };

    private int soundDataLen;
    private int lenMs;
    private byte[] header;
    private byte[] fileBytes;
    private WzBinaryReader wzReader;
    private long dataOffset;

    /** Constructor for cloning */
    private WzBinaryProperty(String name) {
        this.name = name;
    }

    public WzBinaryProperty(String name, WzBinaryReader reader, boolean parseNow) throws IOException {
        this.name = name;
        this.wzReader = reader;

        // Ported from C# WzBinaryProperty constructor
        reader.skip(1); // skip 1 byte

        soundDataLen = reader.readCompressedInt();
        lenMs = reader.readCompressedInt();

        // Read header: soundHeader + wavFormatLen byte + waveFormat bytes
        long headerOff = reader.getPosition();
        reader.skip(SOUND_HEADER.length); // skip GUIDs
        int wavFormatLen = reader.readByte() & 0xFF;
        reader.setPosition(headerOff);

        byte[] soundHeaderBytes = reader.readBytes(SOUND_HEADER.length);
        byte[] unk1 = reader.readBytes(1);
        byte[] waveFormatBytes = reader.readBytes(wavFormatLen);

        // Concatenate: soundHeaderBytes + unk1 + waveFormatBytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(soundHeaderBytes);
        baos.write(unk1);
        baos.write(waveFormatBytes);
        header = baos.toByteArray();

        this.dataOffset = reader.getPosition();
        if (parseNow) {
            fileBytes = reader.readBytes(soundDataLen);
        } else {
            reader.skip(soundDataLen);
        }
    }

    @Override public WzPropertyType getPropertyType() { return WzPropertyType.Sound; }
    @Override public Object getValue() { return fileBytes; }
    @Override public void setValue(Object value) {}

    public int getSoundDataLen() { return soundDataLen; }
    public int getLenMs() { return lenMs; }
    public byte[] getHeader() { return header; }

    @Override
    public WzImageProperty deepClone() {
        WzBinaryProperty clone = new WzBinaryProperty(name);
        clone.soundDataLen = this.soundDataLen;
        clone.lenMs = this.lenMs;
        clone.header = this.header != null ? this.header.clone() : null;
        if (this.fileBytes != null) {
            clone.fileBytes = this.fileBytes.clone();
        } else if (this.wzReader != null) {
            try {
                byte[] data = getSoundBytes(false);
                if (data != null) clone.fileBytes = data.clone();
            } catch (Exception e) { /* skip */ }
        }
        return clone;
    }

    @Override
    public void writeValue(wzlib.util.WzBinaryWriter writer) throws java.io.IOException {
        byte[] data = getSoundBytes(true);
        writer.writeStringValue("Sound_DX8", (int) 0x73, (int) 0x1B);
        writer.writeByte((byte) 0);
        writer.writeCompressedInt(data != null ? data.length : 0);
        writer.writeCompressedInt(lenMs);
        writer.writeBytes(header);
        if (data != null) writer.writeBytes(data);
    }

    public byte[] getSoundBytes(boolean saveInMemory) throws IOException {
        if (fileBytes != null) return fileBytes;
        if (wzReader == null || soundDataLen <= 0) return null;

        synchronized (wzReader) {
            long savedPos = wzReader.getPosition();
            wzReader.setPosition(dataOffset);
            byte[] data = wzReader.readBytes(soundDataLen);
            wzReader.setPosition(savedPos);

            if (saveInMemory) fileBytes = data;
            return data;
        }
    }
}
