"use client";

import type { ComponentProps } from "react";
import { Button } from "@open-elements/ui";

type PrimaryButtonProps = ComponentProps<typeof Button>;

const PRIMARY_CLASS = "bg-oe-green hover:bg-oe-green-dark text-white";

export function PrimaryButton({ className, ...props }: PrimaryButtonProps) {
  return (
    <Button className={className ? `${PRIMARY_CLASS} ${className}` : PRIMARY_CLASS} {...props} />
  );
}
