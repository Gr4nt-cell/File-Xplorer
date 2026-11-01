package com.gr4nt.filexplorer;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.os.Handler;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

/** Uses Android's user-confirmed package installer for APKs and split-APK sets. */
public final class PackageInstallFlow {
    public interface Listener {
        void onSubmitted();
        void onFinished(boolean success, String message);
    }

    private PackageInstallFlow() {}

    public static void install(Activity activity, List<File> apks, String packageName,
                               Handler main, Listener listener) throws IOException {
        if (apks == null || apks.isEmpty()) throw new IOException("No APK files were selected.");
        PackageInstaller installer = activity.getPackageManager().getPackageInstaller();
        long total = 0;
        for (File apk : apks) {
            if (!apk.isFile() || !apk.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".apk")) {
                throw new IOException("The package set contains an invalid APK file.");
            }
            if (apk.length() > Long.MAX_VALUE - total) throw new IOException("Package size is too large.");
            total += apk.length();
        }
        PackageInstaller.SessionParams params =
                new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setSize(total);
        if (Build.VERSION.SDK_INT >= 29 && packageName != null) params.setAppPackageName(packageName);
        int sessionId = installer.createSession(params);
        String action = activity.getPackageName() + ".INSTALL_STATUS." + UUID.randomUUID();
        boolean[] receiverRegistered = {false};
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS,
                        PackageInstaller.STATUS_FAILURE);
                if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                    Intent confirmation = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                    if (confirmation != null) activity.startActivity(confirmation);
                    return;
                }
                try {
                    activity.unregisterReceiver(this);
                    receiverRegistered[0] = false;
                } catch (IllegalArgumentException ignored) {
                    receiverRegistered[0] = false;
                }
                String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
                boolean success = status == PackageInstaller.STATUS_SUCCESS;
                if (success) message = "Package installed.";
                if (message == null || message.trim().isEmpty()) message = "Android package installer returned status " + status + ".";
                listener.onFinished(success, message);
            }
        };
        IntentFilter filter = new IntentFilter(action);
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                activity.registerReceiver(receiver, filter, null, main, Context.RECEIVER_NOT_EXPORTED);
            } else {
                activity.registerReceiver(receiver, filter, null, main);
            }
            receiverRegistered[0] = true;
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 31) flags |= PendingIntent.FLAG_MUTABLE;
            PendingIntent statusIntent = PendingIntent.getBroadcast(
                    activity, sessionId, new Intent(action).setPackage(activity.getPackageName()), flags);
            try (PackageInstaller.Session session = installer.openSession(sessionId)) {
                for (int i = 0; i < apks.size(); i++) {
                    File apk = apks.get(i);
                    String name = i == 0 ? "base.apk" : apk.getName();
                    if (name.contains("/") || name.contains("\\") || !name.toLowerCase(java.util.Locale.ROOT).endsWith(".apk")) {
                        name = "split_" + i + ".apk";
                    }
                    try (FileInputStream input = new FileInputStream(apk);
                         OutputStream output = session.openWrite(name, 0, apk.length())) {
                        byte[] buffer = new byte[65536];
                        int read;
                        while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
                        session.fsync(output);
                    }
                }
                session.commit(statusIntent.getIntentSender());
            }
            main.post(listener::onSubmitted);
        } catch (Exception error) {
            installer.abandonSession(sessionId);
            if (receiverRegistered[0]) {
                main.post(() -> {
                    try {
                        activity.unregisterReceiver(receiver);
                    } catch (IllegalArgumentException ignored) {
                        // It may already have delivered a terminal status.
                    }
                });
            }
            if (error instanceof IOException) throw (IOException) error;
            throw new IOException("Android could not start the package installation.", error);
        }
    }
}
