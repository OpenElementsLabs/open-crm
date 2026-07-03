import type { MetadataRoute } from "next";
import { createManifest } from "@/lib/pwa/create-manifest";

/**
 * App Web Manifest, served at `/manifest.webmanifest`. Built from the generic {@link createManifest}
 * helper with Open CRM branding. English-only (browsers read the manifest once, at install time).
 */
export default function manifest(): MetadataRoute.Manifest {
  return createManifest({
    name: "Open CRM",
    shortName: "Open CRM",
    description: "Customer Relationship Management by Open Elements",
    themeColor: "#ffffff",
    backgroundColor: "#ffffff",
    icons: [
      { src: "/icons/icon-192.png", sizes: "192x192", type: "image/png", purpose: "any" },
      { src: "/icons/icon-512.png", sizes: "512x512", type: "image/png", purpose: "any" },
      { src: "/icons/icon-maskable-512.png", sizes: "512x512", type: "image/png", purpose: "maskable" },
    ],
  });
}
