package no.nav.etterlatte.saksbehandler

import no.nav.etterlatte.Kontekst

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

    fun hentSaksbehandlereForEnhet(enhet: String) = dao.hentSaksbehandlereForEnhet(enhet)
}
