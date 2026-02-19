package no.nav.etterlatte.behandling.sjekkliste

import java.util.UUID

data class Sjekkliste(
    val id: UUID,
    val kommentar: String? = null,
    val kontonrRegistrert: String? = null,
    val onsketSkattetrekk: String? = null,
    val bekreftet: Boolean = false,
    val sjekklisteItems: List<SjekklisteItem>,
    val versjon: Long,
)

data class SjekklisteItem(
    val id: Long,
    val beskrivelse: String,
    val avkrysset: Boolean = false,
    val versjon: Long,
)

data class OppdatertSjekkliste(
    val kommentar: String? = null,
    val kontonrRegistrert: String? = null,
    val onsketSkattetrekk: String? = null,
    val bekreftet: Boolean = false,
    val versjon: Long,
)

data class OppdaterSjekklisteItem(
    val avkrysset: Boolean,
    val versjon: Long,
)

internal val skjekklisteItemsFoerstegangsbehandlingBP =
    listOf(
        "Utbetalt bidragsforskudd som skal trekkes inn i etterbetaling - sendt oppgave til Bidrag",
        "Mulighet for, og ikke søkt om (utvidet) barnetrygd - sendt oppgave til barnetrygd",
        "Etterbetaling av barnepensjon blir mer enn 2G- kontaktet statsforvalter for informasjon",
        "Ikke registrert/registrert annet kontonummer i saken - opprettet oppgave til NØP",
        "Sendt oppgave til NAY om innvilget BP, slik at de kan avkorte AAP og DP",
        "Ved avslag: Informert om at barnet kan ha rett på bidragsforskudd",
        "Barnet mottar uføretrygd fra Nav - pensjonen er avkortet",
        "Bosatt Norge: Innhenter EØS og bank-opplysninger",
        "Bosatt Norge: Sendt brev til bruker om krav til avtaleland",
        "Bosatt Norge: Opprettet oppfølgingsoppgave på sluttbehandling i Gosys",
        "Bosatt utland: Nødvendige SED er opprettet og klar for utsendelse. P6000 må opprettes etter attestering",
        "Ved vergemål - sjekk at riktig verge er registrert, og at brev/vedtak blir sendt til riktig person",
        "Bruker mottar AFP livsvarig. Registreres som inntekt (se tjenestepensjon i Pesys og A-ordningen)",
        "Dersom bruker er mellom 18 og 20 år, sjekk om bruker har ytelse gjennom ungdomsprogrammet",
    )

internal val sjekklisteItemsFoerstegangsbehandlingOMS =
    listOf(
        "Vedtaket er tilpasset brukers sivilstand",
        "Behov for stønad til barnetilsyn og/eller utdanningsstønad. Oppfølgingsoppgave er opprettet.",
        "Ikke søkt om, men kan ha rett på stønad til barnetilsyn - informert søker",
        "Mulighet for, og ikke søkt om (utvidet) barnetrygd. Oppgave til barnetrygd sendt",
        "Gjenlevende har gradert uføretrygd. Oppgave til NAY sendt.",
        "Ikke registrert/feil kontonummer i saken - opprettet oppgave til NØP",
        "Refusjonskrav (annen Nav-ytelse) i etterbetaling av OMS - opprettet oppgave til NØP",
        "Bosatt Norge: Innhenter EØS og bank-opplysninger",
        "Bosatt Norge: Sendt brev til bruker om krav til avtaleland",
        "Bosatt Norge: Opprettet oppfølgingsoppgave på sluttbehandling i Gosys",
        "Bosatt utland: Nødvendige SED er opprettet og klar for utsendelse. P6000 må opprettes etter attestering",
        "Bosatt utland: Opprettet oppgave til NØP om kildeskatt",
        "Sjekk om bruker har overgangsstønad",
        "Forutsigbart opphør (aldersovergang e.l.) før desember i år eller neste år: Oppgitt inntekt for i år/ neste år i søknaden er kontrollert, og måneder etter opphør er trukket fra i beregningen.",
        "Bruker mottar AFP livsvarig. Registreres som inntekt (se tjenestepensjon i Pesys og A-ordningen)",
    )

internal val sjekklisteItemsRevurderingBP =
    listOf(
        "Etterbetaling av barnepensjon blir mer enn 2G- kontaktet statsforvalter for informasjon",
        "Barnet mottar uføretrygd fra Nav - pensjonen er avkortet",
        "Mottatt nye bankopplysninger - sendt oppgave til NØP",
        "Ved vergemål - sjekk at riktig verge er registrert, og at brev/vedtak blir sendt til riktig person",
        "Utbetalt bidragsforskudd som skal trekkes inn i etterbetaling - sendt oppgave til Bidrag",
    )

internal val sjekklisteItemsRevurderingOMS =
    listOf(
        "Behov for stønad til barnetilsyn og/eller utdanningsstønad. Oppfølgingsoppgave er opprettet.",
        "Ikke søkt om, men kan ha rett på stønad til barnetilsyn - informert søker",
        "Gjenlevende er innvilget gradert uføretrygd - oppgave til NAY om endring av OMS",
        "Refusjonskrav (annen Nav-ytelse) i etterbetaling av OMS - opprettet oppgave til NØP",
        "Gjenlevende er innvilget AAP/OMS - oppgave sendt til Nav lokal pga spesiell oppfølging for de med OMS",
        "Inntekt neste år: sjekket at bruker har oppgitt inntekt frem til forutsigbart opphør pga aldersovergang e.l.",
        "Sjekket at forutsigbart opphør (aldersovergang e.l.) inneværende år er tatt hensyn til i beregningen av forventet inntekt for innvilget periode i år",
        "Bruker mottar AFP livsvarig. Registreres som inntekt (se tjenestepensjon i Pesys og A-ordningen)",
    )

internal val sjekklisteItemsBosattNorgeSluttbehandling =
    listOf("Har opprettet P7000 og P1. Attestant må huske å sende etter attestering.")
