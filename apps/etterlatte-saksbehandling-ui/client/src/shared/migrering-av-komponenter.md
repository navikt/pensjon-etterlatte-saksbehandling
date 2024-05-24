# Migrering av hjemmesnekra komponenter til Aksel komponenter
[Design tokens for spacing](https://aksel.nav.no/grunnleggende/styling/design-tokens#0cc9fb32f213)

## ~shared/styled.tsx
`<Content />` --> `<Box padding="8" />`

`<ContentHeader />` --> `<Box paddingInline="16" paddingBlock="4" />`

`<Content />` --> `<></>` (i de fleste tilfeller hvor det ikke trengs ekstra luft i høyden)