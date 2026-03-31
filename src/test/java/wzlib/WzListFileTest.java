package wzlib;

import java.io.*;
import java.nio.file.*;
import java.util.List;

/**
 * Test List.wz parsing and round-trip.
 */
public class WzListFileTest {

    static final String CLIENT_DIR = "C:\\Users\\Joey\\Documents\\楓之谷\\MapleStory客戶端\\乾淨主程式v113";
    static int pass = 0, fail = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== List.wz Test ===\n");

        Path listWz = Paths.get(CLIENT_DIR, "List.wz");

        // Test 1: isListFile
        System.out.println("--- Test 1: isListFile detection ---");
        check("List.wz is list format", WzListFile.isListFile(listWz.toString()));

        Path itemWz = Paths.get(CLIENT_DIR, "Item.wz");
        check("Item.wz is NOT list format", !WzListFile.isListFile(itemWz.toString()));

        // Test 2: Parse List.wz with different IV versions
        System.out.println("\n--- Test 2: Parse List.wz ---");
        List<String> entries = null;
        WzMapleVersion[] tryVersions = {WzMapleVersion.BMS, WzMapleVersion.EMS, WzMapleVersion.GMS};

        for (WzMapleVersion ver : tryVersions) {
            try {
                entries = WzListFile.parse(listWz.toString(), ver);
                // Check if entries look valid (should contain .img paths)
                boolean hasValidEntries = false;
                for (String entry : entries) {
                    if (entry.contains(".img")) {
                        hasValidEntries = true;
                        break;
                    }
                }
                if (hasValidEntries) {
                    System.out.println("  " + ver + ": " + entries.size() + " entries (valid)");
                    check("Parsed with " + ver, true);
                    break;
                } else {
                    System.out.println("  " + ver + ": " + entries.size() + " entries (garbled)");
                    entries = null;
                }
            } catch (Exception e) {
                System.out.println("  " + ver + ": FAIL - " + e.getMessage());
            }
        }

        if (entries == null) {
            check("Parse List.wz", false);
            System.out.println("  All versions failed");
        } else {
            // Print first 20 entries
            System.out.println("\n--- Entries (first 20) ---");
            for (int i = 0; i < Math.min(20, entries.size()); i++) {
                System.out.println("  " + entries.get(i));
            }
            if (entries.size() > 20) {
                System.out.println("  ... +" + (entries.size() - 20) + " more");
            }

            check("Has .img entries", entries.stream().anyMatch(e -> e.contains(".img")));
            check("Has .wz entries", entries.stream().anyMatch(e -> e.contains(".wz")));

            // Test 3: Round-trip save
            System.out.println("\n--- Test 3: Round-trip save ---");
            String savedPath = "C:/Users/Joey/Desktop/List_saved.wz";
            WzMapleVersion ver = WzMapleVersion.BMS; // assume BMS worked
            // Find which version worked
            for (WzMapleVersion v : tryVersions) {
                try {
                    List<String> test = WzListFile.parse(listWz.toString(), v);
                    if (test.stream().anyMatch(e -> e.contains(".img"))) {
                        ver = v;
                        break;
                    }
                } catch (Exception e) { /* skip */ }
            }

            WzListFile.save(savedPath, ver, entries);
            long origSize = Files.size(listWz);
            long savedSize = Files.size(Paths.get(savedPath));
            System.out.println("  Original: " + origSize + " bytes");
            System.out.println("  Saved:    " + savedSize + " bytes");

            // Re-read saved file
            List<String> reread = WzListFile.parse(savedPath, ver);
            check("Re-read count matches (" + entries.size() + " vs " + reread.size() + ")",
                entries.size() == reread.size());

            // Compare entries
            boolean allMatch = true;
            for (int i = 0; i < Math.min(entries.size(), reread.size()); i++) {
                if (!entries.get(i).equals(reread.get(i))) {
                    System.out.println("  MISMATCH at " + i + ": '" + entries.get(i) + "' vs '" + reread.get(i) + "'");
                    allMatch = false;
                    break;
                }
            }
            check("All entries match", allMatch);

            // Cleanup
            Files.deleteIfExists(Paths.get(savedPath));
        }

        System.out.printf("\n=== Results: %d/%d PASS ===%n", pass, pass + fail);
    }

    static void check(String name, boolean ok) {
        System.out.println("  " + (ok ? "PASS" : "FAIL") + ": " + name);
        if (ok) pass++; else fail++;
    }
}
