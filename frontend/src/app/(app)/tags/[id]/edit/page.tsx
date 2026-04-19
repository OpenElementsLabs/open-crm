import { notFound } from "next/navigation";
import { getTag } from "@/lib/api";
import { EditTagClient } from "./edit-tag-client";

export default async function EditTagPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  let tag;
  try {
    tag = await getTag(id);
  } catch {
    notFound();
  }
  return <EditTagClient tag={tag} />;
}
