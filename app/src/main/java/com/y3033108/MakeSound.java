package com.y3033108;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class MakeSound {
    MakeSound(){

    }

    public void playSound(){
        // sine wave
        AudioTrack track = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                44100,
                AudioFormat.CHANNEL_CONFIGURATION_DEFAULT,
                AudioFormat.ENCODING_DEFAULT,
                44100,
                AudioTrack.MODE_STATIC);
        byte[] sinWave = new byte[44100];

        double freq_c3 = 261.6256;
        double freq_e3 = 329.6276;
        double freq_g3 = 391.9954;
        double t = 0.0;
        double dt = 1.0 / 44100;

        for (int i = 0; i < sinWave.length; i++, t += dt) {
            double sum = Math.sin(2.0 * Math.PI * t * freq_c3)
                    + Math.sin(2.0 * Math.PI * t * freq_e3)
                    + Math.sin(2.0 * Math.PI * t * freq_g3);
            sinWave[i] = (byte) (Byte.MAX_VALUE * (sum / 3));
        }
        track.write(sinWave, 0, sinWave.length);
        track.play();
    }
}
