# Rollback Rehearsal Checklist — spec 115 (`V36` schema move)

Rehearse `rollback.sql` on the **Coolify dev instance** before the production deploy of v1.11.0.
This proves the rollback **mechanics**; it does **not** prove migration duration or behaviour against
production row counts (dev holds independent throwaway data, not a production copy).

The related documents:

- `rollback.sql` — the canonical procedure and post-`COMMIT` verification queries (this is the runbook).
- `design.md` → *Deployment → Rollback* — why each step exists.
- `docs/releases/v1.11.0.md` → *Rollback* — the operator-facing version.
- `behaviors.md` → *Rollback* — the scenarios, including the failure this rehearsal guards against.

## Prerequisites

- [ ] Dev Coolify instance running open-crm.
- [ ] Owner-role access to the dev PostgreSQL database (role `opencrm`, DB `opencrm` — the same role
      that runs migrations, so no extra `CREATE SCHEMA` / `USAGE` grants are needed).
- [ ] The pre-115 app image (**v1.10.0**) available to roll back to in Coolify.
- [ ] A shell with `psql` access to the dev DB, e.g. into the container:
      `docker exec -it <dev-db-container> psql -U opencrm -d opencrm`

## Setup — reach the post-migration state

- [ ] Deploy the `feat/115-dependency-updates` (v1.11.0) image to dev; Flyway applies `V36` on boot.
- [ ] Confirm the migrated state and note the baseline row counts:
      ```sql
      SELECT table_name FROM information_schema.tables
        WHERE table_schema = 'oe_spring_services' ORDER BY 1;   -- expect the 7 tables
      SELECT count(*) AS users FROM oe_spring_services.users;    -- note this number
      SELECT count(*) AS tags  FROM oe_spring_services.tags;     -- note this number
      ```

## Rehearse the rollback

- [ ] In Coolify, roll the app image back to **v1.10.0**. Do **not** let it boot yet — or accept that
      its first boot fails until the next step runs (the old jar has no `V36__*.sql`, so Flyway
      validation aborts until the history row is removed).
- [ ] Run `rollback.sql` as the owner role:
      ```bash
      docker exec -i <dev-db-container> psql -U opencrm -d opencrm \
        < docs/specs/115-dependency-updates/rollback.sql
      ```
      It runs in one `BEGIN…COMMIT`: moves the 7 tables back to `public`,
      `DROP SCHEMA oe_spring_services RESTRICT`, and deletes the `V36` history row.
- [ ] Run the three verification queries (also at the bottom of `rollback.sql`):
      ```sql
      SELECT table_name FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name IN ('users','api_keys','audit_log','comments','settings','tags','webhooks');
      -- expect 7 rows
      SELECT count(*) FROM information_schema.schemata WHERE schema_name = 'oe_spring_services';
      -- expect 0
      SELECT count(*) FROM public.flyway_schema_history WHERE version = '36';
      -- expect 0
      ```
- [ ] Re-check the baseline counts against `public.users` / `public.tags` — must be unchanged (the
      data followed the tables back).
- [ ] Boot **v1.10.0** and confirm it starts (Flyway validation passes).
- [ ] Smoke checks on the rolled-back app: login, global search, create/read a comment, audit log,
      `/admin/backup`, and MCP (if enabled on dev).

## Optional — rehearse the trap (recommended, ~2 min)

Prove the failure the procedure exists to avoid:

- [ ] Re-apply `V36` (redeploy v1.11.0), then move the tables back to `public` **without** deleting the
      `V36` history row (skip step 3 of `rollback.sql`).
- [ ] Boot v1.10.0 → confirm it aborts with
      **"Detected applied migration not resolved locally: 36"** and does not start.
- [ ] Delete the `V36` row and confirm it then boots. This makes step 3 of `rollback.sql`
      un-skippable in everyone's memory.

## Cautions

- **`DROP SCHEMA … RESTRICT` is intentional.** If anything unexpected still lives in
  `oe_spring_services`, the drop fails and the whole transaction rolls back rather than dropping data.
  Investigate what is in the schema before considering any forced drop.
- In a **real production rollback**, restoring the pre-migration dump is the last resort — it discards
  anything written after the backup. The rehearsal is about never needing that.

## Sign-off

- [ ] Rehearsal completed on dev, all verification queries returned the expected results.
- [ ] Rehearsed by: __________________  Date: __________
