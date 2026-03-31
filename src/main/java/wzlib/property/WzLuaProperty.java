package wzlib.property;

import wzlib.WzImageProperty;
import wzlib.WzPropertyType;
import wzlib.crypto.WzKeyGenerator;
import wzlib.crypto.WzMutableKey;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Ported from: MapleLib/WzLib/WzProperties/WzLuaProperty.cs
 *
 * A property containing an encrypted .lua script.
 */
public class WzLuaProperty extends WzImageProperty {

    private byte[] encryptedBytes;
    private final WzMutableKey wzKey;

    /**
     * Ported from: WzLuaProperty(string name, byte[] encryptedBytes)
     */
    public WzLuaProperty(String name, byte[] encryptedBytes) {
        this.name = name;
        this.encryptedBytes = encryptedBytes;
        // C#: this.WzKey = WzKeyGenerator.GenerateLuaWzKey();
        this.wzKey = WzKeyGenerator.generateLuaWzKey();
    }

    @Override public WzPropertyType getPropertyType() { return WzPropertyType.Lua; }
    @Override public Object getValue() { return encryptedBytes; }
    @Override public void setValue(Object value) { this.encryptedBytes = (byte[]) value; }

    public byte[] getEncryptedBytes() { return encryptedBytes; }

    /**
     * Decode the lua script.
     * Ported from: WzLuaProperty.GetString()
     */
    public String getString() {
        byte[] decoded = encodeDecode(encryptedBytes);
        return new String(decoded, StandardCharsets.US_ASCII);
    }

    /**
     * Encode or decode bytes with XOR encryption.
     * Ported from: WzLuaProperty.EncodeDecode(byte[])
     */
    public byte[] encodeDecode(byte[] input) {
        byte[] output = new byte[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = (byte) ((input[i] & 0xFF) ^ (wzKey.at(i) & 0xFF));
        }
        return output;
    }

    @Override
    public WzImageProperty deepClone() {
        byte[] clonedBytes = encryptedBytes != null ? encryptedBytes.clone() : null;
        return new WzLuaProperty(name, clonedBytes);
    }

    /**
     * Ported from: WzLuaProperty.WriteValue()
     */
    @Override
    public void writeValue(wzlib.util.WzBinaryWriter writer) throws IOException {
        writer.writeByte((byte) 0x01);
        writer.writeCompressedInt(encryptedBytes.length);
        writer.writeBytes(encryptedBytes);
    }

    @Override
    public String toString() {
        return getString();
    }
}
