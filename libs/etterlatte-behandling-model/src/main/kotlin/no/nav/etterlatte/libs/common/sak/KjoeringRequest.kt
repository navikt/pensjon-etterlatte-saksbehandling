package no.nav.etterlatte.libs.common.sak

data class KjoeringRequest(
    val kjoering: String,
    val status: KjoeringStatus,
    val sakId: Long,
)

enum class KjoeringStatus {
    KLAR_TIL_REGULERING,
    STARTA,
    FEILA,
    IKKE_LOEPENDE,
    FERDIGSTILT,
}
