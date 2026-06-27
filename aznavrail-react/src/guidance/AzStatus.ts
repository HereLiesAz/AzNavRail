import type { ReactNode } from 'react';

/**
 * Reactive, status-driven guidance model — the replacement for the scripted scene/card tutorial.
 *
 * The app (and `AzNavRail` automatically) describe the userflow as a flowchart of **statuses** (nodes)
 * and **edges** (transitions, each carrying the instruction to traverse it). The developer declares one
 * or more **goals** (target statuses) and activates them on the {@link AzGuidanceController}; the engine
 * observes which statuses are currently true and always shows the instruction to reach the next status
 * on the path toward each active goal, auto-advancing the instant a target status becomes true.
 *
 * A "status" is just a string id, resolved uniformly by the engine from three sources:
 *  - a developer predicate registered via `<AzStatus id predicate />`,
 *  - an active classifier name (true while it is in `activeClassifiers`),
 *  - a built-in `az.*` id derived from the live rail/host/route/help state.
 */

/**
 * The dynamic highlight token: passed anywhere a `highlightItemId` is accepted, it spotlights whatever
 * rail item is currently `*.active` (resolved fresh every frame). Degrades to text-only when none is.
 */
export const AZ_ITEM_ACTIVE = 'az.item.active';

/** What a guidance instruction spotlights. */
export type AzGuideHighlight =
  | { type: 'None' }
  | { type: 'FullScreen' }
  /** Spotlight the rail item with this id (looked up in the item-bounds cache). */
  | { type: 'Item'; id: string }
  /** Spotlight an explicit window-space rectangle. */
  | { type: 'Area'; left: number; top: number; width: number; height: number }
  /** Spotlight whatever rail item is currently active (resolved at render time). Text-only if none. */
  | { type: 'ActiveItem' }
  /** Spotlight the rail item whose id `selector` returns each frame (runtime/dynamic items). */
  | { type: 'Dynamic'; selector: () => string | null }
  /** Spotlight a host-registered arbitrary on-screen shape (see `<AzGuidanceTarget>`). Text-only if absent. */
  | { type: 'Target'; id: string };

/**
 * A spotlight geometry in **window-space px** (the same space as the item-bounds cache, so it aligns
 * with the overlay at zero offset). Returned by an `<AzGuidanceTarget>` shape function each frame, so a
 * target can track a moving on-screen object. `padding` inflates the spotlight uniformly.
 */
export type AzGuideShape =
  | { type: 'Circle'; cx: number; cy: number; radius: number; padding?: number }
  | { type: 'Rect'; left: number; top: number; width: number; height: number; cornerRadius?: number; padding?: number }
  | { type: 'Path'; commands: AzPathCmd[]; padding?: number };

/** One command of an `AzGuideShape` path, in absolute window-space px (SVG-style). */
export type AzPathCmd =
  | { type: 'M'; x: number; y: number }
  | { type: 'L'; x: number; y: number }
  | { type: 'Q'; x1: number; y1: number; x: number; y: number }
  | { type: 'C'; x1: number; y1: number; x2: number; y2: number; x: number; y: number }
  | { type: 'Z' };

/** Axis-aligned window-space bounds of a shape (inflated by its padding); used for callout placement. */
export interface AzShapeBounds {
  left: number;
  top: number;
  width: number;
  height: number;
}

/** Computes the axis-aligned bounds of an `AzGuideShape` (mirrors Kotlin `AzGuideShape.bounds()`). */
export function shapeBounds(shape: AzGuideShape): AzShapeBounds {
  const p = shape.padding ?? 0;
  if (shape.type === 'Circle') {
    return { left: shape.cx - shape.radius - p, top: shape.cy - shape.radius - p, width: 2 * (shape.radius + p), height: 2 * (shape.radius + p) };
  }
  if (shape.type === 'Rect') {
    return { left: shape.left - p, top: shape.top - p, width: shape.width + 2 * p, height: shape.height + 2 * p };
  }
  let minX = Infinity;
  let minY = Infinity;
  let maxX = -Infinity;
  let maxY = -Infinity;
  const acc = (x: number, y: number) => {
    minX = Math.min(minX, x); minY = Math.min(minY, y); maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
  };
  for (const c of shape.commands) {
    if (c.type === 'M' || c.type === 'L') acc(c.x, c.y);
    else if (c.type === 'Q') { acc(c.x1, c.y1); acc(c.x, c.y); }
    else if (c.type === 'C') { acc(c.x1, c.y1); acc(c.x2, c.y2); acc(c.x, c.y); }
  }
  if (minX > maxX) return { left: 0, top: 0, width: 0, height: 0 };
  return { left: minX - p, top: minY - p, width: maxX - minX + 2 * p, height: maxY - minY + 2 * p };
}

/** Preferred placement of a callout relative to its highlight target. */
export type AzCalloutSide = 'Auto' | 'Above' | 'Below' | 'Start' | 'End';

/**
 * The instruction shown while the user traverses one edge. Rendered as a callout placed **adjacent to
 * its [highlight] target** — next to the control the user would use to accomplish that hop. When several
 * goals are active, every active instruction is shown at once, each by its own target.
 */
export interface AzInstruction {
  text: string;
  title?: string;
  highlight?: AzGuideHighlight;
  side?: AzCalloutSide;
  /** Optional inline media rendered under the text. */
  media?: () => ReactNode;
}

/**
 * One paged sub-step of a multi-step edge. A single milestone can carry several, revealed one at a time
 * (informational steps advance on tap; a step with `advanceWhen` advances reactively), moving the
 * spotlight as the user reads.
 */
export interface AzInstructionStep {
  text: string;
  title?: string;
  /** Rail item id to spotlight; the `AZ_ITEM_ACTIVE` token resolves to the active item. */
  highlightItemId?: string;
  /** A host-registered arbitrary on-screen target (see `<AzGuidanceTarget>`); highest precedence. */
  highlightTargetId?: string;
  side?: AzCalloutSide;
  /** Resolved every frame to the rail item id to spotlight (runtime/dynamic items). */
  highlightSelector?: () => string | null;
  /** Status id that auto-advances past this step when true (reactive wins over the tap cursor). */
  advanceWhen?: string;
}

/**
 * One edge of the flowchart: while status `from` is true, performing `instruction` is expected to make
 * status `to` true (an interactive hop). A passive edge (`to === null`) just shows info while `from`
 * holds. When `steps` is non-empty the edge is **paged** (revealed one step at a time). Authored via
 * `<AzEdge>`, or auto-generated for rail affordances.
 */
export interface AzEdge {
  from: string;
  to: string | null;
  instruction: AzInstruction;
  steps?: AzInstructionStep[];
}

/**
 * A read-only snapshot of one guidance callout currently being shown, published live on the controller
 * (`currentInstructions`) so a host can mirror it with bespoke rendering and analytics.
 */
export interface AzGuidanceSnapshot {
  text: string;
  title?: string;
  goalId: string | null;
  highlight: AzGuideHighlight;
  targetId: string | null;
  resolvedShape: AzGuideShape | null;
  resolvedBounds: AzShapeBounds | null;
  stepIndex: number;
  stepTotal: number;
  stepKey: string;
}

/** Resolves a highlight directive into an `AzGuideHighlight` (mirrors Kotlin `resolveAzHighlight`). */
export function resolveAzHighlight(
  highlightItemId?: string,
  highlightSelector?: () => string | null,
  highlightTargetId?: string,
): AzGuideHighlight {
  if (highlightTargetId != null) return { type: 'Target', id: highlightTargetId };
  if (highlightSelector != null) return { type: 'Dynamic', selector: highlightSelector };
  if (highlightItemId === AZ_ITEM_ACTIVE) return { type: 'ActiveItem' };
  if (highlightItemId != null) return { type: 'Item', id: highlightItemId };
  return { type: 'None' };
}

/** This step's highlight directive as an `AzGuideHighlight`. */
export function stepHighlight(step: AzInstructionStep): AzGuideHighlight {
  return resolveAzHighlight(step.highlightItemId, step.highlightSelector, step.highlightTargetId);
}

/** This step as a standalone `AzInstruction` (used when the overlay pages a stepped edge). */
export function stepToInstruction(step: AzInstructionStep, edgeTitle?: string): AzInstruction {
  return { text: step.text, title: step.title ?? edgeTitle, highlight: stepHighlight(step), side: step.side };
}

/** The rail item id a highlight points at (folding ActiveItem/Dynamic against the live active id). */
export function resolveItemId(h: AzGuideHighlight, activeItemId: string | null): string | null {
  if (h.type === 'Item') return h.id;
  if (h.type === 'ActiveItem') return activeItemId;
  if (h.type === 'Dynamic') {
    const id = h.selector();
    return id === AZ_ITEM_ACTIVE ? activeItemId : id;
  }
  return null;
}

/** The resolved target/item id this highlight points at, if any (for the snapshot/analytics). */
export function resolveTargetId(h: AzGuideHighlight, activeItemId: string | null): string | null {
  if (h.type === 'Target') return h.id;
  return resolveItemId(h, activeItemId);
}

/** Window-space item bounds as tracked by the rail (`UIManager.measureInWindow`). */
export interface AzItemBounds {
  x: number;
  y: number;
  width: number;
  height: number;
}

/** Resolves a highlight to the concrete `AzGuideShape` to spotlight (window-space px), or null. */
export function resolveShape(
  h: AzGuideHighlight,
  cache: Record<string, AzItemBounds>,
  activeItemId: string | null,
  targets: Record<string, () => AzGuideShape | null>,
): AzGuideShape | null {
  if (h.type === 'Area') return { type: 'Rect', left: h.left, top: h.top, width: h.width, height: h.height };
  if (h.type === 'Target') return targets[h.id]?.() ?? null;
  const itemId = resolveItemId(h, activeItemId);
  if (itemId == null) return null;
  const b = cache[itemId];
  return b ? { type: 'Rect', left: b.x, top: b.y, width: b.width, height: b.height } : null;
}

/** Stable per-edge cursor key (string fields only), matching Kotlin `AzEdge.stepKey()`. */
export function edgeStepKey(edge: AzEdge): string {
  return `${edge.from} ${edge.to ?? ''} ${edge.instruction.text}`;
}

/**
 * A developer-declared guidance target. The engine routes from the current status toward `target`; the
 * developer activates/deactivates goals on the controller (several may guide at once). When
 * `autoStartWhen` is set the goal self-activates once that status becomes true (onboarding-style).
 */
export interface AzGoal {
  id: string;
  target: string;
  label?: string;
  autoStartWhen?: string | null;
}

/** A status predicate — a named boolean over app state. */
export type AzStatusPredicate = () => boolean;

/** A moving-target shape provider (window-space px each frame), registered via `<AzGuidanceTarget>`. */
export type AzGuideShapeProvider = () => AzGuideShape | null;

/** Host renderer replacing the built-in callout body (the dim/spotlight still draws). */
export type AzGuidanceRenderer = (snapshot: AzGuidanceSnapshot, bounds: AzShapeBounds | null) => ReactNode;

/** A registered guidance suppressor: `[settleMs, predicate]`. */
export type AzGuidanceSuppressor = [number, () => boolean];
