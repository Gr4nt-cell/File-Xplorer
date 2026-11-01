package com.gr4nt.filexplorer;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.Environment;
import android.os.IInterface;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.json.JSONArray;
import org.json.JSONObject;

/** Restricted Shizuku user service for the shared Android/data and Android/obb folders. */
public final class ShizukuFileAccessService extends Binder implements IInterface {
    public static final String DESCRIPTOR = "com.gr4nt.filexplorer.IProtectedStorage";
    public static final int SET_ROOTS = IBinder.FIRST_CALL_TRANSACTION;
    public static final int LIST = IBinder.FIRST_CALL_TRANSACTION + 1;
    public static final int OPEN_READ = IBinder.FIRST_CALL_TRANSACTION + 2;
    public static final int OPEN_WRITE = IBinder.FIRST_CALL_TRANSACTION + 3;
    public static final int CREATE = IBinder.FIRST_CALL_TRANSACTION + 4;
    public static final int RENAME = IBinder.FIRST_CALL_TRANSACTION + 5;
    public static final int DELETE = IBinder.FIRST_CALL_TRANSACTION + 6;

    private String dataRoot;
    private String obbRoot;

    public ShizukuFileAccessService() {
        attachInterface(this, DESCRIPTOR);
    }

    @Override public IBinder asBinder() { return this; }

    @Override
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
            throws RemoteException {
        if (code == IBinder.INTERFACE_TRANSACTION) {
            reply.writeString(DESCRIPTOR);
            return true;
        }
        if (code == 16777114 || code == 16777115) {
            reply.writeNoException();
            System.exit(0);
            return true;
        }
        if (code < SET_ROOTS || code > DELETE) return super.onTransact(code, data, reply, flags);
        try {
            data.enforceInterface(DESCRIPTOR);
            switch (code) {
                case SET_ROOTS:
                    setRoots(data.readString(), data.readString());
                    success(reply);
                    return true;
                case LIST:
                    list(data.readString(), reply);
                    return true;
                case OPEN_READ:
                    open(data.readString(), false, reply);
                    return true;
                case OPEN_WRITE:
                    open(data.readString(), true, reply);
                    return true;
                case CREATE:
                    create(data.readString(), data.readString(), data.readInt() != 0);
                    success(reply);
                    return true;
                case RENAME:
                    rename(data.readString(), data.readString());
                    success(reply);
                    return true;
                case DELETE:
                    delete(data.readString());
                    success(reply);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        } catch (Exception error) {
            failure(reply, error.getMessage());
            return true;
        }
    }

    private void setRoots(String requestedData, String requestedObb) throws IOException {
        String external = Environment.getExternalStorageDirectory().getCanonicalPath();
        String android = new File(external, "Android").getCanonicalPath();
        String expectedData = new File(android, "data").getCanonicalPath();
        String expectedObb = new File(android, "obb").getCanonicalPath();
        String data = new File(requestedData).getCanonicalPath();
        String obb = new File(requestedObb).getCanonicalPath();
        if (!data.equals(expectedData) || !obb.equals(expectedObb)) {
            throw new IOException("Shizuku access is limited to this device's Android/data and Android/obb folders.");
        }
        dataRoot = data;
        obbRoot = obb;
    }

    private void list(String path, Parcel reply) throws IOException {
        File folder = allowed(path);
        if (!folder.isDirectory()) throw new IOException("This protected location is not a folder.");
        File[] children = folder.listFiles();
        if (children == null) throw new IOException("Shizuku could not read this folder. Check that its service is running.");
        JSONArray result = new JSONArray();
        for (File child : children) {
            if (Files.isSymbolicLink(child.toPath())) continue;
            JSONObject item = new JSONObject();
            try {
                item.put("name", child.getName());
                item.put("dir", child.isDirectory());
                item.put("size", child.isDirectory() ? 0 : child.length());
                item.put("modified", child.lastModified());
                result.put(item);
            } catch (Exception error) {
                throw new IOException("Could not read a folder entry.", error);
            }
        }
        reply.writeNoException();
        reply.writeInt(0);
        reply.writeString(result.toString());
    }

    private void open(String path, boolean write, Parcel reply) throws IOException {
        File file = allowed(path);
        if (file.isDirectory()) throw new IOException("Select a file, not a folder.");
        int mode = write
                ? ParcelFileDescriptor.MODE_WRITE_ONLY | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_TRUNCATE
                : ParcelFileDescriptor.MODE_READ_ONLY;
        ParcelFileDescriptor descriptor = ParcelFileDescriptor.open(file, mode);
        reply.writeNoException();
        reply.writeInt(0);
        descriptor.writeToParcel(reply, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
        descriptor.close();
    }

    private void create(String parentPath, String name, boolean directory) throws IOException {
        validName(name);
        File parent = allowed(parentPath);
        if (!parent.isDirectory()) throw new IOException("The parent folder is unavailable.");
        File target = allowed(new File(parent, name).getPath());
        boolean created = directory ? target.mkdir() : target.createNewFile();
        if (!created) throw new IOException("Could not create the item. It may already exist.");
    }

    private void rename(String path, String name) throws IOException {
        validName(name);
        File source = allowed(path);
        File parent = source.getParentFile();
        if (parent == null) throw new IOException("Cannot rename a protected storage root.");
        File target = allowed(new File(parent, name).getPath());
        if (target.exists()) throw new IOException("An item with that name already exists.");
        if (!source.renameTo(target)) throw new IOException("Android denied the rename operation.");
    }

    private void delete(String path) throws IOException {
        File file = allowed(path);
        if (file.equals(new File(dataRoot)) || file.equals(new File(obbRoot))) {
            throw new IOException("Protected storage roots cannot be deleted.");
        }
        if (!file.delete()) throw new IOException("Android denied the delete operation.");
    }

    private File allowed(String path) throws IOException {
        if (dataRoot == null || obbRoot == null) throw new IOException("Shizuku storage access is not initialized.");
        File file = new File(path).getCanonicalFile();
        String canonical = file.getPath();
        if (inside(canonical, dataRoot) || inside(canonical, obbRoot)) return file;
        throw new IOException("Shizuku access is limited to Android/data and Android/obb.");
    }

    private static boolean inside(String path, String root) {
        return path.equals(root) || path.startsWith(root + File.separator);
    }

    private static void validName(String name) throws IOException {
        if (name == null || name.trim().isEmpty() || name.equals(".") || name.equals("..")
                || name.indexOf('/') >= 0 || name.indexOf('\\') >= 0 || name.indexOf('\0') >= 0) {
            throw new IOException("Choose a valid single file or folder name.");
        }
    }

    private static void success(Parcel reply) {
        reply.writeNoException();
        reply.writeInt(0);
    }

    private static void failure(Parcel reply, String message) {
        reply.writeNoException();
        reply.writeInt(1);
        reply.writeString(message == null || message.trim().isEmpty()
                ? "Protected folder operation failed." : message);
    }
}
