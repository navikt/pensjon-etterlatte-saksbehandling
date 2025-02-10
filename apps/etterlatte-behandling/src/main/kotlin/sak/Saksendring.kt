package no.nav.etterlatte.sak

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import sak.KomplettSak
import java.util.UUID

data class Saksendring(
    val id: UUID,
    val endringstype: Endringstype,
    val foer: KomplettSak?,
    val etter: KomplettSak,
    val tidspunkt: Tidspunkt,
    val ident: String,
    val identtype: Identtype,
) {
    enum class Identtype { SAKSBEHANDLER, GJENNY }

    enum class Endringstype {
        OPPRETT_SAK,
        ENDRE_SKJERMING,
        ENDRE_IDENT,
        ENDRE_ENHET,
        ENDRE_ADRESSEBESKYTTELSE,
    }
}
