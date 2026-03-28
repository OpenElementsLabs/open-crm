package com.openelements.crm;

/**
 * Holds binary image data together with its MIME content type.
 *
 * @param data        the image bytes
 * @param contentType the MIME content type (e.g. "image/png")
 */
public record ImageData(byte[] data, String contentType) {
}
