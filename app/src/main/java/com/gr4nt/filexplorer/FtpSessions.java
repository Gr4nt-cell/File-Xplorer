package com.gr4nt.filexplorer;

import android.net.Uri;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

/** In-memory FTP profiles. Passwords are intentionally never persisted. */
public final class FtpSessions {
    private static final class Profile {
        final String host;
        final String username;
        final String password;
        final int port;
        final boolean secure;

        Profile(String host, int port, String username, String password, boolean secure) {
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
            this.secure = secure;
        }
    }

    private final ConcurrentHashMap<String, Profile> profiles = new ConcurrentHashMap<>();

    public String connect(String host, int port, String username, String password, boolean secure)
            throws IOException {
        String cleanHost = host == null ? "" : host.trim();
        String cleanUser = username == null ? "" : username.trim();
        if (cleanHost.isEmpty() || cleanHost.length() > 253 || cleanHost.matches(".*\\s+.*")
                || cleanHost.contains("://")) {
            throw new IOException("Enter a valid FTP server host name or IP address.");
        }
        if (port < 1 || port > 65535) {
            throw new IOException("Port must be between 1 and 65535.");
        }
        if (cleanUser.isEmpty()) {
            throw new IOException("Enter a username, or use anonymous.");
        }
        Profile profile = new Profile(cleanHost, port, cleanUser,
                password == null ? "" : password, secure);
        FTPClient test = open(profile);
        disconnect(test);
        String id = UUID.randomUUID().toString();
        profiles.put(id, profile);
        return id;
    }

    FTPClient open(String nodePath) throws IOException {
        String id = Uri.parse(nodePath).getAuthority();
        Profile profile = profiles.get(id);
        if (profile == null) {
            throw new IOException("This FTP session has expired. Connect to the server again; passwords are not saved.");
        }
        return open(profile);
    }

    private FTPClient open(Profile profile) throws IOException {
        FTPClient client = profile.secure ? new FTPSClient("TLS", false) : new FTPClient();
        client.setConnectTimeout(10000);
        client.setDefaultTimeout(12000);
        client.setDataTimeout(30000);
        client.setControlEncoding("UTF-8");
        if (client instanceof FTPSClient) {
            ((FTPSClient) client).setEndpointCheckingEnabled(true);
        }
        try {
            client.connect(profile.host, profile.port);
            if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
                throw new IOException("FTP server rejected the connection: " + client.getReplyString());
            }
            if (!client.login(profile.username, profile.password)) {
                throw new IOException("FTP login failed: " + client.getReplyString());
            }
            client.enterLocalPassiveMode();
            client.setFileType(FTP.BINARY_FILE_TYPE);
            if (client instanceof FTPSClient) {
                FTPSClient secure = (FTPSClient) client;
                secure.execPBSZ(0);
                secure.execPROT("P");
            }
            return client;
        } catch (IOException | RuntimeException error) {
            disconnect(client);
            if (error instanceof IOException) throw (IOException) error;
            throw new IOException("Could not establish the FTP connection.", error);
        }
    }

    public void disconnect(String id) {
        profiles.remove(id);
    }

    static String nodePath(String id, String remotePath) {
        String path = remotePath == null || remotePath.isEmpty() ? "/" : remotePath;
        if (!path.startsWith("/")) path = "/" + path;
        return "ftpfs://" + id + Uri.encode(path, "/");
    }

    static String sessionId(String nodePath) {
        return Uri.parse(nodePath).getAuthority();
    }

    static String remotePath(String nodePath) {
        String path = Uri.parse(nodePath).getPath();
        if (path == null || path.isEmpty()) return "/";
        return path.startsWith("/") ? path : "/" + path;
    }

    static String childPath(String parent, String name) {
        String base = parent == null || parent.isEmpty() ? "/" : parent;
        if (!base.startsWith("/")) base = "/" + base;
        return base.equals("/") ? base + name : base + "/" + name;
    }

    static void disconnect(FTPClient client) {
        if (client == null) return;
        try {
            if (client.isConnected()) {
                try {
                    client.logout();
                } catch (IOException ignored) {
                    // The data command may already have closed the control connection.
                }
                client.disconnect();
            }
        } catch (IOException ignored) {
            // Best-effort cleanup after a completed or failed transfer.
        }
    }
}
