package bhupendra.ai.launcher.managers.music;

import bhupendra.ai.launcher.managers.FileSystemManager;


import android.app.Notification;
import android.app.Notification.Action;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import bhupendra.ai.launcher.LauncherActivity;
import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.tuils.Tuils;

public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    private static final String ACTION_PLAY_PAUSE = "bhupendra.ai.launcher.music.PLAY_PAUSE";
    private static final String ACTION_NEXT = "bhupendra.ai.launcher.music.NEXT";
    private static final String ACTION_PREVIOUS = "bhupendra.ai.launcher.music.PREVIOUS";
    public static final int NOTIFY_ID=100001;

    private MediaPlayer player;
    private List<Song> songs;
    private int songPosn;
    private final IBinder musicBind = new MusicBinder();
    private String songTitle = Tuils.EMPTYSTRING;
    private boolean shuffle=false;
    private MediaSession mediaSession;
    private boolean foregroundStarted;

    private long lastNotificationChange;

//    do not touch the song playback from here

    public void onCreate(){
        super.onCreate();
        songPosn=0;
        player = new MediaPlayer();
        initMusicPlayer();
        initMediaSession();

        lastNotificationChange = System.currentTimeMillis();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            handleTransportAction(intent.getAction());
            return START_STICKY;
        }

        if(System.currentTimeMillis() - lastNotificationChange < 500 || songTitle == null || songTitle.length() == 0) return super.onStartCommand(intent, flags, startId);

        lastNotificationChange = System.currentTimeMillis();
        updatePlaybackState(isPng() ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED);
        updateNotification();

        return START_STICKY;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if(songTitle == null || songTitle.length() == 0) return;

        lastNotificationChange = System.currentTimeMillis();

        mp.start();
        updateMetadata();
        updatePlaybackState(PlaybackState.STATE_PLAYING);
        updateNotification();
    }

    public void initMusicPlayer(){
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }

    public void setList(List<Song> theSongs) {
        songs = theSongs;
        if(shuffle) Collections.shuffle(songs);
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent){
        return super.onUnbind(intent);
    }

    public String playSong(){
        if (songs == null || songs.isEmpty()) return getString(R.string.no_songs);
        try {
            player.reset();
        } catch (Exception e) {
//            no need to log this error, as this will occur everytime
            Tuils.log(e);
        }

        Song playSong = songs.get(songPosn);
        songTitle = playSong.getTitle();

        long id = playSong.getID();
        if(id == -1) {
            String path = playSong.getPath();
            try {
                player.setDataSource(path);
            } catch (IOException e) {
                Tuils.log(e);
                FileSystemManager.toFile(e);
                return null;
            }
        } else {
            long currSong = playSong.getID();
            Uri trackUri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currSong);
            try {
                player.setDataSource(getApplicationContext(), trackUri);
            }
            catch(Exception e) {
                Tuils.log(e);
                FileSystemManager.toFile(e);
                return null;
            }
        }
        updateMetadata();
        updatePlaybackState(PlaybackState.STATE_BUFFERING);
        updateNotification();
        player.prepareAsync();

        return playSong.getTitle();
    }

    public void setSong(int songIndex){
        songPosn = songIndex;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if(player.getCurrentPosition()>0){
            mp.reset();
            playNext();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        updatePlaybackState(PlaybackState.STATE_ERROR);
        return false;
    }

    private Notification buildNotification() {
        Context context = getApplicationContext();
        String channelId = "music_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Music", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Music playback");
            channel.setShowBadge(false);
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Intent notIntent = new Intent(context, LauncherActivity.class);
        PendingIntent pendInt = PendingIntent.getActivity(context, 0, notIntent, Tuils.pendingIntentFlags(PendingIntent.FLAG_UPDATE_CURRENT));

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, channelId);
        } else {
            builder = new Notification.Builder(context);
        }

        builder.setContentIntent(pendInt)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(isPng())
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setContentTitle(songTitle != null && songTitle.length() > 0 ? songTitle : "T-UI Player")
            .setContentText(isPng() ? "Playing" : "Paused");

        builder.addAction(new Action.Builder(
            android.R.drawable.ic_media_previous,
            "Previous",
            buildServiceAction(context, ACTION_PREVIOUS, 1)).build());
        builder.addAction(new Action.Builder(
            isPng() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
            isPng() ? "Pause" : "Play",
            buildServiceAction(context, ACTION_PLAY_PAUSE, 2)).build());
        builder.addAction(new Action.Builder(
            android.R.drawable.ic_media_next,
            "Next",
            buildServiceAction(context, ACTION_NEXT, 3)).build());

        if (mediaSession != null) {
            Notification.MediaStyle style = new Notification.MediaStyle()
                .setMediaSession(mediaSession.getSessionToken())
                .setShowActionsInCompactView(0, 1, 2);
            builder.setStyle(style);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        }

        return builder.build();
    }

    public int getPosn(){
        return player.getCurrentPosition();
    }

    public int getDur(){
        return player.getDuration();
    }

    public boolean isPng(){
        return player.isPlaying();
    }

    public void pausePlayer(){
        if (player != null && player.isPlaying()) {
            player.pause();
        }
        updatePlaybackState(PlaybackState.STATE_PAUSED);
        updateNotification();
    }

    public void stop() {
        try {
            player.stop();
        } catch (Exception e) {}

        try {
            player.reset();
        } catch (Exception e) {}

        setSong(0);
        songTitle = Tuils.EMPTYSTRING;
        updatePlaybackState(PlaybackState.STATE_STOPPED);
        if (foregroundStarted) {
            stopForeground(true);
            foregroundStarted = false;
        }
    }

    public void playPlayer() {
        player.start();
        updatePlaybackState(PlaybackState.STATE_PLAYING);
        updateNotification();
    }

    public void seek(int posn){
        player.seekTo(posn);
    }

    public void go(){
        player.start();
    }

    public String playPrev(){
        if(songs.size() == 0) return getString(R.string.no_songs);
        songPosn = previous();
        return playSong();
    }

    public String playNext() {
        if(songs.size() == 0) return getString(R.string.no_songs);
        songPosn = next();
        return playSong();
    }

    private int next() {
        int pos = songPosn + 1;
        if(pos == songs.size()) pos = 0;
        return pos;
    }

    private int previous() {
        int pos = songPosn - 1;
        if(pos < 0) pos = songs.size() - 1;
        return pos;
    }

    public int getSongIndex() {
        return songPosn;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        player.release();
        if (songs != null) songs.clear();
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }

        stopForeground(true);
    }

    public void setShuffle(boolean shuffle){
        this.shuffle = shuffle;
    }

    private void initMediaSession() {
        mediaSession = new MediaSession(this, "TUI-Music");
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                if (!playbackReady()) return;
                if (songTitle != null && songTitle.length() > 0 && !isPng()) {
                    playPlayer();
                } else {
                    playSong();
                }
            }

            @Override
            public void onPause() {
                pausePlayer();
            }

            @Override
            public void onSkipToNext() {
                playNext();
            }

            @Override
            public void onSkipToPrevious() {
                playPrev();
            }
        });
        mediaSession.setActive(true);
        updatePlaybackState(PlaybackState.STATE_NONE);
    }

    private void handleTransportAction(String action) {
        if (ACTION_PLAY_PAUSE.equals(action)) {
            if (isPng()) {
                pausePlayer();
            } else if (songTitle != null && songTitle.length() > 0) {
                playPlayer();
            } else {
                playSong();
            }
        } else if (ACTION_NEXT.equals(action)) {
            playNext();
        } else if (ACTION_PREVIOUS.equals(action)) {
            playPrev();
        }
    }

    private PendingIntent buildServiceAction(Context context, String action, int requestCode) {
        Intent intent = new Intent(context, MusicService.class);
        intent.setAction(action);
        return PendingIntent.getService(context, requestCode, intent,
            Tuils.pendingIntentFlags(PendingIntent.FLAG_UPDATE_CURRENT));
    }

    private void updateNotification() {
        if (songTitle == null || songTitle.length() == 0) return;
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFY_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFY_ID, notification);
        }
        foregroundStarted = true;
    }

    private void updateMetadata() {
        if (mediaSession == null) return;
        MediaMetadata.Builder builder = new MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, songTitle != null ? songTitle : "");
        Song current = getCurrentSong();
        if (current != null) {
            builder.putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, current.getTitle());
        }
        mediaSession.setMetadata(builder.build());
    }

    private void updatePlaybackState(int state) {
        if (mediaSession == null) return;
        long actions = PlaybackState.ACTION_PLAY
            | PlaybackState.ACTION_PAUSE
            | PlaybackState.ACTION_PLAY_PAUSE
            | PlaybackState.ACTION_SKIP_TO_NEXT
            | PlaybackState.ACTION_SKIP_TO_PREVIOUS;
        long position = 0L;
        try {
            position = player != null ? player.getCurrentPosition() : 0L;
        } catch (Exception ignored) {}
        mediaSession.setPlaybackState(new PlaybackState.Builder()
            .setActions(actions)
            .setState(state, position, 1f)
            .build());
    }

    private Song getCurrentSong() {
        if (songs == null || songs.isEmpty() || songPosn < 0 || songPosn >= songs.size()) return null;
        return songs.get(songPosn);
    }

    private boolean playbackReady() {
        return songs != null && !songs.isEmpty();
    }

    private boolean stoppedState() {
        try {
            return player == null || !player.isPlaying();
        } catch (Exception e) {
            return true;
        }
    }

}
