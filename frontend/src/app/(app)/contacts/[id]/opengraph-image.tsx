import { ImageResponse } from "next/og";
import { OgImageFrame } from "@/components/og/og-image-frame";
import { getContactPhotoDataUri } from "@/lib/api";
import { getCachedContact } from "@/lib/cached-entities";
import { contactImageModel } from "@/lib/metadata/entity";

export const dynamic = "force-dynamic";

export const alt = "Open CRM contact preview";
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

interface ImageRouteProps {
  readonly params: Promise<{ id: string }>;
}

export default async function ContactOpengraphImage({ params }: ImageRouteProps) {
  const { id } = await params;
  const contact = await getCachedContact(id).catch(() => null);
  // Only fetch the photo when the entity says it has one; a failed/non-image fetch returns null and
  // the frame falls back to initials. A missing entity renders the neutral fallback frame.
  const imageSrc = contact?.hasPhoto ? await getContactPhotoDataUri(id) : null;
  const model = contactImageModel(contact, imageSrc);
  return new ImageResponse(<OgImageFrame model={model} />, size);
}
