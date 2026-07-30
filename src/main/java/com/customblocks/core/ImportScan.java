/**
 * ImportScan.java — read the import folder and decide what WOULD happen (Group 12 §B, rules 1-7, 12, 15, 17).
 *
 * This is the whole "preview" half of `/cb importfolder`. It creates nothing, deletes nothing and moves
 * nothing except unpacking a dropped ZIP: it walks {@link CbPaths#IMPORT}, turns each file into an
 * {@link ImportEntry}, works out the id and display name each would claim, marks every problem against
 * the specific file that caused it, and reports how many slots the run needs versus how many exist. The
 * command then renders that and waits for a confirmation.
 *
 * Rules this file owns:
 *   1  · deliberate — nothing here polls; it runs when the command runs.
 *   4  · a name clash is a PROBLEM entry, never a silent overwrite.
 *   5  · ids follow the /cb create rules (safe characters only); display names get a capital on every
 *        word through {@link NameCase#titleCase}, the same call SlotManager.create makes.
 *   7  · more ready files than free slots → the run reports the overflow as leftovers rather than
 *        truncating silently. ({@link Scan#leftover} — the caller lists them as retryable.)
 *   12 · no file type aborts the scan. Anything unusable becomes a named problem and the rest survives.
 *   13 · a MOVING gif/webp is a problem, not an import — animated folder-import is parked for its own
 *        session (this server has crashed on GIFs before).
 *   15 · the folder is dedicated and is created, with its done/ child, on first run.
 *   17 · a ZIP dropped in the folder is unpacked in place and its images treated as loose files.
 *
 * Heavy work (reading bytes, probing a picture) is safe off the server thread — it only reads SlotManager.
 *
 * Depends on: CbPaths, SlotManager, SlotPools, NameCase, ImageProcessor, AnimationDecoder, ImportEntry
 * Called by:  ImportFolderCommands (preview), ImportService (re-scan before a commit)
 */
package com.customblocks.core;

import com.customblocks.image.AnimationDecoder;
import com.customblocks.image.ImageProcessor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ImportScan {

    private ImportScan() {} // static-only

    /** Still-image types the run accepts. Kept in step with the old mod's list, which was proven in use. */
    private static final Set<String> IMAGE_EXT = Set.of(
            "png", "jpg", "jpeg", "gif", "bmp", "webp", "tiff", "tif");

    /** Per-file byte cap. A picture past this is refused by name rather than allowed to exhaust memory. */
    public static final long MAX_FILE_BYTES = 8L * 1024 * 1024;
    /** Total bytes one dropped ZIP may expand to — the zip-bomb ceiling. */
    private static final long MAX_UNZIP_BYTES = 256L * 1024 * 1024;
    /** Entry count ceiling for one dropped ZIP, so a million tiny entries can't stall the scan either. */
    private static final int MAX_UNZIP_ENTRIES = 2_000;

    /**
     * The outcome of one scan.
     *
     * @param ready     entries that would be created, already trimmed to the free-slot count.
     * @param leftover  ready entries that did NOT fit — retryable after slots are freed (rule 7).
     * @param problems  files that cannot be imported as they are, each with its reason (rules 3, 4, 12).
     * @param freeSlots how many slots exist right now.
     * @param unpacked  how many files came out of dropped ZIPs (rule 17), for the preview line.
     * @param created   true when this run had to create the folder itself (rule 15 → first-run message).
     */
    public record Scan(List<ImportEntry> ready, List<ImportEntry> leftover, List<ImportEntry> problems,
                       int freeSlots, int unpacked, boolean created) {

        /** True when there is nothing at all to look at — no ready file, no leftover, no problem. */
        public boolean empty() {
            return ready.isEmpty() && leftover.isEmpty() && problems.isEmpty();
        }

        /** Slots still free once this run's ready entries have taken theirs (B2). */
        public int slotsLeftAfter() {
            return Math.max(0, freeSlots - ready.size());
        }
    }

    /** Make sure the import folder and its done/ child exist. Returns true if we had to create them. */
    public static boolean ensureFolder() throws IOException {
        boolean fresh = !Files.isDirectory(CbPaths.IMPORT);
        Files.createDirectories(CbPaths.IMPORT_DONE); // creates IMPORT on the way through
        return fresh;
    }

    /**
     * Walk the import folder and describe the run. Never throws for a bad file — a file that cannot be
     * read becomes a problem entry and the scan carries on (rule 12).
     */
    public static Scan scan() {
        boolean created;
        try {
            created = ensureFolder();
        } catch (IOException e) {
            return new Scan(List.of(), List.of(), List.of(), 0, 0, false);
        }

        int unpacked = unpackZips();

        // Pair a picture with a same-name .json (B17) by file-name stem, keeping folder order stable.
        Map<String, Path> images = new LinkedHashMap<>();
        Map<String, Path> data = new LinkedHashMap<>();
        List<Path> unusable = new ArrayList<>();
        collect(images, data, unusable);

        List<ImportEntry> ready = new ArrayList<>();
        List<ImportEntry> problems = new ArrayList<>();
        // Ids claimed earlier in THIS run, so two files that reduce to the same id clash with each other
        // instead of the second one silently failing at creation time.
        Set<String> claimed = new HashSet<>();

        for (Path p : unusable) {
            problems.add(new ImportEntry(p, null, safeId(stem(p)), displayName(stem(p)),
                    "not a picture the mod can read", false));
        }

        Set<String> stems = new java.util.LinkedHashSet<>(images.keySet());
        stems.addAll(data.keySet());
        for (String stem : stems) {
            Path image = images.get(stem);
            Path meta = data.get(stem);
            String id = safeId(stem);
            String name = displayName(stem);
            ImportEntry e = new ImportEntry(image, meta, id, name, null, false);

            String reason = check(e, id, claimed);
            if (reason != null) { problems.add(e.withProblem(reason)); continue; }
            claimed.add(id);
            ready.add(new ImportEntry(image, meta, id, name, null, isMoving(image)));
        }

        // Rule 7: fill what fits, list the rest as retryable — never truncate in silence.
        int free = SlotPools.freeCount(SlotManager.getMaxSlots(), i -> SlotManager.getBySlot("slot_" + i) != null);
        List<ImportEntry> leftover = new ArrayList<>();
        if (ready.size() > free) {
            leftover.addAll(ready.subList(Math.max(0, free), ready.size()));
            ready = new ArrayList<>(ready.subList(0, Math.max(0, free)));
        }
        problems.sort(Comparator.comparing(ImportEntry::fileName));
        return new Scan(ready, leftover, problems, free, unpacked, created);
    }

    /**
     * Why {@code e} cannot be imported as it stands, or null when it is fine.
     *
     * Order matters: the cheapest checks first, and the byte read last, so a folder of huge junk is not
     * decoded just to learn its name was unusable anyway.
     */
    private static String check(ImportEntry e, String id, Set<String> claimed) {
        if (id.isEmpty())                 return "no usable name could be made from that file name";
        if (SlotManager.hasId(id))        return "a block called \"" + id + "\" already exists";
        if (claimed.contains(id))         return "another file in this folder already claims the id \"" + id + "\"";
        if (e.image() == null && e.data() == null) return "nothing to read";

        if (e.image() != null) {
            long size;
            try { size = Files.size(e.image()); } catch (IOException io) { return "the file could not be read"; }
            if (size <= 0)                return "the file is empty";
            if (size > MAX_FILE_BYTES)    return "the picture is bigger than 8 MB";
            byte[] raw = read(e.image());
            if (raw == null)              return "the file could not be read";
            if (AnimationDecoder.isAnimated(raw))
                return "moving pictures are not supported yet — still images only for now";
            if (ImageProcessor.dimensions(raw) == null)
                return "not a picture the mod can read";
        }
        return null;
    }

    /** True when the picture actually moves (several frames), so the caller can label it in a report. */
    private static boolean isMoving(Path image) {
        if (image == null) return false;
        byte[] raw = read(image);
        return raw != null && AnimationDecoder.isAnimated(raw);
    }

    /** Read a file's bytes, or null when that fails for any reason. Never throws. */
    public static byte[] read(Path p) {
        try {
            return Files.readAllBytes(p);
        } catch (Exception e) {
            return null;
        }
    }

    /** Sort every loose file in the folder into pictures, block data files, and the unusable rest. */
    private static void collect(Map<String, Path> images, Map<String, Path> data, List<Path> unusable) {
        List<Path> files = new ArrayList<>();
        try (var stream = Files.list(CbPaths.IMPORT)) {
            stream.filter(Files::isRegularFile).forEach(files::add);
        } catch (IOException e) {
            return;
        }
        files.sort(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)));
        for (Path p : files) {
            String ext = ext(p);
            if (IMAGE_EXT.contains(ext))      images.putIfAbsent(stem(p), p);
            else if ("json".equals(ext))      data.putIfAbsent(stem(p), p);
            else if ("zip".equals(ext))       continue;   // already handled by unpackZips
            else if (ext.equals("tmp"))       continue;   // our own scratch files are not the owner's drop
            else                              unusable.add(p);
        }
    }

    /**
     * Unpack every ZIP sitting in the import folder in place, then move the ZIP itself into done/ so a
     * re-run does not expand it twice (rule 17 + rule 8). Entry names are reduced to their file name, so
     * a crafted path inside the archive cannot write outside the folder (zip-slip). Junk inside the ZIP
     * is simply not extracted, which makes it a skipped file like any other. Returns how many files came
     * out. Never throws — a broken archive is reported by the files it failed to produce.
     */
    private static int unpackZips() {
        List<Path> zips = new ArrayList<>();
        try (var stream = Files.list(CbPaths.IMPORT)) {
            stream.filter(Files::isRegularFile).filter(p -> "zip".equals(ext(p))).forEach(zips::add);
        } catch (IOException e) {
            return 0;
        }
        int out = 0;
        for (Path zip : zips) {
            long budget = MAX_UNZIP_BYTES;
            int entries = 0;
            try (ZipInputStream zin = new ZipInputStream(Files.newInputStream(zip))) {
                ZipEntry entry;
                while ((entry = zin.getNextEntry()) != null) {
                    if (++entries > MAX_UNZIP_ENTRIES) break;
                    if (entry.isDirectory()) continue;
                    String name = Path.of(entry.getName()).getFileName().toString(); // zip-slip safe
                    String ext = ext(name);
                    if (!IMAGE_EXT.contains(ext) && !"json".equals(ext)) continue;   // junk: skipped
                    Path target = freeName(CbPaths.IMPORT, name);
                    long written = copyCapped(zin, target, Math.min(budget, MAX_FILE_BYTES));
                    if (written < 0) { try { Files.deleteIfExists(target); } catch (IOException ignored) {} continue; }
                    budget -= written;
                    out++;
                    if (budget <= 0) break;
                }
            } catch (Exception e) {
                // A corrupt archive stops at the entry that broke; whatever came out already still imports.
            }
            move(zip, CbPaths.IMPORT_DONE);
        }
        return out;
    }

    /** Copy at most {@code cap} bytes to {@code target}; returns bytes written, or -1 if the cap was hit. */
    private static long copyCapped(InputStream in, Path target, long cap) {
        long total = 0;
        byte[] buf = new byte[8192];
        try (var os = Files.newOutputStream(target)) {
            int n;
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total > cap) return -1;
                os.write(buf, 0, n);
            }
        } catch (IOException e) {
            return -1;
        }
        return total;
    }

    /**
     * Move a source file into {@code dir}, never overwriting: a name already there gains a {@code -2},
     * {@code -3}… suffix. Rule 8 — an original image is moved, never destroyed. Best-effort; a failed
     * move leaves the file where it is, which at worst means the next run sees it again.
     */
    public static boolean move(Path file, Path dir) {
        try {
            Files.createDirectories(dir);
            Files.move(file, freeName(dir, file.getFileName().toString()), StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (Exception atomicFailed) {
            try {
                Files.move(file, freeName(dir, file.getFileName().toString()));
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    /** A path in {@code dir} named {@code name}, suffixed until it does not already exist. */
    private static Path freeName(Path dir, String name) {
        Path candidate = dir.resolve(name);
        if (!Files.exists(candidate)) return candidate;
        String base = name, ext = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0) { base = name.substring(0, dot); ext = name.substring(dot); }
        for (int i = 2; i < 10_000; i++) {
            candidate = dir.resolve(base + "-" + i + ext);
            if (!Files.exists(candidate)) return candidate;
        }
        return dir.resolve(base + "-" + System.currentTimeMillis() + ext);
    }

    // ── file name → id + display name (rule 5: the /cb create rules, nothing new) ─────────

    /**
     * The block id a file name would claim: lower-case, with every character that is not
     * {@code a-z 0-9 _ -} folded to an underscore, runs collapsed and the ends trimmed. Same safe-handle
     * shape {@code /cb create} accepts, so an imported block is indistinguishable from a typed one.
     */
    public static String safeId(String stem) {
        if (stem == null) return "";
        String s = stem.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
        s = s.replaceAll("_{2,}", "_").replaceAll("^[_-]+", "").replaceAll("[_-]+$", "");
        return s;
    }

    /**
     * The display name a file name would get: underscores read as spaces and a capital on EVERY word,
     * through the same {@link NameCase#titleCase} SlotManager.create applies — so {@code red_brick.png}
     * becomes {@code Red Brick} and {@code stone wall.PNG} becomes {@code Stone Wall} (B5).
     */
    public static String displayName(String stem) {
        return NameCase.titleCase(stem == null ? "" : stem.trim());
    }

    /** File name without its extension. */
    private static String stem(Path p) {
        String n = p.getFileName().toString();
        int dot = n.lastIndexOf('.');
        return dot > 0 ? n.substring(0, dot) : n;
    }

    private static String ext(Path p) {
        return ext(p.getFileName().toString());
    }

    private static String ext(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
