"use client";

import type { MouseEvent, ReactNode } from "react";
import Link from "next/link";
import { Button, Tooltip, TooltipContent, TooltipTrigger } from "@open-elements/ui";

type Tone = "default" | "destructive";

type CommonProps = {
  readonly icon: ReactNode;
  readonly tooltip: string;
  readonly tone?: Tone;
};

type AsButtonProps = CommonProps & {
  readonly onClick: (event: MouseEvent<HTMLButtonElement>) => void;
  readonly disabled?: boolean;
  readonly href?: never;
};

type AsLinkProps = CommonProps & {
  readonly href: string;
  readonly onClick?: never;
  readonly disabled?: never;
};

export type TooltipIconButtonProps = AsButtonProps | AsLinkProps;

const TONE_CLASS: Record<Tone, string> = {
  default: "[&_svg]:text-oe-green",
  destructive: "[&_svg]:text-oe-red",
};

export function TooltipIconButton(props: TooltipIconButtonProps) {
  const { icon, tooltip, tone = "default" } = props;
  const className = `[&_svg]:h-4 [&_svg]:w-4 ${TONE_CLASS[tone]}`;

  let trigger: ReactNode;
  if ("href" in props && props.href !== undefined) {
    trigger = (
      <Button
        asChild
        variant="ghost"
        size="icon"
        className={className}
        aria-label={tooltip}
        onClick={(event) => event.stopPropagation()}
      >
        <Link href={props.href}>{icon}</Link>
      </Button>
    );
  } else {
    const button = (
      <Button
        variant="ghost"
        size="icon"
        className={className}
        aria-label={tooltip}
        disabled={props.disabled}
        onClick={(event) => {
          event.stopPropagation();
          props.onClick(event);
        }}
      >
        {icon}
      </Button>
    );
    // Disabled buttons swallow pointer events, so the Tooltip stops working
    // unless we wrap the trigger in a span.
    trigger = props.disabled ? <span>{button}</span> : button;
  }

  return (
    <Tooltip>
      <TooltipTrigger asChild>{trigger}</TooltipTrigger>
      <TooltipContent>{tooltip}</TooltipContent>
    </Tooltip>
  );
}
