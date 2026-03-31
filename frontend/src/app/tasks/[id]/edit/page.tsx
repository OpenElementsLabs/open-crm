import { notFound } from "next/navigation";
import { TaskForm } from "@/components/task-form";
import { getTask } from "@/lib/api";

export const dynamic = "force-dynamic";

interface EditTaskPageProps {
  readonly params: Promise<{ id: string }>;
}

export default async function EditTaskPage({ params }: EditTaskPageProps) {
  const { id } = await params;

  try {
    const task = await getTask(id);
    return <TaskForm task={task} />;
  } catch {
    notFound();
  }
}
