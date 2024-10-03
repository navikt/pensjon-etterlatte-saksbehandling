package no.nav.etterlatte.brev

class SamordningManueltBehandletRequest(
    val tittel: String,
    val vedtakId: Long,
    val samordningsmeldingId: Long,
    val kommentar: String,
    val saksbehandlerId: String,
)
