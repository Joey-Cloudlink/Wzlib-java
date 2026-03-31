package wzlib.util;

import wzlib.property.WzBinaryProperty;

import java.io.IOException;

/**
 * Ported from: MapleLib/WzLib/WzProperties/WzBinaryProperty.cs (sound handling)
 *
 * Utility for extracting audio data from WzBinaryProperty.
 */
public final class WzSoundCodec {

    private WzSoundCodec() {}

    /**
     * Extract raw audio data (MP3 or PCM) from a WzBinaryProperty.
     */
    public static byte[] extractAudioData(WzBinaryProperty prop) throws IOException {
        return prop.getSoundBytes(true);
    }

    /**
     * Get the duration in milliseconds.
     */
    public static int getDurationMs(WzBinaryProperty prop) {
        return prop.getLenMs();
    }

    /**
     * Get the size of the raw audio data.
     */
    public static int getDataLength(WzBinaryProperty prop) {
        return prop.getSoundDataLen();
    }
}
