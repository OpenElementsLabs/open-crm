FROM node:24-alpine AS base

# Corepack resolves the pnpm version from package.json -> packageManager (single source of truth)
RUN corepack enable

FROM base AS deps

WORKDIR /app

# pnpm-workspace.yaml carries minimumReleaseAgeExclude + allowBuilds, which the
# install must honor (and which the committed lockfile was resolved against).
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile

FROM base AS build

WORKDIR /app

COPY --from=deps /app/node_modules ./node_modules
COPY . .

ARG BACKEND_URL=http://backend:8080
ENV BACKEND_URL=${BACKEND_URL}

RUN pnpm build

FROM base AS runner

WORKDIR /app

ENV NODE_ENV=production

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /app/public ./public
COPY --from=build --chown=appuser:appgroup /app/.next/standalone ./
COPY --from=build --chown=appuser:appgroup /app/.next/static ./.next/static

USER appuser

EXPOSE 3000

ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

CMD ["node", "server.js"]
