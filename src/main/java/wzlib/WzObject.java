package wzlib;

import java.io.Closeable;
import java.io.IOException;

/**
 * 所有 WZ 物件的抽象基底類別。
 * Abstract base class for all WZ objects.
 *
 * <p>Ported from: MapleLib/WzLib/WzObject.cs</p>
 */
public abstract class WzObject implements Closeable {

    protected String name;
    protected WzObject parent;

    public abstract WzObjectType getObjectType();

    /** 取得名稱。/ Get the name. */
    public String getName() { return name; }
    /** 設定名稱。/ Set the name. */
    public void setName(String name) { this.name = name; }

    public WzObject getParent() { return parent; }
    public void setParent(WzObject parent) { this.parent = parent; }

    /**
     * 取得從根節點算起的完整路徑。
     * Get the full path from the root node.
     *
     * <p>Ported from: WzObject.FullPath</p>
     */
    public String getFullPath() {
        if (this instanceof WzFile) {
            WzFile file = (WzFile) this;
            return file.getRoot() != null ? file.getRoot().getName() : name;
        }
        StringBuilder result = new StringBuilder(this.name != null ? this.name : "");
        WzObject currObj = this;
        while (currObj.parent != null) {
            currObj = currObj.parent;
            result.insert(0, (currObj.name != null ? currObj.name : "") + "/");
        }
        return result.toString();
    }

    /**
     * Gets the topmost WzObject directory.
     * Ported from: WzObject.GetTopMostWzDirectory()
     */
    public WzObject getTopMostWzDirectory() {
        WzObject p = this.parent;
        if (p == null) return this;
        while (p.parent != null) {
            p = p.parent;
        }
        return p;
    }

    /**
     * Get child by name. Override in subclasses.
     */
    public WzObject getChild(String name) {
        return null;
    }

    /**
     * 依路徑導覽（例如 "dir/image.img/property"）。
     * Navigate by path (e.g. "dir/image.img/property").
     */
    public WzObject getFromPath(String path) {
        if (path == null || path.isEmpty()) return this;
        String[] parts = path.split("/");
        WzObject current = this;
        for (String part : parts) {
            if (part.isEmpty()) continue;
            WzObject child = current.getChild(part);
            if (child == null) return null;
            current = child;
        }
        return current;
    }

    @Override
    public void close() throws IOException {
        // default no-op
    }
}
