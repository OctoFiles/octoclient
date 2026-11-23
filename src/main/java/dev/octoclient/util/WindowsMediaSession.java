package dev.octoclient.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Reads the Windows global media session (SMTC) through a single long-lived
 * PowerShell helper, so Spotify works without an OAuth token and without the
 * periodic one-second hitch a spawned process per poll would cause.
 */
public final class WindowsMediaSession {
    private static final char SEP = 31;
    private static final String COVER_FILE = "octoclient_cover.png";

    /** Latest snapshot of the active media session. */
    public record Info(String source, String title, String artist, String album,
                       boolean playing, long positionMillis, long durationMillis,
                       String coverKey, long polledAtNanos) {
        public static final Info NONE = new Info("", "", "", "", false, 0L, 0L, "", 0L);
    }

    private static volatile Info last = Info.NONE;
    private static volatile Process process;
    private static volatile long nextStartAttempt;
    private static volatile boolean stopping;

    private WindowsMediaSession() {
    }

    static {
        // Make sure the helper dies with the client instead of lingering.
        Runtime.getRuntime().addShutdownHook(new Thread(WindowsMediaSession::shutdown,
                "octoclient-media-session-shutdown"));
    }

    /** Last known session, starting the background helper on first use. */
    public static Info info() {
        ensureRunning();
        return last;
    }

    /** File the current album artwork was written to, or null. */
    public static Path coverFile() {
        String tmp = System.getProperty("java.io.tmpdir");
        return tmp == null ? null : Path.of(tmp, COVER_FILE);
    }

    private static synchronized void ensureRunning() {
        if (process != null && process.isAlive()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < nextStartAttempt) {
            return;
        }
        nextStartAttempt = now + 10_000L;
        try {
            Path script = extractScript();
            ProcessBuilder builder = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                    "-ExecutionPolicy", "Bypass", "-File", script.toString());
            builder.redirectErrorStream(true);
            Process started = builder.start();
            process = started;
            stopping = false;
            Thread reader = new Thread(() -> readLoop(started), "octoclient-media-session");
            reader.setDaemon(true);
            reader.start();
        } catch (IOException error) {
            process = null;
        }
    }

    private static Path extractScript() throws IOException {
        Path target = Files.createTempFile("octoclient_media_session", ".ps1");
        try (InputStream input = WindowsMediaSession.class.getResourceAsStream("/assets/octoclient/media_session.ps1")) {
            if (input == null) {
                throw new IOException("media_session.ps1 missing from resources");
            }
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
        target.toFile().deleteOnExit();
        return target;
    }

    private static void readLoop(Process started) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(started.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (stopping) {
                    break;
                }
                parse(line.trim());
            }
        } catch (IOException ignored) {
            // Process died; ensureRunning restarts it after the cooldown.
        } finally {
            if (process == started) {
                process = null;
            }
        }
    }

    private static void parse(String line) {
        if (line.isEmpty()) {
            return;
        }
        if (line.startsWith("ERROR:")) {
            System.err.println("[OctoClient] Media-session artwork error: " + line.substring("ERROR:".length()).trim());
            return;
        }
        if (line.equals("NONE")) {
            last = new Info("", "", "", "", false, 0L, 0L, "", System.nanoTime());
            return;
        }
        String[] parts = line.split(String.valueOf(SEP), -1);
        if (parts.length < 7) {
            return;
        }
        long position = parseLong(parts[5]);
        long duration = parseLong(parts[6]);
        String coverKey = parts.length > 7 ? parts[7] : "";
        last = new Info(parts[0], parts[1], parts[2], parts[3],
                parts[4].toLowerCase(Locale.ROOT).contains("playing"), position, duration,
                coverKey, System.nanoTime());
    }

    /** Sends a transport command: {@code playpause}, {@code next} or {@code previous}. */
    public static void control(String action) {
        String command = switch (action) {
            case "next" -> "$null = Await ($s.TrySkipNextAsync()) ([bool])";
            case "previous" -> "$null = Await ($s.TrySkipPreviousAsync()) ([bool])";
            default -> "$null = Await ($s.TryTogglePlayPauseAsync()) ([bool])";
        };
        run(setup() + "$s = $mgr.GetCurrentSession(); if ($null -eq $s) { exit }\n" + command);
    }

    /** Seeks the active session to the given position (in milliseconds). */
    public static void seekTo(long millis) {
        long ticks = Math.max(0L, millis) * 10_000L;
        run(setup() + "$s = $mgr.GetCurrentSession(); if ($null -eq $s) { exit }\n"
                + "$null = Await ($s.TryChangePlaybackPositionAsync(" + ticks + ")) ([bool])");
    }

    private static String setup() {
        return "[Console]::OutputEncoding=[System.Text.Encoding]::UTF8\n"
                + "$ErrorActionPreference='SilentlyContinue'\n"
                + "Add-Type -AssemblyName System.Runtime.WindowsRuntime\n"
                + "$asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | "
                + "Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and "
                + "$_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]\n"
                + "function Await($WinRtTask, $ResultType) { "
                + "$asTask = $asTaskGeneric.MakeGenericMethod($ResultType); "
                + "$netTask = $asTask.Invoke($null, @($WinRtTask)); "
                + "$netTask.Wait(-1) | Out-Null; $netTask.Result }\n"
                + "$null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,"
                + "Windows.Media.Control,ContentType=WindowsRuntime]\n"
                + "$mgr = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]"
                + "::RequestAsync()) "
                + "([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])\n";
    }

    private static void run(String script) {
        ProcessBuilder builder = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                "-ExecutionPolicy", "Bypass", "-Command", script);
        builder.redirectErrorStream(true);
        Process controlProcess = null;
        try {
            controlProcess = builder.start();
            controlProcess.getOutputStream().close();
            if (!controlProcess.waitFor(8, TimeUnit.SECONDS)) {
                controlProcess.destroyForcibly();
            }
        } catch (IOException | InterruptedException error) {
            if (error instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        } finally {
            if (controlProcess != null) {
                controlProcess.destroy();
            }
        }
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException error) {
            return 0L;
        }
    }

    /** Stops the helper when the client shuts down. */
    public static void shutdown() {
        stopping = true;
        Process current = process;
        process = null;
        if (current != null) {
            current.destroy();
        }
    }
}
