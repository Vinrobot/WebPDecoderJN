package webpdecoderjn;

import java.awt.Color;
import java.util.List;

/**
 * A decoded image containing the individual frames (for static images just
 * one) and some meta info.
 */
public class WebPImage {
    public final List<WebPImageFrame> frames;
    public final int canvasWidth;
    public final int canvasHeight;
    public final int loopCount;
    public final Color bgColor;
    public final int frameCount;

    WebPImage(List<WebPImageFrame> frames, int canvasWidth, int canvasHeight, int loopCount, Color bgColor, int frameCount) {
        this.frames = frames;
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        this.frameCount = frameCount;
        this.loopCount = loopCount;
        this.bgColor = bgColor;
    }

    @Override
    public String toString() {
        return String.format("%d x %d / %d loops / %d frames %s", canvasWidth, canvasHeight, loopCount, frameCount, frames);
    }
}
