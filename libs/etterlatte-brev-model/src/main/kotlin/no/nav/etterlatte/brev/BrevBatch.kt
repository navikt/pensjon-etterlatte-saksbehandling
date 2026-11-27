package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Innsender
import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.person.Verge
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import java.time.Instant
import java.util.UUID

data class BrevBatchRequest(
    val sakIder: List<SakId>,
    val tittel: String,
    val innhold: Slate,
    val spraak: Spraak,
)

enum class BrevBatchStatus {
    UNDER_ARBEID,
    FERDIG,
    FERDIG_MED_FEIL,
    AVVENTER_AVKLARING,
}

enum class BrevBatchSakStatus {
    KLAR,
    UNDER_ARBEID,
    FERDIG,
    FEILA,
    AVVENTER_AVKLARING,
}

enum class BrevBatchSteg {
    GRUNNLAG,
    OPPRETT_BREV,
    SEND_BREV,
    FERDIG,
}

data class BrevBatchOversikt(
    val batchId: UUID,
    val status: BrevBatchStatus,
    val antallSaker: Int,
    val antallFerdige: Int,
    val antallFeila: Int,
    val antallAvventerAvklaring: Int,
    val antallVentende: Int,
    val opprettet: Instant,
)

data class BrevBatchSakResultat(
    val utsendingsId: UUID,
    val sakId: SakId,
    val status: BrevBatchSakStatus,
    val steg: BrevBatchSteg,
    val brevId: Long?,
    val antallForsoek: Int,
    val feilkode: String?,
)

data class BrevBatchSaker(
    val saker: List<BrevBatchSakResultat>,
    val offset: Int,
    val limit: Int,
    val totalt: Int,
)

data class StrukturertInformasjonsbrevRequest(
    val sak: Sak,
    val soeker: Soeker,
    val innsender: Innsender?,
    val avdoede: List<Avdoed>,
    val verge: Verge?,
    val gjenlevende: List<String>,
    val tittel: String,
    val innhold: Slate,
    val spraak: Spraak,
)

enum class InformasjonsbrevUtsendingStatus {
    OPPRETTET,
    PDF_LAGRET,
    FERDIGSTILT,
    JOURNALFOERING_PAABEGYNT,
    JOURNALFOERT,
    DISTRIBUSJON_PAABEGYNT,
    FERDIG,
    AVVENTER_AVKLARING,
    FEILA,
}

data class InformasjonsbrevUtsending(
    val utsendingsId: UUID,
    val sakId: SakId,
    val brevId: Long,
    val status: InformasjonsbrevUtsendingStatus,
    val feilkode: String? = null,
)
