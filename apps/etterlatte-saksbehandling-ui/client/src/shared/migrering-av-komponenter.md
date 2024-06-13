# Migrering av hjemmesnekra komponenter til Aksel komponenter
[Design tokens for spacing](https://aksel.nav.no/grunnleggende/styling/design-tokens#0cc9fb32f213)

## Kort oppsumert
Ca 98% av våres `styled-component's` går an relativt lett å byttes ut Aksel komponenter, spesielt disse 4 som har mye av funksjonaliteten til `display: flex`:

### `<Box />` 
Hvis man bare trenger litt luft rundt et komponent er det bare å wrappe det i en `<Box />`

### `<VStack />`
Hvis man skal ha komponenter i loddrett rekkefølge og muligens ha litt luft mellom komponentene, så kan man wrappe de i en `<VStack />`

### `<HStack />`
Hvis man skal ha komponenter i vannrett rekke og muligens ha litt luft mellom komponentene, så kan man wrappe de i en `<HStack />`

### `<HGrid />`
Hvis man trenger å ha komponentene strukturert som et rutenett, så kan man wrappe de i en `<HGrid />`

## ~shared/styled.tsx
`<Content />` --> `<Box padding="8" />`

`<ContentHeader />` --> `<Box paddingInline="16" paddingBlock="4" />`

`<Content />` --> `<></>` (i de fleste tilfeller hvor det ikke trengs ekstra luft i høyden)

`<ButtonGroup />` --> `<HStack gap="2" justify="end" />` Kan være at man må wrappe komponentene i en `<VStack gap="4" />` for å få riktig spacing.

`<SpaceChildren />` og `<SpaceChildren direction="column" />` --> `<VStack gap="4" />`

`<SpaceChildren direction="row" />` --> `<HStack gap="4" />`

`<FlexRow />` --> `<HStack gap="4"/>` Noen steder trenger man bare en `<div />` for å stoppe cascading av CSS. Hvis `$spacing` proppen er brukt kan det hende at man må wrappe komponenter i en `<VStack gap="2" />`

## ~components/behandling/soeknadsoversikt/styled.tsx
`<InnholdPadding />` --> `<Box paddingBlock="8" paddingInline="16 8" />`

`<InfobokserWrapper />` --> `<HStack gap="4" />`

`<InfoWrapper />` --> `<VStack gap="4" />` Noen steder trenger man bare en `<div />`

`<InfoList />` --> `<VStack gap="4" />`

`<HeadingWrapper />` er merget inn i eksisterende `<Box paddingInline="16" paddingBlock="4" />` rundt `<Heading />` --> `<Box paddingInline="16" paddingBlock="16 4"><Heading /><Box>`

`<Border />` er litt komplisert siden den blir brukt i kombinasjon med andre komponenter og kan dermed ha forskjellige marginer eller paddinger. Men de fleste stedene skal dette være riktig `<Box paddingBlock="4 0" borderWidth="1" borderColor="border-subtle"/>`

`<Historikk />` --> `<Box paddingBlock="2 0"/>`

`<HistorikkElement />` --> `<VStack />`

`<PersonDetailWrapper />` --> `<Box paddingBlock="2 0" />`

`<VurderingsContainerWrapper />` og `<VurderingsContainer />` --> `<Vurdering />`

## ~components/behandling/vilkaarsvurdering/styled.ts
`<Innhold />` --> `<Box paddingInline="8" />`

`<VilkaarWrapper />` --> `<Box paddingBlock="4" paddingInline="8 4"><HStack justify="space-between" /></Box>`
