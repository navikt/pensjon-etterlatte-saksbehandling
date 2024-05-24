# Migrering av hjemmesnekra komponenter til Aksel komponenter
[Design tokens for spacing](https://aksel.nav.no/grunnleggende/styling/design-tokens#0cc9fb32f213)

## Mange av "wrapperne" våres
Veldig mange steder bruke vi f.eks `margin-top: 1rem` for å lage litt spacing med komponentet over, da burder man heller vurdere å wrapper komponentene i en `<VStack gap="4"/>` for å få samme effekt

## ~shared/styled.tsx
`<Content />` --> `<Box padding="8" />`

`<ContentHeader />` --> `<Box paddingInline="16" paddingBlock="4" />`

`<Content />` --> `<></>` (i de fleste tilfeller hvor det ikke trengs ekstra luft i høyden)