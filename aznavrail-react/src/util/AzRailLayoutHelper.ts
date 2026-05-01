import { AzDockingSide, AzOrientation, AzVisualSide } from '../types';

/** Resolved layout parameters produced by `AzRailLayoutHelper.calculateLayout`. */
export interface RailLayoutConfig {
  /** The physical screen edge where the rail appears after rotation is applied. */
  visualSide: AzVisualSide;
  /** Whether items in the rail flow vertically or horizontally after rotation. */
  orientation: AzOrientation;
  /** Alignment corner used when placing the rail in landscape/rotated layouts. */
  alignment: 'TopStart' | 'TopEnd' | 'BottomStart' | 'BottomEnd';
  /** When true, items within the rail should be rendered in reverse order. */
  reverseLayout: boolean;
}

/** Utility class that maps a `dockingSide` + device rotation to resolved visual-layout parameters. */
export class AzRailLayoutHelper {
  /**
   * Calculates the visual layout of the rail given device rotation and docking preferences.
   * @param dockingSide - The logical docking side chosen by the user.
   * @param rotation - Current device rotation in degrees (0, 90, 180, or 270).
   * @param usePhysicalDocking - When true, the rail follows the physical edge across rotations.
   */
  static calculateLayout(
    dockingSide: AzDockingSide,
    rotation: number, // 0, 90, 180, 270
    usePhysicalDocking: boolean
  ): RailLayoutConfig {
    let visualSide: AzVisualSide;

    if (usePhysicalDocking) {
      if (dockingSide === AzDockingSide.LEFT) {
        switch (rotation) {
          case 0: visualSide = AzVisualSide.LEFT; break;
          case 90: visualSide = AzVisualSide.BOTTOM; break;
          case 180: visualSide = AzVisualSide.RIGHT; break;
          case 270: visualSide = AzVisualSide.TOP; break;
          default: visualSide = AzVisualSide.LEFT;
        }
      } else {
        switch (rotation) {
          case 0: visualSide = AzVisualSide.RIGHT; break;
          case 90: visualSide = AzVisualSide.TOP; break;
          case 180: visualSide = AzVisualSide.LEFT; break;
          case 270: visualSide = AzVisualSide.BOTTOM; break;
          default: visualSide = AzVisualSide.RIGHT;
        }
      }
    } else {
      visualSide = dockingSide === AzDockingSide.LEFT ? AzVisualSide.LEFT : AzVisualSide.RIGHT;
    }

    const orientation = (visualSide === AzVisualSide.TOP || visualSide === AzVisualSide.BOTTOM)
      ? AzOrientation.HORIZONTAL
      : AzOrientation.VERTICAL;

    let reverseLayout = false;
    if (usePhysicalDocking) {
      if (dockingSide === AzDockingSide.LEFT) {
        reverseLayout = (rotation === 180 || rotation === 270);
      } else {
        reverseLayout = (rotation === 180 || rotation === 90);
      }
    }

    let alignment: 'TopStart' | 'TopEnd' | 'BottomStart' | 'BottomEnd';
    switch (visualSide) {
      case AzVisualSide.LEFT: alignment = 'TopStart'; break;
      case AzVisualSide.RIGHT: alignment = 'TopEnd'; break;
      case AzVisualSide.TOP: alignment = 'TopStart'; break;
      case AzVisualSide.BOTTOM: alignment = 'BottomStart'; break;
    }

    return { visualSide, orientation, alignment, reverseLayout };
  }
}
