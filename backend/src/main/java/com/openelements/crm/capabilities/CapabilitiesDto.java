package com.openelements.crm.capabilities;

/**
 * Runtime-capability flags reported by {@code GET /api/admin/capabilities} and rendered on the
 * admin status page.
 *
 * <p>Each flag reflects a value probed once at container startup (native libraries do not appear
 * or disappear during a container's lifetime). The record is intended to grow as further optional
 * runtime features are surfaced (e.g. {@code webpAvailable}, {@code pdfRenderingAvailable}).
 *
 * @param heicAvailable whether the runtime can decode HEIC images; mirrors
 *                      {@link com.openelements.crm.contact.CrmHeicSupportCheck#isHeicAvailable()}
 */
public record CapabilitiesDto(boolean heicAvailable) {
}
