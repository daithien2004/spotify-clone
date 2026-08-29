#!/bin/bash
# block-hardcoded-hex.sh
# Hard gate for Claude Code (PreToolUse hook on the Write / Edit tools).
# Denies writing hardcoded color-hex arbitrary classes (text-[#XXXXXX])
# into app/frontend source files when a design token already answers that
# exact hex — forcing the model to use `bg-*`/`text-*`/`border-*` utilities
# mapped from frontend/tokens.css via @theme inline.
#
# Wired from .claude/settings.json -> hooks.PreToolUse (matcher: Write|Edit).
# Hook input JSON is read on stdin; python (no jq) decodes + checks.
# Deny (exit 2) with a token hint on match; exit 0 (allow) otherwise.
#
# Exempt by design: *.md, .claude/** (docs/skills), globals.css, tokens.css
# (token definitions legitimately contain hex), and any hex that has no
# matching design token yet (suggest adding a token instead of blocking).

input=$(cat 2>/dev/null)
[ -z "$input" ] && exit 0

tmp=$(mktemp 2>/dev/null) || exit 0
printf '%s' "$input" > "$tmp"

python - "$tmp" <<'PY'
import json, os, re, sys

with open(sys.argv[1], encoding='utf-8') as f:
    raw = f.read()

try:
    payload = json.loads(raw)
    tool_input = payload.get('tool_input', {})
    fpath = tool_input.get('file_path', '') or ''
    content = tool_input.get('content', '') or ''
except Exception:
    sys.exit(0)

if not fpath or not content:
    sys.exit(0)

# --- Scope: only frontend app source files ----------------------------
path = fpath.replace('\\', '/')
path = path.split('/frontend/', 1)[-1]  # normalize repo-relative
base = os.path.basename(path).lower()

if not (path.startswith(('app/', 'components/', 'hooks/', 'lib/', 'services/'))) \
        or not (path.endswith(('.tsx', '.ts', '.css'))):
    sys.exit(0)
if base in {'globals.css', 'tokens.css'}:
    sys.exit(0)

def deny(reason):
    msg = reason.replace('"', '\\"')
    payload = ('{"hookSpecificOutput":{"hookEventName":"PreToolUse",'
               '"permissionDecision":"deny",'
               '"permissionDecisionReason":"%s"}}' % msg)
    print(payload, file=sys.stdout)
    print(payload, file=sys.stderr)
    sys.exit(2)

# --- Design-token hex map (from frontend/tokens.css) -------------------
# hex -> the semantic utility to use instead of the arbitrary value.
TOKENS = {
    '#000000': 'bg-background / text-text-* (canvas)',
    '#111111': 'bg-bg-muted',
    '#111212': 'bg-bg-primary',
    '#121212': 'bg-bg-elevated',
    '#131313': 'bg-bg-secondary',
    '#1e1e1e': 'bg-bg-tertiary',
    '#101111': 'bg-bg-deep',
    '#fefeff': 'bg-surface-primary',
    '#feffff': 'bg-surface-secondary',
    '#242424': 'text-text-on-white / surface-foreground',
    '#dfdfdf': 'text-text-primary',
    '#e3e3e3': 'text-text-secondary',
    '#e4e4e4': 'text-text-strong',
    '#c2c2c2': 'text-text-soft',
    '#868686': 'text-text-muted',
    '#8d8d8d': 'text-text-hint',
    '#3d3d3d': 'text-text-on-white-sub',
    '#414141': 'text-text-on-white-soft',
    '#1dd760': 'bg-accent-primary / text-accent-primary',
    '#073417': 'text-accent-primary-foreground',
    '#e2d9ee': 'text-accent-secondary',
    '#edd9eb': 'text-accent-tertiary',
    '#020202': 'border-feed',
}

HEX_RE = re.compile(
    r'(?:text|bg|border|ring|from|to|via|decoration|fill|stroke|outline)'
    r'(?:-color)?-\[(#[0-9a-fA-F]{3,8})(?:/[0-9]{1,3})?\]'
)

for m in HEX_RE.finditer(content):
    hexv = m.group(1).lower()
    if hexv in TOKENS:
        deny(
            f"Hardcoded color '{hexv}' -> use '{TOKENS[hexv]}' instead. "
            "Design tokens from frontend/tokens.css only (no arbitrary-hex "
            "Tailwind classes). See .claude/skills/frontend-conventions."
        )

sys.exit(0)
PY
code=$?
rm -f "$tmp"
exit $code