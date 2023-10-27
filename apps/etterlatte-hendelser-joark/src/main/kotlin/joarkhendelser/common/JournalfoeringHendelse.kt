package joarkhendelser.common

import no.nav.etterlatte.libs.common.behandling.SakType

data class JournalfoeringHendelse(
    val hendelsesId: String,
    val versjon: Int,
    val hendelsesType: String,
    val journalpostId: Long,
    val journalpostStatus: String,
    val temaGammelt: String,
    val temaNytt: String,
    val mottaksKanal: String,
    val kanalReferanseId: String,
    val behandlingstema: String,
) {
    fun erTemaEtterlatte(): Boolean =
        temaNytt == SakType.BARNEPENSJON.tema ||
            temaNytt == SakType.OMSTILLINGSSTOENAD.tema
}
