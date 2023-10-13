package no.nav.etterlatte.behandling.sjekkliste

import java.util.UUID

data class Sjekkliste(
    val id: UUID,
    val kommentar: String? = null,
    val adresseForBrev: String? = null,
    val kontonrRegistrert: String? = null,
    val sjekklisteItems: List<SjekklisteItem>,
    val versjon: Long,
)

data class SjekklisteItem(
    val id: Long,
    val beskrivelse: String,
    val avkrysset: Boolean = false,
    val versjon: Long,
)

data class OppdaterSjekkliste(
    val kommentar: String? = null,
    val adresseForBrev: String? = null,
    val kontonrRegistrert: String? = null,
)

data class OppdaterSjekklisteItem(
    val avkrysset: Boolean,
)

internal val defaultSjekklisteItemsBP =
    listOf(
        "Behov for stønad til barnetilsyn og/eller utdanningsstønad. Oppfølgingsoppgave er opprettet.",
        "Ikke søkt om, men kan ha rett på stønad til barnetilsyn - limt inn tekst i vedtak",
        "Mulighet for, og ikke søkt om (utvidet) barnetrygd. Oppgave til barnetrygd sendt",
        "Gjenlevende har gradert uføretrygd. Oppgave til NAY sendt.",
        "Ikke registrert/feil kontonummer i saken - opprettet oppgave til NØP",
        "Refusjonskrav (annen NAV-ytelse) i etterbetaling av OMS - opprettet oppgave til NØP",
        "Samordning - løst opp pga ingen refusjon, se eget notat",
        "Bosatt Norge: Innhenter EØS og bank-opplysninger",
        "Bosatt Norge: Opprettet behandling/oppgave \"Utsendelse av kravpakke til utland\"",
        "Bosatt Norge: Sendt brev til bruker om krav til avtaleland",
    )

internal val defaultSjekklisteItemsOMS =
    listOf(
        "Behov for stønad til barnetilsyn og/eller utdanningsstønad. Oppfølgingsoppgave er opprettet.",
        "Ikke søkt om, men kan ha rett på stønad til barnetilsyn - informert søker",
        "Mulighet for, og ikke søkt om (utvidet) barnetrygd. Oppgave til barnetrygd sendt",
        "Gjenlevende har gradert uføretrygd. Oppgave til NAY sendt.",
        "Ikke registrert/feil kontonummer i saken - opprettet oppgave til NØP",
        "Refusjonskrav (annen NAV-ytelse) i etterbetaling av OMS - opprettet oppgave til NØP",
        "Samordning - løst opp pga ingen refusjon, se eget notat",
        "Bosatt Norge: Innhenter EØS og bank-opplysninger",
        "Bosatt Norge: Sendt brev til bruker om krav til avtaleland",
    )
