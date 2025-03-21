package no.nav.etterlatte.sak

import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.User
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.Flyktning
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.UUID

enum class Identtype { SAKSBEHANDLER, GJENNY }

enum class Endringstype {
    OPPRETT_SAK,
    ENDRE_SKJERMING,
    ENDRE_IDENT,
    ENDRE_ENHET,
    ENDRE_ADRESSEBESKYTTELSE,
}

data class KomplettSak(
    val id: SakId,
    val ident: String,
    val sakType: SakType,
    val adressebeskyttelse: AdressebeskyttelseGradering?,
    val erSkjermet: Boolean?,
    val enhet: Enhetsnummer,
    val flyktning: Flyktning?,
    // TODO: Denne burde være non-nullable, men det krever opprydding i databasen
    val opprettet: Tidspunkt?,
)

data class Saksendring(
    val id: UUID,
    val endringstype: Endringstype,
    val foer: KomplettSak?,
    val etter: KomplettSak,
    val tidspunkt: Tidspunkt,
    val ident: String,
    val identtype: Identtype,
    val kommentar: String? = null,
) {
    companion object {
        fun create(
            user: User,
            endringstype: Endringstype,
            sakFoer: KomplettSak?,
            sakEtter: KomplettSak,
            kommentar: String? = null,
        ): Saksendring =
            Saksendring(
                id = UUID.randomUUID(),
                endringstype = endringstype,
                foer = sakFoer,
                etter = sakEtter,
                tidspunkt = Tidspunkt.now(),
                ident = user.name(),
                identtype =
                    when (user) {
                        is SaksbehandlerMedEnheterOgRoller -> Identtype.SAKSBEHANDLER
                        else -> Identtype.GJENNY
                    },
                kommentar = kommentar,
            )
    }
}
