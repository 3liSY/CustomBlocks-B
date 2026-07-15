/**
 * CaptureOverlayNative.java - GROUP 29 (Shorts Framing Overlay). CLIENT-SIDE ONLY.
 *
 * Responsibility: the small Win32 surface behind the capture helper: read Minecraft's
 * native window bounds, make the overlay click-through/topmost, apply capture exclusion,
 * and toggle a native borderless window style.
 *
 * Depends on: JNA user32.dll bindings.
 * Called by: CaptureOverlayWindow, CaptureOverlayManager.
 */
package com.customblocks.client.capture;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.glfw.GLFWNativeWin32;

import java.awt.Window;

@Environment(EnvType.CLIENT)
final class CaptureOverlayNative {

    private CaptureOverlayNative() {}

    private static final int WDA_EXCLUDEFROMCAPTURE = 0x00000011;
    private static final int WS_EX_TOPMOST = 0x00000008;
    private static final int WS_EX_NOACTIVATE = 0x08000000;
    private static final int WS_EX_TOOLWINDOW = 0x00000080;
    private static final int WS_POPUP = 0x80000000;
    private static final int WS_VISIBLE = 0x10000000;
    private static final WinDef.HWND HWND_TOP = new WinDef.HWND(Pointer.createConstant(0));
    private static final WinDef.HWND HWND_TOPMOST = new WinDef.HWND(Pointer.createConstant(-1));

    private static SavedWindow saved;

    static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
    }

    static long hwndFromGlfw(long glfwHandle) {
        if (!isWindows() || glfwHandle == 0L) return 0L;
        try {
            return GLFWNativeWin32.glfwGetWin32Window(glfwHandle);
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    static Rect windowRect(long hwndValue) {
        if (!isWindows() || hwndValue == 0L) return null;
        WinDef.RECT r = new WinDef.RECT();
        if (!User32.INSTANCE.GetWindowRect(hwnd(hwndValue), r)) return null;
        return new Rect(r.left, r.top, r.right - r.left, r.bottom - r.top);
    }

    static boolean applyOverlayStyles(Window window) {
        if (!isWindows() || window == null) return false;
        WinDef.HWND h = new WinDef.HWND(Native.getWindowPointer(window));
        int ex = User32.INSTANCE.GetWindowLong(h, WinUser.GWL_EXSTYLE);
        ex |= WinUser.WS_EX_LAYERED | WinUser.WS_EX_TRANSPARENT | WS_EX_TOPMOST
                | WS_EX_TOOLWINDOW | WS_EX_NOACTIVATE;
        User32.INSTANCE.SetWindowLong(h, WinUser.GWL_EXSTYLE, ex);
        User32.INSTANCE.SetWindowPos(h, HWND_TOPMOST, 0, 0, 0, 0,
                WinUser.SWP_NOMOVE | WinUser.SWP_NOSIZE | WinUser.SWP_NOACTIVATE
                        | WinUser.SWP_FRAMECHANGED | WinUser.SWP_SHOWWINDOW);
        boolean affinity = ExtraUser32.INSTANCE.SetWindowDisplayAffinity(h, WDA_EXCLUDEFROMCAPTURE);
        return affinity;
    }

    static boolean toggleBorderless(long hwndValue) {
        if (!isWindows() || hwndValue == 0L) return false;
        WinDef.HWND h = hwnd(hwndValue);
        if (saved != null) {
            User32.INSTANCE.SetWindowLong(h, WinUser.GWL_STYLE, saved.style);
            User32.INSTANCE.SetWindowLong(h, WinUser.GWL_EXSTYLE, saved.exStyle);
            User32.INSTANCE.SetWindowPos(h, null, saved.rect.x, saved.rect.y, saved.rect.w, saved.rect.h,
                    WinUser.SWP_NOZORDER | WinUser.SWP_NOOWNERZORDER | WinUser.SWP_FRAMECHANGED | WinUser.SWP_SHOWWINDOW);
            saved = null;
            return true;
        }

        Rect r = windowRect(hwndValue);
        WinUser.MONITORINFO monitor = new WinUser.MONITORINFO();
        WinUser.HMONITOR mon = User32.INSTANCE.MonitorFromWindow(h, WinUser.MONITOR_DEFAULTTONEAREST);
        if (r == null || !User32.INSTANCE.GetMonitorInfo(mon, monitor).booleanValue()) return false;

        saved = new SavedWindow(
                User32.INSTANCE.GetWindowLong(h, WinUser.GWL_STYLE),
                User32.INSTANCE.GetWindowLong(h, WinUser.GWL_EXSTYLE),
                r
        );
        int x = monitor.rcMonitor.left;
        int y = monitor.rcMonitor.top;
        int w = monitor.rcMonitor.right - monitor.rcMonitor.left;
        int hgt = monitor.rcMonitor.bottom - monitor.rcMonitor.top;
        User32.INSTANCE.SetWindowLong(h, WinUser.GWL_STYLE, WS_POPUP | WS_VISIBLE);
        User32.INSTANCE.SetWindowPos(h, HWND_TOP, x, y, w, hgt,
                WinUser.SWP_NOOWNERZORDER | WinUser.SWP_FRAMECHANGED | WinUser.SWP_SHOWWINDOW);
        return true;
    }

    static boolean borderlessActive() {
        return saved != null;
    }

    private static WinDef.HWND hwnd(long value) {
        return new WinDef.HWND(Pointer.createConstant(value));
    }

    record Rect(int x, int y, int w, int h) {}
    private record SavedWindow(int style, int exStyle, Rect rect) {}

    private interface ExtraUser32 extends StdCallLibrary {
        ExtraUser32 INSTANCE = Native.load("user32", ExtraUser32.class, W32APIOptions.DEFAULT_OPTIONS);
        boolean SetWindowDisplayAffinity(WinDef.HWND hWnd, int affinity);
    }
}
