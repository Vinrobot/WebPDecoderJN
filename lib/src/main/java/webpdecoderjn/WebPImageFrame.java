package webpdecoderjn;

import java.awt.image.BufferedImage;

/**
 * A single frame of a decoded image.
 */
public class WebPImageFrame {
    /**
     * The image.
     */
    public final BufferedImage img;

    /**
     * Counted from the start of the animation until when to show the frame
     * (in ms).
     */
    public final int timestamp;

    /**
     * How long to show the frame (in ms).
     */
    public final int delay;

    WebPImageFrame(BufferedImage img, int timestamp, int delay) {
        this.img = img;
        this.timestamp = timestamp;
        this.delay = delay;
    }

    @Override
    public String toString() {
        return String.valueOf(delay);
    }
}
