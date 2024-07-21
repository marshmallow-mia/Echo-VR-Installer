package bl00dy_c0d3_.echovr_installer;

import com.frostwire.jlibtorrent.AlertListener;
import com.frostwire.jlibtorrent.LibTorrent;
import com.frostwire.jlibtorrent.SessionManager;
import com.frostwire.jlibtorrent.SettingsPack;
import com.frostwire.jlibtorrent.TorrentInfo;
import com.frostwire.jlibtorrent.alerts.*;
import com.frostwire.jlibtorrent.swig.settings_pack;

import javax.swing.*;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class TorrentDownload implements Runnable {
    private String torrentFile0;
    private String localFilePath;
    private String filename;
    private JLabel labelProgress;
    private JDialog frame;
    private JFrame frameMain;
    private int platform = -1; // 0=PC, 1=don't unzip
    private boolean flg_CancelDownload = false;
    private SessionManager sessionManager;
    private Downloader downloader;

    public TorrentDownload(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void startDownload(String torrentFile0, String localFilePath, String filename, JLabel labelProgress, JDialog frame, JFrame frameMain, int platform) {
        this.torrentFile0 = torrentFile0;
        this.localFilePath = localFilePath;
        this.filename = filename;
        this.labelProgress = labelProgress;
        this.frame = frame;
        this.frameMain = frameMain;
        this.platform = platform;
        this.flg_CancelDownload = false;
        checkIfTorrentStarted();
        new Thread(this).start();
    }

    @Override
    public void run() {
        File torrentFile = new File(torrentFile0);
        System.out.println("Using libtorrent version: " + LibTorrent.version());

        File file = new File(localFilePath);
        System.out.println(localFilePath);
        // Attempt to create the directory and check the result
        if (!file.exists()) {
            if (file.mkdirs()) {
                System.out.println("Directory created successfully");
            } else {
                System.out.println("Failed to create directory");
                new ErrorDialog().errorDialog(frame, "Restart as Admin", "<html>You tried to download into a path that requires Admin rights<br>or the folder couldn't be created for another reason.<br>Try again or Restart the App in Admin Mode!</html>", -1);
                return; // Exit if the directory could not be created
            }
        } else {
            System.out.println("Directory already exists");
        }

        final CountDownLatch signal = new CountDownLatch(1);

        sessionManager.addListener(new AlertListener() {
            @Override
            public int[] types() {
                return null; // Listen to all alert types
            }

            @Override
            public void alert(Alert<?> alert) {
                AlertType type = alert.type();

                switch (type) {
                    case ADD_TORRENT:
                        System.out.println("Torrent added");
                        labelProgress.setText("Wait!");
                        frame.repaint();
                        ((AddTorrentAlert) alert).handle().resume();
                        break;
                    case BLOCK_FINISHED:
                        BlockFinishedAlert bfa = (BlockFinishedAlert) alert;
                        double progress = bfa.handle().status().progress() * 100;
                        String formattedProgress = String.format("%.2f", progress);
                        if (labelProgress != null) {
                            labelProgress.setText(formattedProgress + "%");
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
                            UnzipFile.unzip(frame, frameMain, localFilePath + "\\" + filename, localFilePath);
                            JOptionPane.showMessageDialog(frame, "<html>Installation is done. Path is:<br>" + localFilePath + "</html>", "Notification", JOptionPane.INFORMATION_MESSAGE);
                        }
                        if (platform == 3) {
                            JOptionPane.showMessageDialog(frame, "<html>Installation is done. Path is:<br>" + localFilePath + "</html>", "Notification", JOptionPane.INFORMATION_MESSAGE);
                        }
                        break;
                    case TORRENT_ERROR:
                        TorrentErrorAlert tea = (TorrentErrorAlert) alert;
                        System.out.println("Torrent error: " + tea.error().message());
                        break;
                    case PEER_LOG:
                        PeerLogAlert pla = (PeerLogAlert) alert;
                        System.out.println("Peer log: " + pla.message());
                        break;
                    case FILE_ERROR:
                        FileErrorAlert fea = (FileErrorAlert) alert;
                        System.out.println("File error: " + fea.error().message());
                        break;
                    case STATE_UPDATE:
                        StateUpdateAlert sua = (StateUpdateAlert) alert;
                        System.out.println("State update: " + sua.status().toString());
                        break;
                    default:
                        System.out.println("Alert: " + alert.toString());
                        break;
                }
            }
        });

        try {
            // Configure session settings to disable local peer discovery
            settings_pack sp = new settings_pack();
            sp.set_bool(settings_pack.bool_types.enable_lsd.swigValue(), false); // Disable local peer discovery
            sp.set_bool(settings_pack.bool_types.enable_dht.swigValue(), false); // Disable DHT
            sp.set_bool(settings_pack.bool_types.enable_upnp.swigValue(), false); // Disable UPnP
            sp.set_bool(settings_pack.bool_types.enable_natpmp.swigValue(), false); // Disable NAT-PMP
            SettingsPack settings = new SettingsPack(sp);
            sessionManager.applySettings(settings);

            TorrentInfo ti = new TorrentInfo(torrentFile);
            sessionManager.download(ti, new File(localFilePath));




            signal.await();

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    public void cancelDownload() {
        if (downloader != null) {
            downloader.cancelDownload();
        }

        flg_CancelDownload = true;
        new Thread(() -> {
            sessionManager.stop(); // Attempt to stop the session
            // Wait for a short period to ensure the session stops
            try {
                Thread.sleep(5000); // Wait for 5 seconds
            } catch (InterruptedException e) {
                // Handle the interruption
                e.printStackTrace();
            }
            // Check if the session is still running and force stop if needed
            // No force stop method available, relying on stop() for now
        }).start();
    }

    public void checkIfTorrentStarted() {

        //Check after 30 sec if the torrent download started at all.
        // ScheduledExecutorService to run tasks after a delay
        ScheduledExecutorService checkIfTorrentWorks     = Executors.newScheduledThreadPool(1);
        // Schedule a task to run after 30 seconds
        checkIfTorrentWorks.schedule(new Runnable() {
            @Override
            public void run() {
                if (labelProgress.getText().equals("Wait!")){
                    cancelDownload();
                    startDownloadWithoutTorrent();
                }
            }
        }, 30, TimeUnit.SECONDS);


    }


    public void startDownloadWithoutTorrent(){
        //Delete File
        File myObj = new File(localFilePath + "/" + filename);
        System.out.println(localFilePath + "/" + filename);
        System.out.println("Fallback to HTTP");

        try {
            if (myObj.delete()) {
                System.out.println("Fallback to HTTP. Deleted the file: " + myObj.getName());
            } else {
                throw new Exception("Unknown error occurred.");
            }
        } catch (Exception e) {
            String error = e.toString();
            System.out.println(error);
        }

        downloader = new Downloader();
        String fixedURL = "https://echo.marceldomain.de:6969/" + filename;
        downloader.startDownload(fixedURL, localFilePath , filename, labelProgress, frame, frameMain, platform);

    }
}
