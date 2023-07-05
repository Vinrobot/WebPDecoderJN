package webpdecoderjn.internal;

import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

public interface LibWebP extends Library {
    int WEBP_DEMUX_ABI_VERSION = 0x0107;

    /*
    [webp/types.h]
        // Allocates 'size' bytes of memory. Returns NULL upon error. Memory
        // must be deallocated by calling WebPFree(). This function is made available
        // by the core 'libwebp' library.
        WEBP_EXTERN void* WebPMalloc(size_t size);
    */
    Pointer WebPMalloc(int size);

    /*
    [webp/types.h]
        // Releases memory returned by the WebPDecode*() functions (from decode.h).
        WEBP_EXTERN void WebPFree(void* ptr);
    */
    void WebPFree(Pointer pointer);

    /*
    [webp/demux.h]
        // Internal, version-checked, entry point.
        WEBP_EXTERN WebPAnimDecoder* WebPAnimDecoderNewInternal(
            const WebPData*, const WebPAnimDecoderOptions*, int);

        // Creates and initializes a WebPAnimDecoder object.
        // Parameters:
        //   webp_data - (in) WebP bitstream. This should remain unchanged during the
        //                    lifetime of the output WebPAnimDecoder object.
        //   dec_options - (in) decoding options. Can be passed NULL to choose
        //                      reasonable defaults (in particular, color mode MODE_RGBA
        //                      will be picked).
        // Returns:
        //   A pointer to the newly created WebPAnimDecoder object, or NULL in case of
        //   parsing error, invalid option or memory error.
        static WEBP_INLINE WebPAnimDecoder* WebPAnimDecoderNew(
            const WebPData* webp_data, const WebPAnimDecoderOptions* dec_options) {
          return WebPAnimDecoderNewInternal(webp_data, dec_options,
                                            WEBP_DEMUX_ABI_VERSION);
        }
    */
    Pointer WebPAnimDecoderNewInternal(WebPData webp_data, Structure dec_options, int version);

    /*
    [webp/demux.h]
        // Get global information about the animation.
        // Parameters:
        //   dec - (in) decoder instance to get information from.
        //   info - (out) global information fetched from the animation.
        // Returns:
        //   True on success.
        WEBP_EXTERN int WebPAnimDecoderGetInfo(const WebPAnimDecoder* dec,
                                               WebPAnimInfo* info);
    */
    int WebPAnimDecoderGetInfo(Pointer dec, WebPAnimInfo info);

    /*
    [webp/demux.h]
        // Check if there are more frames left to decode.
        // Parameters:
        //   dec - (in) decoder instance to be checked.
        // Returns:
        //   True if 'dec' is not NULL and some frames are yet to be decoded.
        //   Otherwise, returns false.
        WEBP_EXTERN int WebPAnimDecoderHasMoreFrames(const WebPAnimDecoder* dec);
    */
    int WebPAnimDecoderHasMoreFrames(Pointer dec);

    /*
    [webp/demux.h]
        // Fetch the next frame from 'dec' based on options supplied to
        // WebPAnimDecoderNew(). This will be a fully reconstructed canvas of size
        // 'canvasWidth * 4 * canvasHeight', and not just the frame sub-rectangle. The
        // returned buffer 'buf' is valid only until the next call to
        // WebPAnimDecoderGetNext(), WebPAnimDecoderReset() or WebPAnimDecoderDelete().
        // Parameters:
        //   dec - (in/out) decoder instance from which the next frame is to be fetched.
        //   buf - (out) decoded frame.
        //   timestamp - (out) timestamp of the frame in milliseconds.
        // Returns:
        //   False if any of the arguments are NULL, or if there is a parsing or
        //   decoding error, or if there are no more frames. Otherwise, returns true.
        WEBP_EXTERN int WebPAnimDecoderGetNext(WebPAnimDecoder* dec,
                                               uint8_t** buf, int* timestamp);
    */
    int WebPAnimDecoderGetNext(Pointer dec, PointerByReference buf, IntByReference timestamp);

    /*
    [webp/demux.h]
        // Deletes the WebPAnimDecoder object.
        // Parameters:
        //   dec - (in/out) decoder instance to be deleted
        WEBP_EXTERN void WebPAnimDecoderDelete(WebPAnimDecoder* dec);
    */
    void WebPAnimDecoderDelete(Pointer dec);
}
