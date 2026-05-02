"use client";

import { Mail } from "lucide-react";
import { ActionIconButton } from "@/components/action-icon-button";

type MailtoButtonProps = {
  readonly email: string;
  readonly title?: string;
};

export function MailtoButton({ email, title }: MailtoButtonProps) {
  return (
    <ActionIconButton onClick={() => { window.location.href = `mailto:${email}`; }} title={title}>
      <Mail />
    </ActionIconButton>
  );
}
