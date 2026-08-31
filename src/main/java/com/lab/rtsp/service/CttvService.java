package com.lab.rtsp.service;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class CttvService {

    @Value("${dvr.username}")
    private String dvrUsername;
    @Value("${dvr.password}")
    private String dvrPassword;
    @Value("${dvr.ipaddress}")
    private String dvrIpAddress;

    public byte[] capture(String cameraId) throws Exception {

        String rtspUrl = "rtsp://" + dvrUsername + ":" + dvrPassword + "@" + dvrIpAddress + ":554/Streaming/channels/" + cameraId;

        if (rtspUrl == null) {
            throw new IllegalArgumentException(
                "Camera tidak ditemukan: " + cameraId
            );
        }

        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(rtspUrl);

        // Gunakan TCP untuk RTSP
        grabber.setOption("rtsp_transport", "tcp");

        // Timeout dalam microseconds
        grabber.setOption("stimeout", "5000000");

        try {
            grabber.start();

            Java2DFrameConverter converter = new Java2DFrameConverter();

            Frame frame;

            // Kadang frame pertama belum berisi image.
            // Coba beberapa frame.
            for (int i = 0; i < 10; i++) {

                frame = grabber.grabImage();

                if (frame != null) {

                    BufferedImage image = converter.convert(frame);

                    if (image != null) {
                        return toJpeg(image);
                    }
                }
            }

            throw new IOException(
                "Tidak berhasil mendapatkan frame dari CCTV"
            );

        } finally {
            try {
                grabber.stop();
            } catch (Exception ignored) {
            }

            try {
                grabber.release();
            } catch (Exception ignored) {
            }
        }
    }

    private byte[] toJpeg(BufferedImage image) throws IOException {

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        ImageIO.write(image, "jpg", output);

        return output.toByteArray();
    }

}
