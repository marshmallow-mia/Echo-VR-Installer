package bl00dy_c0d3_.echovr_installer;

import com.frostwire.jlibtorrent.AlertListener;
import com.frostwire.jlibtorrent.LibTorrent;
import com.frostwire.jlibtorrent.SessionManager;
import com.frostwire.jlibtorrent.TorrentInfo;
import com.frostwire.jlibtorrent.alerts.AddTorrentAlert;
import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.alerts.AlertType;
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert;

import javax.swing.*;
import java.io.File;
import java.util.concurrent.CountDownLatch;

public class TorrentDownload implements Runnable {
    private String torrentFile0;
    private String localFilePath;
    private String filename;
    private JLabel labelProgress;
    private JDialog frame;
    private int platform = -1; // 0=PC, 1=don't unzip
    private boolean flg_CancelDownload = false;
    private SessionManager sessionManager;

    public TorrentDownload(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void startDownload(String torrentFile0, String localFilePath, String filename, JLabel labelProgress, JDialog frame, int platform) {
        this.torrentFile0 = torrentFile0;
        this.localFilePath = localFilePath;
        this.filename = filename;
        this.labelProgress = labelProgress;
        this.frame = frame;
        this.platform = platform;
        this.flg_CancelDownload = false;

        new Thread(this).start();
    }

    @Override
    public void run() {
        File torrentFile = new File(torrentFile0);
        System.out.println("Using libtorrent version: " + LibTorrent.version());

        final CountDownLatch signal = new CountDownLatch(1);

        sessionManager.addListener(new AlertListener() {
            @Override
            public int[] types() {
                return null;
            }

            @Override
            public void alert(Alert<?> alert) {
                AlertType type = alert.type();

                switch (type) {
                    case ADD_TORRENT:
                        System.out.println("Torrent added");
                        ((AddTorrentAlert) alert).handle().resume();
                        break;
                    case BLOCK_FINISHED:
                        BlockFinishedAlert a = (BlockFinishedAlert) alert;
                        double p = a.handle().status().progress() * 100;
                        String formatted_p = String.format("%.2f", p);
                        System.out.println("Progress: " + formatted_p + " for torrent name: " + a.torrentName());
                        if (labelProgress != null) {
                            labelProgress.setText(" " + formatted_p + "%");
                            frame.repaint();
                        }
                        if (flg_CancelDownload) {
                            sessionManager.stop();
                            return;
                        }
                        break;
                    case TORRENT_FINISHED:
                        System.out.println("Torrent finished");
                        if (labelProgress != null) {
                            labelProgress.setText("100.00%");
                            frame.repaint();
                        }
                        signal.countDown();
                        if (platform == 0) {
                            UnzipFile.unzip(frame, localFilePath + "\\" + filename, localFilePath);
                        }
                        break;
                    default:
                        break;
                }
            }
        });

        try {
            TorrentInfo ti = new TorrentInfo(torrentFile);
            sessionManager.download(ti, new File(localFilePath));
            signal.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void cancelDownload() {
        flg_CancelDownload = true;
    }
}
