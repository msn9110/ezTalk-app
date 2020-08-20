package com.hhs.waverecorder.core;


import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.hhs.waverecorder.utils.Check;
import com.hhs.waverecorder.utils.Divider;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

public class Speaker implements Serializable {

    public void setSpeakingListener(SpeakingListener listener) {
        this.listener = listener;
    }
    private SpeakingListener listener; // callback to show the current speaking text
    private static final long serialVersionUID = -7060210544600464481L;
    //=============================================語音==============================================
    private static final int TW = 1, EN = 0;
    private static String[] LANG = new String[]{"EN", "TW"};
    private static Locale[] locales = new Locale[]{Locale.ENGLISH, Locale.TAIWAN};
    private TextToSpeech[] mTTS;
    private Deque<String> queue = new ArrayDeque<>(); // queue for speaking text
    private static final String TAG = "## Speaker";
    private SpeakerQueueMonitor monitor; // a thread to monitor whether there is a text in a queue
    public Speaker(Context context){
        mTTS = new TextToSpeech[locales.length];
        initSpeaker(context);
        monitor = new SpeakerQueueMonitor(this);
        monitor.start();
    }

    private void initSpeaker(Context context){
        for (int i = 0; i < mTTS.length; i++){
            final int j = i;
            mTTS[j] = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    setLanguage(status, mTTS[j], locales[j], LANG[j]);
                }
            });
        }
    }

    private void setLanguage(int status, TextToSpeech tts, Locale locale, String lang){
        // status can be either TextToSpeech.SUCCESS or TextToSpeech.ERROR.
        if (status == TextToSpeech.SUCCESS) {
            int result;
            result = tts.setLanguage(locale);//<<<===================================
            if (result == TextToSpeech.LANG_MISSING_DATA) { ///|| result == TextToSpeech.LANG_NOT_SUPPORTED
                Log.e(TAG, lang);
            }
            else{
                float speed = (float) 0.75;
                tts.setSpeechRate(speed);
                Log.i(TAG, lang);
            }
        } else {
            Log.e(TAG, "Could not initialize TextToSpeech.");
        }
    }

    public void addSpeak(String string){
        ArrayList<String> sentences = Divider.getSentences(string);
        queue.addAll(sentences);
        //queue.add(string);
        monitor.wake();
    }
    public void setEnable(boolean enable){
        monitor.setEnable(enable);
    }

    public void stop(){
        queue.clear();
        stopCurrent();
    }

    @SuppressWarnings("deprecation")
    private void speak(String hello) {
        hello = " " + hello;
        int count = processString(hello);
        int speaker = ((Check.checkChar(hello.charAt(0)) == EN) ? EN : TW);
        List<String> list = Arrays.asList(msg).subList(0, count + 1);
        for(String s : list) {
            Log.i(TAG, s);
            speaker %= 2;
            mTTS[speaker++].speak(s, TextToSpeech.QUEUE_ADD, null);

        }
    }

    private void speakSync(String hello) {
        speak(hello);
        BusySpeakerListener listener = new BusySpeakerListener(this);
        listener.start();
        try {
            listener.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            if(listener.isAlive()){
                listener.cancel();
                listener.interrupt();
            }
            Log.i(TAG, "Interrupted in Speak Sync");
        }
    }

    private String[] msg = new String[200];

    private int processString(String hello){
        for (int i = 0; i < msg.length; i++)
            msg[i] = "";
        int previous = TW, current;
        int count = 0;
        for (int i = 0; i < hello.length(); i++)
        {
            char ch = hello.charAt(i);
            switch (Check.checkChar(ch)){
                case -1:
                    current = previous;
                    break;
                case EN:
                    current = EN;
                    break;
                default:
                    current = TW;
            }

            if (current != previous)
            {
                previous = current;
                count++;
            }
            msg[count] += String.valueOf(ch);
        }
        return count;
    }

    public boolean pause(){
        if (! isNotSpeaking()){
            String tmp = queue.peek();
            queue.addFirst(tmp);
        }
        stopCurrent();
        return monitor.pause();
    }

    private void stopCurrent(){
        for (TextToSpeech tts : mTTS){
            if (tts != null)
                tts.stop();
        }
    }

    public void shutdown(){
        stop();
        for (TextToSpeech tts : mTTS){
            if (tts != null)
                tts.shutdown();
            // tts = null;
        }
        monitor.stopMonitor();
        monitor.interrupt();
        try {
            monitor.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private boolean isNotSpeaking(){
        for (TextToSpeech tts : mTTS){
            if (tts != null && !tts.isSpeaking())
                return true;
        }
        return false;
    }

    private class SpeakerQueueMonitor extends Thread {
        private Speaker speaker;
        private boolean toMonitor;
        private boolean isPaused = false;

        SpeakerQueueMonitor(Speaker speaker){
            this.speaker = speaker;
        }

        @Override
        public void run() {
            toMonitor = true;
            while (toMonitor){
                waiting();
                if (! queue.isEmpty() && ! isPaused){
                    final String message = queue.peek();
                    if (message != null && message.length() > 0) {
                        Log.i("## SpeakerQueueMonitor", message);
                        if (listener != null)
                            listener.onPreSpeak(message);
                        speaker.speakSync(message);
                        queue.poll(); // replace remove
                    }
                }
            }
        }
        private synchronized void waiting(){
            if (queue.isEmpty() || isPaused) {
                try {
                    Log.d("## SpeakerQueueMonitor", "Waiting.....");
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private synchronized boolean pause(){
            isPaused = !isPaused;
            notify();
            return isPaused;
        }

        private synchronized void setEnable(boolean enable){
            isPaused = !enable;
            notify();
        }

        private synchronized void stopMonitor(){
            toMonitor = false;
            notify();
        }

        private synchronized void wake(){
            notify();
        }
    }

    private class BusySpeakerListener extends Thread {
        private Speaker speaker;
        private boolean toSpeak;

        BusySpeakerListener(Speaker speaker) {
            this.speaker = speaker;
        }

        @Override
        public void run() {
            try {
                toSpeak = true;
                Thread.sleep(20);
                while (!speaker.isNotSpeaking() && toSpeak) // speaker is busy now
                    Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.i("## BusySpeakerListener", "interrupt !");
            }
        }

        void cancel(){
            toSpeak = false;
        }
    }
}

