#!/usr/bin/env bash
# smoke-auth.sh — end-to-end smoke test của luồng auth + dữ liệu thật qua gateway 9000.
#
#  Subcommands:
#    boot   — docker-compose up (infra + mailpit), cài common-lib, chạy 4 service + gateway
#             (background, env trỏ Mailpit), chờ readiness từng cổng.
#    run    — thực thi luồng smoke (giả định stack đã up). Trả exit code = số FAIL.
#    stop   — kill các service đã launch bởi boot (không chạm docker-compose).
#    all    — boot + run + stop (mặc định).
#
#  Không yêu cầu jq / openssl. Phụ thuộc: bash, curl, python3 (JSON), node (TOTP RFC 6238).
#  Docker Desktop phải đang chạy trước khi gọi `boot`.
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GW="http://localhost:9000/api/v1"
MP="http://localhost:8025/api/v1"
LOG_DIR="$ROOT/scripts/.smoke-logs"
PID_FILE="$ROOT/scripts/.smoke-pids"
JAR="$(mktemp)"
TMP_BODY="$(mktemp)"
TMP_HDR="$(mktemp)"
PASS=0
FAIL=0
PY=python
command -v python3 >/dev/null 2>&1 && PY=python3

step() { printf '\n### %s\n' "$*"; }
ok()   { PASS=$((PASS + 1)); printf '  \033[32mPASS\033[0m  %s\n' "$*"; }
bad()  { FAIL=$((FAIL + 1)); printf '  \033[31mFAIL\033[0m  %s\n' "$*"; }

# ---- JSON helper (python, không cần jq). Hỗ trợ list index (data.0.id) ----
json_get() {
  "$PY" - "$1" "$2" <<'PY'
import json, sys
try:
    d = json.loads(sys.argv[1])
except Exception:
    print(""); sys.exit(0)
for p in sys.argv[2].split('.'):
    if isinstance(d, list) and p.isdigit():
        d = d[int(p)]
    elif isinstance(d, dict) and p in d:
        d = d[p]
    else:
        print(""); sys.exit(0)
print("" if d is None else d)
PY
}

# ---- HTTP helper: qua gateway, giữ cookie jar. Set HTTP_STATUS + BODY ----
HTTP_STATUS=""
BODY=""
request() {
  # request <method> <path> [json-body] [extra-curl-args...]
  local method=$1 path=$2 body=${3:-}
  shift 3
  HTTP_STATUS=$(curl -s -o "$TMP_BODY" -w '%{http_code}' -D "$TMP_HDR" \
    -c "$JAR" -b "$JAR" -X "$method" "$@" "$GW$path")
  BODY=$(cat "$TMP_BODY")
}

wait_port() {
  # wait_port <port> [path] [timeout_s] — port "up" = curl nhận được bất kỳ HTTP code
  local port=$1 path=${2:-/} timeout=${3:-240} i code
  for i in $(seq 1 "$timeout"); do
    code=$(curl -s -o /dev/null --max-time 2 -w '%{http_code}' "http://localhost:$port$path" 2>/dev/null || echo 000)
    [ "$code" != "000" ] && return 0
    sleep 1
  done
  return 1
}

# ============================================================ infra ==========
docker_compose() {
  if command -v docker-compose >/dev/null 2>&1; then
    docker-compose "$@"
  else
    docker compose "$@"
  fi
}

boot_infra() {
  step "Infra: docker-compose up (DBs, minio, elasticsearch, redis, kafka, mailpit)"
  if ! docker info >/dev/null 2>&1; then
    echo "  Docker Desktop chưa chạy — khởi động rồi gọi lại."
    return 1
  fi
  (cd "$ROOT/backend" && docker_compose up -d) || return 1
  echo "  Chờ mailpit (:8025)..."
  wait_port 8025 "" 60 || { echo "  mailpit không lên."; return 1; }
  echo "  Chờ Elasticsearch (:9200)... (lần đầu kéo image + bootstrap lâu)"
  wait_port 9200 "" 180 || echo "  (elasticsearch chưa kịp lên — search smoke có thể fail)"
}

find_mvn() {
  # Tìm mvn.cmd hợp lệ trong Maven wrapper dist (~/.m2) — validate từng dist
  # bằng `mvn -v` (bỏ dist rác/thiếu). Lý do: bash/MSYS không chạy được
  # 'mvnw.cmd' (cmd //c báo "not recognized"), mà quoting 'cmd //c' qua MSYS
  # cũng hỏng — đường chạy ổn định là python subprocess.
  "$PY" - <<'PY'
import os, subprocess, sys
base = os.path.join(os.environ["USERPROFILE"], ".m2", "wrapper", "dists")
if not os.path.isdir(base):
    sys.exit(1)
cands = []
for dd in os.listdir(base):
    d = os.path.join(base, dd)
    if not (os.path.isdir(d) and dd.startswith("apache-maven-")):
        continue
    for sd in os.listdir(d):
        cand = os.path.join(d, sd, "bin", "mvn.cmd")
        if os.path.isfile(cand):
            cands.append(cand)
for cand in cands:
    try:
        r = subprocess.run([cand, "-v"], capture_output=True, timeout=60)
    except Exception:
        continue
    if r.returncode == 0:
        print(cand)
        sys.exit(0)
sys.exit(1)
PY
}

start_service() {
  # start_service <log-name> <maven-dir> [maven-args...] — spawn `spring-boot:run`
  # nền qua python Popen (không qua cmd //c), ghi PID để stop_services kill cả cây.
  local log=$1
  shift
  local dir=$1
  shift
  "$PY" - "$LOG_DIR" "$log" "$PID_FILE" "$ROOT/$dir" "$MVN" "$@" <<'PY'
import os, subprocess, sys
logdir, logname, pidfile, cwd = sys.argv[1:5]
mvn, args = sys.argv[5], sys.argv[6:]
os.makedirs(logdir, exist_ok=True)
f = open(os.path.join(logdir, logname + ".log"), "wb", buffering=0)
p = subprocess.Popen([mvn, *args] + ["spring-boot:run"], cwd=cwd,
                     stdout=f, stderr=subprocess.STDOUT)
with open(pidfile, "a") as pf:
    pf.write(str(p.pid) + "\n")
print(f"  launch {logname} → logs/{logname}.log (pid {p.pid})")
sys.exit(0)
PY
}

boot_services() {
  [ -d "$LOG_DIR" ] || mkdir -p "$LOG_DIR"
  : >"$PID_FILE"

  # env smoke: mail → Mailpit local (STARTTLS tắt); còn lại đọc từ backend/.env
  set -a
  [ -f "$ROOT/backend/.env" ] && . "$ROOT/backend/.env"
  export MAIL_HOST=localhost
  export MAIL_PORT=1025
  export MAIL_USERNAME=mailpit
  export MAIL_PASSWORD=mailpit
  export MAIL_STARTTLS_ENABLE=false
  export MAIL_STARTTLS_REQUIRED=false
  set +a

  MVN=$(find_mvn) || { echo "  Không tìm thấy Maven dist — chạy './mvnw' trước."; return 1; }

  echo "  Cài common-lib vào ~/.m2 (service chạy standalone, không reactor)..."
  "$PY" - "$ROOT/backend" "$MVN" <<'PY'
import subprocess, sys
cwd, mvn = sys.argv[1], sys.argv[2]
sys.exit(subprocess.run([mvn, "-pl", "common-lib", "install", "-q"], cwd=cwd).returncode)
PY
  if [ $? -ne 0 ]; then echo "  common-lib install FAIL"; return 1; fi

  start_service auth     backend -pl auth-service
  start_service playlist backend -pl playlist-service
  start_service track    backend -pl track-service
  start_service search   backend -pl search-service
  start_service gateway  gateway

  echo "  Chờ readiness (4 service + gateway, mỗi port tối đa 240s):"
  for port in 8081 8084 8085 8086 9000; do
    if wait_port "$port"; then
      echo "    OK  :$port up"
    else
      echo "    Fail :$port không lên — xem logs/$log.log"
    fi
  done
}

stop_services() {
  [ -f "$PID_FILE" ] || return 0
  local pid
  while read -r pid; do
    [ -z "$pid" ] && continue
    kill "$pid" 2>/dev/null || true
    taskkill //F //T //PID "$pid" >/dev/null 2>&1 || true
  done <"$PID_FILE"
  : >"$PID_FILE"
  echo "  Đã kill các service launch bởi boot."
}

# ============================================================ mailpit ========
# mail_token <to> <marker(verify-email|reset-password)> — trích token từ link gần nhất
mail_token() {
  local res to=$1 marker=$2
  res=$(curl -s "$MP/search?query=href" || true)
  "$PY" - "$res" "$to" "$marker" <<'PY'
import json, re, sys
try:
    data = json.loads(sys.argv[1])
except Exception:
    sys.exit(1)
to, marker = sys.argv[2], sys.argv[3]
for msg in data.get('searchResults', []):
    addrs = {a.get('Address', '') for a in msg.get('To', [])}
    if to not in addrs:
        continue
    m = re.search(r'href="[^"]*' + re.escape(marker) + r'\?token=([0-9a-fA-F-]{36})', msg.get('HTML') or '')
    if m:
        print(m.group(1))
        sys.exit(0)
sys.exit(1)
PY
}

poll_token() {
  # poll_token <to> <marker> [timeout_s] — email gửi async (@Async), nên retry
  local to=$1 marker=$2 timeout=${3:-30} i tok
  for i in $(seq 1 "$timeout"); do
    tok=$(mail_token "$to" "$marker") && { echo "$tok"; return 0; }
    sleep 1
  done
  return 1
}

# ============================================================ flow ===========
run_smoke() {
  EMAIL1="smoke.$(date +%s).a@example.com"
  EMAIL2="smoke.$(date +%s).b@example.com"
  PWD1="SmokePass2026!x"
  PWD2="SmokeReset2026!x"
  T1="20000000-0000-4000-8000-000000000001"
  T2="20000000-0000-4000-8000-000000000002"
  jar_has() { grep -q "$1" "$JAR"; }

  step "A1. REGISTER người dùng 1 (auto-send verification email)"
  request POST /auth/register "{\"email\":\"$EMAIL1\",\"password\":\"$PWD1\",\"displayName\":\"Smoke User\"}"
  [ "$HTTP_STATUS" = "201" ] && ok "register → 201" || bad "register → $HTTP_STATUS"
  jar_has auth-token && ok "Set-Cookie auth-token" || bad "thiếu auth-token cookie"

  step "A2. GET /me ban đầu (emailVerified=false)"
  request GET /auth/me
  [ "$HTTP_STATUS" = "200" ] && ok "/me → 200" || bad "/me → $HTTP_STATUS"
  [ "$(json_get "$BODY" data.data.emailVerified)" = "false" ] && ok "emailVerified=false" || bad "emailVerified ≠ false (binh: $BODY)"

  step "A3. VERIFY EMAIL qua link trong mailpit"
  VTOK=$(poll_token "$EMAIL1" verify-email 60)
  [ -n "$VTOK" ] && ok "mailpit có link verify-email ($EMAIL1)" || bad "không tìm thấy email verify"
  if [ -n "$VTOK" ]; then
    request POST /auth/verify-email "{\"token\":\"$VTOK\"}"
    [ "$HTTP_STATUS" = "200" ] && ok "verify-email → 200" || bad "verify-email → $HTTP_STATUS ($BODY)"
    request GET /auth/me
    [ "$(json_get "$BODY" data.data.emailVerified)" = "true" ] && ok "/me sau verify → emailVerified=true" || bad "emailVerified vẫn ≠ true"
  fi

  step "A4. 2FA enroll → TOTP local → verify setup"
  request POST /auth/2fa/enroll ""
  [ "$HTTP_STATUS" = "200" ] && ok "2fa/enroll → 200" || bad "2fa/enroll → $HTTP_STATUS"
  OTPAUTH=$(json_get "$BODY" data.otpauthUrl)
  SECRET=$("$PY" - "$OTPAUTH" <<'PY'
import sys, urllib.parse
sys.argv[1] and None
from urllib.parse import urlparse, parse_qs
print(parse_qs(urlparse(sys.argv[1]).query).get('secret', [''])[0])
PY
)
  [ -n "$SECRET" ] && ok "secret từ otpauthUrl (${#SECRET} chars)" || bad "không lấy được secret"
  if [ -n "$SECRET" ]; then
    CODE=$(node "$ROOT/scripts/totp.mjs" "$SECRET" 30)
    request POST /auth/2fa/verify "{\"code\":\"$CODE\"}"
    [ "$HTTP_STATUS" = "200" ] && ok "2fa/verify (TOTP $CODE) → 200" || bad "2fa/verify → $HTTP_STATUS ($BODY)"
  fi

  step "A5. LOGOUT (xóa cookie)"
  request POST /auth/logout ""
  [ "$HTTP_STATUS" = "200" ] && ok "logout → 200" || bad "logout → $HTTP_STATUS"
  jar_has auth-token && bad "auth-token còn trong jar sau logout" || ok "auth-token đã xóa"

  step "A6. LOGIN lúc 2FA đang bật → mfaRequired + mfaToken, KHÔNG set cookie"
  request POST /auth/login "{\"email\":\"$EMAIL1\",\"password\":\"$PWD1\"}"
  [ "$HTTP_STATUS" = "200" ] && ok "login → 200" || bad "login → $HTTP_STATUS ($BODY)"
  [ "$(json_get "$BODY" data.mfaRequired)" = "true" ] && ok "mfaRequired=true" || bad "mfaRequired ≠ true (binh: $BODY)"
  MFA_TOKEN=$(json_get "$BODY" data.mfaToken)
  [ -n "$MFA_TOKEN" ] && ok "mfaToken trả về" || bad "thiếu mfaToken"
  jar_has auth-token && bad "login 2FA vẫn set auth-token" || ok "chưa có auth-token (đúng)"

  step "A7. 2FA verify-login → set cookie → /me xác nhận session"
  CODE2=$(node "$ROOT/scripts/totp.mjs" "$SECRET" 30)
  request POST /auth/2fa/verify-login "{\"mfaToken\":\"$MFA_TOKEN\",\"code\":\"$CODE2\"}"
  [ "$HTTP_STATUS" = "200" ] && ok "2fa/verify-login → 200" || bad "2fa/verify-login → $HTTP_STATUS ($BODY)"
  jar_has auth-token && ok "đã có auth-token sau verify-login" || bad "thiếu auth-token sau verify-login"
  request GET /auth/me
  [ "$(json_get "$BODY" data.data.email)" = "$EMAIL1" ] && ok "/me đúng user sau 2FA" || bad "/me sai user"

  step "A8. REFRESH (rotate refresh-token)"
  request POST /auth/refresh ""
  [ "$HTTP_STATUS" = "200" ] && ok "refresh → 200" || bad "refresh → $HTTP_STATUS ($BODY)"
  request GET /auth/me
  [ "$HTTP_STATUS" = "200" ] && ok "/me sau refresh → 200" || bad "/me sau refresh → $HTTP_STATUS"

  step "B1. NGƯỜI DÙNG 2: forgot-password + reset + login password mới"
  request POST /auth/register "{\"email\":\"$EMAIL2\",\"password\":\"$PWD1\",\"displayName\":\"Smoke Two\"}"
  [ "$HTTP_STATUS" = "201" ] && ok "register user2 → 201" || bad "register user2 → $HTTP_STATUS"
  jarv2="$JAR"
  request POST /auth/forgot-password "{\"email\":\"$EMAIL2\"}"
  [ "$HTTP_STATUS" = "200" ] && ok "forgot-password → 200" || bad "forgot-password → $HTTP_STATUS ($BODY)"
  RTOK=$(poll_token "$EMAIL2" reset-password 60)
  [ -n "$RTOK" ] && ok "mailpit có link reset-password" || bad "không tìm thấy email reset"
  if [ -n "$RTOK" ]; then
    request POST /auth/reset-password "{\"token\":\"$RTOK\",\"newPassword\":\"$PWD2\"}"
    [ "$HTTP_STATUS" = "200" ] && ok "reset-password → 200" || bad "reset-password → $HTTP_STATUS ($BODY)"
    request POST /auth/login "{\"email\":\"$EMAIL2\",\"password\":\"$PWD2\"}"
    [ "$HTTP_STATUS" = "200" ] && ok "login password mới → 200" || bad "login → $HTTP_STATUS ($BODY)"
    [ "$(json_get "$BODY" data.mfaRequired)" = "false" ] && ok "mfaRequired=false (không bật 2FA)" || bad "mfaRequired ≠ false"
    jar_has auth-token && ok "auth-token set sau login" || bad "thiếu auth-token"
  fi

  step "C1. DỮ LIỆU THẬT qua gateway (JWT cookie từ A7): playlists / tracks / search / audio"
  request GET /playlists
  [ "$HTTP_STATUS" = "200" ] && ok "GET /playlists → 200" || bad "GET /playlists → $HTTP_STATUS"
  PLID=$(json_get "$BODY" data.0.id)
  [ -n "$PLID" ] && ok "playlist đầu: $PLID" || bad "playlists rỗng?"
  if [ -n "$PLID" ]; then
    request GET "/playlists/$PLID/tracks"
    [ "$HTTP_STATUS" = "200" ] && ok "GET /playlists/$PLID/tracks → 200" || bad "→ $HTTP_STATUS"
  fi
  request GET "/tracks?ids=$T1,$T2"
  [ "$HTTP_STATUS" = "200" ] && ok "GET /tracks?ids= (batch) → 200" || bad "→ $HTTP_STATUS ($BODY)"
  request GET "/search/tracks?q=Free+Spirit&limit=5"
  [ "$HTTP_STATUS" = "200" ] && ok "GET /search/tracks?q=Free+Spirit → 200" || bad "→ $HTTP_STATUS"
  request GET "/tracks/$T1/audio" "" -H "Range: bytes=0-100"
  [ "$HTTP_STATUS" = "206" ] && ok "GET /tracks/$T1/audio Range → 206" || bad "audio → $HTTP_STATUS"
  grep -qi '^Content-Range:' "$TMP_HDR" && ok "có Content-Range header" || bad "thiếu Content-Range"

  step "KẾT QUẢ"
  echo "  PASS=$PASS  FAIL=$FAIL"
  return "$FAIL"
}

case "${1:-all}" in
  boot) boot_infra && boot_services ;;
  run)  run_smoke
        code=$?
        [ "$code" -eq 0 ] && exit 0 || exit "$code" ;;
  stop) stop_services ;;
  all)
        boot_infra && boot_services && run_smoke
        code=$?
        stop_services
        exit "$code" ;;
  *) echo "usage: $0 {boot|run|stop|all}"; exit 2 ;;
esac