package no.nav.etterlatte.saksbehandler

import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.klienter.SaksbehandlerInfo

data class Saksbehandler(
    val ident: String,
    val navn: String,
    val enheter: List<String>,
    val kanAttestere: Boolean,
    val skriveEnheter: List<String>,
    val kanSeOppgaveliste: Boolean,
)

class SaksbehandlerService(private val dao: SaksbehandlerInfoDao) {
    fun hentKomplettSaksbehandler(ident: String): Saksbehandler {
        val innloggetSaksbehandler = Kontekst.get().appUserAsSaksbehandler()

        val saksbehandlerNavn: String? = dao.hentSaksbehandlerNavn(ident)

        return Saksbehandler(
            ident = ident,
            navn = if (!saksbehandlerNavn.isNullOrEmpty()) saksbehandlerNavn else ident,
            enheter = innloggetSaksbehandler.enheter(),
            kanAttestere = innloggetSaksbehandler.saksbehandlerMedRoller.harRolleAttestant(),
            skriveEnheter = innloggetSaksbehandler.enheterMedSkrivetilgang(),
            kanSeOppgaveliste = innloggetSaksbehandler.enheterMedSkrivetilgang().isNotEmpty(),
        )
    }

    fun hentSaksbehandlereForEnhet(enhet: List<String>): Set<SaksbehandlerInfo> =
        enhet.flatMap { dao.hentSaksbehandlereForEnhet(it) }.toSet()
}
