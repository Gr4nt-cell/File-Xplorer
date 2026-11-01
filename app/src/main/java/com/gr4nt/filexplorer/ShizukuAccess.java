package com.gr4nt.filexplorer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import rikka.shizuku.Shizuku;

/** User-approved Shizuku bridge, scoped to Android/data and Android/obb. */
public final class ShizukuAccess {
    public interface Listener {
        void onReady();
        void onError(String message);
    }

    private static final int REQUEST_CODE = 7814;
    private final Activity activity;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final String dataRoot;
    private final String obbRoot;
    private final Shizuku.UserServiceArgs args;
    private IBinder binder;
    private Listener pending;
    private boolean binding;

    private final Shizuku.OnRequestPermissionResultListener permissionListener =
            (requestCode, grantResult) -> {
                if (requestCode != REQUEST_CODE) return;
                Listener listener = pending;
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    bind(listener);
                } else {
                    pending = null;
                    if (listener != null) listener.onError("Shizuku permission was not granted.");
                }
            };

    private final Shizuku.OnBinderDeadListener binderDeadListener = () -> {
        binder = null;
        binding = false;
        Listener listener = pending;
        pending = null;
        if (listener != null) listener.onError("The Shizuku service stopped. Start it again and retry.");
    };

    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder service) {
            binder = service;
            binding = false;
            try {
                setRoots();
                Listener listener = pending;
                pending = null;
                if (listener != null) listener.onReady();
            } catch (IOException error) {
                binder = null;
                Listener listener = pending;
                pending = null;
                if (listener != null) listener.onError(error.getMessage());
            }
        }

        @Override public void onServiceDisconnected(ComponentName name) {
            binder = null;
            binding = false;
        }
    };

    public ShizukuAccess(Activity activity) {
        this.activity = activity;
        String external = Environment.getExternalStorageDirectory().getAbsolutePath();
        dataRoot = new File(external, "Android/data").getAbsolutePath();
        obbRoot = new File(external, "Android/obb").getAbsolutePath();
        args = new Shizuku.UserServiceArgs(
                new ComponentName(activity, ShizukuFileAccessService.class))
                .daemon(false).processNameSuffix("protected-files").version(1);
        Shizuku.addRequestPermissionResultListener(permissionListener, main);
        Shizuku.addBinderDeadListener(binderDeadListener, main);
    }

    public boolean ready() {
        return binder != null && binder.isBinderAlive();
    }

    public void request(Listener listener) {
        if (ready()) {
            listener.onReady();
            return;
        }
        pending = listener;
        try {
            if (!Shizuku.pingBinder()) {
                pending = null;
                listener.onError("Install and start Shizuku, then grant File Xplorer permission.");
                return;
            }
            if (Shizuku.isPreV11()) {
                pending = null;
                listener.onError("This Shizuku version is too old. Update Shizuku and try again.");
                return;
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                bind(listener);
            } else {
                Shizuku.requestPermission(REQUEST_CODE);
            }
        } catch (RuntimeException error) {
            pending = null;
            listener.onError("Could not contact Shizuku: " + error.getMessage());
        }
    }

    private void bind(Listener listener) {
        pending = listener;
        if (binding) return;
        binding = true;
        try {
            Shizuku.bindUserService(args, connection);
        } catch (RuntimeException error) {
            binding = false;
            pending = null;
            if (listener != null) listener.onError("Could not start the Shizuku file service: " + error.getMessage());
        }
    }

    private void setRoots() throws IOException {
        Parcel data = Parcel.obtain();
        try {
            data.writeInterfaceToken(ShizukuFileAccessService.DESCRIPTOR);
            data.writeString(dataRoot);
            data.writeString(obbRoot);
            Parcel reply = transact(ShizukuFileAccessService.SET_ROOTS, data);
            reply.recycle();
        } finally {
            data.recycle();
        }
    }

    public List<Storage.Node> list(String path) throws IOException {
        Parcel data = Parcel.obtain();
        try {
            data.writeInterfaceToken(ShizukuFileAccessService.DESCRIPTOR);
            data.writeString(path);
            Parcel reply = transact(ShizukuFileAccessService.LIST, data);
            try {
                JSONArray values = new JSONArray(reply.readString());
                List<Storage.Node> result = new ArrayList<>();
                for (int i = 0; i < values.length(); i++) {
                    JSONObject item = values.getJSONObject(i);
                    result.add(new Storage.Node(pathFor(path, item.getString("name")),
                            item.getString("name"), item.getBoolean("dir"), item.optLong("size"),
                            item.optLong("modified"), false, true));
                }
                return result;
            } catch (Exception error) {
                if (error instanceof IOException) throw (IOException) error;
                throw new IOException("Could not read the Shizuku folder listing.", error);
            } finally {
                reply.recycle();
            }
        } finally {
            data.recycle();
        }
    }

    public ParcelFileDescriptor open(String path, boolean write) throws IOException {
        Parcel data = Parcel.obtain();
        try {
            data.writeInterfaceToken(ShizukuFileAccessService.DESCRIPTOR);
            data.writeString(path);
            Parcel reply = transact(write ? ShizukuFileAccessService.OPEN_WRITE
                    : ShizukuFileAccessService.OPEN_READ, data);
            try {
                return ParcelFileDescriptor.CREATOR.createFromParcel(reply);
            } finally {
                reply.recycle();
            }
        } finally {
            data.recycle();
        }
    }

    public void create(String parent, String name, boolean directory) throws IOException {
        Parcel data = Parcel.obtain();
        try {
            data.writeInterfaceToken(ShizukuFileAccessService.DESCRIPTOR);
            data.writeString(parent);
            data.writeString(name);
            data.writeInt(directory ? 1 : 0);
            Parcel reply = transact(ShizukuFileAccessService.CREATE, data);
            reply.recycle();
        } finally {
            data.recycle();
        }
    }

    public void rename(String path, String name) throws IOException {
        Parcel data = Parcel.obtain();
        try {
            data.writeInterfaceToken(ShizukuFileAccessService.DESCRIPTOR);
            data.writeString(path);
            data.writeString(name);
            Parcel reply = transact(ShizukuFileAccessService.RENAME, data);
            reply.recycle();
        } finally {
            data.recycle();
        }
    }

    public void delete(String path) throws IOException {
        Parcel data = Parcel.obtain();
        try {
            data.writeInterfaceToken(ShizukuFileAccessService.DESCRIPTOR);
            data.writeString(path);
            Parcel reply = transact(ShizukuFileAccessService.DELETE, data);
            reply.recycle();
        } finally {
            data.recycle();
        }
    }

    private Parcel transact(int code, Parcel data) throws IOException {
        Parcel reply = Parcel.obtain();
        try {
            IBinder remote = binder;
            if (remote == null || !remote.isBinderAlive()) {
                throw new IOException("Shizuku is disconnected. Reopen the protected folder and grant access again.");
            }
            if (!remote.transact(code, data, reply, 0)) {
                throw new IOException("Shizuku did not handle the protected-folder operation.");
            }
            reply.readException();
            if (reply.readInt() != 0) {
                String message = reply.readString();
                throw new IOException(message == null ? "Shizuku operation failed." : message);
            }
            return reply;
        } catch (RemoteException error) {
            reply.recycle();
            throw new IOException("Shizuku could not complete the file operation.", error);
        } catch (IOException | RuntimeException error) {
            reply.recycle();
            if (error instanceof IOException) throw (IOException) error;
            throw new IOException("Shizuku file access failed.", error);
        }
    }

    private static String pathFor(String parent, String name) {
        if (parent.endsWith("/")) return parent + name;
        return parent + "/" + name;
    }

    public void close() {
        Shizuku.removeRequestPermissionResultListener(permissionListener);
        Shizuku.removeBinderDeadListener(binderDeadListener);
        if (binding || binder != null) {
            try {
                Shizuku.unbindUserService(args, connection, true);
            } catch (RuntimeException ignored) {
                // Shizuku may already be stopped.
            }
        }
        binder = null;
        binding = false;
        pending = null;
    }
}
