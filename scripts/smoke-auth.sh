#!/usr/bin/env bash
# smoke-auth.sh — end-to-end smoke test của luồng auth + dữ liệu thật qua gateway 9000.
#
#  Subcommands:
#    boot   — docker-compose up (infra + mailpit), cài common-lib, chạy 5 service + gateway
#             (auth/playlist/track/search/user, background, env trỏ Mailpit), chờ readiness từng cổng.
#             user-service (8088) cần Kafka lên để consume user.registered từ auth.
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
# Penting: dùng path dự án thay vì mktemp (/tmp). MSYS /tmp path bị mangled
# khi truyền cho curl.exe (Windows native) — curl ghi body ra stdout thay vì
# file, phá HTTP_STATUS cookie jar. Path dưới không chứa ký tự特殊 của $HOME.
[ -d "$LOG_DIR" ] || mkdir -p "$LOG_DIR"
JAR="$LOG_DIR/.smoke-cookies"
TMP_BODY="$LOG_DIR/.smoke-body"
TMP_HDR="$LOG_DIR/.smoke-hdr"
rm -f "$JAR" "$TMP_BODY" "$TMP_HDR" 2>/dev/null
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
if isinstance(d, bool):
    print("true" if d else "false")
elif d is None:
    print("")
else:
    print(d)
PY
}

# ---- HTTP helper: qua gateway, giữ cookie jar. Set HTTP_STATUS + BODY ----
HTTP_STATUS=""
BODY=""
request() {
  # request <method> <path> [json-body] [extra-curl-args...]
  local method=$1 path=$2 body=${3:-}
  # Consume chỉ số positional param có thật. `shift 3` sẽ FAIL (count out of range)
  # khi gọi không có body (GET: chỉ 2 args) — $@ không bị shift, hàm trả về
  # `$@ = "GET /auth/me"` → curl nhận các URL thừa "GET" và "/auth/me" → body tràn
  # ra stdout → HTTP_STATUS 000{body}200. Đây là root cause thật (bổ sung cho fix
  # truncate temp files phía dưới): min(3, $#) để body tùy chọn.
  shift $(( $# < 3 ? $# : 3 ))
  local data_args=()
  [ -n "$body" ] && data_args=("--data" "$body" "-H" "Content-Type: application/json")
  # Truncate temp files trước mỗi curl. Nếu không, file -o/-D còn sót từ request
  # trước làm curl bỏ qua `-o` trên Windows → body tràn ra stdout → HTTP_STATUS bị
  # ghép kiểu `000{body}200`. Fix này đã xác minh làm sạch (GET → đúng 200).
  : > "$TMP_BODY"
  : > "$TMP_HDR"
  HTTP_STATUS=$(curl -s -o "$TMP_BODY" -w '%{http_code}' -D "$TMP_HDR" \
    -c "$JAR" -b "$JAR" -X "$method" "${data_args[@]}" "$@" "$GW$path")
  BODY=$(cat "$TMP_BODY")
}

wait_port() {
  # wait_port <port> [path] [timeout_s] — port "up" = curl nhận được HTTP code thật.
  # Lưu ý: khi chưa connect được, `-w '%{http_code}'` in "000" VÀ `|| echo 000` in thêm
  # "000" → "000000" với rc≠0. Cả "000" lẫn "000000" đều = chưa up; chỉ chấp nhận
  # status thật (2xx/3xx/4xx/5xx). Đây là nguồn cho rằng port "up" quá sớm.
  local port=$1 path=${2:-/} timeout=${3:-240} i code
  for i in $(seq 1 "$timeout"); do
    code=$(curl -s -o /dev/null --max-time 2 -w '%{http_code}' "http://localhost:$port$path" 2>/dev/null || true)
    if [ -n "$code" ] && [ "$code" != "000" ] && [ "$code" != "000000" ]; then
      return 0
    fi
    sleep 1
  done
  return 1
}

wait_tcp() {
  # wait_tcp <port> [timeout_s] — Kafka không trả HTTP (wait_port dùng curl sẽ timeout),
  # nên chờ bằng TCP connect. Bắt buộc trước boot services: nếu register (A1) xảy ra trước
  # khi broker up, auth producer drop event user.registered → user-service không có user → D1 fail.
  local port=$1 timeout=${2:-120}
  "$PY" - "$port" "$timeout" <<'PY'
import socket, sys, time
port, t = int(sys.argv[1]), int(sys.argv[2])
for _ in range(t):
    try:
        socket.create_connection(("127.0.0.1", port), timeout=2).close()
        sys.exit(0)
    except OSError:
        time.sleep(1)
sys.exit(1)
PY
}

wait_auth_ready() {
  # Auth-service mở TCP trước khi route /auth/** được đăng ký (~vài giây). Nếu smoke
  # chạy ngay khi :8081 mới "up", gateway proxy register → 500 "Connection refused:
  # 8081". Chờ thật sự route trả HTTP code (401 khi chưa có cookie) thay vì 000.
  # Đây là nguồn FAIL ngẫu nhiên 'register → 500' khi boot fresh.
  # Lưu ý: khi curl không kết nối được, `-w '%{http_code}'` in "000" VÀ `|| echo 000`
  # in thêm "000" → chuỗi "000000". Phải coi mọi giá trị chứa "000" là chưa sẵn sàng.
  local i code
  for i in $(seq 1 120); do
    code=$(curl -s -o /dev/null --max-time 2 -w '%{http_code}' \
      "http://localhost:8081/api/v1/auth/me" 2>/dev/null || true)
    # "sẵn sàng" = HTTP status thật (401/200/...). 000/000000 = chưa connect được.
    if [ -n "$code" ] && [ "$code" != "000" ] && [ "$code" != "000000" ]; then
      printf '    OK  :8081 auth route đã sẵn sàng (%s)\n' "$code"
      return 0
    fi
    sleep 2
  done
  echo "    Fail :8081 auth route không lên — xem logs/auth.log"
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
  echo "  Chờ Kafka (:9092 TCP)... (user-service consume user.registered từ auth)"
  wait_tcp 9092 120 || echo "  (kafka chưa kịp lên — user.registered ban đầu có thể mất)"
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
print(f"  launch {logname} -> logs/{logname}.log (pid {p.pid})")
sys.exit(0)
PY
}

boot_services() {
  [ -d "$LOG_DIR" ] || mkdir -p "$LOG_DIR"
  # Idempotent: kill bất kỳ service còn sót từ boot trước — nếu không, port bind fail
  # (orphan giữ 8081/8085...) và smoke chạy vào instance cũ.
  stop_services

  # env smoke: mail → Mailpit local (STARTTLS tắt); còn lại đọc từ backend/.env
  set -a
  [ -f "$ROOT/backend/.env" ] && . "$ROOT/backend/.env"
  export MAIL_HOST=localhost
  export MAIL_PORT=1025
  export MAIL_USERNAME=mailpit
  export MAIL_PASSWORD=mailpit
  export MAIL_STARTTLS_ENABLE=false
  export MAIL_STARTTLS_REQUIRED=false
  # Smoke/E2E full suite gộp nhiều register+login cùng IP (127.0.0.1) trong 1 phút →
  # vượt limit mặc định 10 → 429. Nâng limit cho môi trường test (production vẫn 10).
  export APP_SECURITY_RATE_LIMIT_PER_MINUTE=100
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
  start_service user     backend -pl user-service
  start_service gateway  gateway

  echo "  Chờ readiness (5 service + gateway, mỗi port tối đa 240s):"
  for port in 8081 8084 8085 8086 8088 9000; do
    if wait_port "$port"; then
      echo "    OK  :$port up"
    else
      echo "    Fail :$port không lên — xem logs/$log.log"
    fi
  done
  # Auth mở TCP trước khi route sẵn sàng — chờ route thật trước khi smoke chạy
  # (tránh gateway proxy register → 500 "Connection refused: 8081").
  wait_auth_ready
}

stop_services() {
  # Kill cả launcher PID đã ghi + các process java đang LISTEN trên port service.
  # Lý do: start_service ghi PID của launcher (mvn.cmd/python), nhưng process thật
  # giữ port là java.exe con — taskkill //T trên launcher không phải lúc nào cũng
  # chạm tới. Vậy ngoài PID_FILE, dò theo netstat theo port rồi kill chính xác.
  local pid
  if [ -f "$PID_FILE" ]; then
    while read -r pid; do
      [ -z "$pid" ] && continue
      taskkill //F //T //PID "$pid" >/dev/null 2>&1 || true
    done <"$PID_FILE"
  fi
  for port in 8081 8084 8085 8086 8088 9000; do
    "$PY" - "$port" <<'PY'
import subprocess, sys
port = sys.argv[1]
r = subprocess.run(["netstat", "-ano", "-p", "TCP"], capture_output=True, text=True)
seen = set()
for line in r.stdout.splitlines():
    if (f":{port}" in line and "LISTENING" in line and line.split()[-1] not in seen):
        pid = line.split()[-1]
        seen.add(pid)
        subprocess.run(["taskkill", "/F", "/T", "/PID", pid], capture_output=True)
PY
  done
  : >"$PID_FILE"
  echo "  Đã kill các service (bởi PID_FILE + theo port)."
}

# ============================================================ mailpit ========
# mail_token <to> <marker(verify-email|reset-password)> — trích token từ link gần nhất
mail_token() {
  local to=$1 marker=$2
  # Mailpit /messages list endpoint returns HTML:0 — fetch individual messages by ID instead.
  "$PY" - "$MP" "$to" "$marker" <<'PY'
import json, re, sys, urllib.request
mp, to, marker = sys.argv[1], sys.argv[2], sys.argv[3]
try:
    data = json.loads(urllib.request.urlopen(mp + "/messages").read())
except Exception:
    sys.exit(1)
for msg in data.get("messages", []):
    addrs = {a.get("Address", "") for a in msg.get("To", [])}
    if to not in addrs:
        continue
    mid = msg.get("ID", "")
    try:
        detail = json.loads(urllib.request.urlopen(f"{mp}/message/{mid}").read())
        html = detail.get("HTML") or ""
    except Exception:
        continue
    m = re.search(r'href="[^"]*' + re.escape(marker) + r'\?token=([0-9a-fA-F-]{36})', html)
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
  # Lưu UID1 cho section D (user-service) — id của người dùng vừa register (A1).
  UID1=$(json_get "$BODY" data.id)
  # /me trả single envelope {success,data:{id,...}} (đã fix double-wrap) → đọc data.emailVerified
  [ "$(json_get "$BODY" data.emailVerified)" = "false" ] && ok "emailVerified=false" || bad "emailVerified ≠ false (binh: $BODY)"

  step "A3. VERIFY EMAIL qua link trong mailpit"
  VTOK=$(poll_token "$EMAIL1" verify-email 60)
  [ -n "$VTOK" ] && ok "mailpit có link verify-email ($EMAIL1)" || bad "không tìm thấy email verify"
  if [ -n "$VTOK" ]; then
    request POST /auth/verify-email "{\"token\":\"$VTOK\"}"
    [ "$HTTP_STATUS" = "200" ] && ok "verify-email → 200" || bad "verify-email → $HTTP_STATUS ($BODY)"
    request GET /auth/me
    [ "$(json_get "$BODY" data.emailVerified)" = "true" ] && ok "/me sau verify → emailVerified=true" || bad "emailVerified vẫn ≠ true"
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
  [ "$HTTP_STATUS" = "200" ] || [ "$HTTP_STATUS" = "204" ] && ok "logout → $HTTP_STATUS" || bad "logout → $HTTP_STATUS"
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
  [ "$(json_get "$BODY" data.email)" = "$EMAIL1" ] && ok "/me đúng user sau 2FA" || bad "/me sai user"

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
    # Lưu UID2 cho section D (user-service): sau login, cookie = user2 → GET /me lấy id.
    if jar_has auth-token; then
      request GET /auth/me
      UID2=$(json_get "$BODY" data.id)
      [ -n "$UID2" ] && ok "user2 id: $UID2" || bad "không lấy được UID2"
    fi
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

  step "D1. USER-SERVICE: profile + follows qua gateway 9000"
  # user1 register ở A1, user2 ở B1 → auth publish user.registered → user-service consume
  # (async). Poll GET /users/$UID1 tới 200 để đảm bảo user đã được upsert (tránh flake).
  d_found=0
  for i in $(seq 1 60); do
    request GET "/users/$UID1"
    [ "$HTTP_STATUS" = "200" ] && { d_found=1; break; }
    sleep 1
  done
  [ "$d_found" = "1" ] && ok "user1 sync vào user-service (GET /users/$UID1 → 200)" \
    || bad "user1 chưa xuất hiện (event user.registered không được consume?) → $HTTP_STATUS"
  if [ "$d_found" = "1" ]; then
    # Profile public không lộ email/PII; có displayName + counters.
    request GET "/users/$UID1"
    [ "$(json_get "$BODY" data.displayName)" = "Smoke User" ] && ok "profile trả displayName" \
      || bad "displayName sai (binh: $BODY)"
    case "$BODY" in *\"email\"*) bad "profile public lộ field email" ;; *) ok "profile public không lộ email" ;; esac
    [ "$(json_get "$BODY" data.followersCount)" = "0" ] && ok "followersCount=0 ban đầu" \
      || bad "followersCount ≠ 0 (binh: $BODY)"
  fi
  request GET "/users/$UID1/followers"
  [ "$HTTP_STATUS" = "200" ] && ok "GET /users/$UID1/followers → 200" || bad "→ $HTTP_STATUS"

  step "D2. SELF-FOLLOW bị chặn (400)"
  request POST "/users/$UID2/follow" ""
  [ "$HTTP_STATUS" = "400" ] && ok "self-follow → 400" \
    || bad "self-follow → $HTTP_STATUS (binh: $BODY)"

  step "D3. FOLLOW thật (user2 follow user1) + verify counts"
  request POST "/users/$UID1/follow" ""
  [ "$HTTP_STATUS" = "200" ] && ok "user2 follow user1 → 200" \
    || bad "follow → $HTTP_STATUS (binh: $BODY)"
  request GET "/users/$UID1/followers"
  [ "$(json_get "$BODY" data.0.id)" = "$UID2" ] && ok "user1 có follower = user2" \
    || bad "follower sai (binh: $BODY)"
  request GET "/users/$UID2/following"
  [ "$(json_get "$BODY" data.0.id)" = "$UID1" ] && ok "user2 đang follow user1" \
    || bad "following sai (binh: $BODY)"
  request GET "/users/$UID1"
  [ "$(json_get "$BODY" data.followersCount)" = "1" ] && ok "profile followersCount=1" \
    || bad "followersCount ≠ 1 (binh: $BODY)"

  step "D4. UNFOLLOW (user2 bỏ follow user1) → rỗng lại"
  request DELETE "/users/$UID1/follow"
  [ "$HTTP_STATUS" = "200" ] && ok "unfollow → 200" || bad "unfollow → $HTTP_STATUS (binh: $BODY)"
  request GET "/users/$UID1/followers"
  [ "$HTTP_STATUS" = "200" ] && ok "GET /followers sau unfollow → 200" || bad "→ $HTTP_STATUS"

  step "E1. CREATE PLAYLIST (cookie A7 → nhưng D-section đã login user2) → owner resolve cross-service"
  # LƯU Ý: sau section D (unfollow), cookie jar đang = user2 ("Smoke Two") — chứ không phải
  # user1 A7. Vì vậy ownerName phải resolve = "Smoke Two" — đây chính là bằng chứng chức năng
  # resolve cross-service chạy ĐÚNG với principal thật (X-User-Id → user-service). Fallback "You"
  # chỉ khi user chưa sync. Không hardcode giả định cookie=A7 ở đây.
  request POST /playlists "{\"title\":\"Smoke Build Mix\",\"description\":\"from section E\"}"
  [ "$HTTP_STATUS" = "201" ] && ok "POST /playlists → 201" || bad "POST /playlists → $HTTP_STATUS (binh: $BODY)"
  NEWPL=$(json_get "$BODY" data.id)
  [ -n "$NEWPL" ] && ok "playlist mới id: $NEWPL" || bad "không lấy được id playlist mới"
  [ "$(json_get "$BODY" data.ownerName)" = "Smoke Two" ] && ok "ownerName='Smoke Two' (resolve theo principal thật)" \
    || bad "ownerName ≠ 'Smoke Two' (binh: $BODY)"

  step "E2. LIST /playlists chứa playlist vừa tạo"
  request GET /playlists
  [ "$HTTP_STATUS" = "200" ] && ok "GET /playlists → 200" || bad "→ $HTTP_STATUS"
  case "$BODY" in *"$NEWPL"*) ok "playlist mới $NEWPL có trong list" ;; *) bad "playlist mới không xuất hiện (binh: $BODY)" ;; esac

  step "E3. CREATE PLAYLIST validation: title blank → 400"
  request POST /playlists "{\"title\":\"   \",\"description\":\"x\"}"
  [ "$HTTP_STATUS" = "400" ] && ok "blank title → 400" || bad "blank title → $HTTP_STATUS (binh: $BODY)"

  step "E4. ADD TRACK (T1) vào playlist mới → append LexoRank"
  request POST "/playlists/$NEWPL/tracks" "{\"trackId\":\"$T1\"}"
  [ "$HTTP_STATUS" = "201" ] && ok "POST /playlists/$NEWPL/tracks → 201" || bad "add track → $HTTP_STATUS (binh: $BODY)"
  [ "$(json_get "$BODY" data.trackId)" = "$T1" ] && ok "trả về trackId=$T1" || bad "trackId sai (binh: $BODY)"
  [ -n "$(json_get "$BODY" data.lexoRank)" ] && ok "có lexoRank (append)" || bad "thiếu lexoRank"

  step "E5. GET /playlists/$NEWPL/tracks round-trip chứa T1"
  request GET "/playlists/$NEWPL/tracks"
  [ "$HTTP_STATUS" = "200" ] && ok "GET /playlists/$NEWPL/tracks → 200" || bad "→ $HTTP_STATUS"
  case "$BODY" in *"$T1"*) ok "track T1 có trong playlist (round-trip)" ;; *) bad "track T1 không có trong playlist (binh: $BODY)" ;; esac

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