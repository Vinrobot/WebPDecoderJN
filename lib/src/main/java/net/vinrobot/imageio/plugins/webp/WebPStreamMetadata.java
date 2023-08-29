package net.vinrobot.imageio.plugins.webp;

import org.w3c.dom.Node;
import webpdecoderjn.internal.WebPAnimInfo;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;

public final class WebPStreamMetadata extends IIOMetadata {
	public static final String NATIVE_METADATA_FORMAT_NAME = "net_vinrobot_imageio_webp_stream_1.0";
	public static final String NATIVE_METADATA_FORMAT_CLASS_NAME = "net.vinrobot.imageio.plugins.webp.WebPStreamMetadataFormat";

	private final WebPAnimInfo webpAnimInfo;

	WebPStreamMetadata(final WebPAnimInfo webpAnimInfo) {
		super(false, NATIVE_METADATA_FORMAT_NAME, NATIVE_METADATA_FORMAT_CLASS_NAME, null, null);
		this.webpAnimInfo = webpAnimInfo;
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
		documentNode.setAttribute("CanvasWidth", String.valueOf(this.webpAnimInfo.canvasWidth()));
		documentNode.setAttribute("CanvasHeight", String.valueOf(this.webpAnimInfo.canvasHeight()));
		documentNode.setAttribute("LoopCount", String.valueOf(this.webpAnimInfo.loopCount()));
		documentNode.setAttribute("FrameCount", String.valueOf(this.webpAnimInfo.frameCount()));
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
