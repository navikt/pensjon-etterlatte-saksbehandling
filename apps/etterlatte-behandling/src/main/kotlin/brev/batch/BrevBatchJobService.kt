package no.nav.etterlatte.brev.batch

import no.nav.etterlatte.brev.BrevBatchSakStatus
import no.nav.etterlatte.brev.BrevBatchSteg
import no.nav.etterlatte.brev.BrevKlient
import no.nav.etterlatte.brev.InformasjonsbrevUtsending
import no.nav.etterlatte.brev.InformasjonsbrevUtsendingStatus
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.time.Instant

class BrevBatchJobService(
    private val dao: BrevBatchDao,
    private val snapshot: BrevBatchSnapshot,
    private val brevKlient: BrevKlient,
    private val config: BrevBatchConfig,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun kjoer(
        bruker: BrukerTokenInfo,
        erLeader: () -> Boolean = { true },
    ) {
        repeat(1000) {
            if (!config.aktivert || !erLeader()) return
            val arbeid = dao.reserver() ?: return
            behandle(arbeid, bruker)
        }
    }

    internal suspend fun behandle(
        opprinnelig: BrevBatchArbeid,
        bruker: BrukerTokenInfo,
    ) {
        var arbeid = opprinnelig
        var steg = arbeid.resultat.steg
        try {
            val id = arbeid.resultat.utsendingsId
            val sakId = arbeid.resultat.sakId
            // Alle gjenopptakelser av et påbegynt brev leser autoritativ status først.
            var utsending =
                if (steg != BrevBatchSteg.GRUNNLAG) brevKlient.hentInformasjonsbrev(id, sakId, bruker) else null
            if (utsending != null) {
                validerSvar(arbeid, utsending)
                if (avsluttHvisAvklart(arbeid, utsending)) return
            }
            if (steg == BrevBatchSteg.SEND_BREV) {
                dao.oppdater(arbeid, BrevBatchSakStatus.AVVENTER_AVKLARING, steg, feilkode = "SEND_RESULTAT_UAVKLART")
                return
            }
            if (arbeid.resultat.antallForsoek > config.maksForsoek) {
                dao.oppdater(arbeid, BrevBatchSakStatus.FEILA, steg, feilkode = "MAKS_FORSOEK")
                return
            }
            if (!config.aktivert) {
                dao.oppdater(arbeid, BrevBatchSakStatus.KLAR, steg)
                return
            }
            if (utsending == null) {
                val data = arbeid.snapshot ?: snapshot.bygg(sakId, arbeid.request)
                steg = BrevBatchSteg.OPPRETT_BREV
                if (!dao.oppdater(arbeid, BrevBatchSakStatus.UNDER_ARBEID, steg, snapshot = data)) return
                arbeid = arbeid.copy(snapshot = data, resultat = arbeid.resultat.copy(steg = steg))
                utsending = brevKlient.opprettInformasjonsbrev(id, data, bruker)
                validerSvar(arbeid, utsending)
                if (avsluttHvisAvklart(arbeid, utsending)) return
            }
            arbeid = arbeid.copy(resultat = arbeid.resultat.copy(brevId = utsending.brevId))
            if (!config.aktivert) {
                dao.oppdater(arbeid, BrevBatchSakStatus.KLAR, BrevBatchSteg.OPPRETT_BREV)
                return
            }
            steg = BrevBatchSteg.SEND_BREV
            if (!dao.oppdater(arbeid, BrevBatchSakStatus.UNDER_ARBEID, steg)) return
            val sendt = brevKlient.sendInformasjonsbrev(id, sakId, bruker)
            validerSvar(arbeid, sendt)
            if (!avsluttHvisAvklart(arbeid, sendt)) {
                dao.oppdater(arbeid, BrevBatchSakStatus.AVVENTER_AVKLARING, steg, feilkode = "SEND_RESULTAT_UAVKLART")
            }
        } catch (e: Exception) {
            val kode =
                when {
                    steg == BrevBatchSteg.SEND_BREV -> "SEND_RESULTAT_UAVKLART"
                    e is BrevBatchSakFeil -> e.kode
                    e is ForespoerselException && e.status in 400..499 -> "BREV_REQUEST_AVVIST"
                    else -> "MIDLERTIDIG_FEIL"
                }
            val status =
                when {
                    steg == BrevBatchSteg.SEND_BREV -> BrevBatchSakStatus.AVVENTER_AVKLARING
                    kode != "MIDLERTIDIG_FEIL" || arbeid.resultat.antallForsoek >= config.maksForsoek -> BrevBatchSakStatus.FEILA
                    else -> BrevBatchSakStatus.KLAR
                }
            dao.oppdater(arbeid, status, steg, feilkode = kode, nesteForsoek = Instant.now().plusSeconds(30))
            logger.warn("Brevbatch utsending={} steg={} feilkode={}", arbeid.resultat.utsendingsId, steg, kode)
        }
    }

    private fun validerSvar(
        arbeid: BrevBatchArbeid,
        svar: InformasjonsbrevUtsending,
    ) {
        if (svar.utsendingsId != arbeid.resultat.utsendingsId ||
            svar.sakId != arbeid.resultat.sakId ||
            (arbeid.resultat.brevId != null && arbeid.resultat.brevId != svar.brevId)
        ) {
            throw BrevBatchSakFeil("BREV_IDENTITET_AVVIK")
        }
    }

    private fun avsluttHvisAvklart(
        arbeid: BrevBatchArbeid,
        svar: InformasjonsbrevUtsending,
    ): Boolean {
        val (status, steg, kode) =
            when (svar.status) {
                InformasjonsbrevUtsendingStatus.FERDIG -> {
                    Triple(BrevBatchSakStatus.FERDIG, BrevBatchSteg.FERDIG, null)
                }

                InformasjonsbrevUtsendingStatus.FEILA -> {
                    Triple(BrevBatchSakStatus.FEILA, BrevBatchSteg.SEND_BREV, "BREV_UTSENDING_FEILA")
                }

                InformasjonsbrevUtsendingStatus.AVVENTER_AVKLARING,
                InformasjonsbrevUtsendingStatus.JOURNALFOERING_PAABEGYNT,
                InformasjonsbrevUtsendingStatus.DISTRIBUSJON_PAABEGYNT,
                -> {
                    Triple(BrevBatchSakStatus.AVVENTER_AVKLARING, BrevBatchSteg.SEND_BREV, "SEND_RESULTAT_UAVKLART")
                }

                else -> {
                    return false
                }
            }
        dao.oppdater(arbeid, status, steg, svar.brevId, kode)
        return true
    }
}
