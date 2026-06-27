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

/** What a guidance instruction spotlights. */
export type AzGuideHighlight =
  | { type: 'None' }
  | { type: 'FullScreen' }
  /** Spotlight the rail item with this id (looked up in the item-bounds cache). */
  | { type: 'Item'; id: string }
  /** Spotlight an explicit window-space rectangle. */
  | { type: 'Area'; left: number; top: number; width: number; height: number };

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
 * One edge of the flowchart: while status `from` is true, performing `instruction` is expected to make
 * status `to` true (an interactive hop). A passive edge (`to === null`) just shows info while `from`
 * holds. Authored via `<AzEdge>`, or auto-generated for rail affordances.
 */
export interface AzEdge {
  from: string;
  to: string | null;
  instruction: AzInstruction;
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
