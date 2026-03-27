# Frontend Notes

## shadcn/ui Theming: Semantic CSS Variables

### Problem

shadcn/ui components reference semantic Tailwind utility classes like `bg-background`, `bg-popover`, `text-foreground`, and `border`. These map to CSS custom properties (e.g., `--color-background`, `--color-popover`). If these variables are not defined in `globals.css`, the computed values are empty — resulting in transparent backgrounds on dialogs, select dropdowns, and other overlays, and browser-default black borders on tables and inputs.

### Solution

All semantic CSS variables must be defined in the `@theme` block of `frontend/src/app/globals.css`, mapped to the Open Elements brand colors. The required tokens are:

| Token | Purpose |
|-------|---------|
| `background` / `foreground` | Main page background and text |
| `card` / `card-foreground` | Card surfaces |
| `popover` / `popover-foreground` | Dialogs, dropdowns, popovers |
| `muted` / `muted-foreground` | Hover states, disabled elements, secondary text |
| `accent` / `accent-foreground` | Highlighted items (e.g., hovered select option) |
| `primary` / `primary-foreground` | Primary action buttons |
| `secondary` / `secondary-foreground` | Secondary action buttons |
| `destructive` / `destructive-foreground` | Delete/error buttons |
| `border` | All component borders (tables, cards, inputs) |
| `input` | Input field borders |
| `ring` | Focus ring indicators |

### Why this matters

- shadcn/ui is designed around these semantic tokens. Without them, components look broken out of the box.
- Defining them globally in one place ensures every current and future shadcn/ui component renders correctly without per-component overrides.
- This is the intended theming approach — see [shadcn/ui Theming docs](https://ui.shadcn.com/docs/theming).

### When adding new shadcn/ui components

New components installed via `npx shadcn@latest add` will work immediately if all semantic tokens are defined. If a new component references a token not yet in `globals.css`, add it there — never hardcode colors in the component file.
