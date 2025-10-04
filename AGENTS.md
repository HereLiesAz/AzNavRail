- The rail buttons must be a transparent circle with a colored stroke.
- Make sure the app icon is displayed at the top by default. It's the app icon up there, or it's the
  app name. Do not stick either of them inside a circle like it's just another button. Tapping the
  appIcon/appName expands the rail and should NEVER be constrained into a circle. It should be
  allowed to be whatever it is, no resizing. 
- On the other hand, I DO want the appIcon to be as wide as the circle buttons. Not the appName, mind you. That can be however big it needs to be, according to the developer's theme. Just the appIcon.
- And I don't give a shit how "unstable" you think it is, RESIZE THE TEXT IN THE BUTTONS. The item
  text MUST MUST MUST fit inside the circle, no wrapping allowed without the developer explicitly
  deciding to do so!
- To be clear, if the developer has a multi-word string for a rail item, they DO need the ability to
  put the words on separate lines.
- For a toggle option, there should NOT be a toggle component. There should be just the menu option,
  which switches to the other text when it changes states.
- So, for example, The menu option would be Power On when the power is on. Tap it, and it then
  displays Power Off and also changes the state, accordingly.
- The cycler must work similarly. It displays Option A and that's what's enabled. Tap it and you see
  Option B take its place. Tap it again and you see Option C. Leave it at Option C for 1 second, and
  Option C enables.
- So, to be clear, for a toggle, you need to collect TWO strings. For a cycler, you need to collect
  at least THREE strings. The developer should be able to use whatever text they want for each state
  of both items.
- When any item in the menu is tapped, this should execute whatever action it is for AND collapse
  the rail.