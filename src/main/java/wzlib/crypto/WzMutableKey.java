package wzlib.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Ported from: MapleLib/WzLib/Util/WzMutableKey.cs
 *
 * Lazy-expanding AES keystream for WZ string/offset encryption.
 * Thread-safe via synchronized.
 */
public final class WzMutableKey {

    private static final int BATCH_SIZE = 4096;

    private final byte[] iv;
    private final byte[] aesUserKey; // 32 bytes
    private byte[] keys;

    public WzMutableKey(byte[] iv, byte[] aesUserKey) {
        this.iv = iv.clone();
        this.aesUserKey = aesUserKey.clone();
        this.keys = null;
    }

    /**
     * Get key byte at index, expanding lazily if needed.
     * Corresponds to C#: this[int index] indexer
     */
    public byte at(int index) {
        ensureKeySize(index + 1);
        return keys[index];
    }

    /**
     * Ensure key is at least 'size' bytes.
     * Ported from: WzMutableKey.EnsureKeySize()
     */
    public synchronized void ensureKeySize(int size) {
        if (keys != null && keys.length >= size) {
            return;
        }

        // Round up to next multiple of BATCH_SIZE
        size = (int) Math.ceil(1.0 * size / BATCH_SIZE) * BATCH_SIZE;
        byte[] newKeys = new byte[size];

        // If IV is all zeros (BMS/Classic), key is all zeros
        int ivAsInt = ByteBuffer.wrap(iv).order(ByteOrder.LITTLE_ENDIAN).getInt();
        if (ivAsInt == 0) {
            this.keys = newKeys;
            return;
        }

        int startIndex = 0;
        if (keys != null) {
            System.arraycopy(keys, 0, newKeys, 0, keys.length);
            startIndex = keys.length;
        }

        try {
            // AES-256-ECB, no padding — matching C# exactly
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(aesUserKey, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);

            for (int i = startIndex; i < size; i += 16) {
                byte[] block;
                if (i == 0) {
                    // First block: IV repeated to fill 16 bytes
                    block = new byte[16];
                    for (int j = 0; j < 16; j++) {
                        block[j] = iv[j % 4];
                    }
                } else {
                    // Subsequent blocks: encrypt previous ciphertext block
                    block = Arrays.copyOfRange(newKeys, i - 16, i);
                }
                byte[] encrypted = cipher.doFinal(block);
                System.arraycopy(encrypted, 0, newKeys, i, 16);
            }
        } catch (Exception e) {
            throw new RuntimeException("AES encryption failed", e);
        }

        this.keys = newKeys;
    }

    public byte[] getKeys() {
        return keys != null ? keys.clone() : new byte[0];
    }

    public byte[] getIv() {
        return iv.clone();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof WzMutableKey)) return false;
        WzMutableKey other = (WzMutableKey) obj;
        return Arrays.equals(iv, other.iv) && Arrays.equals(aesUserKey, other.aesUserKey);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(iv) * 31 + Arrays.hashCode(aesUserKey);
    }
}
