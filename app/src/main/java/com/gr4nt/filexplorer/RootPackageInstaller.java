package com.gr4nt.filexplorer;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Root-only package installation; this does not grant general filesystem access. */
public final class RootPackageInstaller {
    private static final long INSTALL_TIMEOUT_MINUTES = 3;
    private static final int MAX_OUTPUT_BYTES = 64 * 1024;

    private RootPackageInstaller() {}

    static String commandFor(List<File> apks) {
        if (apks == null || apks.isEmpty()) {
            throw new IllegalArgumentException("No APK files were selected.");
        }
        StringBuilder command = new StringBuilder(
                apks.size() == 1 ? "pm install -r" : "pm install-multiple -r");
        for (File apk : apks) {
            if (apk == null || !apk.getName().toLowerCase(Locale.ROOT).endsWith(".apk")) {
                throw new IllegalArgumentException("The package set contains an invalid APK file.");
            }
            command.append(' ').append(shellQuote(apk.getAbsolutePath()));
        }
        return command.toString();
    }

    static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    public static String install(List<File> apks, File logDirectory,
                                 AtomicBoolean cancelled) throws IOException {
        if (apks == null || apks.isEmpty()) {
            throw new IOException("No APK files were selected.");
        }
        for (File apk : apks) {
            if (apk == null || !apk.isFile()
                    || !apk.getName().toLowerCase(Locale.ROOT).endsWith(".apk")) {
                throw new IOException("The staged package set contains an invalid APK file.");
            }
        }
        if (cancelled != null && cancelled.get()) throw new InterruptedIOException("Cancelled");
        if (logDirectory == null || !logDirectory.isDirectory()) {
            throw new IOException("The package staging folder is unavailable.");
        }

        File output = new File(logDirectory, "root-install.log");
        Process process;
        try {
            process = new ProcessBuilder("su", "-c", commandFor(apks))
                    .redirectErrorStream(true)
                    .redirectOutput(output)
                    .start();
        } catch (IOException error) {
            throw new IOException(
                    "Root installation requires a rooted device and an available su manager.",
                    error);
        }

        long deadline = System.nanoTime()
                + TimeUnit.MINUTES.toNanos(INSTALL_TIMEOUT_MINUTES);
        try {
            while (!process.waitFor(200, TimeUnit.MILLISECONDS)) {
                if (cancelled != null && cancelled.get()) {
                    stop(process);
                    throw new InterruptedIOException("Root installation cancelled.");
                }
                if (System.nanoTime() >= deadline) {
                    stop(process);
                    throw new IOException("Root installation timed out.");
                }
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            stop(process);
            throw new InterruptedIOException("Root installation interrupted.");
        }

        String result = readOutput(output);
        if (process.exitValue() != 0 || !result.contains("Success")) {
            String detail = result.trim();
            if (detail.isEmpty()) detail = "The root package manager returned no details.";
            throw new IOException("Root installation failed: " + detail);
        }
        return result.trim();
    }

    private static void stop(Process process) {
        process.destroy();
        try {
            if (!process.waitFor(2, TimeUnit.SECONDS)) process.destroyForcibly();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    private static String readOutput(File file) throws IOException {
        if (!file.isFile()) return "";
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        try (InputStream input = Files.newInputStream(file.toPath())) {
            int read;
            while (output.size() < MAX_OUTPUT_BYTES
                    && (read = input.read(buffer, 0,
                    Math.min(buffer.length, MAX_OUTPUT_BYTES - output.size()))) != -1) {
                output.write(buffer, 0, read);
            }
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
}
