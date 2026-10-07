#include "touch_controller.h"
#include <sms_gx/gx_pc.h>
#include <algorithm>
#include <cmath>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <vector>

extern "C" {
__attribute__((weak)) void GXPC_DrawOverlay(const uint8_t* rgba, int w, int h, int x, int y, int scale, int winW, int winH);
}

namespace {

enum TouchButtonId {
    TBTN_A,
    TBTN_B,
    TBTN_X,
    TBTN_Y,
    TBTN_L,
    TBTN_R,
    TBTN_R_HARD,
    TBTN_Z,
    TBTN_START,
    TBTN_OPACITY,
    TBTN_COUNT
};

struct TouchButton {
    TouchButtonId id;
    const char* label;
    float relX, relY;    // Fraction of width / height
    float relRadius;     // Fraction of height
    uint16_t padMask;
    uint8_t baseR, baseG, baseB;
    bool pressed;
    int64_t fingerId;
};

// Button layout definitions
TouchButton s_buttons[TBTN_COUNT] = {
    // ID          Label   RelX   RelY   RelR    PadMask             R    G    B
    { TBTN_A,      "A",    0.88f, 0.76f, 0.088f, PAD_BUTTON_A,       46,  204, 113, false, -1 }, // Emerald Green
    { TBTN_B,      "B",    0.78f, 0.83f, 0.076f, PAD_BUTTON_B,       231, 76,  60,  false, -1 }, // Coral Red
    { TBTN_X,      "X",    0.80f, 0.65f, 0.065f, PAD_BUTTON_X,       241, 196, 15,  false, -1 }, // Amber Yellow
    { TBTN_Y,      "Y",    0.71f, 0.72f, 0.065f, PAD_BUTTON_Y,       52,  152, 219, false, -1 }, // Sky Blue
    { TBTN_L,      "L",    0.08f, 0.16f, 0.072f, PAD_TRIGGER_L,      149, 165, 166, false, -1 }, // Silver
    { TBTN_R,      "R",    0.91f, 0.54f, 0.075f, 0,                  41,  128, 185, false, -1 }, // Ocean Blue (Soft Run Spray)
    { TBTN_R_HARD, "R+",   0.81f, 0.44f, 0.066f, PAD_TRIGGER_R,      31,  78,  121, false, -1 }, // Deep Blue (Aim Spray)
    { TBTN_Z,      "Z",    0.92f, 0.16f, 0.065f, PAD_TRIGGER_Z,      155, 89,  182, false, -1 }, // Purple
    { TBTN_START,  "START",0.50f, 0.08f, 0.052f, PAD_BUTTON_START,  230, 126, 34,  false, -1 }, // Orange
    { TBTN_OPACITY,"HUD",  0.40f, 0.08f, 0.045f, 0,                  127, 140, 141, false, -1 }  // Settings/HUD
};

// Virtual Analog Stick State
struct VirtualStick {
    bool active;
    int64_t fingerId;
    float anchorX, anchorY; // In screen pixels
    float currX, currY;     // In screen pixels
    float radius;           // Outer max deflection
    int stickX, stickY;     // Clamped to -100 .. 100
};

VirtualStick s_stick = { false, -1, 0, 0, 0, 0, 0, 0, 0 };

// Camera Touch Swipe State
struct CameraSwipe {
    bool active;
    int64_t fingerId;
    float lastX, lastY;
    int substickX, substickY;
};

CameraSwipe s_cam = { false, -1, 0, 0, 0, 0 };

int s_winW = 1280;
int s_winH = 720;
float s_hudOpacity = 0.70f;
bool s_visible = true;
bool s_inited = false;

// Sprite Cache
struct Sprite {
    int w, h;
    std::vector<uint8_t> pixels;
};

Sprite s_buttonSprites[TBTN_COUNT][2]; // [id][0=normal, 1=pressed]
Sprite s_stickBaseSprite;
Sprite s_stickKnobSprite;
Sprite s_stickKnobPressedSprite;

// Simple 5x7 bitmap font for rendering labels
const uint8_t kFont5x7[128][5] = {
    ['A'] = { 0x7E, 0x11, 0x11, 0x11, 0x7E },
    ['B'] = { 0x7F, 0x49, 0x49, 0x49, 0x36 },
    ['C'] = { 0x3E, 0x41, 0x41, 0x41, 0x22 },
    ['D'] = { 0x7F, 0x41, 0x41, 0x22, 0x1C },
    ['E'] = { 0x7F, 0x49, 0x49, 0x49, 0x41 },
    ['F'] = { 0x7F, 0x09, 0x09, 0x09, 0x01 },
    ['G'] = { 0x3E, 0x41, 0x49, 0x49, 0x7A },
    ['H'] = { 0x7F, 0x08, 0x08, 0x08, 0x7F },
    ['I'] = { 0x00, 0x41, 0x7F, 0x41, 0x00 },
    ['J'] = { 0x20, 0x40, 0x41, 0x3F, 0x01 },
    ['K'] = { 0x7F, 0x08, 0x14, 0x22, 0x41 },
    ['L'] = { 0x7F, 0x40, 0x40, 0x40, 0x40 },
    ['M'] = { 0x7F, 0x02, 0x0C, 0x02, 0x7F },
    ['N'] = { 0x7F, 0x04, 0x08, 0x10, 0x7F },
    ['O'] = { 0x3E, 0x41, 0x41, 0x41, 0x3E },
    ['P'] = { 0x7F, 0x09, 0x09, 0x09, 0x06 },
    ['R'] = { 0x7F, 0x09, 0x19, 0x29, 0x46 },
    ['S'] = { 0x46, 0x49, 0x49, 0x49, 0x31 },
    ['T'] = { 0x01, 0x01, 0x7F, 0x01, 0x01 },
    ['U'] = { 0x3F, 0x40, 0x40, 0x40, 0x3F },
    ['V'] = { 0x1F, 0x20, 0x40, 0x20, 0x1F },
    ['X'] = { 0x63, 0x14, 0x08, 0x14, 0x63 },
    ['Y'] = { 0x07, 0x08, 0x70, 0x08, 0x07 },
    ['Z'] = { 0x61, 0x51, 0x49, 0x45, 0x43 },
    ['+'] = { 0x08, 0x08, 0x3E, 0x08, 0x08 },
    ['-'] = { 0x08, 0x08, 0x08, 0x08, 0x08 },
};

void drawCharToBuffer(std::vector<uint8_t>& buf, int w, int h, int cx, int cy, char ch, int scale, uint8_t r, uint8_t g, uint8_t b, uint8_t a)
{
    if (ch < 0 || ch >= 128) return;
    const uint8_t* colBits = kFont5x7[(unsigned char)ch];
    int startX = cx - (5 * scale) / 2;
    int startY = cy - (7 * scale) / 2;

    for (int col = 0; col < 5; ++col) {
        uint8_t bits = colBits[col];
        for (int row = 0; row < 7; ++row) {
            if (bits & (1 << row)) {
                for (int sy = 0; sy < scale; ++sy) {
                    for (int sx = 0; sx < scale; ++sx) {
                        int px = startX + col * scale + sx;
                        int py = startY + row * scale + sy;
                        if (px >= 0 && px < w && py >= 0 && py < h) {
                            size_t idx = (size_t(py) * w + px) * 4;
                            buf[idx + 0] = r;
                            buf[idx + 1] = g;
                            buf[idx + 2] = b;
                            buf[idx + 3] = a;
                        }
                    }
                }
            }
        }
    }
}

void drawTextToBuffer(std::vector<uint8_t>& buf, int w, int h, int cx, int cy, const char* str, int scale, uint8_t r, uint8_t g, uint8_t b, uint8_t a)
{
    if (!str) return;
    int len = (int)strlen(str);
    int totalWidth = len * 6 * scale;
    int curX = cx - totalWidth / 2 + (3 * scale);

    for (int i = 0; i < len; ++i) {
        drawCharToBuffer(buf, w, h, curX, cy, str[i], scale, r, g, b, a);
        curX += 6 * scale;
    }
}

// Generate circular button sprite
Sprite makeButtonSprite(int size, uint8_t r, uint8_t g, uint8_t b, const char* label, bool pressed)
{
    Sprite sp;
    sp.w = size;
    sp.h = size;
    sp.pixels.resize(size * size * 4, 0);

    float center = (size - 1) * 0.5f;
    float radius = size * 0.46f;
    float innerR = radius - 3.5f;

    for (int y = 0; y < size; ++y) {
        for (int x = 0; x < size; ++x) {
            float dx = x - center;
            float dy = y - center;
            float dist = std::sqrt(dx * dx + dy * dy);
            size_t idx = (size_t(y) * size + x) * 4;

            if (dist <= radius) {
                float edge = std::max(0.0f, std::min(1.0f, radius - dist));
                float ring = (dist >= innerR) ? 1.0f : 0.0f;

                uint8_t alpha = uint8_t(edge * (pressed ? 240 : 180));
                uint8_t cr = r;
                uint8_t cg = g;
                uint8_t cb = b;

                if (ring > 0.5f) {
                    // Crisp luminous border
                    cr = std::min(255, r + 70);
                    cg = std::min(255, g + 70);
                    cb = std::min(255, b + 70);
                    alpha = uint8_t(edge * 245);
                } else if (pressed) {
                    // Bright pressed core
                    cr = std::min(255, r + 90);
                    cg = std::min(255, g + 90);
                    cb = std::min(255, b + 90);
                } else {
                    // Radial gradient glass depth
                    float grad = 1.0f - (dist / innerR) * 0.35f;
                    cr = uint8_t(r * grad);
                    cg = uint8_t(g * grad);
                    cb = uint8_t(b * grad);
                }

                sp.pixels[idx + 0] = cr;
                sp.pixels[idx + 1] = cg;
                sp.pixels[idx + 2] = cb;
                sp.pixels[idx + 3] = alpha;
            }
        }
    }

    // Draw label in center
    int fontScale = size >= 90 ? 3 : (size >= 55 ? 2 : 1);
    drawTextToBuffer(sp.pixels, size, size, (int)center, (int)center, label, fontScale, 255, 255, 255, 255);

    return sp;
}

// Generate joystick base ring sprite
Sprite makeStickBaseSprite(int size)
{
    Sprite sp;
    sp.w = size;
    sp.h = size;
    sp.pixels.resize(size * size * 4, 0);

    float center = (size - 1) * 0.5f;
    float outerR = size * 0.47f;
    float innerR = outerR * 0.82f;

    for (int y = 0; y < size; ++y) {
        for (int x = 0; x < size; ++x) {
            float dx = x - center;
            float dy = y - center;
            float dist = std::sqrt(dx * dx + dy * dy);
            size_t idx = (size_t(y) * size + x) * 4;

            if (dist <= outerR) {
                float alpha = 0.0f;
                uint8_t cr = 220, cg = 220, cb = 230;

                if (dist >= innerR) {
                    // Outer ring
                    float edgeOut = std::max(0.0f, std::min(1.0f, outerR - dist));
                    float edgeIn = std::max(0.0f, std::min(1.0f, dist - innerR));
                    alpha = edgeOut * edgeIn * 190.0f;
                } else {
                    // Translucent frosted glass dish
                    alpha = 50.0f * (1.0f - dist / innerR * 0.5f);
                    cr = 80; cg = 100; cb = 120;
                }

                sp.pixels[idx + 0] = cr;
                sp.pixels[idx + 1] = cg;
                sp.pixels[idx + 2] = cb;
                sp.pixels[idx + 3] = uint8_t(alpha);
            }
        }
    }

    // Directional tick marks (Up, Down, Left, Right)
    int tickLen = int(size * 0.08f);
    int mid = int(center);
    for (int i = 0; i < tickLen; ++i) {
        // Up
        drawCharToBuffer(sp.pixels, size, size, mid, mid - int(innerR) - i, '|', 1, 255, 255, 255, 180);
        // Down
        drawCharToBuffer(sp.pixels, size, size, mid, mid + int(innerR) + i, '|', 1, 255, 255, 255, 180);
        // Left
        drawCharToBuffer(sp.pixels, size, size, mid - int(innerR) - i, mid, '-', 1, 255, 255, 255, 180);
        // Right
        drawCharToBuffer(sp.pixels, size, size, mid + int(innerR) + i, mid, '-', 1, 255, 255, 255, 180);
    }

    return sp;
}

// Generate joystick knob sprite
Sprite makeStickKnobSprite(int size, bool pressed)
{
    Sprite sp;
    sp.w = size;
    sp.h = size;
    sp.pixels.resize(size * size * 4, 0);

    float center = (size - 1) * 0.5f;
    float outerR = size * 0.46f;
    float ringR = outerR * 0.60f;

    for (int y = 0; y < size; ++y) {
        for (int x = 0; x < size; ++x) {
            float dx = x - center;
            float dy = y - center;
            float dist = std::sqrt(dx * dx + dy * dy);
            size_t idx = (size_t(y) * size + x) * 4;

            if (dist <= outerR) {
                float edge = std::max(0.0f, std::min(1.0f, outerR - dist));
                uint8_t cr, cg, cb;
                uint8_t alpha = uint8_t(edge * (pressed ? 245 : 210));

                if (pressed) {
                    cr = 65; cg = 165; cb = 245; // Vibrant glowing blue knob
                } else {
                    cr = 160; cg = 175; cb = 195; // Tactile GameCube grey
                }

                // Inner concentric grip ring
                if (std::abs(dist - ringR) < 1.5f) {
                    cr = std::min(255, cr + 50);
                    cg = std::min(255, cg + 50);
                    cb = std::min(255, cb + 50);
                }

                sp.pixels[idx + 0] = cr;
                sp.pixels[idx + 1] = cg;
                sp.pixels[idx + 2] = cb;
                sp.pixels[idx + 3] = alpha;
            }
        }
    }
    return sp;
}

void rebuildSprites()
{
    int baseStickSize = int(s_winH * 0.28f);
    if (baseStickSize < 64) baseStickSize = 64;
    if (baseStickSize > 256) baseStickSize = 256;
    s_stickBaseSprite = makeStickBaseSprite(baseStickSize);

    int knobSize = int(baseStickSize * 0.44f);
    s_stickKnobSprite = makeStickKnobSprite(knobSize, false);
    s_stickKnobPressedSprite = makeStickKnobSprite(knobSize, true);

    for (int i = 0; i < TBTN_COUNT; ++i) {
        int btnPx = int(s_buttons[i].relRadius * 2.0f * s_winH);
        if (btnPx < 32) btnPx = 32;
        if (btnPx > 180) btnPx = 180;
        s_buttonSprites[i][0] = makeButtonSprite(btnPx, s_buttons[i].baseR, s_buttons[i].baseG, s_buttons[i].baseB, s_buttons[i].label, false);
        s_buttonSprites[i][1] = makeButtonSprite(btnPx, s_buttons[i].baseR, s_buttons[i].baseG, s_buttons[i].baseB, s_buttons[i].label, true);
    }
}

} // namespace

extern "C" {

void touch_controller_init(void)
{
    if (s_inited) return;
    s_inited = true;

    // Default positioning for stick
    s_stick.active = false;
    s_stick.fingerId = -1;
    s_stick.radius = s_winH * 0.14f;
    s_stick.anchorX = s_winW * 0.16f;
    s_stick.anchorY = s_winH * 0.72f;
    s_stick.currX = s_stick.anchorX;
    s_stick.currY = s_stick.anchorY;
    s_stick.stickX = 0;
    s_stick.stickY = 0;

    rebuildSprites();
}

void touch_controller_resize(int winW, int winH)
{
    if (winW <= 0 || winH <= 0) return;
    if (winW == s_winW && winH == s_winH && s_inited) return;

    s_winW = winW;
    s_winH = winH;
    s_stick.radius = winH * 0.14f;
    s_stick.anchorX = winW * 0.16f;
    s_stick.anchorY = winH * 0.72f;
    s_stick.currX = s_stick.anchorX;
    s_stick.currY = s_stick.anchorY;

    rebuildSprites();
    s_inited = true;
}

void touch_controller_on_finger_down(int64_t fingerId, float normX, float normY)
{
    if (!s_visible) return;
    if (!s_inited) touch_controller_init();

    float px = normX * s_winW;
    float py = normY * s_winH;

    // Check buttons first
    for (int i = 0; i < TBTN_COUNT; ++i) {
        float bx = s_buttons[i].relX * s_winW;
        float by = s_buttons[i].relY * s_winH;
        float br = s_buttons[i].relRadius * s_winH;
        float dx = px - bx;
        float dy = py - by;

        if (std::sqrt(dx * dx + dy * dy) <= br * 1.25f) {
            s_buttons[i].pressed = true;
            s_buttons[i].fingerId = fingerId;

            if (s_buttons[i].id == TBTN_OPACITY) {
                touch_controller_cycle_opacity();
            }
            return;
        }
    }

    // Left half: Virtual Joystick
    if (px < s_winW * 0.45f && py > s_winH * 0.28f) {
        if (!s_stick.active) {
            s_stick.active = true;
            s_stick.fingerId = fingerId;
            s_stick.anchorX = px;
            s_stick.anchorY = py;
            s_stick.currX = px;
            s_stick.currY = py;
            s_stick.stickX = 0;
            s_stick.stickY = 0;
            return;
        }
    }

    // Right-middle area: Camera swipe look
    if (px >= s_winW * 0.45f && px <= s_winW * 0.78f) {
        if (!s_cam.active) {
            s_cam.active = true;
            s_cam.fingerId = fingerId;
            s_cam.lastX = px;
            s_cam.lastY = py;
            s_cam.substickX = 0;
            s_cam.substickY = 0;
            return;
        }
    }
}

void touch_controller_on_finger_move(int64_t fingerId, float normX, float normY)
{
    if (!s_visible) return;
    float px = normX * s_winW;
    float py = normY * s_winH;

    // Update joystick if finger matches
    if (s_stick.active && s_stick.fingerId == fingerId) {
        s_stick.currX = px;
        s_stick.currY = py;

        float dx = px - s_stick.anchorX;
        float dy = py - s_stick.anchorY;
        float dist = std::sqrt(dx * dx + dy * dy);
        float maxR = s_stick.radius;

        if (dist > maxR && dist > 0.001f) {
            dx = (dx / dist) * maxR;
            dy = (dy / dist) * maxR;
            s_stick.currX = s_stick.anchorX + dx;
            s_stick.currY = s_stick.anchorY + dy;
            dist = maxR;
        }

        float normDist = dist / maxR;
        if (normDist < 0.10f) {
            s_stick.stickX = 0;
            s_stick.stickY = 0;
        } else {
            s_stick.stickX = (int)std::round((dx / maxR) * 100.0f);
            s_stick.stickY = (int)std::round((-dy / maxR) * 100.0f); // Invert Y: up is positive
        }
        return;
    }

    // Update camera swipe if finger matches
    if (s_cam.active && s_cam.fingerId == fingerId) {
        float dx = px - s_cam.lastX;
        float dy = py - s_cam.lastY;
        s_cam.lastX = px;
        s_cam.lastY = py;

        // Scale swipe delta to C-stick values
        s_cam.substickX = (int)std::max(-100.0f, std::min(100.0f, dx * 4.0f));
        s_cam.substickY = (int)std::max(-100.0f, std::min(100.0f, -dy * 4.0f));
        return;
    }

    // Check sliding into or out of buttons
    for (int i = 0; i < TBTN_COUNT; ++i) {
        if (s_buttons[i].fingerId == fingerId) {
            float bx = s_buttons[i].relX * s_winW;
            float by = s_buttons[i].relY * s_winH;
            float br = s_buttons[i].relRadius * s_winH;
            float dx = px - bx;
            float dy = py - by;
            if (std::sqrt(dx * dx + dy * dy) > br * 1.5f) {
                s_buttons[i].pressed = false;
                s_buttons[i].fingerId = -1;
            }
        }
    }
}

void touch_controller_on_finger_up(int64_t fingerId, float /*normX*/, float /*normY*/)
{
    // Release joystick
    if (s_stick.active && s_stick.fingerId == fingerId) {
        s_stick.active = false;
        s_stick.fingerId = -1;
        s_stick.currX = s_stick.anchorX;
        s_stick.currY = s_stick.anchorY;
        s_stick.stickX = 0;
        s_stick.stickY = 0;
    }

    // Release camera swipe
    if (s_cam.active && s_cam.fingerId == fingerId) {
        s_cam.active = false;
        s_cam.fingerId = -1;
        s_cam.substickX = 0;
        s_cam.substickY = 0;
    }

    // Release buttons
    for (int i = 0; i < TBTN_COUNT; ++i) {
        if (s_buttons[i].fingerId == fingerId) {
            s_buttons[i].pressed = false;
            s_buttons[i].fingerId = -1;
        }
    }
}

void touch_controller_update_pad(PADStatus* status)
{
    if (!status || !s_visible) return;

    // Merge digital buttons
    for (int i = 0; i < TBTN_COUNT; ++i) {
        if (s_buttons[i].pressed && s_buttons[i].padMask != 0) {
            status->button |= s_buttons[i].padMask;
        }
    }

    // Trigger L
    if (s_buttons[TBTN_L].pressed) {
        status->triggerLeft = 255;
        status->button |= PAD_TRIGGER_L;
    }

    // Trigger R modes:
    // TBTN_R: Soft spray (180) -> Mario runs while spraying!
    if (s_buttons[TBTN_R].pressed) {
        if (status->triggerRight < 180) status->triggerRight = 180;
    }
    // TBTN_R_HARD: Full spray (255) -> Lock precision aim spray!
    if (s_buttons[TBTN_R_HARD].pressed) {
        status->triggerRight = 255;
        status->button |= PAD_TRIGGER_R;
    }

    // Analog A / B
    if (s_buttons[TBTN_A].pressed) status->analogA = 255;
    if (s_buttons[TBTN_B].pressed) status->analogB = 255;

    // Left analog stick
    if (s_stick.active && (s_stick.stickX != 0 || s_stick.stickY != 0)) {
        status->stickX = (int8_t)s_stick.stickX;
        status->stickY = (int8_t)s_stick.stickY;
    }

    // Camera swipe substick
    if (s_cam.active) {
        status->substickX = (int8_t)s_cam.substickX;
        status->substickY = (int8_t)s_cam.substickY;
        // Decay swipe delta
        s_cam.substickX = s_cam.substickX * 2 / 3;
        s_cam.substickY = s_cam.substickY * 2 / 3;
    }
}

void touch_controller_draw(int winW, int winH)
{
    if (!s_visible || s_hudOpacity <= 0.01f || !GXPC_DrawOverlay) return;
    if (!s_inited || winW != s_winW || winH != s_winH) {
        touch_controller_resize(winW, winH);
    }

    // 1. Draw virtual joystick
    float baseX = s_stick.active ? s_stick.anchorX : (s_winW * 0.16f);
    float baseY = s_stick.active ? s_stick.anchorY : (s_winH * 0.72f);
    int baseW = s_stickBaseSprite.w;
    int baseH = s_stickBaseSprite.h;
    int drawBaseX = int(baseX - baseW * 0.5f);
    int drawBaseY = int(baseY - baseH * 0.5f);

    GXPC_DrawOverlay(s_stickBaseSprite.pixels.data(), baseW, baseH, drawBaseX, drawBaseY, 1, winW, winH);

    // Knob
    float knobX = s_stick.active ? s_stick.currX : baseX;
    float knobY = s_stick.active ? s_stick.currY : baseY;
    const Sprite& knobSp = s_stick.active ? s_stickKnobPressedSprite : s_stickKnobSprite;
    int drawKnobX = int(knobX - knobSp.w * 0.5f);
    int drawKnobY = int(knobY - knobSp.h * 0.5f);

    GXPC_DrawOverlay(knobSp.pixels.data(), knobSp.w, knobSp.h, drawKnobX, drawKnobY, 1, winW, winH);

    // 2. Draw all buttons
    for (int i = 0; i < TBTN_COUNT; ++i) {
        const Sprite& sp = s_buttonSprites[i][s_buttons[i].pressed ? 1 : 0];
        if (sp.pixels.empty()) continue;

        int bx = int(s_buttons[i].relX * winW - sp.w * 0.5f);
        int by = int(s_buttons[i].relY * winH - sp.h * 0.5f);

        GXPC_DrawOverlay(sp.pixels.data(), sp.w, sp.h, bx, by, 1, winW, winH);
    }
}

void touch_controller_set_opacity(float opacity)
{
    s_hudOpacity = std::max(0.0f, std::min(1.0f, opacity));
}

float touch_controller_get_opacity(void)
{
    return s_hudOpacity;
}

void touch_controller_cycle_opacity(void)
{
    if (s_hudOpacity >= 0.85f) {
        s_hudOpacity = 0.25f;
    } else if (s_hudOpacity >= 0.65f) {
        s_hudOpacity = 0.90f;
    } else if (s_hudOpacity >= 0.40f) {
        s_hudOpacity = 0.70f;
    } else {
        s_hudOpacity = 0.45f;
    }
}

void touch_controller_set_visible(bool visible)
{
    s_visible = visible;
}

bool touch_controller_is_visible(void)
{
    return s_visible;
}

} // extern "C"
