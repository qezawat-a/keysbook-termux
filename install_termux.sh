#!/data/data/com.termux/files/usr/bin/bash
set -e
# Python is usually already installed in Termux. Install it only if needed.
command -v python >/dev/null 2>&1 || pkg install python -y
command -v openssl >/dev/null 2>&1 || pkg install openssl-tool -y
python -m pip install -r requirements.txt
chmod +x keysbook.py
printf '\nKeysBook installed. Run: python keysbook.py\n'
printf 'Clipboard support is optional: install Termux:API separately if desired.\n'
