#!/bin/bash
# PortMaster Launch Script for Super Mario Sunshine
# Platform: RK3326 / Mali-G31 MP2 / OpenGL ES 3.0 / KMSDRM

XDG_DATA_HOME=${XDG_DATA_HOME:-$HOME/.local/share}

if [ -d "/opt/system/Tools/PortMaster/" ]; then
  controlfolder="/opt/system/Tools/PortMaster"
elif [ -d "/opt/tools/PortMaster/" ]; then
  controlfolder="/opt/tools/PortMaster"
elif [ -d "$XDG_DATA_HOME/PortMaster/" ]; then
  controlfolder="$XDG_DATA_HOME/PortMaster"
else
  controlfolder="/roms/ports/PortMaster"
fi

source "$controlfolder/control.txt"
[ -f "${controlfolder}/mod_${CFW_NAME}.txt" ] && source "${controlfolder}/mod_${CFW_NAME}.txt"
get_controls

GAMEDIR="/$directory/ports/sunshine"
cd "$GAMEDIR" || exit 1

> "$GAMEDIR/log.txt" && exec > >(tee "$GAMEDIR/log.txt") 2>&1

# =============================================================================
# AUDIO SETUP
# =============================================================================
amixer -c 0 sset 'Playback Path' 'SPK' >/dev/null 2>&1 || true

# =============================================================================
# AUTO-DETECT MALI GPU DRIVER & CREATE SYMLINKS
# =============================================================================
MALI_DIR="/tmp/sms_mali"
rm -rf "$MALI_DIR"
mkdir -p "$MALI_DIR"

if [ -f "/usr/local/lib/aarch64-linux-gnu/libmali-bifrost-g31-rxp0-gbm.so" ]; then
  MALI_BLOB="/usr/local/lib/aarch64-linux-gnu/libmali-bifrost-g31-rxp0-gbm.so"
elif [ -f "/usr/lib/aarch64-linux-gnu/libmali-bifrost-g31-rxp0-gbm.so" ]; then
  MALI_BLOB="/usr/lib/aarch64-linux-gnu/libmali-bifrost-g31-rxp0-gbm.so"
elif [ -f "/usr/lib/aarch64-linux-gnu/libMali.so" ]; then
  MALI_BLOB="/usr/lib/aarch64-linux-gnu/libMali.so"
elif [ -f "/lib/aarch64-linux-gnu/libMali.so" ]; then
  MALI_BLOB="/lib/aarch64-linux-gnu/libMali.so"
else
  MALI_BLOB=$(find /usr/lib /usr/local/lib /lib -name "libmali-bifrost-g31-*.so" -o -name "libMali.so" 2>/dev/null | head -n 1)
fi

if [ -n "$MALI_BLOB" ]; then
  echo "[port] Using Mali driver: $MALI_BLOB"
  ln -sf "$MALI_BLOB" "$MALI_DIR/libEGL.so.1"
  ln -sf "$MALI_BLOB" "$MALI_DIR/libEGL.so"
  ln -sf "$MALI_BLOB" "$MALI_DIR/libGLESv2.so.2"
  ln -sf "$MALI_BLOB" "$MALI_DIR/libGLESv2.so"
  ln -sf "$MALI_BLOB" "$MALI_DIR/libGL.so.1"
  ln -sf "$MALI_BLOB" "$MALI_DIR/libGL.so"
  ln -sf "$MALI_BLOB" "$MALI_DIR/libgbm.so.1"
  ln -sf "$MALI_BLOB" "$MALI_DIR/libgbm.so"
fi

export LD_LIBRARY_PATH="$MALI_DIR:$GAMEDIR/libs.${DEVICE_ARCH}:$GAMEDIR:$LD_LIBRARY_PATH"
export SDL_VIDEODRIVER="kmsdrm"
export SDL_VIDEO_GL_DRIVER="libGLESv2.so.2"
export SDL_VIDEO_EGL_DRIVER="libEGL.so.1"
export SDL_GAMECONTROLLERCONFIG="$sdl_controllerconfig"

# =============================================================================
# HARDWARE PERFORMANCE GOVERNORS & MEMORY TUNING
# =============================================================================
for cpu in /sys/devices/system/cpu/cpu[0-3]/online; do
  [ -f "$cpu" ] && echo 1 | sudo tee "$cpu" >/dev/null 2>&1 || true
done

if [ -f /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor ]; then
  echo performance | sudo tee /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor >/dev/null 2>&1 || true
fi

for gpu_gov in /sys/devices/platform/*.gpu/devfreq/*.gpu/governor /sys/class/devfreq/*gpu*/governor; do
  [ -f "$gpu_gov" ] && echo performance | sudo tee "$gpu_gov" >/dev/null 2>&1 || true
done

for dmc_gov in /sys/devices/platform/dmc/devfreq/dmc/governor /sys/class/devfreq/*dmc*/governor; do
  [ -f "$dmc_gov" ] && echo performance | sudo tee "$dmc_gov" >/dev/null 2>&1 || true
done

echo ark | sudo -S /sbin/sysctl -w vm.overcommit_memory=1 >/dev/null 2>&1 || true
echo ark | sudo -S /sbin/sysctl -w vm.min_free_kbytes=32768 >/dev/null 2>&1 || true
echo ark | sudo -S /sbin/sysctl -w vm.vfs_cache_pressure=200 >/dev/null 2>&1 || true
echo ark | sudo -S /sbin/sysctl -w vm.swappiness=100 >/dev/null 2>&1 || true
echo 3 | sudo tee /proc/sys/vm/drop_caches >/dev/null 2>&1 || true

# Auto-protect sms process from OOM killer
(
  for _ in {1..30}; do
    PID=$(pgrep -x sms | head -n 1)
    if [ -n "$PID" ]; then
      echo ark | sudo -S sh -c "echo -800 > /proc/$PID/oom_score_adj" 2>/dev/null
      break
    fi
    sleep 0.5
  done
) &

# =============================================================================
# VRAM & MEMORY BUDGET SETUP (700MB VRAM Limit by Default)
# =============================================================================
ES_CFG="/home/ark/.emulationstation/es_settings.cfg"
if [ -f "$ES_CFG" ]; then
  if grep -q 'name="MaxVRAM"' "$ES_CFG"; then
    CURRENT_VRAM=$(grep -oP '(?<=<int name="MaxVRAM" value=")[0-9]+' "$ES_CFG" 2>/dev/null || echo "0")
    if [ -n "$CURRENT_VRAM" ] && [ "$CURRENT_VRAM" -lt 700 ]; then
      sed -i -E 's/<int name="MaxVRAM" value="[0-9]+"/<int name="MaxVRAM" value="730"/' "$ES_CFG"
      echo "[port] Set EmulationStation MaxVRAM to 730 MB (was $CURRENT_VRAM)"
    fi
  else
    echo '<int name="MaxVRAM" value="730" />' >> "$ES_CFG"
    echo "[port] Added EmulationStation MaxVRAM=730"
  fi
fi

# Super Mario Sunshine performance profile for RK3326 (Mali-G31)
export SMS_LAUNCHER=0
export SMS_WINDOW_MODE=fullscreen
# Render scale and draw distance are controlled via $GAMEDIR/settings.txt
# (defaults: render_scale = 0.5, draw_distance = 0.7, far_plane = 0)
export SMS_MSAA=0
export SMS_FXAA=0
export SMS_ANISO=0
export SMS_FRAME_RATE=30
export SMS_VSYNC=0
export SMS_FAST_PEEK=1
export SMS_GX_COPY_WRITEBACK=0
export SMS_DISABLE_SHIMMER=1
export SMS_DISABLE_SHADOWS=1
export SMS_DISABLE_REFLECTIONS=1
export SMS_DISABLE_WATER_ANIM=1
export SMS_NPC_DRAW_DISTANCE=2500
export SMS_ACTOR_DRAW_DISTANCE=3500
export SMS_MAPOBJ_DRAW_DISTANCE=4000
export SMS_STATIC_DRAW_DISTANCE=6000
export SMS_SHADER_CACHE=1
export SMS_R1_SOFT_SPRAY=1
export SMS_TEXTURE_PACK_MB=700
export SMS_SAVE_DIR="$GAMEDIR/save"

# Disc image discovery
if [ -f "$GAMEDIR/GMSE01.iso" ]; then
  export SMS_DISC_IMAGE="$GAMEDIR/GMSE01.iso"
elif [ -f "$GAMEDIR/Super Mario Sunshine (USA).iso" ]; then
  export SMS_DISC_IMAGE="$GAMEDIR/Super Mario Sunshine (USA).iso"
else
  for iso in "$GAMEDIR"/*.iso "$GAMEDIR"/*.ISO "$GAMEDIR"/*.gcm "$GAMEDIR"/*.ciso; do
    if [ -f "$iso" ]; then
      export SMS_DISC_IMAGE="$iso"
      break
    fi
  done
fi

if [ -z "$SMS_DISC_IMAGE" ] || [ ! -f "$SMS_DISC_IMAGE" ]; then
  echo "ERROR: Super Mario Sunshine disc image not found in $GAMEDIR"
  pm_message "No ISO found in ports/sunshine!"
  sleep 4
  pm_finish
  exit 1
fi

BIN="$GAMEDIR/sms"
chmod +x "$BIN"

echo "=== Super Mario Sunshine Launch: $(date) ==="
echo "CFW_NAME=$CFW_NAME  DEVICE_ARCH=$DEVICE_ARCH  DEVICE_NAME=$DEVICE_NAME"
echo "GAMEDIR=$GAMEDIR  controlfolder=$controlfolder"
echo "SMS_DISC_IMAGE=$SMS_DISC_IMAGE"
echo "Kernel: $(uname -a)"
echo "--------------------------------------------------"

# Launch game
pm_platform_helper "$BIN"
"$BIN" "$SMS_DISC_IMAGE"
ret=$?

echo "=== Super Mario Sunshine ended with code $ret ==="

pm_finish
