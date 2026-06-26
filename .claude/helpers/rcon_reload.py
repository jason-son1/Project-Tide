#!/usr/bin/env python3
import sys
import os
import urllib.request
import json
from pathlib import Path

def try_http_reload(target: str, port: int = 8080) -> bool:
    url = f"http://localhost:{port}/api/reload/{target}"
    print(f"[*] HTTP 리로드 시도 중: {url}")
    try:
        req = urllib.request.Request(url, method="POST")
        with urllib.request.urlopen(req, timeout=5) as response:
            res_data = json.loads(response.read().decode("utf-8"))
            if res_data.get("success"):
                print(f"[+] HTTP 리로드 성공! 대상: {target}")
                return True
    except Exception as e:
        print(f"[-] HTTP 리로드 실패: {e}")
    return False

def try_rcon_reload(target: str) -> bool:
    host = os.environ.get("RCON_HOST", "localhost")
    port = int(os.environ.get("RCON_PORT", "25575"))
    password = os.environ.get("RCON_PASSWORD", "")
    
    # Try to parse from server.properties if available in parent dirs
    prop_path = Path("server.properties")
    if not prop_path.exists():
        # Check parents
        for parent in Path(__file__).resolve().parents:
            check_path = parent / "server.properties"
            if check_path.exists():
                prop_path = check_path
                break
                
    if prop_path.exists():
        print(f"[*] server.properties 감지됨: {prop_path}")
        try:
            with open(prop_path, "r") as f:
                for line in f:
                    if "=" in line:
                        k, v = line.strip().split("=", 1)
                        if k == "rcon.port":
                            port = int(v)
                        elif k == "rcon.password":
                            password = v
        except Exception as e:
            print(f"[-] server.properties 파싱 실패: {e}")

    if not password:
        print("[-] RCON 비밀번호가 설정되지 않았습니다.")
        return False

    print(f"[*] RCON 연결 시도 중: {host}:{port}")
    try:
        from mcrcon import MCRcon
        with MCRcon(host, password, port=port) as rcon:
            response = rcon.command(f"tide reload {target}")
            print(f"[+] RCON 리로드 성공! 응답: {response}")
            return True
    except ImportError:
        print("[-] mcrcon 라이브러리가 설치되지 않았습니다. (pip install mcrcon)")
    except Exception as e:
        print(f"[-] RCON 연결 실패: {e}")
    return False

def main():
    if len(sys.argv) < 2:
        print("사용법: python rcon_reload.py <items|runes|mobs|affixes|altars|config>")
        sys.exit(1)
        
    target = sys.argv[1].lower()
    
    # Map synonyms
    mapping = {
        "item": "items",
        "rune": "runes",
        "mob": "mobs",
        "altar": "mobs",
        "affix": "affixes"
    }
    target = mapping.get(target, target)
    
    # Try RCON first, fallback to HTTP
    if try_rcon_reload(target):
        sys.exit(0)
        
    # HTTP server fallback
    if try_http_reload(target):
        sys.exit(0)
        
    print("[ERROR] 모든 리로드 시도가 실패했습니다. 서버가 실행 중인지 확인하세요.")
    sys.exit(1)

if __name__ == "__main__":
    main()
