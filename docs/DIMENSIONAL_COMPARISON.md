# Dimensional Data Comparison: Tag 6.99 vs Current (v7.25)

This document provides a comparative analysis of dimensional constants and default values between version 6.99 and the current codebase (v7.25).

## AzNavRailDefaults

| Property | Tag 6.99 | Current (v7.25) | Comparison |
| :--- | :--- | :--- | :--- |
| `SWIPE_THRESHOLD_PX` | `20f` | `20f` | Unchanged |
| `SNAP_BACK_RADIUS_PX` | `50f` | `50f` | Unchanged |
| `HeaderPadding` | `8.dp` | `8.dp` | Unchanged |
| `HeaderIconSize` | `72.dp` | *Removed* | **Removed** |
| `ButtonWidth` | *N/A* | `64.dp` | **New** (Compulsory unified width) |
| `HeaderHeightDp` | *N/A* | `72.dp` | **New** |
| `HeaderTextSpacer` | `8.dp` | `8.dp` | Unchanged |
| `RailContentHorizontalPadding` | `4.dp` | `4.dp` | Unchanged |
| `RailContentVerticalArrangement` | `8.dp` | `8.dp` | Unchanged |
| `RailContentSpacerHeight` | `72.dp` | `72.dp` | Unchanged |
| `MenuItemHorizontalPadding` | `24.dp` | `24.dp` | Unchanged |
| `MenuItemVerticalPadding` | `12.dp` | `12.dp` | Unchanged |
| `FooterDividerHorizontalPadding` | `16.dp` | `16.dp` | Unchanged |
| `FooterDividerVerticalPadding` | `8.dp` | `8.dp` | Unchanged |
| `FooterSpacerHeight` | `12.dp` | `12.dp` | Unchanged |

## AzLayoutConfig

| Property | Tag 6.99 | Current (v7.25) | Comparison |
| :--- | :--- | :--- | :--- |
| `SafeTopPercent` | `0.2f` | *Renamed* | Renamed to `ContentSafeTopPercent` |
| `SafeBottomPercent` | `0.1f` | *Renamed* | Renamed to `ContentSafeBottomPercent` |
| `RailSafeTopPercent` | *N/A* | `0.1f` | **New** |
| `RailSafeBottomPercent` | *N/A* | `0.1f` | **New** |

## AzNavRailScope Defaults

| Property | Tag 6.99 | Current (v7.25) | Comparison |
| :--- | :--- | :--- | :--- |
| `expandedWidth` | `260.dp` | `130.dp` | **Changed** (Reduced significantly) |
| `collapsedWidth` | `80.dp` | `80.dp` | Unchanged |
