package net.vinrobot.imageio.plugins.webp;

import org.w3c.dom.Node;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;

public final class WebPImageMetadata extends IIOMetadata {
	public static final String NATIVE_METADATA_FORMAT_NAME = "net_vinrobot_imageio_webp_image_1.0";
	public static final String NATIVE_METADATA_FORMAT_CLASS_NAME = "net.vinrobot.imageio.plugins.webp.WebPImageMetadataFormat";

	private final int delay;
	private final int timestamp;

	WebPImageMetadata(final int delay, final int timestamp) {
		super(false, NATIVE_METADATA_FORMAT_NAME, NATIVE_METADATA_FORMAT_CLASS_NAME, null, null);
		this.delay = delay;
		this.timestamp = timestamp;
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public Node getAsTree(final String formatName) {
		if (formatName.equals(nativeMetadataFormatName)) {
			return getNativeTree();
		} else {
			throw new IllegalArgumentException("Unsupported format name: " + formatName);
		}
	}

	public Node getNativeTree() {
		final IIOMetadataNode documentNode = new IIOMetadataNode("Document");
		documentNode.setAttribute("Delay", String.valueOf(this.delay));
		documentNode.setAttribute("Timestamp", String.valueOf(this.timestamp));
		return documentNode;
	}

	@Override
	public void mergeTree(final String formatName, final Node root) {
		throw new IllegalStateException("Metadata is read-only");
	}

	@Override
	public void reset() {
		throw new IllegalStateException("Metadata is read-only");
	}
}
