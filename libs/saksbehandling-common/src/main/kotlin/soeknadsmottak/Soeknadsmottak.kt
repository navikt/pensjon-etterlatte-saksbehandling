package no.nav.etterlatte.libs.common.soeknadsmottak

import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.SakId

/**
 * Kontrakten for søknadsmottaket i innsendt-soeknad (pensjon-etterlatte), via etterlatte-api.
 * Innsendt-soeknad journalfører selv, mens Gjenny avgjør sak, enhet og oppgave.
 * Foreløpig tas bare utvidet omstillingsstønad imot på denne måten. Andre typer avvises med 400.
 */
enum class MottattSoeknadType(
    val sakType: SakType,
) {
    UTVIDET_OMSTILLINGSSTOENAD(SakType.OMSTILLINGSSTOENAD),
}

data class JournalfoeringskontekstRequest(
    val soeknadType: MottattSoeknadType,
    /** Søkerens fødselsnummer fra søknaden. Null eller ugyldig gir journalføring på ukjent bruker. */
    val foedselsnummer: String? = null,
)

data class JournalfoeringskontekstResponse(
    val ukjentBruker: Boolean,
    val sakId: SakId?,
    val sakType: SakType,
    val tema: String,
    val journalfoerendeEnhet: Enhetsnummer,
)

data class SoeknadOverleveringRequest(
    /** Stabil nøkkel per søknad fra innsendt-soeknad, for eksempel `innsendt-soeknad:123`. */
    val mottaksnoekkel: String,
    val soeknadType: MottattSoeknadType,
    val sakId: SakId,
    val journalpostId: String,
    val dokumentInfoIder: List<String>,
)

/** Samme svar for samme nøkkel, også ved retry og parallelle kall. */
data class SoeknadOverleveringResponse(
    val mottaksnoekkel: String,
    val sakId: SakId,
    val journalpostId: String,
    val oppgaveId: String,
)
