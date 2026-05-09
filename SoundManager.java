import javax.sound.sampled.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SoundManager — synthesizes Candy Crush-style audio entirely in code.
 *
 * No external audio files required.  All sounds are generated with Java's
 * javax.sound.sampled API using oscillators, envelopes, and noise.
 *
 * ── HOW TO USE ────────────────────────────────────────────────────────────
 *
 *  In Game.java constructor / startLevel():
 *      SoundManager.startMusic();
 *
 *  On a normal candy match:
 *      SoundManager.playMatch();
 *
 *  On a striped candy created (4-in-a-row):
 *      SoundManager.playStriped();
 *
 *  On a bomb candy burst:
 *      SoundManager.playBomb();
 *
 *  On a combo (cascade):
 *      SoundManager.playCombo(comboCount); // 2, 3, 4 …
 *
 *  On level win:
 *      SoundManager.playWin();
 *
 *  On level fail / out of moves:
 *      SoundManager.playFail();
 *
 *  On candy selection click:
 *      SoundManager.playSelect();
 *
 *  On hint pulsing (call once when hint appears):
 *      SoundManager.playHint();
 *
 *  To stop music (e.g. when navigating away):
 *      SoundManager.stopMusic();
 *
 * ── VOLUME CONTROL ────────────────────────────────────────────────────────
 *  SoundManager.setMusicVolume(0.0f – 1.0f);   default 0.55f
 *  SoundManager.setSfxVolume  (0.0f – 1.0f);   default 0.80f
 *  SoundManager.setMuted(true/false);
 */
public class SoundManager {

    // ── Sample rate ───────────────────────────────────────────────────────
    private static final float SAMPLE_RATE = 44100f;

    // ── Volume ────────────────────────────────────────────────────────────
    private static float musicVolume = 0.55f;
    private static float sfxVolume   = 0.80f;
    private static boolean muted     = false;

    // ── Background music state ────────────────────────────────────────────
    private static Thread musicThread;
    private static volatile boolean musicRunning = false;
    private static SourceDataLine musicLine;

    // ── SFX thread pool (non-blocking) ────────────────────────────────────
    private static final ExecutorService sfxPool =
            Executors.newFixedThreadPool(4, r -> {
                Thread t = new Thread(r, "SoundManager-SFX");
                t.setDaemon(true);
                return t;
            });

    // ─────────────────────────────────────────────────────────────────────
    //  PUBLIC API
    // ─────────────────────────────────────────────────────────────────────

    public static void setMusicVolume(float v) { musicVolume = clamp(v); }
    public static void setSfxVolume  (float v) { sfxVolume   = clamp(v); }
    public static void setMuted(boolean m)      { muted = m; }

    /** Starts the looping Candy Crush-style background music. Call once. */
    public static void startMusic() {
        stopMusic();
        musicRunning = true;
        musicThread  = new Thread(SoundManager::musicLoop, "SoundManager-Music");
        musicThread.setDaemon(true);
        musicThread.start();
    }

    /** Stops background music gracefully. */
    public static void stopMusic() {
        musicRunning = false;
        if (musicLine != null) { musicLine.stop(); musicLine.close(); }
        if (musicThread != null) musicThread.interrupt();
    }

    /** Normal 3-match pop. */
    public static void playMatch()           { sfx(() -> synthMatch(1));          }

    /** 4-in-a-row striped candy creation — ascending shimmer. */
    public static void playStriped()         { sfx(() -> synthStriped());         }

    /** Bomb exploding — deep thud + rumble. */
    public static void playBomb()            { sfx(() -> synthBomb());            }

    /** Cascade combo — escalating chime (pass combo count 2,3,4…). */
    public static void playCombo(int count)  { sfx(() -> synthCombo(count));      }

    /** Level complete — triumphant fanfare. */
    public static void playWin()             { sfx(() -> synthWin());             }

    /** Out of moves — sad descending tones. */
    public static void playFail()            { sfx(() -> synthFail());            }

    /** Candy selection click. */
    public static void playSelect()          { sfx(() -> synthSelect());          }

    /** Hint animation start — soft shimmer. */
    public static void playHint()            { sfx(() -> synthHint());            }

    // ─────────────────────────────────────────────────────────────────────
    //  BACKGROUND MUSIC  (looping candy-pop melody)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * A cheerful, looping Candy Crush-style melody built from three layers:
     *   1. Lead melody   — bright sine/triangle wave playing the iconic theme
     *   2. Bassline      — rounded bass following the chord root
     *   3. Twinkle arp   — high-pitched arpeggiated chime on every beat
     */
    private static void musicLoop() {
        try {
            AudioFormat fmt = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
            musicLine = (SourceDataLine) AudioSystem.getLine(info);
            musicLine.open(fmt, (int)(SAMPLE_RATE * 0.1f) * 2);
            musicLine.start();

            // ── Candy Crush-style melody in C major ───────────────────────
            // Notes: C4=261.63, D4=293.66, E4=329.63, F4=349.23,
            //        G4=392.00, A4=440.00, B4=493.88, C5=523.25
            // Durations in beats (at 120 BPM → 1 beat = 0.5 s)
            double bpm    = 122.0;
            double beat   = 60.0 / bpm;          // seconds per beat
            double eighth = beat / 2.0;

            // The iconic Candy Crush-style 8-bar loop
            // Each entry: { frequency, beats }
            double[][] melody = {
                // Bar 1 – C major rising
                {523.25, 0.5}, {587.33, 0.5}, {659.25, 0.5}, {698.46, 0.5},
                // Bar 2 – back down with a skip
                {783.99, 1.0}, {659.25, 0.5}, {523.25, 0.5},
                // Bar 3 – A minor feel
                {440.00, 0.5}, {493.88, 0.5}, {523.25, 0.5}, {587.33, 0.5},
                // Bar 4 – resolution
                {659.25, 1.5}, {0, 0.5},
                // Bar 5 – C major with hop
                {523.25, 0.5}, {659.25, 0.5}, {783.99, 0.5}, {1046.5, 0.5},
                // Bar 6 – descending twinkle
                {987.77, 0.5}, {880.00, 0.5}, {783.99, 0.5}, {698.46, 0.5},
                // Bar 7 – swinging feel
                {659.25, 0.5}, {587.33, 0.5}, {659.25, 0.5}, {698.46, 0.5},
                // Bar 8 – ending flourish
                {783.99, 0.75}, {659.25, 0.25}, {523.25, 1.0},
            };

            double[][] bass = {
                {130.81, 2}, {130.81, 2},
                {110.00, 2}, {130.81, 2},
                {130.81, 2}, {174.61, 2},
                {196.00, 2}, {130.81, 2},
            };

            // Twinkle arp notes (played on every beat as a short stab)
            double[] arpNotes = {1046.5, 1174.7, 1318.5, 1396.9,
                                 1318.5, 1174.7, 1046.5,  987.77,
                                  987.77, 1046.5, 1174.7, 1318.5,
                                 1396.9, 1318.5, 1174.7, 1046.5};

            int arpIdx   = 0;
            int melodyIdx = 0;
            int bassIdx   = 0;
            double melodyAcc = 0, bassAcc = 0;
            double totalBars = 8 * 4 * eighth;   // 8 bars × 4 eighths

            double t = 0;
            int samplesPerChunk = (int)(SAMPLE_RATE * eighth);
            byte[] buf = new byte[samplesPerChunk * 2];

            while (musicRunning && !Thread.currentThread().isInterrupted()) {

                double melFreq  = melody[melodyIdx][0];
                double melDur   = melody[melodyIdx][1] * beat;
                double bassFreq = bass[bassIdx][0];
                double bassDur  = bass[bassIdx][1] * beat;
                double arpFreq  = arpNotes[arpIdx % arpNotes.length];

                for (int i = 0; i < samplesPerChunk && musicRunning; i++) {
                    double ts = t + i / SAMPLE_RATE;

                    // Lead melody — triangle wave for warmth
                    double melAmp = 0;
                    if (melFreq > 0) {
                        double ph = (ts * melFreq) % 1.0;
                        melAmp = (ph < 0.5 ? 4*ph - 1 : 3 - 4*ph);  // triangle
                        // ADSR-ish fade
                        double pos = (melodyAcc + i / SAMPLE_RATE) / melDur;
                        double env = pos < 0.05 ? pos / 0.05
                                   : pos > 0.80 ? 1.0 - (pos - 0.80) / 0.20
                                   : 1.0;
                        melAmp *= env * 0.35;
                    }

                    // Bass — sine wave
                    double bassAmp = Math.sin(2 * Math.PI * bassFreq * ts);
                    double bpos = (bassAcc + i / SAMPLE_RATE) / bassDur;
                    double benv = bpos < 0.04 ? bpos / 0.04
                                : bpos > 0.70 ? 1.0 - (bpos - 0.70) / 0.30
                                : 1.0;
                    bassAmp *= benv * 0.18;

                    // Twinkle arp — short sine stab at start of each eighth
                    double arpPhase = (i / SAMPLE_RATE) / eighth;
                    double arpAmp = 0;
                    if (arpPhase < 0.25) {
                        double arpEnv = arpPhase < 0.02 ? arpPhase / 0.02
                                      : 1.0 - (arpPhase / 0.25);
                        arpAmp = Math.sin(2 * Math.PI * arpFreq * ts) * arpEnv * 0.20;
                    }

                    // Mix
                    double vol = muted ? 0 : musicVolume;
                    double mix = (melAmp + bassAmp + arpAmp) * vol;
                    mix = Math.max(-1, Math.min(1, mix));

                    short sample = (short)(mix * 32000);
                    buf[i*2]   = (byte)(sample & 0xFF);
                    buf[i*2+1] = (byte)((sample >> 8) & 0xFF);
                }

                musicLine.write(buf, 0, samplesPerChunk * 2);
                t += eighth;

                // Advance arp every eighth note
                arpIdx++;

                // Advance melody
                melodyAcc += eighth;
                if (melodyAcc >= melody[melodyIdx][1] * beat) {
                    melodyAcc = 0;
                    melodyIdx = (melodyIdx + 1) % melody.length;
                }

                // Advance bass
                bassAcc += eighth;
                if (bassAcc >= bass[bassIdx][1] * beat) {
                    bassAcc = 0;
                    bassIdx = (bassIdx + 1) % bass.length;
                }
            }

        } catch (LineUnavailableException e) {
            System.err.println("SoundManager: audio line unavailable — " + e.getMessage());
        } catch (Exception e) {
            if (!(e instanceof InterruptedException)) e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  SFX SYNTHESIS
    // ─────────────────────────────────────────────────────────────────────

    /** Normal pop — short bright pluck. */
    private static void synthMatch(int ignored) throws Exception {
        // Two-note ascending pop: the Candy Crush signature "pop" sound
        double[] freqs    = {880, 1046.5};
        double[] durs     = {0.06, 0.09};
        playSamples(buildMultiPop(freqs, durs, sfxVolume));
    }

    /** Striped — upward shimmer sweep. */
    private static void synthStriped() throws Exception {
        int n      = (int)(SAMPLE_RATE * 0.35);
        byte[] buf = new byte[n * 2];
        for (int i = 0; i < n; i++) {
            double t    = i / SAMPLE_RATE;
            double freq = 600 + 1200 * (t / 0.35);   // sweep 600→1800 Hz
            double env  = t < 0.05 ? t / 0.05
                        : t > 0.20 ? 1.0 - (t - 0.20) / 0.15 : 1.0;
            double s = Math.sin(2 * Math.PI * freq * t) * env;
            // Add shimmer: mix in a slight chorus
            s += Math.sin(2 * Math.PI * (freq * 1.005) * t) * env * 0.4;
            s *= muted ? 0 : sfxVolume * 0.6;
            short v = (short)(clampSample(s) * 32000);
            buf[i*2] = (byte)(v & 0xFF); buf[i*2+1] = (byte)((v>>8)&0xFF);
        }
        playSamples(buf);
    }

    /** Bomb — deep thud with low rumble. */
    private static void synthBomb() throws Exception {
        int n      = (int)(SAMPLE_RATE * 0.55);
        byte[] buf = new byte[n * 2];
        java.util.Random rng = new java.util.Random(42);
        for (int i = 0; i < n; i++) {
            double t    = i / SAMPLE_RATE;
            // Pitch-dropping thud (starts at 180 Hz, falls to 40 Hz)
            double freq = 180 * Math.pow(40.0/180.0, t / 0.55);
            double env  = t < 0.01 ? t / 0.01 : Math.exp(-8 * t);
            double thud = Math.sin(2 * Math.PI * freq * t) * env;
            // Noise burst at attack
            double noise = (rng.nextDouble()*2-1) * Math.exp(-25 * t);
            double s = (thud * 0.7 + noise * 0.5) * (muted ? 0 : sfxVolume);
            short v = (short)(clampSample(s) * 32000);
            buf[i*2] = (byte)(v & 0xFF); buf[i*2+1] = (byte)((v>>8)&0xFF);
        }
        playSamples(buf);
    }

    /** Combo — escalating chime sequence. */
    private static void synthCombo(int count) throws Exception {
        count = Math.max(1, Math.min(count, 6));
        // Each successive combo hit plays a higher note
        double[] scale = {523.25, 587.33, 659.25, 740.00, 830.61, 932.33};
        int notes = Math.min(count, scale.length);
        double noteDur = 0.12;
        int totalSamps = (int)(SAMPLE_RATE * (noteDur * notes + 0.1));
        byte[] buf = new byte[totalSamps * 2];

        for (int ni = 0; ni < notes; ni++) {
            double freq    = scale[ni];
            double offset  = ni * noteDur;
            int    start   = (int)(offset * SAMPLE_RATE);
            int    noteLen = (int)(noteDur * SAMPLE_RATE);
            for (int i = 0; i < noteLen && (start + i) < totalSamps; i++) {
                double t   = i / SAMPLE_RATE;
                double env = t < 0.01 ? t / 0.01 : Math.exp(-12 * t);
                double s   = Math.sin(2 * Math.PI * freq * t) * env
                           + Math.sin(2 * Math.PI * freq * 2 * t) * env * 0.3;
                s *= muted ? 0 : sfxVolume * 0.65;
                int idx = (start + i) * 2;
                if (idx + 1 < buf.length) {
                    short old = (short)((buf[idx+1]<<8) | (buf[idx]&0xFF));
                    short add = (short)(clampSample(s) * 32000);
                    short mix = (short)(clampSample((old + add) / 32000.0) * 32000);
                    buf[idx]   = (byte)(mix & 0xFF);
                    buf[idx+1] = (byte)((mix>>8) & 0xFF);
                }
            }
        }
        playSamples(buf);
    }

    /** Win fanfare — bright ascending arpeggio + sustain. */
    private static void synthWin() throws Exception {
        double[] notes  = {523.25, 659.25, 783.99, 1046.5, 1318.5};
        double[] timings = {0.0, 0.12, 0.24, 0.36, 0.55};
        double   total   = 1.2;
        int n      = (int)(SAMPLE_RATE * total);
        byte[] buf = new byte[n * 2];

        for (int ni = 0; ni < notes.length; ni++) {
            double freq  = notes[ni];
            double start = timings[ni];
            int    iStart = (int)(start * SAMPLE_RATE);
            for (int i = iStart; i < n; i++) {
                double t   = (i - iStart) / SAMPLE_RATE;
                double env = t < 0.02 ? t / 0.02
                           : t > 0.70 ? Math.exp(-6*(t-0.70)) : 1.0;
                double s   = Math.sin(2*Math.PI*freq*t) * env * 0.5
                           + Math.sin(2*Math.PI*freq*2*t) * env * 0.15;
                s *= muted ? 0 : sfxVolume * 0.55;
                short old  = (short)((buf[i*2+1]<<8)|(buf[i*2]&0xFF));
                double mix = old/32000.0 + s;
                short  val = (short)(clampSample(mix) * 32000);
                buf[i*2]   = (byte)(val & 0xFF);
                buf[i*2+1] = (byte)((val>>8) & 0xFF);
            }
        }
        playSamples(buf);
    }

    /** Fail — descending sad tones. */
    private static void synthFail() throws Exception {
        double[] notes   = {523.25, 466.16, 415.30, 349.23};
        double[] timings = {0.0, 0.18, 0.36, 0.54};
        double   total   = 1.0;
        int n      = (int)(SAMPLE_RATE * total);
        byte[] buf = new byte[n * 2];

        for (int ni = 0; ni < notes.length; ni++) {
            double freq  = notes[ni];
            double start = timings[ni];
            int    iStart = (int)(start * SAMPLE_RATE);
            for (int i = iStart; i < n; i++) {
                double t   = (i - iStart) / SAMPLE_RATE;
                double env = t < 0.03 ? t/0.03
                           : t > 0.30 ? Math.exp(-4*(t-0.30)) : 1.0;
                // Minor-ish timbre with slight detuned second harmonic
                double s = (Math.sin(2*Math.PI*freq*t)
                          + Math.sin(2*Math.PI*freq*1.98*t)*0.3) * env * 0.4;
                s *= muted ? 0 : sfxVolume * 0.55;
                short old  = (short)((buf[i*2+1]<<8)|(buf[i*2]&0xFF));
                double mix = old/32000.0 + s;
                short  val = (short)(clampSample(mix) * 32000);
                buf[i*2]   = (byte)(val & 0xFF);
                buf[i*2+1] = (byte)((val>>8) & 0xFF);
            }
        }
        playSamples(buf);
    }

    /** Select click — short bright tap. */
    private static void synthSelect() throws Exception {
        playSamples(buildMultiPop(new double[]{1200}, new double[]{0.04}, sfxVolume * 0.6));
    }

    /** Hint shimmer — soft high twinkling. */
    private static void synthHint() throws Exception {
        int n      = (int)(SAMPLE_RATE * 0.25);
        byte[] buf = new byte[n * 2];
        double[] hfreqs = {1568, 1760, 1975.5};
        for (int hi = 0; hi < hfreqs.length; hi++) {
            double freq  = hfreqs[hi];
            double delay = hi * 0.05;
            int    start = (int)(delay * SAMPLE_RATE);
            for (int i = start; i < n; i++) {
                double t   = (i - start) / SAMPLE_RATE;
                double env = t < 0.01 ? t/0.01 : Math.exp(-15*t);
                double s   = Math.sin(2*Math.PI*freq*t) * env * 0.35;
                s *= muted ? 0 : sfxVolume * 0.5;
                short old  = (short)((buf[i*2+1]<<8)|(buf[i*2]&0xFF));
                double mix = old/32000.0 + s;
                short  val = (short)(clampSample(mix) * 32000);
                buf[i*2]   = (byte)(val & 0xFF);
                buf[i*2+1] = (byte)((val>>8) & 0xFF);
            }
        }
        playSamples(buf);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  UTILITIES
    // ─────────────────────────────────────────────────────────────────────

    /** Builds a short multi-note pop (the signature candy-crush "pop"). */
    private static byte[] buildMultiPop(double[] freqs, double[] durs, double vol) {
        double totalDur = 0;
        for (double d : durs) totalDur += d;
        int n      = (int)(SAMPLE_RATE * totalDur);
        byte[] buf = new byte[n * 2];
        double offset = 0;
        for (int ni = 0; ni < freqs.length; ni++) {
            double freq = freqs[ni];
            double dur  = durs[ni];
            int start   = (int)(offset * SAMPLE_RATE);
            int len     = (int)(dur * SAMPLE_RATE);
            for (int i = 0; i < len && (start+i) < n; i++) {
                double t = i / SAMPLE_RATE;
                // Quick decay envelope
                double env = Math.exp(-18 * t);
                // Pluck-like timbre: fundamental + 2nd harmonic
                double s = (Math.sin(2*Math.PI*freq*t) * 0.7
                          + Math.sin(2*Math.PI*freq*2*t) * 0.3) * env;
                s *= muted ? 0 : vol;
                int idx = (start+i)*2;
                short old = (short)((buf[idx+1]<<8)|(buf[idx]&0xFF));
                double mix = old/32000.0 + s;
                short  val = (short)(clampSample(mix) * 32000);
                buf[idx]   = (byte)(val & 0xFF);
                buf[idx+1] = (byte)((val>>8) & 0xFF);
            }
            offset += dur;
        }
        return buf;
    }

    /** Plays a raw PCM byte array on a temporary SourceDataLine (non-blocking). */
    private static void playSamples(byte[] buf) throws Exception {
        AudioFormat fmt = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(fmt);
        line.start();
        line.write(buf, 0, buf.length);
        line.drain();
        line.close();
    }

    /** Submits a sound task to the sfx thread pool. */
    private static void sfx(SfxTask task) {
        sfxPool.submit(() -> {
            try { task.run(); }
            catch (InterruptedException ignored) {}
            catch (Exception e) { /* silently skip unavailable audio */ }
        });
    }

    private static float  clamp(float v) { return Math.max(0, Math.min(1, v)); }
    private static double clampSample(double v) { return Math.max(-1, Math.min(1, v)); }

    @FunctionalInterface
    private interface SfxTask { void run() throws Exception; }
}