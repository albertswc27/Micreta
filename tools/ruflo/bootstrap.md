# tools/ruflo/

Helper directory para arrancar ruflo en este repo. Ver [`RUFLO_INTEGRATION.md`](../../RUFLO_INTEGRATION.md)
para el contexto completo.

## Bootstrap rápido (Path A — plugins ligeros)

Desde una sesión Claude Code en la raíz del repo:

```
/plugin marketplace add ruvnet/ruflo
/plugin install ruflo-core@ruflo
/plugin install ruflo-sparc@ruflo
/plugin install ruflo-testgen@ruflo
/plugin install ruflo-docs@ruflo
/plugin install ruflo-adr@ruflo
/plugin install ruflo-iot-cognitum@ruflo
/plugin install ruflo-security-audit@ruflo
```

## Bootstrap completo (Path B — daemon + MCP)

PowerShell (Windows nativo):
```powershell
npx ruflo@latest init wizard
```

Bash / WSL:
```bash
curl -fsSL https://cdn.jsdelivr.net/gh/ruvnet/ruflo@main/scripts/install.sh | bash
```

MCP server:
```bash
claude mcp add ruflo -- npx ruflo@latest mcp start
```

## Qué archivos crea ruflo (no tocar a mano)

Después del `init`:

- `.claude/` — agent definitions y hooks.
- `.claude-flow/` — daemon state.
- `CLAUDE.md` — ruflo añade su sección; respeta la que ya tienes arriba.
- `settings.local.json` — preferencias del CLI.
- `helpers/` — scripts auxiliares.

Estos directorios están en `.gitignore` para no contaminar el repo.

## Comandos típicos

Ver `RUFLO_INTEGRATION.md` para la tabla completa. Los más útiles ahora:

- `/sparc start "v0.2.x feature"` — sprint guiado con 5 fases.
- `/testgen target=app/src/main/java/com/micreta/app/data/obd` — JUnit tests.
- `/docs sync` — README + FEATURES + ADR al día.
- `/adr create "..."` — añade decisión arquitectónica.
