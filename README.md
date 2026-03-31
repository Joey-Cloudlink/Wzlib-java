# wzlib-java

[繁體中文](#繁體中文) | [English](#english)

---

# 繁體中文

用於讀取、修改、儲存 MapleStory `.wz` 檔案的 Java 函式庫。移植自 [C# MapleLib](https://github.com/lastbattle/MapleLib)。

## 功能特色

- **讀取**所有 `.wz` 檔案（GMS / EMS / BMS / KMS / TMS / 私服）
- **修改**任何屬性值（int、string、canvas、vector 等）
- **儲存**回 `.wz` 格式
- **自動偵測** WZ 版本號（暴力嘗試 0~2000）
- **PNG 圖片解碼**（BGRA4444、BGRA8888、RGB565、ARGB1555、DXT3、DXT5）
- **音效擷取**（MP3 / PCM）
- **深層複製（DeepClone）**完整屬性樹
- **XML 匯出 / 匯入**（與 HaRepacker 格式相容）
- **List.wz** 解析與寫入
- **64-bit WZ** 格式支援（GMSv230+ / KMST1132+）
- **私服支援**（自定義 IV + 自定義 AES UserKey）
- **所有屬性類型**：Null、Short、Int、Long、Float、Double、String、SubProperty、Canvas、Vector、Convex、UOL、Sound、RawData、Lua、Video
- **零外部依賴**（純 JDK 11+）

## API 一覽表

| 你想拿的 | 怎麼拿 | 回傳型別 |
|---------|--------|---------|
| 圖片 | `canvas.getPngProperty().getImage(false)` | `BufferedImage` |
| 音效 | `sound.getSoundBytes(true)` | `byte[]`（MP3/PCM） |
| 整數值 | `((WzIntProperty) prop).getInt()` | `int` |
| 字串 | `((WzStringProperty) prop).getString()` | `String` |
| 短整數 | `((WzShortProperty) prop).getShort()` | `short` |
| 長整數 | `((WzLongProperty) prop).getLong()` | `long` |
| 浮點數 | `((WzFloatProperty) prop).getFloat()` | `float` |
| 倍精度 | `((WzDoubleProperty) prop).getDouble()` | `double` |
| 座標 | `vec.getX().getInt()`, `vec.getY().getInt()` | `int, int` |
| UOL 連結 | `((WzUOLProperty) prop).getUOL()` | `String`（路徑） |
| Lua 腳本 | `((WzLuaProperty) prop).getString()` | `String`（解密後原始碼） |
| 原始資料 | `rawData.getBytes(true)` | `byte[]` |
| 影片資料 | `video.getBytes(true)` | `byte[]` |
| 壓縮圖片 | `png.getCompressedBytes(false)` | `byte[]`（WZ 原始格式） |
| 目錄結構 | `root.getWzDirectories()` / `.getWzImages()` | `List` |
| 路徑查找 | `root.getFromPath("Cash/0526.img/info")` | `WzObject` |
| 快取查找 | `wz.getFromPathCached("Cash/0526.img")` | `WzObject` |
| 複製節點 | `prop.deepClone()` | `WzImageProperty` |
| 匯出 XML | `WzXmlSerializer.serializeFile(wz, dir)` | XML 檔案 |
| 匯入 XML | `WzXmlDeserializer.parseXml(file)` | `List<WzImageProperty>` |
| List.wz | `WzListFile.parse("List.wz", ver)` | `List<String>` |

## 快速開始

### 加入你的專案

從 [Releases](../../releases) 下載 `wzlib-java-1.0.0.jar` 並加入 classpath。

或使用 Maven：
```xml
<dependency>
    <groupId>com.github.wzlib</groupId>
    <artifactId>wzlib-java</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 讀取 .wz 檔案

```java
import wzlib.*;
import wzlib.property.*;

// 自動偵測版本
WzFile wz = new WzFile("Item.wz", WzMapleVersion.EMS);
wz.parseWzFile();

// 瀏覽目錄
WzDirectory root = wz.getRoot();
System.out.println("Images: " + root.countImages());

// 用路徑查找
WzObject prop = root.getFromPath("Cash/0526.img/05260000/info/slotMax");
int slotMax = ((WzIntProperty) prop).getInt();

wz.close();
```

### 修改並儲存

```java
WzFile wz = new WzFile("Item.wz", (short) 113, WzMapleVersion.EMS);
wz.parseWzFile();

// 導航到屬性
WzImage img = (WzImage) wz.getRoot().getFromPath("Cash/0526.img");
WzIntProperty slotMax = (WzIntProperty) img.getFromPath("05260000/info/slotMax");

// 修改
slotMax.setInt(999);
img.setChanged(true);

// 儲存
wz.saveToDisk("Item_modified.wz");
wz.close();
```

### 解碼 PNG 圖片

```java
WzCanvasProperty canvas = (WzCanvasProperty) img.getFromPath("05260000/info/icon");
BufferedImage image = canvas.getPngProperty().getImage(false);
ImageIO.write(image, "PNG", new File("icon.png"));
```

### 深層複製

```java
WzSubProperty original = (WzSubProperty) img.getFromPath("05260000");
WzSubProperty clone = (WzSubProperty) original.deepClone();
clone.setName("05269999");
// 修改複製品不會影響原始資料
```

### XML 匯出 / 匯入

```java
// 匯出
WzXmlSerializer.serializeImage(image, new File("0526.img.xml"));
WzXmlSerializer.serializeFile(wzFile, new File("output_dir/"));

// 匯入
List<WzImageProperty> props = WzXmlDeserializer.parseXml(new File("0526.img.xml"));
```

### List.wz

```java
List<String> entries = WzListFile.parse("List.wz", WzMapleVersion.EMS);
// entries: ["dummy", "character/00002009.img", ...]

WzListFile.save("List_new.wz", WzMapleVersion.EMS, entries);
```

### 私服（自定義 IV / UserKey）

```java
// 自定義 IV
byte[] myIv = {0x11, 0x22, 0x33, 0x44};
WzFile wz = new WzFile("Item.wz", (short) 113, myIv);

// 自定義 UserKey（全域設定）
byte[] myKey = new byte[128]; // 你的私服金鑰
WzCryptoConstants.setActiveUserKey(myKey);
WzFile wz = new WzFile("Item.wz", WzMapleVersion.EMS);

// 從 ZLZ.dll 讀取 IV
WzFile wz = new WzFile("Item.wz", WzMapleVersion.GETFROMZLZ);
```

## 已測試版本

| 版本 | 測試結果 |
|------|---------|
| TMS v113（乾淨客戶端，16 個 .wz 檔案） | ✅ 100% |
| TMS 高版本 UI（v276） | ✅ 156/156 images |
| GMS / EMS / BMS 加密 | ✅ |
| 私服自定義 IV | ✅ |
| 64-bit WZ（GMSv230+） | ✅（偵測邏輯） |

## 編譯

需要 **JDK 11+**。

```bash
# 編譯
javac -d target/classes src/main/java/wzlib/*.java src/main/java/wzlib/**/*.java

# 或使用 Maven
mvn clean package
```

## 架構

從 C# [MapleLib](https://github.com/lastbattle/MapleLib) 1:1 對照移植：

| C# | Java |
|----|------|
| `WzFile` | `wzlib.WzFile` |
| `WzDirectory` | `wzlib.WzDirectory` |
| `WzImage` | `wzlib.WzImage` |
| `WzBinaryReader` | `wzlib.util.WzBinaryReader` |
| `WzBinaryWriter` | `wzlib.util.WzBinaryWriter` |
| `WzProperties/*` | `wzlib.property.*` |
| `MapleCryptoConstants` | `wzlib.crypto.WzCryptoConstants` |

## 授權

MIT License - 詳見 [LICENSE](LICENSE)

## 致謝

- [MapleLib (C#)](https://github.com/lastbattle/MapleLib) by lastbattle - 原始 WZ 函式庫
- [HaRepacker](https://github.com/lastbattle/Harepacker-resurrected) - 使用 MapleLib 的 WZ 編輯器

---

# English

Java library for reading, modifying, and saving MapleStory `.wz` files. Ported from [C# MapleLib](https://github.com/lastbattle/MapleLib).

## Features

- **Read** all `.wz` files (GMS / EMS / BMS / KMS / TMS / Private Servers)
- **Modify** any property value (int, string, canvas, vector, etc.)
- **Save** back to `.wz` format
- **Auto-detect** WZ version (brute force 0~2000)
- **PNG decode** (BGRA4444, BGRA8888, RGB565, ARGB1555, DXT3, DXT5)
- **Sound extract** (MP3 / PCM)
- **DeepClone** any property tree
- **XML export / import** (compatible with HaRepacker format)
- **List.wz** parsing and writing
- **64-bit WZ** format support (GMSv230+ / KMST1132+)
- **Private server** support (custom IV + custom AES UserKey)
- **All property types**: Null, Short, Int, Long, Float, Double, String, SubProperty, Canvas, Vector, Convex, UOL, Sound, RawData, Lua, Video
- **Zero external dependencies** (pure JDK 11+)

## API Reference

| What you need | How to get it | Return type |
|--------------|---------------|-------------|
| Image | `canvas.getPngProperty().getImage(false)` | `BufferedImage` |
| Sound | `sound.getSoundBytes(true)` | `byte[]` (MP3/PCM) |
| Integer | `((WzIntProperty) prop).getInt()` | `int` |
| String | `((WzStringProperty) prop).getString()` | `String` |
| Short | `((WzShortProperty) prop).getShort()` | `short` |
| Long | `((WzLongProperty) prop).getLong()` | `long` |
| Float | `((WzFloatProperty) prop).getFloat()` | `float` |
| Double | `((WzDoubleProperty) prop).getDouble()` | `double` |
| Coordinates | `vec.getX().getInt()`, `vec.getY().getInt()` | `int, int` |
| UOL link | `((WzUOLProperty) prop).getUOL()` | `String` (path) |
| Lua script | `((WzLuaProperty) prop).getString()` | `String` (decrypted) |
| Raw data | `rawData.getBytes(true)` | `byte[]` |
| Video data | `video.getBytes(true)` | `byte[]` |
| Compressed PNG | `png.getCompressedBytes(false)` | `byte[]` (WZ format) |
| Directory tree | `root.getWzDirectories()` / `.getWzImages()` | `List` |
| Path lookup | `root.getFromPath("Cash/0526.img/info")` | `WzObject` |
| Cached lookup | `wz.getFromPathCached("Cash/0526.img")` | `WzObject` |
| Clone node | `prop.deepClone()` | `WzImageProperty` |
| Export XML | `WzXmlSerializer.serializeFile(wz, dir)` | XML files |
| Import XML | `WzXmlDeserializer.parseXml(file)` | `List<WzImageProperty>` |
| List.wz | `WzListFile.parse("List.wz", ver)` | `List<String>` |

## Quick Start

### Add to your project

Download `wzlib-java-1.0.0.jar` from [Releases](../../releases) and add it to your classpath.

Or with Maven:
```xml
<dependency>
    <groupId>com.github.wzlib</groupId>
    <artifactId>wzlib-java</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Read a .wz file

```java
import wzlib.*;
import wzlib.property.*;

// Auto-detect version
WzFile wz = new WzFile("Item.wz", WzMapleVersion.EMS);
wz.parseWzFile();

// Browse directory
WzDirectory root = wz.getRoot();
System.out.println("Images: " + root.countImages());

// Navigate by path
WzObject prop = root.getFromPath("Cash/0526.img/05260000/info/slotMax");
int slotMax = ((WzIntProperty) prop).getInt();

wz.close();
```

### Modify and save

```java
WzFile wz = new WzFile("Item.wz", (short) 113, WzMapleVersion.EMS);
wz.parseWzFile();

// Navigate to property
WzImage img = (WzImage) wz.getRoot().getFromPath("Cash/0526.img");
WzIntProperty slotMax = (WzIntProperty) img.getFromPath("05260000/info/slotMax");

// Modify
slotMax.setInt(999);
img.setChanged(true);

// Save
wz.saveToDisk("Item_modified.wz");
wz.close();
```

### Decode PNG image

```java
WzCanvasProperty canvas = (WzCanvasProperty) img.getFromPath("05260000/info/icon");
BufferedImage image = canvas.getPngProperty().getImage(false);
ImageIO.write(image, "PNG", new File("icon.png"));
```

### DeepClone

```java
WzSubProperty original = (WzSubProperty) img.getFromPath("05260000");
WzSubProperty clone = (WzSubProperty) original.deepClone();
clone.setName("05269999");
// Modify clone without affecting original
```

### XML export / import

```java
// Export
WzXmlSerializer.serializeImage(image, new File("0526.img.xml"));
WzXmlSerializer.serializeFile(wzFile, new File("output_dir/"));

// Import
List<WzImageProperty> props = WzXmlDeserializer.parseXml(new File("0526.img.xml"));
```

### List.wz

```java
List<String> entries = WzListFile.parse("List.wz", WzMapleVersion.EMS);
// entries: ["dummy", "character/00002009.img", ...]

WzListFile.save("List_new.wz", WzMapleVersion.EMS, entries);
```

### Private server (custom IV / UserKey)

```java
// Custom IV
byte[] myIv = {0x11, 0x22, 0x33, 0x44};
WzFile wz = new WzFile("Item.wz", (short) 113, myIv);

// Custom UserKey (global setting)
byte[] myKey = new byte[128]; // your server's key
WzCryptoConstants.setActiveUserKey(myKey);
WzFile wz = new WzFile("Item.wz", WzMapleVersion.EMS);

// Read IV from ZLZ.dll
WzFile wz = new WzFile("Item.wz", WzMapleVersion.GETFROMZLZ);
```

## Supported Versions

| Version | Tested |
|---------|--------|
| TMS v113 (clean client, 16 .wz files) | ✅ 100% |
| TMS high-version UI (v276) | ✅ 156/156 images |
| GMS / EMS / BMS encryption | ✅ |
| Private server custom IV | ✅ |
| 64-bit WZ (GMSv230+) | ✅ (detection logic) |

## Build

Requires **JDK 11+**.

```bash
# Compile
javac -d target/classes src/main/java/wzlib/*.java src/main/java/wzlib/**/*.java

# Or with Maven
mvn clean package
```

## Architecture

Ported from C# [MapleLib](https://github.com/lastbattle/MapleLib) with 1:1 class mapping:

| C# | Java |
|----|------|
| `WzFile` | `wzlib.WzFile` |
| `WzDirectory` | `wzlib.WzDirectory` |
| `WzImage` | `wzlib.WzImage` |
| `WzBinaryReader` | `wzlib.util.WzBinaryReader` |
| `WzBinaryWriter` | `wzlib.util.WzBinaryWriter` |
| `WzProperties/*` | `wzlib.property.*` |
| `MapleCryptoConstants` | `wzlib.crypto.WzCryptoConstants` |

## License

MIT License - see [LICENSE](LICENSE)

## Credits

- [MapleLib (C#)](https://github.com/lastbattle/MapleLib) by lastbattle - original WZ library
- [HaRepacker](https://github.com/lastbattle/Harepacker-resurrected) - WZ editor that uses MapleLib
