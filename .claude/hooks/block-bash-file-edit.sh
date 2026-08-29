#!/bin/bash
# block-bash-file-edit.sh
# Hard gate for Claude Code (PreToolUse hook on the Bash tool).
# Refuses bash commands whose purpose is to create or edit project files,
# forcing the model to use the Edit / Write tools instead.
#
# Wired from .claude/settings.json -> hooks.PreToolUse (matcher: Bash).
# The hook input JSON is read on stdin, then handed to a python program
# (no jq dependency) that decodes it and runs the block-pattern checks.
# Deny (exit 2) with a reason on match; exit 0 (allow) otherwise.
#
# Allowed by design: append-redirects (>>) to log files, `2>&1`,
# redirections to /dev/null, and read-only `sed` filters (no -i, no > file).

input=$(cat 2>/dev/null)
[ -z "$input" ] && exit 0

tmp=$(mktemp 2>/dev/null) || exit 0
printf '%s' "$input" > "$tmp"

python - "$tmp" <<'PY'
import re, sys, json

with open(sys.argv[1], encoding='utf-8') as f:
    raw = f.read()

try:
    cmd = json.loads(raw).get('tool_input', {}).get('command', '')
except Exception:
    sys.exit(0)
if not cmd:
    sys.exit(0)

# Neutralize benign redirects so they never trigger the write rules below.
cmd = re.sub(r'\s*[12&]?>\s*/dev/(?:null|stderr|stdout)\b', ' ', cmd)
cmd = re.sub(r'\s*[12]?>&[12]\b', ' ', cmd)

def deny(reason):
    payload = ('{"hookSpecificOutput":{"hookEventName":"PreToolUse",'
               '"permissionDecision":"deny",'
               '"permissionDecisionReason":"%s"}}' % reason)
    print(payload, file=sys.stdout)
    print(payload, file=sys.stderr)
    sys.exit(2)

# A write-redirect is a single '>', never part of an append '>>'.
WRITE = r'(?<!>)>(?!>)'
# A write target is a path-like token (slash, or dot + extension); most
# project files end in a known extension, so this keeps false positives on
# literal '>' characters inside quotes and on '2>&1' styles very low.
TARGET = r'[^;&|]*?' + WRITE + r'\s*\S+?(?:/|\.[A-Za-z0-9])'

# 1. sed in-place (GNU: -i ; BSD: -i '' / -i.bak)
if re.search(r'\bsed\b[\s\S]*?\s-i\b', cmd):
    deny("Bash 'sed -i' edits files in place. Use the Edit tool instead.")

# 2. sed substitution program wired to a file write (redirect to a real file).
#    Read-only filters like 'sed -n 's/x/y/p' file' or 'sed 's/x/y/' file'
#    (no redirect) are NOT blocked here.
if re.search(r'\bsed\b[\s\S]*?s/[\s\S]*?' + TARGET, cmd):
    deny("Bash 'sed <program> ... > file' rewrites a file. Use the Edit tool instead.")

# 3. here-doc writing to a file (cat <<EOF > file, tee <<-EOT >> file, ...)
if re.search(r'<<[^\n]*?(?<!>)>(?!>)\s*\S', cmd, re.M):
    deny("Bash here-doc '<command> <<EOF > file' writes a file. Use the Write tool instead.")

# 4. cat / printf writing to a path-like target
if re.search(r'\b(?:cat|printf)\b' + TARGET, cmd):
    deny("Bash 'cat > file' / 'printf > file' writes a file. Use the Write tool instead.")

# 5. echo writing to a path-like target
if re.search(r'\becho\b' + TARGET, cmd):
    deny("Bash 'echo > file' writes a file. Use the Write tool instead.")

# 6. mv a /tmp/*.tmp file onto a project file (the sed -> mv write detour)
if re.search(r"\bmv\b[ \t]+[\"']?/tmp/[^\"';]*\.tmp[\"']?[ \t]+[\"']?\S", cmd):
    deny("Bash 'mv /tmp/*.tmp <target>' is a file-write detour. Use the Write tool instead.")

sys.exit(0)
PY
code=$?
rm -f "$tmp"
exit $code