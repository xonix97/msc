package org.msc.nativeprobe;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/** Executes the packaged Android ELF; no desktop binary or provider account is used. */
@RunWith(AndroidJUnit4.class)
public class NativeFfmpegTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    private String run(String... arguments) throws Exception {
        File executable = new File(context.getApplicationInfo().nativeLibraryDir, "libmsc_ffmpeg.so");
        assertTrue("Packaged native executable is missing", executable.canExecute());
        List<String> command = new ArrayList<>();
        command.add(executable.getAbsolutePath());
        command.addAll(Arrays.asList(arguments));
        File log = new File(context.getCacheDir(), "ffmpeg-test.log");
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log).start();
        boolean finished = process.waitFor(45, TimeUnit.SECONDS);
        if (!finished) process.destroyForcibly();
        String output = new String(Files.readAllBytes(log.toPath()), StandardCharsets.UTF_8);
        assertTrue("FFmpeg timed out: " + output, finished);
        assertEquals(output, 0, process.exitValue());
        return output;
    }

    @Test public void nativeExecutableReportsExpectedVersion() throws Exception {
        String version = run("-version");
        assertTrue(version, version.contains("8.0.1"));
        assertTrue(version, version.contains("--target-os=android"));
    }

    @Test public void wavFlacWavRoundTripPreservesEveryPcmSample() throws Exception {
        byte[] pcm = new byte[44100 * 2];
        ByteBuffer samples = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < 44100; i++) samples.putShort((short) Math.round(12000 * Math.sin(2 * Math.PI * 440 * i / 44100)));
        ByteBuffer wav = ByteBuffer.allocate(44 + pcm.length).order(ByteOrder.LITTLE_ENDIAN);
        wav.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(36 + pcm.length);
        wav.put("WAVEfmt ".getBytes(StandardCharsets.US_ASCII)).putInt(16);
        wav.putShort((short) 1).putShort((short) 1).putInt(44100).putInt(88200).putShort((short) 2).putShort((short) 16);
        wav.put("data".getBytes(StandardCharsets.US_ASCII)).putInt(pcm.length).put(pcm);
        File input = new File(context.getCacheDir(), "original.wav");
        File flac = new File(context.getCacheDir(), "encoded.flac");
        File decoded = new File(context.getCacheDir(), "decoded.wav");
        Files.write(input.toPath(), wav.array());
        run("-hide_banner", "-nostdin", "-loglevel", "error", "-y", "-i", input.toString(), "-c:a", "flac", flac.toString());
        assertArrayEquals("fLaC".getBytes(StandardCharsets.US_ASCII), Arrays.copyOf(Files.readAllBytes(flac.toPath()), 4));
        run("-hide_banner", "-nostdin", "-loglevel", "error", "-y", "-i", flac.toString(), "-c:a", "pcm_s16le", decoded.toString());
        byte[] result = Files.readAllBytes(decoded.toPath());
        ByteBuffer chunks = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals("RIFF", new String(result, 0, 4, StandardCharsets.US_ASCII));
        for (int offset = 12; offset + 8 <= result.length;) {
            int size = chunks.getInt(offset + 4);
            assertTrue("Invalid WAV chunk", size >= 0 && (long) offset + 8 + size <= result.length);
            if (new String(result, offset, 4, StandardCharsets.US_ASCII).equals("data")) {
                assertArrayEquals("Lossless round trip changed PCM", pcm, Arrays.copyOfRange(result, offset + 8, offset + 8 + size));
                return;
            }
            offset += 8 + size + (size & 1);
        }
        fail("Decoded WAV has no audio data");
    }
}
