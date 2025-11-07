This document serves as a detailed specification and behavior guide for the AzNavRail component. All changes must be backward-compatible.

- The rail buttons must be a transparent shape with a colored stroke.

- Make sure the app icon is displayed at the top by default. It's the app icon up there by default,
  or it's the
  app name.

- And I don't give a shit how "unstable" you think it is, RESIZE THE TEXT IN THE BUTTONS. The item
  text MUST MUST MUST fit inside the shape, no wrapping allowed without the developer explicitly
  deciding to do so with a newline character.

- To be clear, if the developer has a multi-word string for a rail item, they DO need the ability to
  put the words on separate lines.

- For a toggle option, there should NOT be a toggle component. There should be just the menu item's
  text,which switches to the other text when it changes states. So, for example, The menu option
  would be Power On when the power is on. Tap it, and it then displays Power Off and also changes
  the state, accordingly.

- The cycler must work similarly. It displays Option A and that's what's enabled. Tap it and you see
  Option B take its place. Tap it again and you see Option C. Leave it at Option C for 1 second, and
  Option C enables.
-
- So, to be clear, for a toggle, you need to collect TWO strings. For a cycler, you need to collect
  at least THREE strings. The developer should be able to use whatever text they want for each state
  of both items.

- When any item in the menu is tapped, this should execute whatever action it is for AND collapse
  the rail.

Support hierarchical navigation with host and sub-items. This allows you to create nested menus that
are easy to navigate.

- **Host Items**: These are top-level items that can contain sub-items. They can be placed in the
  rail or the menu.

- **Sub-Items**: These are nested items, toggles, and cyclers that are only visible when their host
  item is expanded. They can also be placed in the rail or the menu. A rail Sub item must be the
  child of a rail host, but menu sub items may be the child of a rail host or menu host.

Long press the app icon/name to activate fab mode for dragging around the screen.

Haptic feedback should notify the user when FAB mode is activated and deactivated. If the developer
has activated the App Name instead of app icon at the top, that text should NOT be resized. It
should NOT be constrained to a shape, nor to the width of the AzNavRail. That text should NOT be
wrapped, and it should NOT be clipped. It should be allowed whatever width the developer wants,
extending across the screen. But, when that text is tapped and held to activate dragging around the
screen, it MUST transform into the app icon. The AzNavRail can only be dragged around the screen as
an App icon. So it transforms into app icon when dragging is enabled, and transforms back to the app
name when docked back into place.

When I tap the app icon/name, it should expand or collapse the rail, revealing or hiding the menu.
When activating the drag and place function, otherwise called FAB mode, if the app name was enabled,
it turns into an app icon. All of the rail items
fold up into the app icon, and the icon can be moved anywhere on the screen as a fab. In this mode,
tapping the app icon causes all of the rail items to unfold downward. If the app icon is tapped
again, the rail items fold back up. If the app icon is dragged while the rail items are unfolded,
then they immediately fold back up until the app icon is released. It's important that while the app
icon is draggable,

To disable dragging, the user drags the app icon to its home location, which is where it was when in
docked mode.
If the user brings the app icon within half the app icon's width of its home location, the app icon
should snap back into place, activating the original docked mode.

I need the sample app to show in the logcat every function it actively performs and every user
interaction.

The only haptic feedback should be when fab mode is activated and deactivated, not at the start and
end of every drag event.

The menu SHOULD expand and collapse on single tap of the app icon/name.

the area for swipe to collapse SHOULD be a little wider than the expanded menu.

Tapping outside of the menu should also collapse the menu. 

Also, when in FAB mode, the app icon should snap back into place when brought near its original
docked position in non-fab mode

in FAB mode, if the app icon is long pressed, this should immediately disable FAB mode and redock
the rail.

the MENU is never supposed to be present when in FAB mode. If the app icon is long pressed while the
menu is expanded, it should fold up into the app icon, and when in fab mode and the app icon is
tapped, this should unfold the RAIL, not the menu. The menu should NEVER be available in FAB mode.

both a tap and a long press are defined not by when the touch begins but when it ends. So the logic
that makes a long press shouldn't be interfering with the logic that makes a tap. The gesture
listener hears the touch begin, and then, if it ends before what is considered a long press, then
it's considered a tap.

let's have two kinds of swipes. Horizontal swipes expand and collapse the rail. But a vertical swipe
immediately initiates FAB mode and undocks the rail.
swipe up causes all the rail/menu items to fold up into the app icon. This means the rail is in FAB
mode, in a resting state.
A swipe down when docked immediately initiates FAB mode and causes the app icon to be dragged, so
all the items fold up and the app icon is already being dragged around.
The vertical swipe logic should apply to the entire rail. A swipe up might start at the bottom or
the middle of the rail. A swipe down will always start near the app icon/name.
In fab mode, dragging must not be mistaken for a long press.
In FAB mode, the app icon must NOT be allowed above the top 10% or the bottom 10% of the screen.
Also, in FAB mode, a packed rail must be forced at all times. And, if the rail items are displayed,
when a drag begins, the rail items must immediately fold up into the app icon. When the rail items
are visible when a drag begins, they must unfold downward when the drag ends
In FAB mode, the rail items must also not be allowed above the top 10% of the screen nor the bottom
10% of the screen. This means that the rail should unfold downward, and push the location of the app
icon upward if necessary.