package no.nav.etterlatte.saksbehandler

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.klienter.AxsysKlient
import no.nav.etterlatte.behandling.klienter.SaksbehandlerInfo
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.inTransaction

data class Saksbehandler(
    val ident: String,
    val navn: String,
    val enheter: List<String>,
    val kanAttestere: Boolean,
    val leseTilgang: Boolean,
    val skriveTilgang: Boolean,
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
) : SaksbehandlerService {
    override fun hentKomplettSaksbehandler(ident: String): Saksbehandler {
        val innloggetSaksbehandler = Kontekst.get().appUserAsSaksbehandler()

        val saksbehandlerNavn: String? = dao.hentSaksbehandlerNavn(ident)

        return Saksbehandler(
            ident,
            if (!saksbehandlerNavn.isNullOrEmpty()) saksbehandlerNavn else ident,
            innloggetSaksbehandler.enheter(),
            innloggetSaksbehandler.saksbehandlerMedRoller.harRolleAttestant(),
            innloggetSaksbehandler.harLesetilgang(),
            innloggetSaksbehandler.harSkrivetilgang(),
        )
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
        val enheterForSaksbehandler = dao.hentEnheterIderForSaksbehandler(ident)

        return if (enheterForSaksbehandler == null) {
            runBlocking { axsysKlient.hentEnheterForIdent(ident) }
                .also { saksbehandlerEnhetList ->
                    dao.upsertSaksbehandlerEnheter(Pair(ident, saksbehandlerEnhetList.map { it.enhetsNummer }))
                }
        } else {
            val mapped =
                enheterForSaksbehandler.map {
                    val enhetsnavn = Enheter.finnEnhetForEnhetsnummer(it)?.navn
                    Pair(it, enhetsnavn)
                }
            val ikkeRegistrertEnhet = mapped.any { it.second == null }
            return if (ikkeRegistrertEnhet) {
                runBlocking { axsysKlient.hentEnheterForIdent(ident) }
            } else {
                mapped.map { SaksbehandlerEnhet(it.first, it.second!!) }
            }
        }
    }
}
