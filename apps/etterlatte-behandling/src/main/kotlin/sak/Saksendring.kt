package no.nav.etterlatte.sak

import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import sak.KomplettSak
import java.util.UUID

enum class Identtype { SAKSBEHANDLER, GJENNY }

enum class Endringstype {
    OPPRETT_SAK,
    ENDRE_SKJERMING,
    ENDRE_IDENT,
    ENDRE_ENHET,
    ENDRE_ADRESSEBESKYTTELSE,
}

data class Saksendring(
    val id: UUID,
    val endringstype: Endringstype,
    val foer: KomplettSak?,
    val etter: KomplettSak,
    val tidspunkt: Tidspunkt,
    val ident: String,
    val identtype: Identtype,
    val kommentar: String?,
) {
    fun toSaksendringBegrenset(): SaksendringBegrenset = SaksendringBegrenset.from(this)
}

data class SaksendringBegrenset(
    val id: UUID,
    val endringstype: Endringstype,
    val foer: Sak?,
    val etter: Sak,
    val tidspunkt: Tidspunkt,
    val ident: String,
    val identtype: Identtype,
    val kommentar: String?,
) {
    companion object {
        fun from(saksendring: Saksendring): SaksendringBegrenset =
            SaksendringBegrenset(
                id = saksendring.id,
                endringstype = saksendring.endringstype,
                foer = saksendring.foer?.toSak(),
                etter = saksendring.etter.toSak(),
                tidspunkt = saksendring.tidspunkt,
                ident = saksendring.ident,
                identtype = saksendring.identtype,
                kommentar = saksendring.kommentar,
            )
    }
}

// KomplettSak inneholder sensitive felter adressebeskyttelse og erSkjermet - fjerner disse med denne konverteringen
private fun KomplettSak.toSak() = Sak(ident, sakType, id, enhet)
