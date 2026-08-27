import type { OgImageModel } from "@/lib/metadata/entity";

/**
 * The shared 1200×630 Open Graph frame rendered by `next/og`'s `ImageResponse` (satori) for both the
 * contact and company image routes. Not a DOM component — it is handed to `ImageResponse`, which
 * supports only a flexbox subset of CSS, so every container declares `display: flex` and all colours
 * are literal Open Elements brand hexes (satori cannot resolve CSS variables).
 *
 * A fixed square portrait area centre-crops any source image (`objectFit: cover`) so a tall photo, a
 * wide logo and a square avatar all yield the same output. When `imageSrc` is null the initials are
 * shown on the same brand surface, keeping the two variants visually consistent.
 */

// Open Elements brand palette (mirrors --color-oe-* in @open-elements/ui brand.css).
const OE_DARK = "#020144";
const OE_GRAY_LIGHT = "#e8e6dc";
const OE_GREEN = "#5cba9e";
const OE_WHITE = "#ffffff";

const PORTRAIT_SIZE = 400;

export function OgImageFrame({ model }: { readonly model: OgImageModel }) {
  return (
    <div
      style={{
        display: "flex",
        width: "100%",
        height: "100%",
        background: OE_DARK,
        color: OE_WHITE,
        padding: "64px",
        fontFamily: "sans-serif",
      }}
    >
      <div
        style={{
          display: "flex",
          flexDirection: "column",
          justifyContent: "space-between",
          width: "100%",
          height: "100%",
        }}
      >
        <div style={{ display: "flex", flexDirection: "row", alignItems: "center", gap: "56px" }}>
          <div
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              width: `${PORTRAIT_SIZE}px`,
              height: `${PORTRAIT_SIZE}px`,
              flexShrink: 0,
              borderRadius: "36px",
              background: OE_GRAY_LIGHT,
              overflow: "hidden",
            }}
          >
            {model.imageSrc ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={model.imageSrc}
                width={PORTRAIT_SIZE}
                height={PORTRAIT_SIZE}
                style={{ width: "100%", height: "100%", objectFit: "cover" }}
                alt=""
              />
            ) : (
              <div style={{ display: "flex", fontSize: 180, fontWeight: 700, color: OE_DARK }}>
                {model.initials}
              </div>
            )}
          </div>
          <div style={{ display: "flex", flexDirection: "column", maxWidth: "620px" }}>
            <div style={{ display: "flex", fontSize: 68, fontWeight: 700, lineHeight: 1.1 }}>
              {model.name}
            </div>
            {model.secondaryLine ? (
              <div style={{ display: "flex", fontSize: 36, color: OE_GRAY_LIGHT, marginTop: "20px" }}>
                {model.secondaryLine}
              </div>
            ) : null}
          </div>
        </div>
        <div style={{ display: "flex", flexDirection: "column" }}>
          <div style={{ display: "flex", width: "128px", height: "8px", background: OE_GREEN, marginBottom: "16px" }} />
          <div style={{ display: "flex", fontSize: 34, fontWeight: 700, color: OE_WHITE }}>Open CRM</div>
        </div>
      </div>
    </div>
  );
}
