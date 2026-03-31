package wzlib;

import wzlib.util.WzBinaryReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Ported from: MapleLib/WzLib/WzDirectory.cs
 *
 * A directory in the wz file, which may contain sub directories or wz images.
 */
public class WzDirectory extends WzObject {

    // Directory entry type constants (from WzDirectoryType enum in C#)
    private static final byte TYPE_UNKNOWN_1 = 1;
    private static final byte TYPE_RETRIEVE_STRING_FROM_OFFSET_2 = 2;
    private static final byte TYPE_WZ_DIRECTORY_3 = 3;
    private static final byte TYPE_WZ_IMAGE_4 = 4;

    private final List<WzImage> images = new ArrayList<>();
    private final List<WzDirectory> subDirs = new ArrayList<>();
    WzBinaryReader reader;
    private long offset;
    private long hash;
    private int size;
    private int checksum;
    int offsetSize;
    private byte[] wzIv;
    private WzFile wzFile;

    public WzDirectory() {}

    public WzDirectory(String dirName) {
        this.name = dirName;
    }

    /**
     * Ported from: WzDirectory(WzBinaryReader, string, uint, byte[], WzFile)
     */
    public WzDirectory(WzBinaryReader reader, String dirName, long verHash, byte[] wzIv, WzFile wzFile) {
        this.reader = reader;
        this.name = dirName;
        this.hash = verHash;
        this.wzIv = wzIv;
        this.wzFile = wzFile;
    }

    @Override
    public WzObjectType getObjectType() {
        return WzObjectType.Directory;
    }

    // ---- Properties ----

    public List<WzImage> getWzImages() { return images; }
    public List<WzDirectory> getWzDirectories() { return subDirs; }

    public long getOffset() { return offset; }
    public void setOffset(long offset) { this.offset = offset; }

    public int getBlockSize() { return size; }
    public void setBlockSize(int size) { this.size = size; }

    public int getChecksum() { return checksum; }
    public void setChecksum(int checksum) { this.checksum = checksum; }

    @Override
    public WzObject getChild(String name) {
        String nameLower = name.toLowerCase();
        for (WzImage img : images) {
            if (img.getName().toLowerCase().equals(nameLower)) return img;
        }
        for (WzDirectory dir : subDirs) {
            if (dir.getName().toLowerCase().equals(nameLower)) return dir;
        }
        return null;
    }

    // ---- Parsing ----

    /**
     * Parse the WZ directory structure.
     * Ported from: WzDirectory.ParseDirectory(bool lazyParse)
     */
    public void parseDirectory() throws IOException {
        parseDirectory(false);
    }

    public void parseDirectory(boolean lazyParse) throws IOException {
        long available = reader.available();
        if (available == 0) return;

        int entryCount = reader.readCompressedInt();
        if (entryCount < 0 || entryCount > 100000) {
            throw new WzException("Invalid wz version used for decryption, try parsing other version numbers.");
        }

        for (int i = 0; i < entryCount; i++) {
            byte type = reader.readByte();
            String fname = null;
            int fsize;
            int checksum;
            long offset;

            long rememberPos = 0;

            switch (type) {
                case TYPE_UNKNOWN_1: {
                    // 01 XX 00 00 00 00 00 OFFSET (4 bytes)
                    int unknown = reader.readInt32();
                    reader.readInt16();
                    long offs = reader.readOffset();
                    continue;
                }
                case TYPE_RETRIEVE_STRING_FROM_OFFSET_2: {
                    int stringOffset = reader.readInt32();
                    rememberPos = reader.getPosition();
                    reader.setPosition(reader.getHeader().getFStart() + stringOffset);

                    type = reader.readByte();
                    fname = reader.readString();
                    break;
                }
                case TYPE_WZ_DIRECTORY_3:
                case TYPE_WZ_IMAGE_4: {
                    fname = reader.readString();
                    rememberPos = reader.getPosition();
                    break;
                }
                default:
                    throw new WzException("[WzDirectory] Unknown directory entry type = " + (type & 0xFF) +
                        " at position " + reader.getPosition() + ", entry " + i + "/" + entryCount);
            }

            reader.setPosition(rememberPos);
            fsize = reader.readCompressedInt();
            checksum = reader.readCompressedInt();
            offset = reader.readOffset();

            if (type == TYPE_WZ_DIRECTORY_3) {
                WzDirectory subDir = new WzDirectory(reader, fname, hash, wzIv, wzFile);
                subDir.setBlockSize(fsize);
                subDir.setChecksum(checksum);
                subDir.setOffset(offset);
                subDir.setParent(this);
                subDirs.add(subDir);

                if (lazyParse) break;
            } else {
                WzImage img = new WzImage(fname, reader, checksum);
                img.setBlockSize(fsize);
                img.setOffset(offset);
                img.setParent(this);
                images.add(img);

                if (lazyParse) break;
            }
        }

        // Recursively parse subdirectories
        for (WzDirectory subdir : subDirs) {
            reader.setPosition(subdir.offset);
            subdir.parseDirectory();
        }
    }

    /**
     * Count total images recursively.
     */
    public int countImages() {
        int count = images.size();
        for (WzDirectory sub : subDirs) {
            count += sub.countImages();
        }
        return count;
    }

    // ---- Save methods (Phase 5) ----

    public void setVersionHash(long newHash) {
        this.hash = newHash;
        for (WzDirectory dir : subDirs) {
            dir.setVersionHash(newHash);
        }
    }

    /**
     * Generate data file: serialize images to temp stream.
     * For unmodified round-trip, unchanged images just record original offsets.
     * Ported from: WzDirectory.GenerateDataFile()
     */
    public int generateDataFile(byte[] useIv, boolean bIsWzUserKeyDefault, java.io.OutputStream tempStream, wzlib.util.WzBinaryReader originalReader) throws IOException {
        size = 0;
        int entryCount = subDirs.size() + images.size();
        if (entryCount == 0) {
            offsetSize = 1;
            return (size = 0);
        }
        size = wzlib.util.WzTool.getCompressedIntLength(entryCount);
        offsetSize = wzlib.util.WzTool.getCompressedIntLength(entryCount);

        for (WzImage img : images) {
            if (img.isChanged()) {
                // Serialize changed image to temp stream
                java.io.ByteArrayOutputStream memStream = new java.io.ByteArrayOutputStream();
                wzlib.util.WzBinaryWriter imgWriter = new wzlib.util.WzBinaryWriter(memStream, this.wzIv);
                img.saveImage(imgWriter);
                imgWriter.flush();
                byte[] imgData = memStream.toByteArray();
                img.calculateAndSetChecksum(imgData);
                long currentTempPos = tempStream instanceof java.io.ByteArrayOutputStream
                    ? ((java.io.ByteArrayOutputStream) tempStream).size() : 0;
                img.tempFileStart = currentTempPos;
                tempStream.write(imgData);
                img.tempFileEnd = currentTempPos + imgData.length;
                img.setBlockSize(imgData.length);
            } else {
                img.tempFileStart = img.getOffset();
                img.tempFileEnd = img.getOffset() + img.getBlockSize();
            }

            int nameLen = wzlib.util.WzTool.getWzObjectValueLength(img.getName(), (byte) 4);
            size += nameLen;
            int imgLen = img.getBlockSize();
            size += wzlib.util.WzTool.getCompressedIntLength(imgLen);
            size += imgLen;
            size += wzlib.util.WzTool.getCompressedIntLength(img.getChecksum());
            size += 4; // offset
            offsetSize += nameLen;
            offsetSize += wzlib.util.WzTool.getCompressedIntLength(imgLen);
            offsetSize += wzlib.util.WzTool.getCompressedIntLength(img.getChecksum());
            offsetSize += 4;
        }

        for (WzDirectory dir : subDirs) {
            int nameLen = wzlib.util.WzTool.getWzObjectValueLength(dir.getName(), (byte) 3);
            size += nameLen;
            size += dir.generateDataFile(useIv, bIsWzUserKeyDefault, tempStream, originalReader);
            size += wzlib.util.WzTool.getCompressedIntLength(dir.size);
            size += wzlib.util.WzTool.getCompressedIntLength(dir.checksum);
            size += 4;
            offsetSize += nameLen;
            offsetSize += wzlib.util.WzTool.getCompressedIntLength(dir.size);
            offsetSize += wzlib.util.WzTool.getCompressedIntLength(dir.checksum);
            offsetSize += 4;
        }
        return size;
    }

    /**
     * Calculate directory offsets.
     * Ported from: WzDirectory.GetOffsets()
     */
    public long getOffsets(long curOffset) {
        offset = curOffset;
        curOffset += offsetSize;
        for (WzDirectory dir : subDirs) {
            curOffset = dir.getOffsets(curOffset);
        }
        return curOffset;
    }

    /**
     * Calculate image offsets after all directories.
     * Ported from: WzDirectory.GetImgOffsets()
     */
    public long getImgOffsets(long curOffset) {
        for (WzImage img : images) {
            img.setOffset(curOffset);
            curOffset += img.getBlockSize();
        }
        for (WzDirectory dir : subDirs) {
            curOffset = dir.getImgOffsets(curOffset);
        }
        return curOffset;
    }

    /**
     * Write directory entries to writer.
     * Ported from: WzDirectory.SaveDirectory()
     */
    public void saveDirectory(wzlib.util.WzBinaryWriter writer) throws IOException {
        offset = writer.getPosition();
        int entryCount = subDirs.size() + images.size();
        if (entryCount == 0) {
            size = 0;
            return;
        }
        writer.writeCompressedInt(entryCount);
        for (WzImage img : images) {
            writer.writeWzObjectValue(img.getName(), (byte) 4); // WzImage type = 4
            writer.writeCompressedInt(img.getBlockSize());
            writer.writeCompressedInt(img.getChecksum());
            writer.writeOffset(img.getOffset());
        }
        for (WzDirectory dir : subDirs) {
            writer.writeWzObjectValue(dir.getName(), (byte) 3); // WzDirectory type = 3
            writer.writeCompressedInt(dir.getBlockSize());
            writer.writeCompressedInt(dir.getChecksum());
            writer.writeOffset(dir.getOffset());
        }
        for (WzDirectory dir : subDirs) {
            if (dir.size > 0) {
                dir.saveDirectory(writer);
            } else {
                writer.writeByte((byte) 0);
            }
        }
    }

    /**
     * Write image data from temp file or original reader.
     * Ported from: WzDirectory.SaveImages()
     */
    public void saveImages(wzlib.util.WzBinaryWriter writer, java.io.RandomAccessFile tempFile, wzlib.util.WzBinaryReader originalReader) throws IOException {
        for (WzImage img : images) {
            if (img.isChanged()) {
                // Read from temp file
                tempFile.seek(img.tempFileStart);
                int len = (int) (img.tempFileEnd - img.tempFileStart);
                byte[] buffer = new byte[len];
                tempFile.readFully(buffer);
                writer.writeBytes(buffer);
            } else {
                // Read from original file
                synchronized (originalReader) {
                    originalReader.setPosition(img.tempFileStart);
                    int len = (int) (img.tempFileEnd - img.tempFileStart);
                    byte[] buffer = originalReader.readBytes(len);
                    writer.writeBytes(buffer);
                }
            }
        }
        for (WzDirectory dir : subDirs) {
            dir.saveImages(writer, tempFile, originalReader);
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
