package bl00dy_c0d3_.echovr_installer;

import com.frostwire.jlibtorrent.*;

import java.io.File;

public class TorrentDownloader {

    public static void main(String[] args) {
        SessionManager sessionManager = new SessionManager();
        sessionManager.start();

        String torrentUrl = "magnet:?xt=urn:btih:...";
        File saveDir = new File("/path/to/download/directory");

        // Add torrent and get the TorrentHandle
        //TorrentHandle handle = sessionManager.addTorrent(torrentUrl, saveDir);
        sessionManager.download(torrentUrl, saveDir);


        System.out.println("Download completed!");
        sessionManager.stop();
    }
}