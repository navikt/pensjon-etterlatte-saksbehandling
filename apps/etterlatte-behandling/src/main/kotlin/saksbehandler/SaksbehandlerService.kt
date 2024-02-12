package no.nav.etterlatte.saksbehandler

import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.klienter.SaksbehandlerInfo

data class Saksbehandler(
    val ident: String,
    val navn: String,
    val enheter: List<String>,
    val kanAttestere: Boolean,
    val leseTilgang: Boolean,
    val skriveTilgang: Boolean,
)

class SaksbehandlerService(private val dao: SaksbehandlerInfoDaoTrans) {
    fun hentKomplettSaksbehandler(ident: String): Saksbehandler {
        val innloggetSaksbehandler = Kontekst.get().appUserAsSaksbehandler()

        return Saksbehandler(
            ident,
            dao.hentSaksbehandlerNavn(ident),
            innloggetSaksbehandler.enheter(),
            innloggetSaksbehandler.saksbehandlerMedRoller.harRolleAttestant(),
            innloggetSaksbehandler.harLesetilgang(),
            innloggetSaksbehandler.harSkrivetilgang(),
        )
    }

    fun hentSaksbehandlereForEnhet(enhet: List<String>): Set<SaksbehandlerInfo> =
        enhet.flatMap { dao.hentSaksbehandlereForEnhet(it) }.toSet()
}
