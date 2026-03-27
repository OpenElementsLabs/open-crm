# Execution Notes

Dieses Dokument sammelt Änderungen und Fixes rund um Start, Konfiguration, Docker und Deployment.

---

## 2026-03-27: BACKEND_URL im Frontend-Docker-Build setzen

**Fehler:**
Das Frontend im Docker-Container konnte das Backend nicht erreichen. Alle API-Aufrufe schlugen mit `ECONNREFUSED` auf `localhost:8080` fehl, obwohl `BACKEND_URL=http://backend:8080` in `docker-compose.yml` als Environment-Variable gesetzt war.

**Was wurde geändert:**
In `frontend/Dockerfile` wurde im Build-Stage ein `ARG` und `ENV` für `BACKEND_URL` ergänzt:

```dockerfile
ARG BACKEND_URL=http://backend:8080
ENV BACKEND_URL=${BACKEND_URL}
```

Diese Zeilen stehen vor `RUN pnpm build`, sodass Next.js die Variable beim Build auswerten kann.

**Begründung:**
Next.js wertet `next.config.ts` (inkl. `rewrites()`) zur **Build-Zeit** aus, nicht zur Laufzeit. Die Rewrite-Regeln werden fest in das Build-Artefakt eingebaut. Eine Environment-Variable, die erst beim Container-Start gesetzt wird, kommt zu spät — `next.config.ts` wurde bereits ausgewertet und der Fallback `localhost:8080` fest verdrahtet. In einem Docker-Netzwerk zeigt `localhost` aber auf den eigenen Container, nicht auf den Backend-Service. Die Lösung über `ARG`/`ENV` im Build-Stage ist die empfohlene Vorgehensweise für Next.js im Standalone-Modus, weil sie sicherstellt, dass die Konfiguration zum Zeitpunkt des Builds verfügbar ist.
