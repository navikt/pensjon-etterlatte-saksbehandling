package no.nav.etterlatte.saksbehandler

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.klienter.AxsysKlient
import no.nav.etterlatte.behandling.klienter.NavAnsattKlient
import no.nav.etterlatte.behandling.klienter.SaksbehandlerInfo
import no.nav.etterlatte.inTransaction

data class Saksbehandler(
    val ident: String,
    val navn: String,
    val enheter: List<String>,
    val kanAttestere: Boolean,
    val skriveEnheter: List<String>,
    val kanSeOppgaveliste: Boolean,
)

data class SaksbehandlerEnhet(
    val enhetsNummer: String,
    val navn: String,
)

interface SaksbehandlerService {
    fun hentKomplettSaksbehandler(ident: String): Saksbehandler

    fun hentSaksbehandlereForEnhet(enhet: List<String>): Set<SaksbehandlerInfo>

    fun hentEnheterForSaksbehandlerIdentWrapper(ident: String): List<SaksbehandlerEnhet>
}

class SaksbehandlerServiceImpl(
    private val dao: SaksbehandlerInfoDao,
    private val axsysKlient: AxsysKlient,
    private val navAnsattKlient: NavAnsattKlient,
) : SaksbehandlerService {
    override fun hentKomplettSaksbehandler(ident: String): Saksbehandler {
        val innloggetSaksbehandler = Kontekst.get().appUserAsSaksbehandler()

        val saksbehandlerNavn: String? = dao.hentSaksbehandlerNavn(ident)
        if (saksbehandlerNavn.isNullOrBlank()) {
            updateNySaksbehandlerAsync(ident)
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

    @OptIn(DelicateCoroutinesApi::class)
    fun updateNySaksbehandlerAsync(ident: String) {
        newSingleThreadContext("oppdaternysaksbehandler").use { ctx ->
            Runtime.getRuntime().addShutdownHook(Thread { ctx.close() })
            runBlocking(ctx) {
                val enheterForSaksbehandler =
                    runBlocking {
                        axsysKlient.hentEnheterForIdent(ident)
                    }

                dao.upsertSaksbehandlerEnheter(Pair(ident, enheterForSaksbehandler))
                val saksbehandlerInfo = navAnsattKlient.hentSaksbehanderNavn(ident)
                if (saksbehandlerInfo == null) {
                    dao.upsertSaksbehandlerNavn(SaksbehandlerInfo(ident, ident))
                } else {
                    dao.upsertSaksbehandlerNavn(saksbehandlerInfo)
                }
            }
        }
    }

    override fun hentSaksbehandlereForEnhet(enhet: List<String>): Set<SaksbehandlerInfo> =
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

    private fun hentEnheterForSaksbehandler(ident: String): List<SaksbehandlerEnhet> {
        return dao.hentSaksbehandlerEnheter(ident)
            ?: runBlocking { axsysKlient.hentEnheterForIdent(ident) }
    }
}
