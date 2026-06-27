/** Public entry point — re-exports the entire AzNavRail React Native library surface. */
export * from './AzNavRail';
export * from './AzNavRailScope';
export * from './AzNavHost';
export * from './components/AzButton';
export * from './components/AzToggle';
export * from './components/AzCycler';
export * from './components/AzTextBox';
export * from './components/AzForm';
export * from './components/AzLoad';
export * from './components/AzRoller';
export * from './components/AzDivider';
export * from './components/AzDropdownMenu';
export * from './components/AzBottomSheet';
export * from './components/AzBottomSheetInsetAware';
export * from './components/AzFloatingRail';
export * from './components/AboutOverlay';
export * from './components/MoreFromAzOverlay';
export * from './components/useAzSheetController';
export * from './services/githubDocs';
export * from './services/moreFromAz';
export * from './types';
// `AzToggleProps` / `AzCyclerProps` are declared both as the DSL prop types in `./types` and as the
// component prop types in the component files above, so the two `export *` statements collide.
// Explicitly re-export the canonical DSL versions from `./types` to resolve the ambiguity
// (an explicit re-export takes precedence over `export *`).
export type { AzToggleProps, AzCyclerProps } from './types';
export { AzHelpRailItem, AzHelpSubItem } from './AzNavRailScope';

// --- Status-driven guidance framework (replaces the scripted tutorial) ---
export { AzStatus, AzEdge, AzGoal, AzGuidanceTarget, AzSuppressGuide, AzGuideRenderer } from './guidance/AzGuidanceScope';
export type {
  AzStatusProps,
  AzEdgeProps,
  AzGoalProps,
  AzGuidanceTargetProps,
  AzSuppressGuideProps,
  AzGuideRendererProps,
} from './guidance/AzGuidanceScope';
export { AzGuidanceProvider, useAzGuidanceController } from './guidance/AzGuidanceController';
export type { AzGuidanceController } from './guidance/AzGuidanceController';
export {
  AZ_ITEM_ACTIVE,
  shapeBounds,
  resolveAzHighlight,
  resolveShape,
  resolveItemId,
  resolveTargetId,
  edgeStepKey,
} from './guidance/AzStatus';
export type {
  AzGuideHighlight,
  AzGuideShape,
  AzPathCmd,
  AzShapeBounds,
  AzItemBounds,
  AzCalloutSide,
  AzInstruction,
  AzInstructionStep,
  AzGuidanceSnapshot,
  AzGuideShapeProvider,
  AzGuidanceRenderer,
  AzGuidanceSuppressor,
  AzGoal as AzGoalDef,
  AzEdge as AzEdgeDef,
  AzStatusPredicate,
} from './guidance/AzStatus';
export { AzInstructionOverlay } from './components/AzInstructionOverlay';
export { useActiveStatuses, computeBuiltinStatuses, useGuidanceSuppressed, anySuppressorActive } from './guidance/AzStatusEngine';
export { nextHop, routeInstructions, computeAutoEdges, resolveEdge, toSnapshot, snapshotsOf } from './guidance/AzGuidance';
export type { ResolvedInstruction, GuidanceFrame } from './guidance/AzGuidance';
