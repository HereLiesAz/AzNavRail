import { AzDockingSide, AzOrientation, AzVisualSide } from '../types';

export interface RailLayoutConfig {
  visualSide: AzVisualSide;
  orientation: AzOrientation;
  alignment: 'TopStart' | 'TopEnd' | 'BottomStart' | 'BottomEnd';
  reverseLayout: boolean;
}

export class AzRailLayoutHelper {
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
