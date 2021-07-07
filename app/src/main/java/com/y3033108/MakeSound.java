package com.y3033108;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class MakeSound extends Thread{
    AudioTrack track;
    byte[] sinWave;
    boolean isPlay = true;
    MakeSound(){

    }

    public void playSound(){
        // sine wave
        track = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                44100,
                AudioFormat.CHANNEL_CONFIGURATION_DEFAULT,
                AudioFormat.ENCODING_DEFAULT,
                44100,
                AudioTrack.MODE_STATIC);
        sinWave = new byte[44100];

        double freq_c3 = 220;
        double freq_e3 = 440;
        double freq_g3 = 660;
        double t = 0.0;
        double dt = 1.0 / 44100;

        for (int i = 0; i < sinWave.length; i++, t += dt) {
            double sum = 2*Math.sin(2.0 * Math.PI * t * freq_c3);
            sinWave[i] = (byte) (Byte.MAX_VALUE * sum);
        }

    }

    public void run(){
        playSound();

        while (isPlay) {
            track.write(sinWave, 0, sinWave.length);
            track.play();
        }

        track.stop();
        track.release();
    }

    public void stoprun(){
        isPlay = false;
    }

}
