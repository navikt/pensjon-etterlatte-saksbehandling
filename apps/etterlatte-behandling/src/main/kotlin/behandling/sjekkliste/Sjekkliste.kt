package no.nav.etterlatte.behandling.sjekkliste

import java.util.UUID

data class Sjekkliste(
    val id: UUID,
    val kommentar: String? = null,
    val kontonrRegistrert: String? = null,
    val onsketSkattetrekk: Int? = null,
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
    val onsketSkattetrekk: Int? = null,
    val bekreftet: Boolean = false,
    val versjon: Long,
)

data class OppdaterSjekklisteItem(
    val avkrysset: Boolean,
    val versjon: Long,
)

internal val defaultSjekklisteItemsBP =
    listOf(
        "Utbetalt bidragsforskudd som skal trekkes inn i etterbetaling - sendt oppgave til Bidrag",
        "Mulighet for, og ikke søkt om (utvidet) barnetrygd - sendt oppgave til barnetrygd",
        "Etterbetaling av barnepensjon blir mer enn 2G- kontaktet statsforvalter for informasjon",
        "Ikke registrert/registrert annet kontonummer i saken - opprettet oppgave til NØP",
        "Opprettet oppgave til NØP om skattetrekk",
        "Ved avslag: Informert om at barnet kan ha rett på bidragsforskudd",
        "Barnet mottar uføretrygd fra NAV - pensjonen er avkortet",
        "Bosatt Norge: Innhenter EØS og bank-opplysninger",
        "Bosatt Norge: Sendt brev til bruker om krav til avtaleland",
        "Bosatt Norge: Opprettet oppfølgingsoppgave på sluttbehandling i Gosys",
        "Bosatt utland: Nødvendige SED er opprettet og klar for utsendelse. P6000 må opprettes etter attestering",
    )

internal val defaultSjekklisteItemsOMS =
    listOf(
        "Behov for stønad til barnetilsyn og/eller utdanningsstønad. Oppfølgingsoppgave er opprettet.",
        "Ikke søkt om, men kan ha rett på stønad til barnetilsyn - informert søker",
        "Mulighet for, og ikke søkt om (utvidet) barnetrygd. Oppgave til barnetrygd sendt",
        "Gjenlevende har gradert uføretrygd. Oppgave til NAY sendt.",
        "Ikke registrert/feil kontonummer i saken - opprettet oppgave til NØP",
        "Refusjonskrav (annen NAV-ytelse) i etterbetaling av OMS - opprettet oppgave til NØP",
        "Bosatt Norge: Innhenter EØS og bank-opplysninger",
        "Bosatt Norge: Sendt brev til bruker om krav til avtaleland",
        "Bosatt Norge: Opprettet oppfølgingsoppgave på sluttbehandling i Gosys",
        "Bosatt utland: Nødvendige SED er opprettet og klar for utsendelse. P6000 må opprettes etter attestering",
        "Bosatt utland: Opprettet oppgave til NØP om kildeskatt",
    )

internal val defaultFoerstegangsbehandlingItemsOms =
    defaultSjekklisteItemsOMS + "Sjekk om bruker har overgangsstønad"
