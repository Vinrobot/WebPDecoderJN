package net.vinrobot.imageio.plugins.webp;

import webpdecoderjn.internal.LibWebP;
import webpdecoderjn.internal.WebPAnimDecoder;
import webpdecoderjn.internal.WebPAnimInfo;
import webpdecoderjn.internal.WebPData;
import webpdecoderjn.internal.WebPFrame;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class WebPImageReader extends ImageReader {
	private static final int DEFAULT_BUFFER_SIZE = 8192;
	private static final int EOF = -1;

	private final List<Frame> frames = new ArrayList<>();
	private final LibWebP lib;

	private WebPData webpData;
	private WebPAnimDecoder webpAnimDecoder;
	private WebPAnimInfo webpAnimInfo;

	WebPImageReader(final WebPImageReaderSpi originatingProvider, final LibWebP lib) {
		super(originatingProvider);
		this.lib = Objects.requireNonNull(lib, "lib == null");
	}

	@Override
	public void setInput(final Object input, final boolean seekForwardOnly, final boolean ignoreMetadata) {
		super.setInput(input, seekForwardOnly, ignoreMetadata);
		this.resetInternalState();
	}

	private static byte[] readAll(final ImageInputStream stream) throws IOException {
		try (final ByteArrayOutputStream result = new ByteArrayOutputStream()) {
			final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
			int length;
			while ((length = stream.read(buffer)) != EOF) {
				result.write(buffer, 0, length);
			}
			return result.toByteArray();
		}
	}

	private byte[] getData() throws IOException {
		final Object input = this.getInput();
		if (input instanceof ImageInputStream) {
			final ImageInputStream stream = (ImageInputStream) input;
			stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
			return readAll(stream);
		} else if (input instanceof byte[]) {
			return (byte[]) input;
		} else {
			throw new IllegalStateException("Unsupported input type: " + input.getClass().getName());
		}
	}

	private WebPData getWebPData() throws IOException {
		if (this.webpData == null) {
			final byte[] data = Objects.requireNonNull(this.getData(), "data == null");
			if (data.length == 0) {
				throw new IllegalArgumentException("data.length == 0");
			}
			this.webpData = new WebPData(this.lib, data);
		}
		return this.webpData;
	}

	private WebPAnimDecoder getWebPAnimDecoder() throws IOException {
		if (this.webpAnimDecoder == null) {
			final WebPData webpData = Objects.requireNonNull(this.getWebPData(), "webpData == null");
			this.webpAnimDecoder = new WebPAnimDecoder(this.lib, webpData);
		}
		return this.webpAnimDecoder;
	}

	private WebPAnimInfo getWebPAnimInfo() throws IOException {
		if (this.webpAnimInfo == null) {
			this.webpAnimInfo = this.getWebPAnimDecoder().getInfo();
		}
		return this.webpAnimInfo;
	}

	@Override
	public IIOMetadata getStreamMetadata() {
		return null;
	}

	@Override
	public int getNumImages(final boolean allowSearch) throws IOException {
		if (allowSearch) {
			this.readAllFrames();
			return this.frames.size();
		} else {
			return -1;
		}
	}

	@Override
	public Iterator<ImageTypeSpecifier> getImageTypes(final int imageIndex) {
		return null;
	}

	@Override
	public IIOMetadata getImageMetadata(final int imageIndex) {
		return null;
	}

	@Override
	public int getWidth(final int imageIndex) throws IOException {
		if (imageIndex < 0) {
			throw new IndexOutOfBoundsException("imageIndex < 0");
		}
		final WebPAnimInfo info = this.getWebPAnimInfo();
		if (imageIndex >= info.frameCount()) {
			throw new IndexOutOfBoundsException("imageIndex >= frameCount");
		}
		return info.canvasWidth();
	}

	@Override
	public int getHeight(final int imageIndex) throws IOException {
		if (imageIndex < 0) {
			throw new IndexOutOfBoundsException("imageIndex < 0");
		}
		final WebPAnimInfo info = this.getWebPAnimInfo();
		if (imageIndex >= info.frameCount()) {
			throw new IndexOutOfBoundsException("imageIndex >= frameCount");
		}
		return info.canvasHeight();
	}

	private void readAllFrames() throws IOException {
		try {
			this.read(Integer.MAX_VALUE);
		} catch (final IndexOutOfBoundsException ex) {
			// Ignore
		}
	}

	@Override
	public BufferedImage read(final int imageIndex, final ImageReadParam param) throws IOException {
		if (param != null) {
			throw new UnsupportedOperationException("ImageReadParam not supported");
		}
		if (imageIndex < 0) {
			throw new IndexOutOfBoundsException("imageIndex < 0");
		}
		if (imageIndex < this.frames.size()) {
			return this.frames.get(imageIndex).image;
		}

		final WebPAnimDecoder decoder = this.getWebPAnimDecoder();
		final WebPAnimInfo info = this.getWebPAnimInfo();

		while (decoder.hasMoreFrames()) {
			final WebPFrame frame = decoder.getNext(info);
			final BufferedImage image = createImage(frame.pixels(), info.canvasWidth(), info.canvasHeight());
			this.frames.add(new Frame(image, frame.timestamp()));
			if (this.frames.size() - 1 == imageIndex) {
				return image;
			}
		}

		// Should never happen since we check imageIndex < info.frameCount() unless the webp file is malformed.
		throw new IndexOutOfBoundsException("imageIndex >= frameCount");
	}

	private static BufferedImage createImage(final int[] pixels, final int width, final int height) {
		assert pixels.length == width * height;
		final ColorModel colorModel = new DirectColorModel(32, 0x000000ff, 0x0000ff00, 0x00ff0000, 0xff000000);
		final SampleModel sampleModel = colorModel.createCompatibleSampleModel(width, height);
		final DataBufferInt dataBufferInt = new DataBufferInt(pixels, width * height);
		final WritableRaster writableRaster = WritableRaster.createWritableRaster(sampleModel, dataBufferInt, null);
		return new BufferedImage(colorModel, writableRaster, false, null);
	}

	@Override
	public void dispose() {
		super.dispose();
		this.setInput(null);
	}

	private void resetInternalState() {
		this.frames.clear();
		this.webpAnimInfo = null;
		if (this.webpAnimDecoder != null) {
			this.webpAnimDecoder.close();
			this.webpAnimDecoder = null;
		}
		if (this.webpData != null) {
			this.webpData.close();
			this.webpData = null;
		}
	}

	private record Frame(
			BufferedImage image,
			int timestamp
	) {
	}
}
