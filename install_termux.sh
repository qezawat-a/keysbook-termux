#!/data/data/com.termux/files/usr/bin/bash
set -e
pkg update -y
pkg install python termux-api -y
python -m pip install --upgrade pip
python -m pip install -r requirements.txt
chmod +x keysbook.py
printf '\nKeysBook installed. Run: python keysbook.py\n'
