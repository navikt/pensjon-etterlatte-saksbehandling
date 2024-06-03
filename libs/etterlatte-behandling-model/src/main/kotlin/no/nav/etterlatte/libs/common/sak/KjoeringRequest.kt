package no.nav.etterlatte.libs.common.sak

data class KjoeringRequest(
    val kjoering: String,
    val status: KjoeringStatus,
    val sakId: Long,
)

enum class KjoeringStatus {
    STARTA,
    FEILA,
    IKKE_LOEPENDE,
    FERDIGSTILT,
}
