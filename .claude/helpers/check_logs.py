#!/usr/bin/env python3
import sys
from pathlib import Path

def find_latest_log() -> Path | None:
    # Check parent folders for logs/latest.log
    for parent in Path(__file__).resolve().parents:
        # Check direct logs/latest.log under workspace or server folder
        log_path = parent / "logs" / "latest.log"
        if log_path.exists():
            return log_path
        # Or look next to the workspace (in E:/Minecraft/Minecraft Server/...)
        server_dir = parent.parent / "Minecraft Server"
        if server_dir.exists() and server_dir.is_dir():
            for sub in server_dir.iterdir():
                check_log = sub / "logs" / "latest.log"
                if check_log.exists():
                    return check_log
                    
    # Fallback to checking local path
    fallback = Path("logs/latest.log")
    if fallback.exists():
        return fallback
    return None

def check_log_errors(log_path: Path, num_lines: int = 100) -> bool:
    print(f"[*] 로그 감시 시작: {log_path}")
    try:
        with open(log_path, "r", encoding="utf-8", errors="ignore") as f:
            lines = f.readlines()
    except Exception as e:
        print(f"[-] 로그 파일을 읽는 중 오류 발생: {e}")
        return False

    recent_lines = lines[-num_lines:] if len(lines) > num_lines else lines
    
    errors_found = []
    stack_trace = []
    recording_stack = False

    error_keywords = ["Exception", "Error", "SEVERE", "FATAL", "YAMLException", "NullPointerException"]
    exclude_keywords = [" spark ", "spark-"] # exclude Spark profiler logs if any

    for line in recent_lines:
        line_strip = line.strip()
        
        # Check if line contains error keywords
        has_error = any(kw in line for kw in error_keywords) and not any(ex in line for ex in exclude_keywords)
        
        if has_error:
            errors_found.append(line_strip)
            recording_stack = True
            stack_trace = [line_strip]
        elif recording_stack:
            # If line is part of a stack trace (starts with \tat or caused by)
            if line_strip.startswith("at ") or line_strip.startswith("Caused by:"):
                stack_trace.append(line_strip)
            else:
                recording_stack = False
                if len(stack_trace) > 1:
                    errors_found.append("\n".join(stack_trace[1:6]) + ("\n  ..." if len(stack_trace) > 6 else ""))

    if errors_found:
        print(f"\n[!] 최근 로그에서 {len(errors_found)}개의 에러/경고가 발견되었습니다:")
        for idx, err in enumerate(errors_found):
            print(f"\n[{idx + 1}] {err}")
        return False
        
    print("\n[+] 최근 100라인 내에 플러그인 에러나 Exception이 발견되지 않았습니다. 정상 작동 중입니다.")
    return True

def main():
    log_path = find_latest_log()
    if not log_path:
        print("[-] 최신 로그 파일(latest.log)을 찾을 수 없습니다.")
        sys.exit(1)
        
    success = check_log_errors(log_path)
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()
