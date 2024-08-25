package bl00dy_c0d3_.echovr_installer;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class PlayMusic {
    private static Clip clip;

    public static void playMusic(String filePath) {
        try {
            // Open an audio input stream.
            File musicPath = new File(filePath);

            if (musicPath.exists()) {
                AudioInputStream audioInput = AudioSystem.getAudioInputStream(musicPath);
                clip = AudioSystem.getClip();
                clip.open(audioInput);

                // Play the audio clip in a loop
                clip.loop(Clip.LOOP_CONTINUOUSLY);

                // Start playing the clip
                clip.start();
            } else {
                System.out.println("Can't find the file");
            }
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public static void stopMusic() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }



}
