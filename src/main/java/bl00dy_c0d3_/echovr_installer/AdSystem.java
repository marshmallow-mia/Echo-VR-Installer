package bl00dy_c0d3_.echovr_installer;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class AdSystem {
    private static final String[] AD_IMAGES = {
        "bread.jpg", "butter.jpg", "coffee.jpg", "lard.jpg", "meth.jpg",
        "nokia.jpg", "ok.jpg", "owning_florida.jpg", "weenut_butter.jpg", "youtube.jpg"
    };
    
    private JFrame parentFrame;
    private JDialog adDialog;
    private Random random;
    private Clip musicClip;
    private Timer skipTimer;
    private Timer autoCloseTimer;
    private boolean canSkip = false;
    private Runnable afterAdCallback;
    
    public AdSystem(JFrame parentFrame) {
        this.parentFrame = parentFrame;
        this.random = new Random();
    }
    
    private ImageIcon loadImageIcon(String imageName) {
        try {
            java.net.URL imageURL = getClass().getClassLoader().getResource(imageName);
            if (imageURL != null) {
                return new ImageIcon(imageURL);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private void playAdMusic() {
        try {
            InputStream audioSrc = getClass().getClassLoader().getResourceAsStream("ad_music.wav");
            if (audioSrc == null) {
                audioSrc = getClass().getClassLoader().getResourceAsStream("ad_music.mp3");
            }
            if (audioSrc != null) {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioSrc);
                AudioFormat baseFormat = audioStream.getFormat();
                AudioFormat decodedFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false
                );
                AudioInputStream decodedStream = AudioSystem.getAudioInputStream(decodedFormat, audioStream);
                musicClip = AudioSystem.getClip();
                musicClip.open(decodedStream);
                musicClip.loop(Clip.LOOP_CONTINUOUSLY);
                musicClip.start();
                System.out.println("Ad music started playing");
            } else {
                System.out.println("Ad music file not found");
            }
        } catch (Exception e) {
            System.out.println("Error playing ad music: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void stopAdMusic() {
        if (musicClip != null) {
            if (musicClip.isRunning()) {
                musicClip.stop();
            }
            musicClip.close();
            musicClip = null;
            System.out.println("Ad music stopped");
        }
    }
    
    private void closeAd() {
        if (skipTimer != null) {
            skipTimer.cancel();
        }
        if (autoCloseTimer != null) {
            autoCloseTimer.cancel();
        }
        stopAdMusic();
        if (adDialog != null) {
            adDialog.dispose();
            adDialog = null;
        }
        if (afterAdCallback != null) {
            SwingUtilities.invokeLater(afterAdCallback);
            afterAdCallback = null;
        }
    }
    
    public void showAd() {
        showAdInternal(parentFrame, null);
    }
    
    // These few down here are needed because it spawns child processes for certain buttons, which ads don't play on.
    public void showAdThenExecute(Runnable callback) {
        showAdInternal(parentFrame, callback);
    }
    
    public void showAdOnChildWindow(Window childWindow, Runnable callback) {
        showAdInternal(childWindow, callback);
    }
    
    private void showAdInternal(Window owner, Runnable callback) {
        if (adDialog != null && adDialog.isVisible()) {
            return;
        }
        
        this.afterAdCallback = callback;
        
        String adImage = AD_IMAGES[random.nextInt(AD_IMAGES.length)];
        canSkip = false;
        
        adDialog = new JDialog(owner, "Advertisement", Dialog.ModalityType.APPLICATION_MODAL);
        adDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        adDialog.setUndecorated(true);
        adDialog.setSize(800, 600);
        adDialog.setLocationRelativeTo(owner);
        adDialog.setAlwaysOnTop(true);
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(null);
        mainPanel.setBackground(Color.BLACK);
        
        ImageIcon adIcon = loadImageIcon(adImage);
        if (adIcon != null) {
            Image scaledImage = adIcon.getImage().getScaledInstance(800, 560, Image.SCALE_SMOOTH);
            JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
            imageLabel.setBounds(0, 0, 800, 560);
            mainPanel.add(imageLabel);
        }
        
        JButton skipButton = new JButton("X");
        skipButton.setBounds(760, 10, 30, 30);
        skipButton.setBackground(Color.RED);
        skipButton.setForeground(Color.WHITE);
        skipButton.setEnabled(false);
        skipButton.setFont(new Font("Arial", Font.BOLD, 16));
        skipButton.addActionListener(e -> closeAd());
        mainPanel.add(skipButton);
        
        JLabel timerLabel = new JLabel("Skip in 5...");
        timerLabel.setBounds(650, 560, 140, 30);
        timerLabel.setForeground(Color.WHITE);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 14));
        mainPanel.add(timerLabel);
        
        adDialog.setContentPane(mainPanel);
        
        playAdMusic();
        
        skipTimer = new Timer();
        skipTimer.schedule(new TimerTask() {
            int countdown = 5;
            @Override
            public void run() {
                countdown--;
                if (countdown > 0) {
                    SwingUtilities.invokeLater(() -> timerLabel.setText("Skip in " + countdown + "..."));
                } else {
                    SwingUtilities.invokeLater(() -> {
                        timerLabel.setText("Skip available!");
                        skipButton.setEnabled(true);
                        canSkip = true;
                    });
                    skipTimer.cancel();
                }
            }
        }, 1000, 1000);
        
        autoCloseTimer = new Timer();
        autoCloseTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> closeAd());
            }
        }, 60000);
        
        new Thread(() -> adDialog.setVisible(true)).start();
    }
}
