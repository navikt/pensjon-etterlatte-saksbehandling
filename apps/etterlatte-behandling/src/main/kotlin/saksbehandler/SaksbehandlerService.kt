package no.nav.etterlatte.saksbehandler

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerToggles
import no.nav.etterlatte.behandling.klienter.AxsysKlient
import no.nav.etterlatte.behandling.klienter.EntraProxyKlient
import no.nav.etterlatte.behandling.klienter.NavAnsattKlient
import no.nav.etterlatte.behandling.klienter.SaksbehandlerInfo
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Enhetsnummer

data class Saksbehandler(
    val ident: String,
    val navn: String,
    val enheter: List<Enhetsnummer>,
    val kanAttestere: Boolean,
    val skriveEnheter: List<Enhetsnummer>,
    val kanSeOppgaveliste: Boolean,
)

data class SaksbehandlerEnhet(
    val enhetsNummer: Enhetsnummer,
    val navn: String,
)

interface SaksbehandlerService {
    fun hentKomplettSaksbehandler(ident: String): Saksbehandler

    fun hentSaksbehandlereForEnhet(enhet: List<Enhetsnummer>): Set<SaksbehandlerInfo>

    fun hentEnheterForSaksbehandlerIdentWrapper(ident: String): List<SaksbehandlerEnhet>

    fun hentNavnForIdent(ident: String): String?
}

class SaksbehandlerServiceImpl(
    private val dao: SaksbehandlerInfoDao,
    private val axsysKlient: AxsysKlient,
    private val navAnsattKlient: NavAnsattKlient,
    private val entraProxyKlient: EntraProxyKlient,
    private val featureToggleService: FeatureToggleService,
) : SaksbehandlerService {
    override fun hentKomplettSaksbehandler(ident: String): Saksbehandler {
        val innloggetSaksbehandler = Kontekst.get().appUserAsSaksbehandler()

        val saksbehandlerNavn: String? = dao.hentSaksbehandlerNavn(ident)
        if (saksbehandlerNavn.isNullOrBlank()) {
            updateNySaksbehandler(ident)
        }

        return Saksbehandler(
            ident,
            if (!saksbehandlerNavn.isNullOrEmpty()) saksbehandlerNavn else ident,
            innloggetSaksbehandler.enheter(),
            innloggetSaksbehandler.saksbehandlerMedRoller.harRolleAttestant(),
            skriveEnheter = innloggetSaksbehandler.enheterMedSkrivetilgang(),
            kanSeOppgaveliste = innloggetSaksbehandler.kanSeOppgaveBenken(),
        )
    }

    private fun updateNySaksbehandler(ident: String) {
        val enheterForSaksbehandler =
            runBlocking {
                if (featureToggleService.isEnabled(EtteroppgjoerToggles.HENT_ENHETER_FRA_ENTRA_PROXY, false)) {
                    entraProxyKlient.hentEnheterForIdent(ident)
                } else {
                    axsysKlient.hentEnheterForIdent(ident)
                }
            }

        dao.upsertSaksbehandlerEnheter(Pair(ident, enheterForSaksbehandler))
        val saksbehandlerInfo = runBlocking { navAnsattKlient.hentSaksbehanderNavn(ident) }
        if (saksbehandlerInfo == null) {
            dao.upsertSaksbehandlerNavn(SaksbehandlerInfo(ident, ident))
        } else {
            dao.upsertSaksbehandlerNavn(saksbehandlerInfo)
        }
    }

    override fun hentSaksbehandlereForEnhet(enhet: List<Enhetsnummer>): Set<SaksbehandlerInfo> =
        enhet.flatMap { dao.hentSaksbehandlereForEnhet(it) }.toSet()

    override fun hentEnheterForSaksbehandlerIdentWrapper(ident: String): List<SaksbehandlerEnhet> {
        val harIntransaction = Kontekst.get().databasecontxt.harIntransaction()
        return if (harIntransaction) {
            hentEnheterForSaksbehandler(ident)
        } else {
            inTransaction {
                hentEnheterForSaksbehandler(ident)
            }
        }
    }

    override fun hentNavnForIdent(ident: String): String? = dao.hentSaksbehandlerNavn(ident)

    private fun hentEnheterForSaksbehandler(ident: String): List<SaksbehandlerEnhet> =
        dao.hentSaksbehandlerEnheter(ident)
            ?: runBlocking { axsysKlient.hentEnheterForIdent(ident) }
}
