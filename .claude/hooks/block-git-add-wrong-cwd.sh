#!/bin/bash
# block-git-add-wrong-cwd.sh
# Hard gate for Claude Code (PreToolUse hook on the Bash tool).
# Bash cwd persists between tool calls, so a prior `cd frontend` can leave
# a later `git add frontend/...` running from the wrong directory — the path
# gets double-prefixed (`frontend/frontend/...`) and git fails with
# "pathspec did not match any files". This hook refuses git add/stage when the
# cwd is not the repo root and the command resolves repo-relative paths,
# forcing the agent to cd back to the repo root first.
#
# Wired from .claude/settings.json -> hooks.PreToolUse (matcher: Bash).

input=$(cat 2>/dev/null)
[ -z "$input" ] && exit 0

tmp=$(mktemp 2>/dev/null) || exit 0
printf '%s' "$input" > "$tmp"

python - "$tmp" <<'PY'
import json, os, re, subprocess, sys

with open(sys.argv[1], encoding='utf-8') as f:
    raw = f.read()

try:
    cmd = json.loads(raw).get('tool_input', {}).get('command', '')
    cwd = json.loads(raw).get('cwd') or os.getcwd()
except Exception:
    sys.exit(0)
if not cmd or not re.search(r'\bgit\s+(?:add|stage)\b', cmd):
    sys.exit(0)

def repo_root():
    try:
        r = subprocess.run(
            ['git', '-C', cwd, 'rev-parse', '--show-toplevel'],
            capture_output=True, text=True, timeout=5)
        return r.stdout.strip() if r.returncode == 0 else ''
    except Exception:
        return ''

root = repo_root()
if not root:
    sys.exit(0)  # không trong git repo → để mặc cho git tự báo

def normalize(p):
    return os.path.normcase(os.path.abspath(os.path.expanduser(p)))

if normalize(cwd) == normalize(root):
    sys.exit(0)  # đúng repo root → allow

# Trong cwd sai: chỉ deny khi argument là path repo-relative (bắt đầu bằng
# frontend/ backend/ gateway/ hoặc một thư mục con của root). Các cờ git
# (-u, -A, --all, -h) hoặc path tuyệt đối không phải repo → không đụng.
repo_prefix = normalize(root) + os.sep
prefixes = {normalize(os.path.join(root, d)) + os.sep for d in os.listdir(root)} if os.path.isdir(root) else set()

# Tách path arguments: token không bắt đầu bằng '-' (kể cả -- optional-args)
tokens = [t for t in re.sub(r'\bgit\s+(?:add|stage)\b', ' ', cmd)
          .replace("'", '').replace('"', '')
          .split() if t and not t.startswith('-')]

def is_repo_path(tok):
    t = os.path.normpath(tok)
    # tương đối hoặc bắt đầu bằng prefix của repo → vẫn cần cwd đúng
    if not os.path.isabs(t):
        return True
    return any(t.replace('\\', os.sep).startswith(p) for p in prefixes)

if any(is_repo_path(t) for t in tokens):
    reason = (
        "Bash cwd (%s) != repo root. 'git add %s' would double-prefix the path "
        "and fail ('pathspec did not match'). cd to the repo root first, then re-run."
        % (cwd, tokens[0] if tokens else '<path>'))
    payload = ('{"hookSpecificOutput":{"hookEventName":"PreToolUse",'
               '"permissionDecision":"deny",'
               '"permissionDecisionReason":"%s"}}' % reason)
    print(payload, file=sys.stdout)
    print(payload, file=sys.stderr)
    sys.exit(2)

sys.exit(0)
PY
code=$?
rm -f "$tmp"
exit $code