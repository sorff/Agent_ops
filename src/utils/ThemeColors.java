package utils;

import java.awt.Color;
import java.awt.Font;

/**
 * Central color palette and font definitions for the entire app.
 *
 * The design uses a "dark sidebar + light content" layout, inspired by
 * modern SaaS dashboards. The accent color is indigo (#4F46E5).
 *
 * HOW TO CHANGE THE THEME: just update the hex values here —
 * every panel in the app reads from this file, so it all updates at once.
 */
public class ThemeColors {

    // ── Sidebar (left navigation panel) ──────────────────────
    public static final Color SIDEBAR_BG         = new Color(0x1A, 0x1A, 0x2E);  // very dark navy
    public static final Color SIDEBAR_HOVER      = new Color(0x16, 0x21, 0x3E);  // slightly lighter navy
    public static final Color SIDEBAR_SELECTED   = new Color(0x4F, 0x46, 0xE5);  // accent indigo
    public static final Color SIDEBAR_TEXT       = new Color(0x9C, 0xA3, 0xAF);  // muted gray
    public static final Color SIDEBAR_TEXT_ACTIVE= new Color(0xFF, 0xFF, 0xFF);  // white

    // ── Content area (right side) ─────────────────────────────
    public static final Color CONTENT_BG         = new Color(0xF3, 0xF4, 0xF6);  // light gray page bg
    public static final Color CARD_BG            = new Color(0xFF, 0xFF, 0xFF);  // pure white cards
    public static final Color TOPBAR_BG          = new Color(0xFF, 0xFF, 0xFF);  // white top bar

    // ── Accent (primary brand color — indigo) ─────────────────
    public static final Color ACCENT             = new Color(0x4F, 0x46, 0xE5);  // #4F46E5
    public static final Color ACCENT_LIGHT       = new Color(0x6E, 0x6A, 0xFF);  // lighter indigo
    public static final Color ACCENT_VERY_LIGHT  = new Color(0xEE, 0xF2, 0xFF);  // very pale indigo (backgrounds)

    // ── Text ──────────────────────────────────────────────────
    public static final Color TEXT_PRIMARY       = new Color(0x1F, 0x29, 0x37);  // almost black
    public static final Color TEXT_SECONDARY     = new Color(0x6B, 0x72, 0x80);  // medium gray
    public static final Color TEXT_MUTED         = new Color(0x9C, 0xA3, 0xAF);  // light gray
    public static final Color TEXT_ON_ACCENT     = new Color(0xFF, 0xFF, 0xFF);  // white text on buttons

    // ── Status colors ─────────────────────────────────────────
    public static final Color STATUS_ACTIVE      = new Color(0x10, 0xB9, 0x81);  // emerald green
    public static final Color STATUS_IDLE        = new Color(0xF5, 0x9E, 0x0B);  // amber
    public static final Color STATUS_PAUSED      = new Color(0x6B, 0x72, 0x80);  // gray
    public static final Color STATUS_ERROR       = new Color(0xEF, 0x44, 0x44);  // red

    // ── Severity colors (for alerts) ──────────────────────────
    public static final Color SEVERITY_INFO      = new Color(0x3B, 0x82, 0xF6);  // blue
    public static final Color SEVERITY_WARNING   = new Color(0xF5, 0x9E, 0x0B);  // amber
    public static final Color SEVERITY_CRITICAL  = new Color(0xEF, 0x44, 0x44);  // red
    public static final Color SEVERITY_INFO_BG   = new Color(0xEF, 0xF6, 0xFF);
    public static final Color SEVERITY_WARNING_BG= new Color(0xFF, 0xFB, 0xEB);
    public static final Color SEVERITY_CRITICAL_BG=new Color(0xFF, 0xF5, 0xF5);

    // ── Priority colors ───────────────────────────────────────
    public static final Color PRIORITY_LOW       = new Color(0x10, 0xB9, 0x81);  // green
    public static final Color PRIORITY_MEDIUM    = new Color(0xF5, 0x9E, 0x0B);  // amber
    public static final Color PRIORITY_HIGH      = new Color(0xF9, 0x73, 0x16);  // orange
    public static final Color PRIORITY_URGENT    = new Color(0xEF, 0x44, 0x44);  // red

    // ── Borders & dividers ────────────────────────────────────
    public static final Color BORDER             = new Color(0xE5, 0xE7, 0xEB);  // light gray border
    public static final Color BORDER_FOCUS       = new Color(0x4F, 0x46, 0xE5);  // accent when focused

    // ── Department colors (each dept gets its own color) ──────
    public static final Color[] DEPT_COLORS = {
        new Color(0x4F, 0x46, 0xE5),  // indigo   — Sales
        new Color(0x10, 0xB9, 0x81),  // emerald  — Marketing
        new Color(0x3B, 0x82, 0xF6),  // blue     — Support
        new Color(0xF5, 0x9E, 0x0B),  // amber    — HR
        new Color(0xEF, 0x44, 0x44),  // red      — Finance
        new Color(0x8B, 0x5C, 0xF6),  // violet   — Logistics
    };

    // ── Toast colors ──────────────────────────────────────────
    public static final Color TOAST_SUCCESS_BG   = new Color(0xDC, 0xFC, 0xE7);
    public static final Color TOAST_SUCCESS_BORDER= new Color(0x10, 0xB9, 0x81);
    public static final Color TOAST_ERROR_BG     = new Color(0xFF, 0xE4, 0xE4);
    public static final Color TOAST_ERROR_BORDER = new Color(0xEF, 0x44, 0x44);
    public static final Color TOAST_INFO_BG      = new Color(0xEF, 0xF6, 0xFF);
    public static final Color TOAST_INFO_BORDER  = new Color(0x3B, 0x82, 0xF6);
    public static final Color TOAST_WARN_BG      = new Color(0xFF, 0xFB, 0xEB);
    public static final Color TOAST_WARN_BORDER  = new Color(0xF5, 0x9E, 0x0B);

    // ── Fonts ─────────────────────────────────────────────────
    public static final Font FONT_TITLE      = new Font("Segoe UI", Font.BOLD,  22);
    public static final Font FONT_SUBTITLE   = new Font("Segoe UI", Font.BOLD,  16);
    public static final Font FONT_BODY       = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL      = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_BOLD       = new Font("Segoe UI", Font.BOLD,  13);
    public static final Font FONT_MONO       = new Font("JetBrains Mono", Font.PLAIN, 12);
    public static final Font FONT_KPI_NUMBER = new Font("Segoe UI", Font.BOLD,  32);
    public static final Font FONT_SIDEBAR    = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SIDEBAR_BOLD= new Font("Segoe UI", Font.BOLD, 13);

    /**
     * Returns the Color that corresponds to an agent's status string.
     * Used by StatusDot and agent cards throughout the app.
     */
    public static Color getStatusColor(String status) {
        if (status == null) return STATUS_IDLE;
        switch (status.toUpperCase()) {
            case "ACTIVE":  return STATUS_ACTIVE;
            case "IDLE":    return STATUS_IDLE;
            case "PAUSED":  return STATUS_PAUSED;
            case "ERROR":   return STATUS_ERROR;
            default:        return STATUS_IDLE;
        }
    }

    /** Returns the Color for a given priority string */
    public static Color getPriorityColor(String priority) {
        if (priority == null) return PRIORITY_MEDIUM;
        switch (priority.toUpperCase()) {
            case "LOW":    return PRIORITY_LOW;
            case "MEDIUM": return PRIORITY_MEDIUM;
            case "HIGH":   return PRIORITY_HIGH;
            case "URGENT": return PRIORITY_URGENT;
            default:       return PRIORITY_MEDIUM;
        }
    }

    /** Returns the Color for a given alert severity string */
    public static Color getSeverityColor(String severity) {
        if (severity == null) return SEVERITY_INFO;
        switch (severity.toUpperCase()) {
            case "INFO":     return SEVERITY_INFO;
            case "WARNING":  return SEVERITY_WARNING;
            case "CRITICAL": return SEVERITY_CRITICAL;
            default:         return SEVERITY_INFO;
        }
    }

    /** Returns the background Color for a given alert severity */
    public static Color getSeverityBgColor(String severity) {
        if (severity == null) return SEVERITY_INFO_BG;
        switch (severity.toUpperCase()) {
            case "INFO":     return SEVERITY_INFO_BG;
            case "WARNING":  return SEVERITY_WARNING_BG;
            case "CRITICAL": return SEVERITY_CRITICAL_BG;
            default:         return SEVERITY_INFO_BG;
        }
    }
}
