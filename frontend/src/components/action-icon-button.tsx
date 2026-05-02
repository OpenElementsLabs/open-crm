"use client";

import type { MouseEvent, ReactNode } from "react";

type Tone = "default" | "success";

type ActionIconButtonProps = {
  readonly onClick: (event: MouseEvent<HTMLButtonElement>) => void;
  readonly children: ReactNode;
  readonly title?: string;
  readonly tone?: Tone;
};

const TONE_CLASS: Record<Tone, string> = {
  default: "text-oe-gray-light hover:text-oe-dark [@media(pointer:coarse)]:text-oe-dark",
  success: "text-oe-green",
};

export function ActionIconButton({
  onClick,
  children,
  title,
  tone = "default",
}: ActionIconButtonProps) {
  return (
    <button
      type="button"
      title={title}
      aria-label={title}
      onClick={(event) => {
        event.stopPropagation();
        onClick(event);
      }}
      className={`transition-colors [&_svg]:h-3.5 [&_svg]:w-3.5 ${TONE_CLASS[tone]}`}
    >
      {children}
    </button>
  );
}
