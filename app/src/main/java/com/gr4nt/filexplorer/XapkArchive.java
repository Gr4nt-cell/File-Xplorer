package com.gr4nt.filexplorer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Extracts only package APKs and expansion files from an XAPK container. */
public final class XapkArchive {
    private static final long MAX_EXTRACTED_BYTES = 4L * 1024L * 1024L * 1024L;
    private static final int MAX_APK_COUNT = 64;
    private static final int MAX_OBB_COUNT = 32;

    public interface Progress {
        void bytes(long count, String name);
    }

    public static final class Payload {
        public final List<File> apks;
        public final List<File> obbs;
        public final long bytes;

        Payload(List<File> apks, List<File> obbs, long bytes) {
            this.apks = Collections.unmodifiableList(apks);
            this.obbs = Collections.unmodifiableList(obbs);
            this.bytes = bytes;
        }
    }

    private XapkArchive() {}

    public static Payload extract(File archive, File destination, AtomicBoolean cancelled,
                                  Progress progress) throws IOException {
        if (!destination.isDirectory() && !destination.mkdirs()) {
            throw new IOException("Could not prepare temporary XAPK storage.");
        }
        List<File> apks = new ArrayList<>();
        List<File> obbs = new ArrayList<>();
        long[] total = {0};
        int apkIndex = 0;
        int obbIndex = 0;
        try (ZipFile zip = new ZipFile(archive)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                String original = entry.getName().replace('\\', '/');
                if (original.startsWith("/") || original.matches("^[A-Za-z]:.*")) {
                    throw new IOException("The XAPK contains an unsafe absolute path.");
                }
                for (String segment : original.split("/")) {
                    if (segment.equals("..")) throw new IOException("The XAPK contains an unsafe path.");
                }
                String lower = original.toLowerCase(Locale.ROOT);
                boolean apk = lower.endsWith(".apk");
                boolean obb = lower.endsWith(".obb");
                if (!apk && !obb) continue;
                if (apk && ++apkIndex > MAX_APK_COUNT) {
                    throw new IOException("This XAPK contains too many split APK files.");
                }
                if (obb && ++obbIndex > MAX_OBB_COUNT) {
                    throw new IOException("This XAPK contains too many expansion files.");
                }
                String leaf = original.substring(original.lastIndexOf('/') + 1)
                        .replaceAll("[^A-Za-z0-9._-]", "_");
                if (leaf.isEmpty() || leaf.equals(".") || leaf.equals("..")) {
                    throw new IOException("The XAPK contains an invalid package file name.");
                }
                File output = new File(destination,
                        String.format(Locale.ROOT, "%03d-%s", apk ? apkIndex : obbIndex, leaf));
                long declared = entry.getSize();
                if (declared > 0 && declared > destination.getUsableSpace() - 16L * 1024L * 1024L) {
                    throw new IOException("There is not enough free storage to stage this XAPK.");
                }
                try (InputStream in = zip.getInputStream(entry);
                     FileOutputStream out = new FileOutputStream(output)) {
                    byte[] buffer = new byte[65536];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        if (cancelled.get()) throw new InterruptedIOException("Cancelled");
                        if (read > MAX_EXTRACTED_BYTES - total[0]) {
                            throw new IOException("The XAPK exceeds the 4 GiB extraction safety limit.");
                        }
                        total[0] += read;
                        out.write(buffer, 0, read);
                        if (progress != null) progress.bytes(read, leaf);
                    }
                } catch (IOException e) {
                    if (output.exists()) output.delete();
                    throw e;
                }
                if (apk) apks.add(output);
                else obbs.add(output);
            }
        }
        if (apks.isEmpty()) throw new IOException("This XAPK does not contain any APK files.");
        apks.sort((a, b) -> {
            boolean aBase = a.getName().toLowerCase(Locale.ROOT).endsWith("base.apk");
            boolean bBase = b.getName().toLowerCase(Locale.ROOT).endsWith("base.apk");
            if (aBase != bBase) return aBase ? -1 : 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });
        return new Payload(apks, obbs, total[0]);
    }
}
