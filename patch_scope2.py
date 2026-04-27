with open('aznavrail-react/src/AzNavRailScope.tsx', 'r') as f:
    content = f.read()

# Instead of patching useAzItem, let's fix it properly using useMemo inside the components,
# or use object identity for the properties if possible. But actually the prompt says:
# "The architecture relies on useAzItem pushing objects to context via useEffect.
# Because the item passed from the wrapper components is a newly allocated object literal on every single render, the useEffect dependency array triggers constantly. The React author tried to patch this by doing a massive, computationally heavy JSON.stringify comparison inside the hook to prevent infinite re-renders, rather than fixing the underlying object referential instability."
