1. Move `aznavrail-react-native` to `aznavrail-react` and delete `aznavrail-web`.
Wait, if I delete `aznavrail-web`, is the user's intent to just rename `aznavrail-react-native` to `aznavrail-react`? "There is no need for a "web" library that is distinct from the react library. Bring those two together and merge as one."
Since `aznavrail-react-native` IS a react library (and react native supports web out of the box), renaming it to `aznavrail-react` and deleting `aznavrail-web` seems exactly what "Bring those two together and merge as one" means. We're keeping one codebase for both platforms.

Let's check the contents of `aznavrail-react-native/src/index.tsx`.
