package wzlib.util;

import wzlib.WzException;
import wzlib.crypto.WzMutableKey;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.zip.Inflater;

/**
 * WZ PNG 資料解碼器，將壓縮像素資料轉為 BufferedImage。
 * WZ PNG data decoder that converts compressed pixel data into BufferedImage.
 *
 * <p>Ported from: WzPngProperty.cs + PngUtility.cs</p>
 */
public final class WzPngCodec {

    private WzPngCodec() {}

    /**
     * 解碼壓縮 PNG 資料為 BufferedImage。
     * Decode compressed PNG bytes into a BufferedImage.
     *
     * @param compressedData  壓縮位元組 / raw compressed bytes from WzPngProperty
     * @param width           圖片寬度 / image width
     * @param height          圖片高度 / image height
     * @param format          像素格式代碼 / pixel format code (1, 2, 3, 257, 513, 517, 1026, 2050)
     * @param wzKey           解密金鑰（標準 zlib 可為 null）/ WzMutableKey for decryption (may be null)
     */
    public static BufferedImage decode(byte[] compressedData, int width, int height, int format, WzMutableKey wzKey) {
        if (compressedData == null || compressedData.length < 2) return null;

        // Step 1: Decompress to raw pixel bytes
        byte[] rawPixels = getRawImage(compressedData, width, height, format, wzKey);
        if (rawPixels == null) return null;

        // Step 2: Convert raw pixels to BufferedImage based on format
        return decodePixels(rawPixels, width, height, format);
    }

    /**
     * Decompress compressed data to raw pixel bytes.
     * Ported from: WzPngProperty.GetRawImage()
     */
    static byte[] getRawImage(byte[] compressedData, int width, int height, int format, WzMutableKey wzKey) {
        int header = ((compressedData[1] & 0xFF) << 8) | (compressedData[0] & 0xFF);
        boolean isListWzFormat = header != 0x9C78 && header != 0xDA78 && header != 0x0178 && header != 0x5E78;

        byte[] deflateInput;
        if (!isListWzFormat) {
            // Standard zlib: skip 2-byte header, rest is deflate
            deflateInput = new byte[compressedData.length - 2];
            System.arraycopy(compressedData, 2, deflateInput, 0, deflateInput.length);
        } else {
            // listWz format: XOR-encrypted blocks, then zlib
            deflateInput = decryptListWz(compressedData, wzKey);
        }

        int uncompressedSize = getUncompressedSize(width, height, format);
        return inflate(deflateInput, uncompressedSize);
    }

    /**
     * Decrypt listWz format blocks.
     * Ported from: WzPngProperty.GetRawImage() listWz branch
     */
    private static byte[] decryptListWz(byte[] data, WzMutableKey wzKey) {
        ByteArrayOutputStream decrypted = new ByteArrayOutputStream();
        int pos = 0;
        while (pos < data.length) {
            if (pos + 4 > data.length) break;
            int blockSize = (data[pos] & 0xFF)
                    | ((data[pos + 1] & 0xFF) << 8)
                    | ((data[pos + 2] & 0xFF) << 16)
                    | ((data[pos + 3] & 0xFF) << 24);
            pos += 4;
            for (int i = 0; i < blockSize && pos < data.length; i++) {
                byte decByte = (byte) ((data[pos] & 0xFF) ^ (wzKey != null ? (wzKey.at(i) & 0xFF) : 0));
                decrypted.write(decByte);
                pos++;
            }
        }
        byte[] decData = decrypted.toByteArray();
        // Skip 2-byte zlib header
        if (decData.length < 2) return decData;
        byte[] result = new byte[decData.length - 2];
        System.arraycopy(decData, 2, result, 0, result.length);
        return result;
    }

    /**
     * Get expected uncompressed pixel data size.
     * Ported from: WzPngProperty.GetUncompressedSize()
     */
    private static int getUncompressedSize(int width, int height, int format) {
        switch (format) {
            case 1:    return width * height * 2;     // BGRA4444
            case 2:    return width * height * 4;     // BGRA8888
            case 3:    return width * height * 4;     // DXT3 grayscale
            case 257:  return width * height * 2;     // ARGB1555
            case 513:  return width * height * 2;     // RGB565
            case 517:  return width * height / 128;   // RGB565 block
            case 1026: return width * height * 4;     // DXT3
            case 2050: return width * height;         // DXT5
            default:   return width * height * 4;
        }
    }

    private static byte[] inflate(byte[] deflateData, int expectedSize) {
        try {
            Inflater inflater = new Inflater(true); // raw deflate (no zlib header)
            inflater.setInput(deflateData);
            byte[] result = new byte[expectedSize];
            int totalRead = 0;
            while (totalRead < expectedSize && !inflater.finished()) {
                int read = inflater.inflate(result, totalRead, expectedSize - totalRead);
                if (read == 0 && inflater.needsInput()) break;
                totalRead += read;
            }
            inflater.end();
            return result;
        } catch (Exception e) {
            throw new WzException("Failed to inflate PNG data", e);
        }
    }

    /**
     * Convert raw pixel bytes to BufferedImage based on format.
     * Ported from: WzPngProperty.DecodeBitmap() switch cases + PngUtility methods
     */
    private static BufferedImage decodePixels(byte[] rawData, int width, int height, int format) {
        switch (format) {
            case 1:    return decodeBGRA4444(rawData, width, height);
            case 2:    return decodeBGRA8888(rawData, width, height);
            case 3:    return decodeDXT3Grayscale(rawData, width, height);
            case 257:  return decodeARGB1555(rawData, width, height);
            case 513:  return decodeRGB565(rawData, width, height);
            case 517:  return decodeRGB565Block16(rawData, width, height);
            case 1026: return decodeDXT3(rawData, width, height);
            case 2050: return decodeDXT5(rawData, width, height);
            default:
                throw new WzException("Unknown PNG format: " + format);
        }
    }

    // ---- Format-specific decoders ----

    /**
     * Format 1: BGRA4444 (2 bytes per pixel → expand to 8-bit ARGB)
     * Ported from: PngUtility.DecompressImage_PixelDataBgra4444()
     */
    private static BufferedImage decodeBGRA4444(byte[] rawData, int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int idx = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (idx + 1 >= rawData.length) break;
                int lo = rawData[idx++] & 0xFF;
                int hi = rawData[idx++] & 0xFF;
                // BGRA4444: lo = [g4:b4], hi = [a4:r4]
                int b = (lo & 0x0F); b |= (b << 4);
                int g = (lo >> 4) & 0x0F; g |= (g << 4);
                int r = (hi & 0x0F); r |= (r << 4);
                int a = (hi >> 4) & 0x0F; a |= (a << 4);
                img.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return img;
    }

    /**
     * Format 2: BGRA8888 (4 bytes per pixel, direct copy)
     */
    private static BufferedImage decodeBGRA8888(byte[] rawData, int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int idx = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (idx + 3 >= rawData.length) break;
                int b = rawData[idx++] & 0xFF;
                int g = rawData[idx++] & 0xFF;
                int r = rawData[idx++] & 0xFF;
                int a = rawData[idx++] & 0xFF;
                img.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return img;
    }

    /**
     * Format 257: ARGB1555 (2 bytes per pixel)
     */
    private static BufferedImage decodeARGB1555(byte[] rawData, int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int idx = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (idx + 1 >= rawData.length) break;
                int v = (rawData[idx++] & 0xFF) | ((rawData[idx++] & 0xFF) << 8);
                int a = ((v >> 15) & 1) == 1 ? 255 : 0;
                int r = ((v >> 10) & 0x1F) * 255 / 31;
                int g = ((v >> 5) & 0x1F) * 255 / 31;
                int b = (v & 0x1F) * 255 / 31;
                img.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return img;
    }

    /**
     * Format 513: RGB565 (2 bytes per pixel)
     */
    private static BufferedImage decodeRGB565(byte[] rawData, int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int idx = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (idx + 1 >= rawData.length) break;
                int v = (rawData[idx++] & 0xFF) | ((rawData[idx++] & 0xFF) << 8);
                int r = ((v >> 11) & 0x1F) * 255 / 31;
                int g = ((v >> 5) & 0x3F) * 255 / 63;
                int b = (v & 0x1F) * 255 / 31;
                img.setRGB(x, y, 0xFF000000 | (r << 16) | (g << 8) | b);
            }
        }
        return img;
    }

    /**
     * Format 517: RGB565 block 16x16
     */
    private static BufferedImage decodeRGB565Block16(byte[] rawData, int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int idx = 0;
        for (int by = 0; by < height; by += 16) {
            for (int bx = 0; bx < width; bx += 16) {
                if (idx + 1 >= rawData.length) break;
                int v = (rawData[idx++] & 0xFF) | ((rawData[idx++] & 0xFF) << 8);
                int r = ((v >> 11) & 0x1F) * 255 / 31;
                int g = ((v >> 5) & 0x3F) * 255 / 63;
                int b = (v & 0x1F) * 255 / 31;
                int argb = 0xFF000000 | (r << 16) | (g << 8) | b;
                for (int dy = 0; dy < 16 && by + dy < height; dy++) {
                    for (int dx = 0; dx < 16 && bx + dx < width; dx++) {
                        img.setRGB(bx + dx, by + dy, argb);
                    }
                }
            }
        }
        return img;
    }

    /**
     * Format 3: DXT3 Grayscale
     */
    private static BufferedImage decodeDXT3Grayscale(byte[] rawData, int width, int height) {
        // Use the same DXT3 decoder
        return decodeDXT3(rawData, width, height);
    }

    /**
     * Format 1026: DXT3 (S3TC Block Compression)
     * 16 bytes per 4x4 block: 8 alpha + 8 color
     */
    private static BufferedImage decodeDXT3(byte[] rawData, int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int blockCountX = (width + 3) / 4;
        int blockCountY = (height + 3) / 4;

        for (int by = 0; by < blockCountY; by++) {
            for (int bx = 0; bx < blockCountX; bx++) {
                int blockOffset = (by * blockCountX + bx) * 16;
                if (blockOffset + 16 > rawData.length) break;

                // Alpha: 8 bytes, 4 bits per pixel
                int[] alpha = new int[16];
                for (int i = 0; i < 4; i++) {
                    int lo = rawData[blockOffset + i * 2] & 0xFF;
                    int hi = rawData[blockOffset + i * 2 + 1] & 0xFF;
                    int row = lo | (hi << 8);
                    for (int j = 0; j < 4; j++) {
                        int a4 = (row >> (j * 4)) & 0x0F;
                        alpha[i * 4 + j] = a4 | (a4 << 4);
                    }
                }

                // Color: 2 RGB565 endpoints + 4-byte index table
                int c0 = (rawData[blockOffset + 8] & 0xFF) | ((rawData[blockOffset + 9] & 0xFF) << 8);
                int c1 = (rawData[blockOffset + 10] & 0xFF) | ((rawData[blockOffset + 11] & 0xFF) << 8);
                int[][] colors = interpolateColors565(c0, c1);
                long indices = readDxtColorIndices(rawData, blockOffset + 12);

                for (int py = 0; py < 4; py++) {
                    for (int px = 0; px < 4; px++) {
                        int x = bx * 4 + px;
                        int y = by * 4 + py;
                        if (x >= width || y >= height) continue;
                        int idx = (int) ((indices >> ((py * 4 + px) * 2)) & 0x3);
                        int[] c = colors[idx];
                        int a = alpha[py * 4 + px];
                        img.setRGB(x, y, (a << 24) | (c[0] << 16) | (c[1] << 8) | c[2]);
                    }
                }
            }
        }
        return img;
    }

    /**
     * Format 2050: DXT5 (S3TC with interpolated alpha)
     * 16 bytes per 4x4 block: 8 alpha (2 endpoints + 48-bit indices) + 8 color
     */
    private static BufferedImage decodeDXT5(byte[] rawData, int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int blockCountX = (width + 3) / 4;
        int blockCountY = (height + 3) / 4;

        for (int by = 0; by < blockCountY; by++) {
            for (int bx = 0; bx < blockCountX; bx++) {
                int blockOffset = (by * blockCountX + bx) * 16;
                if (blockOffset + 16 > rawData.length) break;

                // Alpha: 2 endpoint bytes + 6 bytes of 3-bit indices
                int a0 = rawData[blockOffset] & 0xFF;
                int a1 = rawData[blockOffset + 1] & 0xFF;
                int[] alphaTable = expandAlphaDXT5(a0, a1);
                int[] alphaIndices = new int[16];
                {
                    long bits = 0;
                    for (int i = 0; i < 6; i++) {
                        bits |= ((long) (rawData[blockOffset + 2 + i] & 0xFF)) << (i * 8);
                    }
                    for (int i = 0; i < 16; i++) {
                        alphaIndices[i] = (int) ((bits >> (i * 3)) & 0x7);
                    }
                }

                // Color
                int c0 = (rawData[blockOffset + 8] & 0xFF) | ((rawData[blockOffset + 9] & 0xFF) << 8);
                int c1 = (rawData[blockOffset + 10] & 0xFF) | ((rawData[blockOffset + 11] & 0xFF) << 8);
                int[][] colors = interpolateColors565(c0, c1);
                long colorIndices = readDxtColorIndices(rawData, blockOffset + 12);

                for (int py = 0; py < 4; py++) {
                    for (int px = 0; px < 4; px++) {
                        int x = bx * 4 + px;
                        int y = by * 4 + py;
                        if (x >= width || y >= height) continue;
                        int cidx = (int) ((colorIndices >> ((py * 4 + px) * 2)) & 0x3);
                        int[] c = colors[cidx];
                        int a = alphaTable[alphaIndices[py * 4 + px]];
                        img.setRGB(x, y, (a << 24) | (c[0] << 16) | (c[1] << 8) | c[2]);
                    }
                }
            }
        }
        return img;
    }

    // ---- DXT helpers ----

    private static int[] rgb565ToRGB(int c) {
        int r = ((c >> 11) & 0x1F) * 255 / 31;
        int g = ((c >> 5) & 0x3F) * 255 / 63;
        int b = (c & 0x1F) * 255 / 31;
        return new int[]{r, g, b};
    }

    private static int[][] interpolateColors565(int c0, int c1) {
        int[] color0 = rgb565ToRGB(c0);
        int[] color1 = rgb565ToRGB(c1);
        int[][] colors = new int[4][3];
        colors[0] = color0;
        colors[1] = color1;
        if (c0 > c1) {
            for (int i = 0; i < 3; i++) {
                colors[2][i] = (2 * color0[i] + color1[i]) / 3;
                colors[3][i] = (color0[i] + 2 * color1[i]) / 3;
            }
        } else {
            for (int i = 0; i < 3; i++) {
                colors[2][i] = (color0[i] + color1[i]) / 2;
            }
            colors[3] = new int[]{0, 0, 0}; // transparent black
        }
        return colors;
    }

    private static long readDxtColorIndices(byte[] data, int offset) {
        long bits = 0;
        for (int i = 0; i < 4; i++) {
            bits |= ((long) (data[offset + i] & 0xFF)) << (i * 8);
        }
        return bits;
    }

    private static int[] expandAlphaDXT5(int a0, int a1) {
        int[] table = new int[8];
        table[0] = a0;
        table[1] = a1;
        if (a0 > a1) {
            table[2] = (6 * a0 + 1 * a1) / 7;
            table[3] = (5 * a0 + 2 * a1) / 7;
            table[4] = (4 * a0 + 3 * a1) / 7;
            table[5] = (3 * a0 + 4 * a1) / 7;
            table[6] = (2 * a0 + 5 * a1) / 7;
            table[7] = (1 * a0 + 6 * a1) / 7;
        } else {
            table[2] = (4 * a0 + 1 * a1) / 5;
            table[3] = (3 * a0 + 2 * a1) / 5;
            table[4] = (2 * a0 + 3 * a1) / 5;
            table[5] = (1 * a0 + 4 * a1) / 5;
            table[6] = 0;
            table[7] = 255;
        }
        return table;
    }
}
