package com.videosite.backend.transcode;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FfmpegCommandBuilderTest {

    @Test
    void buildSnapshotCommandShouldContainExpectedArgs() {
        FfmpegCommandBuilder builder = new FfmpegCommandBuilder();

        List<String> command = builder.buildSnapshotCommand(
                "ffmpeg",
                Paths.get("videos/raw/demo.mp4"),
                Paths.get("images/covers/demo.jpg"),
                3
        );

        assertEquals("ffmpeg", command.get(0));
        assertTrue(command.contains("-ss"));
        assertTrue(command.contains("3"));
        assertTrue(command.contains("-frames:v"));
        assertTrue(command.contains("1"));
        assertTrue(command.contains("-q:v"));
        assertEquals(Paths.get("images/covers/demo.jpg").toString(), command.get(command.size() - 1));
    }

    @Test
    void buildSnapshotCommandShouldClampNegativeSecond() {
        FfmpegCommandBuilder builder = new FfmpegCommandBuilder();

        List<String> command = builder.buildSnapshotCommand(
                "ffmpeg",
                Paths.get("in.mp4"),
                Paths.get("out.jpg"),
                -10
        );

        int index = command.indexOf("-ss");
        assertTrue(index >= 0);
        assertEquals("0", command.get(index + 1));
    }
}
