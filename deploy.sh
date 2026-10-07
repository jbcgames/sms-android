#!/bin/bash
set -e
CONSOLE_IP="${1:-${CONSOLE_IP:-192.168.1.70}}"
echo "Deploying to ${CONSOLE_IP}..."
sshpass -p ark scp /mnt/c/Users/migue/Port/sunshine/sms-pc-port/build/linux-aarch64/sms.stripped ark@${CONSOLE_IP}:/roms/ports/sunshine/sms.new
sshpass -p ark ssh -o StrictHostKeyChecking=no ark@${CONSOLE_IP} "mv -f /roms/ports/sunshine/sms.new /roms/ports/sunshine/sms"
sshpass -p ark scp /mnt/c/Users/migue/Port/sunshine/sms-pc-port/settings.txt ark@${CONSOLE_IP}:/roms/ports/sunshine/settings.txt
sshpass -p ark scp '/mnt/c/Users/migue/Port/sunshine/sms-pc-port/Super Mario Sunshine.sh' ark@${CONSOLE_IP}:/roms/ports/sunshine/sunshine.sh

echo "Updating launch scripts on device..."
sshpass -p ark ssh -o StrictHostKeyChecking=no ark@${CONSOLE_IP} "cp /roms/ports/sunshine/sunshine.sh '/roms/ports/Super Mario Sunshine.sh'"
sshpass -p ark ssh -o StrictHostKeyChecking=no ark@${CONSOLE_IP} "chmod +x /roms/ports/sunshine/sms /roms/ports/sunshine/sunshine.sh '/roms/ports/Super Mario Sunshine.sh'"

echo "Deploy completed successfully!"

