#!/usr/bin/env python3
"""Chạy Maven trên Windows từ cached dist (mvnw/mvnw.cmd trực tiếp bị lỗi).

Usage:
  python scripts/mvn-run.py [module args...]     # VD:
  python scripts/mvn-run.py -pl auth-service test -Dtest=UserJpaRepositoryIntegrationTest

Mọi argument phía sau được truyền thẳng vào mvn.cmd. Chạy từ backend/ làm cwd.
"""
import os
import subprocess
import sys
from pathlib import Path

BACKEND_DIR = Path(__file__).resolve().parent.parent / "backend"


def find_mvn_cmd() -> str:
    user_home = Path.home()
    dists = (user_home / ".m2" / "wrapper" / "dists")
    if dists.exists():
        for mvn in sorted(dists.rglob("bin/mvn.cmd"), reverse=True):
            return str(mvn)
    return "mvn"


def main() -> int:
    mvn = find_mvn_cmd()
    args = sys.argv[1:]
    print(f"[mvn-run] {mvn} {' '.join(args)}", flush=True)
    return subprocess.run([mvn, *args], cwd=str(BACKEND_DIR)).returncode


if __name__ == "__main__":
    sys.exit(main())
