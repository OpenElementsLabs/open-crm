import { notFound } from "next/navigation";
import { TaskDetail } from "@/components/task-detail";
import { getTask } from "@/lib/api";

export const dynamic = "force-dynamic";

interface TaskDetailPageProps {
  readonly params: Promise<{ id: string }>;
}

export default async function TaskDetailPage({ params }: TaskDetailPageProps) {
  const { id } = await params;

  try {
    const task = await getTask(id);
    return <TaskDetail task={task} />;
  } catch {
    notFound();
  }
}
