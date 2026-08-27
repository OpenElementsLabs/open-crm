import { ImageResponse } from "next/og";
import { OgImageFrame } from "@/components/og/og-image-frame";
import { getCompanyLogoDataUri } from "@/lib/api";
import { getCachedCompany } from "@/lib/cached-entities";
import { companyImageModel } from "@/lib/metadata/entity";

export const dynamic = "force-dynamic";

export const alt = "Open CRM company preview";
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

interface ImageRouteProps {
  readonly params: Promise<{ id: string }>;
}

export default async function CompanyOpengraphImage({ params }: ImageRouteProps) {
  const { id } = await params;
  const company = await getCachedCompany(id).catch(() => null);
  // Only fetch the logo when the entity says it has one; a failed/non-image fetch returns null and
  // the frame falls back to initials. A missing entity renders the neutral fallback frame.
  const imageSrc = company?.hasLogo ? await getCompanyLogoDataUri(id) : null;
  const model = companyImageModel(company, imageSrc);
  return new ImageResponse(<OgImageFrame model={model} />, size);
}
