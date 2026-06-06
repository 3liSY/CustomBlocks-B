/**
 * VideoDecoder.java
 *
 * Responsibility: Extract individual frames from MP4/H264 video files as
 * block-sized PNG textures, using jcodec's pure-Java decoder.
 * Place video files in config/customblocks/videos/.
 *
 * Depends on: jcodec 0.2.5 (core + javase), ImageProcessor, CustomBlocksConfig
 * Called by: VideoCommands
 */
package com.customblocks.video;

import com.customblocks.CustomBlocksConfig;
import com.customblocks.image.ImageProcessor;
import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public final class VideoDecoder {

    private VideoDecoder() {}

    /**
     * Extract the frame at frameIndex (0-based) from an MP4 video, scale it to
     * the configured texture size, and return PNG bytes ready for TextureStore.save().
     *
     * @throws IOException     if the file cannot be read or the frame is out of range
     * @throws JCodecException if jcodec cannot parse the video container
     */
    public static byte[] extractFrameAsPng(File videoFile, int frameIndex)
            throws IOException, JCodecException {
        SeekableByteChannel channel = NIOUtils.readableChannel(videoFile);
        try {
            FrameGrab grab = FrameGrab.createFrameGrab(channel);
            grab.seekToFramePrecise(frameIndex);
            Picture pic = grab.getNativeFrame();
            if (pic == null) {
                throw new IOException("Frame " + frameIndex + " is out of range.");
            }
            BufferedImage img = AWTUtil.toBufferedImage(pic);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", baos);
            try {
                return ImageProcessor.toBlockPng(baos.toByteArray(), CustomBlocksConfig.textureSize);
            } catch (Exception e) {
                throw new IOException("Frame post-processing failed: " + e.getMessage(), e);
            }
        } finally {
            try { channel.close(); } catch (IOException ignored) {}
        }
    }
}
