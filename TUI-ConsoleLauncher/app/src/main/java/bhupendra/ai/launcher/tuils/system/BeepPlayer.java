package bhupendra.ai.launcher.tuils.system;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.ToneGenerator;
import android.os.Build;

public final class BeepPlayer {

    private static final int SAMPLE_RATE_HZ = 44100;
    private static final int DEFAULT_FREQUENCY_HZ = 3200;
    private static final int DEFAULT_PULSE_MS = 180;
    private static final int DEFAULT_GAP_MS = 55;
    private static final int DEFAULT_PULSE_COUNT = 3;
    private static final int DEFAULT_MAX_TOTAL_MS = 30000;
    private static final int MAX_REPEAT_UNTIL_ACK_MS = 300000;
    private static final int MIN_FREQUENCY_HZ = 500;
    private static final int MAX_FREQUENCY_HZ = 4500;
    private static final int MIN_PULSE_MS = 50;
    private static final int MAX_PULSE_MS = 1500;
    private static final int MIN_GAP_MS = 25;
    private static final int MAX_GAP_MS = 5000;
    private static final int MAX_PULSE_COUNT = 24;
    private static final float MAX_TRACK_VOLUME = 0.75f;
    private static final double MAX_AMPLITUDE = 0.35;

    private static final Object LOCK = new Object();
    private static int generation;

    private BeepPlayer() {
    }

    public static void playAlert() {
        playAlert(null, Options.defaults());
    }

    public static void playAlert(Options options) {
        playAlert(null, options);
    }

    public static void playAlert(Context context, Options options) {
        final Options safeOptions = options != null ? options.sanitized() : Options.defaults();
        final Context appContext = context != null ? context.getApplicationContext() : null;
        final int alertGeneration;
        synchronized (LOCK) {
            if (safeOptions.repeatUntilAcknowledged) {
                generation++;
            }
            alertGeneration = generation;
        }

        bhupendra.ai.launcher.tuils.LauncherExecutors.bgExecutor.execute(() -> {
                long startedAt = System.currentTimeMillis();
                VolumeOverride volumeOverride = VolumeOverride.acquire(appContext, safeOptions.forceAudible);
                try {
                    do {
                        try {
                            playGeneratedAlert(safeOptions);
                        } catch (RuntimeException e) {
                            playFallbackAlert(safeOptions);
                        }
                        if (!safeOptions.repeatUntilAcknowledged) {
                            break;
                        }
                        try {
                            Thread.sleep(safeOptions.gapMs);
                        } catch (InterruptedException e) {}
                    } while (isCurrent(alertGeneration)
                            && System.currentTimeMillis() - startedAt < safeOptions.maxTotalMs);
                } finally {
                    volumeOverride.restore();
                }
        });
    }

    public static void stopRepeatingAlert() {
        synchronized (LOCK) {
            generation++;
        }
    }

    private static boolean isCurrent(int alertGeneration) {
        synchronized (LOCK) {
            return generation == alertGeneration;
        }
    }

    private static void sleep(int durationMs) {
        try {
            Thread.sleep(durationMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static final class Options {
        public final int frequencyHz;
        public final int pulseMs;
        public final int gapMs;
        public final int pulseCount;
        public final float volume;
        public final boolean repeatUntilAcknowledged;
        public final int maxTotalMs;
        public final boolean forceAudible;

        public Options(int frequencyHz, int pulseMs, int gapMs, int pulseCount, float volume,
                       boolean repeatUntilAcknowledged, int maxTotalMs) {
            this(frequencyHz, pulseMs, gapMs, pulseCount, volume, repeatUntilAcknowledged,
                    maxTotalMs, false);
        }

        public Options(int frequencyHz, int pulseMs, int gapMs, int pulseCount, float volume,
                       boolean repeatUntilAcknowledged, int maxTotalMs, boolean forceAudible) {
            this.frequencyHz = frequencyHz;
            this.pulseMs = pulseMs;
            this.gapMs = gapMs;
            this.pulseCount = pulseCount;
            this.volume = volume;
            this.repeatUntilAcknowledged = repeatUntilAcknowledged;
            this.maxTotalMs = maxTotalMs;
            this.forceAudible = forceAudible;
        }

        public static Options defaults() {
            return new Options(DEFAULT_FREQUENCY_HZ, DEFAULT_PULSE_MS, DEFAULT_GAP_MS,
                    DEFAULT_PULSE_COUNT, 0.70f, false, DEFAULT_MAX_TOTAL_MS);
        }

        private Options sanitized() {
            int sanitizedPulseMs = clamp(pulseMs, MIN_PULSE_MS, MAX_PULSE_MS);
            int sanitizedMaxTotalMs = repeatUntilAcknowledged
                    ? clamp(maxTotalMs, sanitizedPulseMs, MAX_REPEAT_UNTIL_ACK_MS)
                    : clamp(maxTotalMs, sanitizedPulseMs, DEFAULT_MAX_TOTAL_MS);
            return new Options(
                    clamp(frequencyHz, MIN_FREQUENCY_HZ, MAX_FREQUENCY_HZ),
                    sanitizedPulseMs,
                    clamp(gapMs, MIN_GAP_MS, MAX_GAP_MS),
                    clamp(pulseCount, 1, MAX_PULSE_COUNT),
                    clamp(volume, 0.10f, MAX_TRACK_VOLUME),
                    repeatUntilAcknowledged,
                    sanitizedMaxTotalMs,
                    forceAudible);
        }

        private static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }

        private static float clamp(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }
    }

    private static void playGeneratedAlert(Options options) {
        short[] samples = buildSamples(options);
        int minBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (minBufferSize <= 0) {
            throw new IllegalStateException("AudioTrack minimum buffer unavailable: " + minBufferSize);
        }

        int bufferSizeBytes = Math.max(minBufferSize, samples.length * 2);
        AudioTrack track = createAudioTrack(bufferSizeBytes, options);

        if (track.getState() != AudioTrack.STATE_INITIALIZED) {
            track.release();
            throw new IllegalStateException("AudioTrack was not initialized");
        }

        try {
            track.setVolume(options.volume);
            track.play();
            int offset = 0;
            while (offset < samples.length) {
                int written = track.write(samples, offset, samples.length - offset);
                if (written <= 0) {
                    throw new IllegalStateException("AudioTrack write failed: " + written);
                }
                offset += written;
            }
            sleep(100);
        } finally {
            try {
                track.stop();
            } catch (IllegalStateException ignored) {
            }
            track.release();
        }
    }

    private static AudioTrack createAudioTrack(int bufferSizeBytes, Options options) {
        AudioFormat format = new AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE_HZ)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(options.forceAudible
                            ? AudioAttributes.USAGE_ALARM
                            : AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            return new AudioTrack.Builder()
                    .setAudioAttributes(attributes)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(bufferSizeBytes)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();
        }

        return createLegacyAudioTrack(bufferSizeBytes);
    }

    @SuppressWarnings("deprecation")
    private static AudioTrack createLegacyAudioTrack(int bufferSizeBytes) {
        return new AudioTrack(
                AudioManager.STREAM_ALARM,
                SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSizeBytes,
                AudioTrack.MODE_STREAM);
    }

    private static short[] buildSamples(Options options) {
        int pulseSamples = msToSamples(options.pulseMs);
        int gapSamples = msToSamples(options.gapMs);
        int totalSamples = (pulseSamples * options.pulseCount) + (gapSamples * (options.pulseCount - 1));
        short[] samples = new short[totalSamples];
        int index = 0;

        for (int pulse = 0; pulse < options.pulseCount; pulse++) {
            for (int i = 0; i < pulseSamples; i++) {
                double angle = 2.0 * Math.PI * options.frequencyHz * i / SAMPLE_RATE_HZ;
                samples[index++] = (short) (Math.sin(angle) * Short.MAX_VALUE * MAX_AMPLITUDE);
            }
            if (pulse < options.pulseCount - 1) {
                index += gapSamples;
            }
        }

        return samples;
    }

    private static int msToSamples(int ms) {
        return SAMPLE_RATE_HZ * ms / 1000;
    }

    private static void playFallbackAlert(Options options) {
        ToneGenerator toneGenerator = null;
        try {
            int fallbackVolume = Math.round(20 + (options.volume * 60));
            toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, fallbackVolume);
            for (int i = 0; i < options.pulseCount; i++) {
                toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, options.pulseMs);
                sleep(options.pulseMs + options.gapMs);
            }
        } catch (RuntimeException ignored) {
        } finally {
            if (toneGenerator != null) {
                toneGenerator.release();
            }
        }
    }

    private static final class VolumeOverride {
        private final AudioManager audioManager;
        private final int originalVolume;
        private final boolean shouldRestore;

        private VolumeOverride(AudioManager audioManager, int originalVolume, boolean shouldRestore) {
            this.audioManager = audioManager;
            this.originalVolume = originalVolume;
            this.shouldRestore = shouldRestore;
        }

        static VolumeOverride acquire(Context context, boolean forceAudible) {
            if (!forceAudible || context == null) {
                return new VolumeOverride(null, 0, false);
            }

            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager == null) {
                return new VolumeOverride(null, 0, false);
            }

            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
            int originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
            int targetVolume = Math.max(1, Math.round(maxVolume * MAX_TRACK_VOLUME));
            boolean shouldRestore = false;

            if (originalVolume < targetVolume) {
                try {
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVolume, 0);
                    shouldRestore = true;
                } catch (SecurityException ignored) {
                } catch (RuntimeException ignored) {
                }
            }

            return new VolumeOverride(audioManager, originalVolume, shouldRestore);
        }

        void restore() {
            if (!shouldRestore || audioManager == null) {
                return;
            }

            try {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0);
            } catch (SecurityException ignored) {
            } catch (RuntimeException ignored) {
            }
        }
    }
}
