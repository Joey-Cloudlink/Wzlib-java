package wzlib;

import wzlib.util.WzBinaryReader;
import wzlib.util.WzBinaryWriter;
import wzlib.util.WzTool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * WZ 檔案的主要入口。
 * Main entry point for WZ files.
 *
 * Ported from: MapleLib/WzLib/WzFile.cs
 *
 * Main entry point for opening, parsing, and saving .wz files.
 */
public class WzFile extends WzObject {

    private final File filePath;
    private WzDirectory wzDir;
    private WzHeader header;
    private WzBinaryReader reader;

    private int wzVersionHeader = 0;
    private static final int WZ_VERSION_HEADER_64BIT_START = 770;

    private long versionHash = 0;
    private short mapleStoryPatchVersion = 0;
    private WzMapleVersion mapleVersion;
    private boolean wz_withEncryptVersionHeader = true;

    private byte[] wzIv;

    // ---- Constructors (ported from C# WzFile constructors) ----

    /**
     * 以自動偵測版本開啟 .wz。
     * Open .wz with auto version detection.
     *
     * @param filePath WZ 檔案路徑 / path to the .wz file
     * @param version  MapleStory 版本類型 / MapleStory version type
     */
    public WzFile(String filePath, WzMapleVersion version) {
        this(filePath, (short) -1, version);
    }

    /**
     * 以指定版本開啟 .wz。
     * Open .wz with known game version.
     *
     * @param filePath    WZ 檔案路徑 / path to the .wz file
     * @param gameVersion 遊戲版本號 / game patch version number
     * @param version     MapleStory 版本類型 / MapleStory version type
     */
    public WzFile(String filePath, short gameVersion, WzMapleVersion version) {
        this.filePath = new File(filePath);
        this.name = this.filePath.getName();
        this.mapleStoryPatchVersion = gameVersion;
        this.mapleVersion = version;

        if (version == WzMapleVersion.GETFROMZLZ) {
            // Read IV from ZLZ.dll in the same directory
            // Ported from: C# WzFile constructor GETFROMZLZ branch
            File zlzFile = new File(this.filePath.getParent(), "ZLZ.dll");
            if (zlzFile.exists()) {
                try (java.io.RandomAccessFile zlz = new java.io.RandomAccessFile(zlzFile, "r")) {
                    zlz.seek(0x10040);
                    byte[] iv = new byte[4];
                    zlz.readFully(iv);
                    this.wzIv = iv;
                } catch (IOException e) {
                    throw new WzException("Failed to read IV from ZLZ.dll", e);
                }
            } else {
                throw new WzException("ZLZ.dll not found at: " + zlzFile.getAbsolutePath());
            }
        } else {
            this.wzIv = WzTool.getIvByMapleVersion(version);
        }
    }

    /**
     * 以自定義 IV 開啟（私服用）。
     * Open with custom IV (private server).
     *
     * @param filePath    WZ 檔案路徑 / path to the .wz file
     * @param customWzIv  自定義初始化向量 / custom initialization vector
     */
    public WzFile(String filePath, byte[] customWzIv) {
        this.filePath = new File(filePath);
        this.name = this.filePath.getName();
        this.mapleStoryPatchVersion = -1;
        this.mapleVersion = WzMapleVersion.CUSTOM;
        this.wzIv = customWzIv.clone();
    }

    /**
     * Open a wz file with a custom IV and known game version (for private servers).
     */
    public WzFile(String filePath, short gameVersion, byte[] customWzIv) {
        this.filePath = new File(filePath);
        this.name = this.filePath.getName();
        this.mapleStoryPatchVersion = gameVersion;
        this.mapleVersion = WzMapleVersion.CUSTOM;
        this.wzIv = customWzIv.clone();
    }

    // ---- Properties ----

    @Override
    public WzObjectType getObjectType() {
        return WzObjectType.File;
    }

    /** 取得根目錄。/ Get root directory. */
    public WzDirectory getRoot() { return wzDir; }
    /** 取得檔案標頭。/ Get file header. */
    public WzHeader getHeader() { return header; }
    /** 取得偵測到的版本號。/ Get detected version number. */
    public short getVersion() { return mapleStoryPatchVersion; }
    /** 是否為 64-bit WZ 格式。/ Whether this is a 64-bit WZ format. */
    public boolean is64BitWzFile() { return !wz_withEncryptVersionHeader; }
    /** 取得檔案路徑。/ Get file path. */
    public String getFilePath() { return filePath.getAbsolutePath(); }

    @Override
    public WzObject getChild(String name) {
        return wzDir != null ? wzDir.getChild(name) : null;
    }

    private final java.util.Map<String, WzObject> pathCache = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 以路徑快取查找物件。
     * Lookup object by path with caching.
     *
     * @param path 從根目錄開始的路徑 / path from root
     * @return 找到的物件或 null / the object found, or null
     */
    public WzObject getFromPathCached(String path) {
        return pathCache.computeIfAbsent(path, p -> {
            if (wzDir == null) return null;
            return wzDir.getFromPath(p);
        });
    }

    /**
     * 清除路徑快取（修改後應呼叫）。
     * Clear path cache (call after modifications).
     */
    public void clearPathCache() {
        pathCache.clear();
    }

    // ---- Parsing ----

    /**
     * Parse the WZ file.
     * Ported from: WzFile.ParseWzFile() + ParseMainWzDirectory()
     */
    public void parseWzFile() throws IOException {
        reader = new WzBinaryReader(filePath, wzIv);

        // Read header — ported from ParseMainWzDirectory
        this.header = new WzHeader();
        this.header.setIdent(reader.readString(4));
        this.header.setFSize(reader.readInt64());      // ReadUInt64 in C#
        this.header.setFStart(reader.readUInt32());     // ReadUInt32 in C#
        this.header.setCopyright(reader.readString((int) (this.header.getFStart() - 17)));

        byte unk1 = reader.readByte();
        // Read remaining padding bytes until FStart
        long currentPos = reader.getPosition();
        int remaining = (int) (this.header.getFStart() - currentPos);
        if (remaining > 0) {
            reader.readBytes(remaining);
        }

        reader.setHeader(this.header);

        // Check if this is a 64-bit WZ file (no encVer header)
        check64BitClient(reader);

        // Read wzVersionHeader or use fixed value for 64-bit
        this.wzVersionHeader = this.wz_withEncryptVersionHeader
                ? reader.readUInt16()
                : WZ_VERSION_HEADER_64BIT_START;

        if (mapleStoryPatchVersion == -1) {
            // Auto-detect version by brute force
            short detectedVersion = -1;

            // For 64-bit files, try 770~780 first
            if (!this.wz_withEncryptVersionHeader) {
                for (int ver = WZ_VERSION_HEADER_64BIT_START; ver < WZ_VERSION_HEADER_64BIT_START + 10; ver++) {
                    if (tryDecodeWithVersion(reader, wzVersionHeader, ver)) {
                        detectedVersion = this.mapleStoryPatchVersion;
                        break;
                    }
                }
            }

            // Brute force 0~2000
            if (detectedVersion == -1) {
                for (int j = 0; j < 2000; j++) {
                    if (tryDecodeWithVersion(reader, wzVersionHeader, j)) {
                        detectedVersion = this.mapleStoryPatchVersion;
                        break;
                    }
                }
            }

            if (detectedVersion == -1) {
                throw new WzException("Unable to detect WZ version. Tried versions 0-2000.");
            }

            // Re-parse with known version for full accuracy
            this.mapleStoryPatchVersion = detectedVersion;
            this.versionHash = checkAndGetVersionHash(wzVersionHeader, detectedVersion);
            long dirStart = this.wz_withEncryptVersionHeader
                    ? this.header.getFStart() + 2
                    : this.header.getFStart();
            reader.setPosition(dirStart);
            reader.setHash(this.versionHash);
            WzDirectory fullDir = new WzDirectory(reader, this.name, this.versionHash, this.wzIv, this);
            fullDir.parseDirectory();
            this.wzDir = fullDir;
        } else {
            // Known version
            this.versionHash = checkAndGetVersionHash(wzVersionHeader, mapleStoryPatchVersion);
            reader.setHash(this.versionHash);

            WzDirectory directory = new WzDirectory(reader, this.name, this.versionHash, this.wzIv, this);
            directory.parseDirectory();
            this.wzDir = directory;
        }
    }

    /**
     * Check if this is a 64-bit WZ file.
     * Ported from: WzFile.Check64BitClient(WzBinaryReader)
     */
    private void check64BitClient(WzBinaryReader reader) throws IOException {
        if (this.header.getFSize() >= 2) {
            reader.setPosition(this.header.getFStart());
            int encver = reader.readUInt16();

            if (encver > 0xFF) {
                this.wz_withEncryptVersionHeader = false;
            } else if (encver == 0x80) {
                if (this.header.getFSize() >= 5) {
                    reader.setPosition(this.header.getFStart());
                    int propCount = reader.readInt32();
                    if (propCount > 0 && (propCount & 0xFF) == 0 && propCount <= 0xFFFF) {
                        this.wz_withEncryptVersionHeader = false;
                    }
                }
            }
        } else {
            this.wz_withEncryptVersionHeader = false;
        }

        // Reset position
        reader.setPosition(this.header.getFStart());
    }

    /**
     * Try to decode with a specific WZ version number.
     * Ported from: WzFile.TryDecodeWithWZVersionNumber()
     */
    /**
     * Try to decode with a specific WZ version number.
     * Ported from: WzFile.TryDecodeWithWZVersionNumber()
     *
     * Key C# behavior: even if checkByte is not 0x73, as long as directory parse
     * succeeds and has images, it returns true (trusting the parse result).
     */
    private boolean tryDecodeWithVersion(WzBinaryReader reader, int useWzVersionHeader, int useMapleStoryPatchVersion) throws IOException {
        this.mapleStoryPatchVersion = (short) useMapleStoryPatchVersion;

        this.versionHash = checkAndGetVersionHash(useWzVersionHeader, mapleStoryPatchVersion);
        if (this.versionHash == 0) return false;

        reader.setHash(this.versionHash);
        long fallbackPosition = reader.getPosition();

        // Try parse directory (first try block in C#)
        WzDirectory testDirectory;
        try {
            testDirectory = new WzDirectory(reader, this.name, this.versionHash, this.wzIv, this);
            testDirectory.parseDirectory(true); // lazy parse for version detection only
        } catch (Exception e) {
            reader.setPosition(fallbackPosition);
            return false;
        }

        // Verify image (second try block in C#)
        try {
            WzImage testImage = testDirectory.getWzImages().isEmpty() ? null : testDirectory.getWzImages().get(0);
            if (testImage != null) {
                try {
                    reader.setPosition(testImage.getOffset());
                    byte checkByte = reader.readByte();
                    reader.setPosition(fallbackPosition);

                    if (checkByte == 0x73 || checkByte == 0x1B) {
                        // C# re-parses to advance reader, but saves testDirectory.
                        // If re-parse fails, still use testDirectory.
                        try {
                            WzDirectory directory = new WzDirectory(reader, this.name, this.versionHash, this.wzIv, this);
                            directory.parseDirectory(false);
                        } catch (Exception ignored) {
                            // re-parse failed, but testDirectory is valid
                        }
                        this.wzDir = testDirectory;
                        return true;
                    }
                    // C# default case: log warning, reset position, fall through to return true
                    reader.setPosition(fallbackPosition);
                } catch (Exception e) {
                    reader.setPosition(fallbackPosition);
                    return false;
                }
                // C# line 407: return true even for unknown checkByte
                this.wzDir = testDirectory;
                return true;
            } else {
                // No images — trust the directory parse
                if (is64BitWzFile() && mapleStoryPatchVersion == 113) {
                    reader.setPosition(fallbackPosition);
                    return false;
                }
                this.wzDir = testDirectory;
                return true;
            }
        } catch (Exception e) {
            reader.setPosition(fallbackPosition);
            return false;
        }
    }

    /**
     * Check and get version hash.
     * Ported from: WzFile.CheckAndGetVersionHash(int, int)
     *
     * C#:
     *   uint versionHash = 0;
     *   foreach (char ch in maplestoryPatchVersion.ToString())
     *     versionHash = (versionHash * 32) + (byte)ch + 1;
     *   if (wzVersionHeader == wzVersionHeader64bit_start) return versionHash;
     *   int decryptedVersionNumber = (byte)~((...) ^ (...) ^ (...) ^ (...));
     *   if (wzVersionHeader == decryptedVersionNumber) return versionHash;
     *   return 0;
     */
    private static long checkAndGetVersionHash(int wzVersionHeader, int maplestoryPatchVersion) {
        long versionHash = 0;
        for (char ch : String.valueOf(maplestoryPatchVersion).toCharArray()) {
            versionHash = ((versionHash * 32) + (ch & 0xFF) + 1) & 0xFFFFFFFFL;
        }

        if (wzVersionHeader == WZ_VERSION_HEADER_64BIT_START) {
            return versionHash;
        }

        int decryptedVersionNumber = (byte) ~(
                (byte) ((versionHash >> 24) & 0xFF) ^
                (byte) ((versionHash >> 16) & 0xFF) ^
                (byte) ((versionHash >> 8) & 0xFF) ^
                (byte) (versionHash & 0xFF)
        );
        decryptedVersionNumber &= 0xFF; // ensure unsigned byte comparison

        if (wzVersionHeader == decryptedVersionNumber) {
            return versionHash;
        }
        return 0;
    }

    /**
     * Save the WZ file to disk.
     * Ported from: WzFile.SaveToDisk()
     */
    public void saveToDisk(String outputPath) throws IOException {
        // Compute version hash
        createWzVersionHash();
        wzDir.setVersionHash(versionHash);

        boolean bSaveAs64BitWz = !this.wz_withEncryptVersionHeader;

        // Step 1: Generate data file (serialize changed images to temp)
        File tempFile = new File(outputPath + ".TEMP");
        try (FileOutputStream tempFs = new FileOutputStream(tempFile)) {
            wzDir.generateDataFile(null, true, tempFs, reader);
        }

        WzTool.stringCache.clear();

        // Step 2: Write the final file
        try (FileOutputStream fos = new FileOutputStream(outputPath);
             WzBinaryWriter wzWriter = new WzBinaryWriter(fos, wzIv)) {
            wzWriter.setHash(versionHash);

            // Calculate offsets
            long startOffset = header.getFStart() + (bSaveAs64BitWz ? 0 : 2);
            long totalLen = wzDir.getImgOffsets(wzDir.getOffsets(startOffset));
            header.setFSize(totalLen - header.getFStart());

            // Write header
            for (int i = 0; i < 4; i++) {
                wzWriter.writeByte((byte) header.getIdent().charAt(i));
            }
            wzWriter.writeInt64(header.getFSize());
            wzWriter.writeInt32((int) header.getFStart());
            wzWriter.writeNullTerminatedString(header.getCopyright());

            // Padding to FStart
            long extraLen = header.getFStart() - wzWriter.getPosition();
            if (extraLen > 0) {
                wzWriter.writeBytes(new byte[(int) extraLen]);
            }

            // Version header (not for 64-bit)
            if (!bSaveAs64BitWz) {
                wzWriter.writeUInt16(wzVersionHeader);
            }

            wzWriter.setHeader(header);

            // Write directories
            wzDir.saveDirectory(wzWriter);
            wzWriter.getStringCache().clear();

            // Write images from temp file
            try (RandomAccessFile tempRaf = new RandomAccessFile(tempFile, "r")) {
                wzDir.saveImages(wzWriter, tempRaf, reader);
            }

            wzWriter.getStringCache().clear();
        } finally {
            tempFile.delete();
        }
    }

    /**
     * Compute version hash and wzVersionHeader.
     * Ported from: WzFile.CreateWZVersionHash()
     */
    private void createWzVersionHash() {
        versionHash = 0;
        for (char ch : String.valueOf(mapleStoryPatchVersion).toCharArray()) {
            versionHash = ((versionHash * 32) + (ch & 0xFF) + 1) & 0xFFFFFFFFL;
        }
        wzVersionHeader = (int) ((byte) ~(
                (byte) ((versionHash >> 24) & 0xFF) ^
                (byte) ((versionHash >> 16) & 0xFF) ^
                (byte) ((versionHash >> 8) & 0xFF) ^
                (byte) (versionHash & 0xFF)
        )) & 0xFF;
    }

    @Override
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
            reader = null;
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
