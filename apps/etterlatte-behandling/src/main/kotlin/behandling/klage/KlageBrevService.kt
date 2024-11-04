package no.nav.etterlatte.behandling.klage

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.libs.common.behandling.InnstillingTilKabal
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.KlageOversendelsebrev
import no.nav.etterlatte.libs.common.behandling.KlageUtfallMedData
import no.nav.etterlatte.libs.common.behandling.KlageVedtaksbrev
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import org.slf4j.LoggerFactory
import java.util.UUID

class KlageBrevService(
    private val brevApiKlient: BrevApiKlient,
) {
    private val logger: org.slf4j.Logger = LoggerFactory.getLogger(this::class.java)

    fun oversendelsesbrev(
        klage: Klage,
        saksbehandler: Saksbehandler,
    ): KlageOversendelsebrev =
        when (val utfall = klage.utfall) {
            is KlageUtfallMedData.DelvisOmgjoering -> utfall.innstilling.brev
            is KlageUtfallMedData.StadfesteVedtak -> utfall.innstilling.brev
            else -> {
                val brev =
                    runBlocking {
                        brevApiKlient.hentOversendelsesbrev(klage.id, saksbehandler)
                            ?: brevApiKlient.opprettKlageOversendelsesbrevISak(
                                klage.id,
                                saksbehandler,
                            )
                    }
                KlageOversendelsebrev(brev.id)
            }
        }

    fun vedtaksbrev(
        klage: Klage,
        saksbehandler: Saksbehandler,
    ): KlageVedtaksbrev {
        val brev =
            runBlocking {
                brevApiKlient.hentVedtaksbrev(klage.id, saksbehandler)
                    ?: brevApiKlient.opprettVedtaksbrev(klage.id, klage.sak.id, saksbehandler)
            }
        return KlageVedtaksbrev(brev.id)
    }

    fun ferdigstillBrevOgNotatTilKa(
        innstilling: InnstillingTilKabal,
        klage: Klage,
        saksbehandler: Saksbehandler,
    ): FerdigstillResultat {
        val innstillingsbrev = innstilling.brev
        return runBlocking {
            val (tidJournalfoert, journalpostId) =
                ferdigstillOgDistribuerBrev(
                    sakId = klage.sak.id,
                    brevId = innstillingsbrev.brevId,
                    saksbehandler = saksbehandler,
                )
            val oversendelsesbrev =
                hentBrev(
                    sakId = klage.sak.id,
                    brevId = innstillingsbrev.brevId,
                    brukerTokenInfo = saksbehandler,
                )
            val notatTilKa = brevApiKlient.journalfoerNotatKa(klage, saksbehandler)

            logger.info(
                "Journalførte notat til KA for innstilling i klageId=${klage.id} på " +
                    "journalpostId=${notatTilKa.journalpostId}",
            )
            FerdigstillResultat(
                oversendelsesbrev = oversendelsesbrev,
                notatTilKa = notatTilKa,
                journalpostIdOversendelsesbrev = journalpostId,
                journalfoertOversendelsesbrevTidspunkt = tidJournalfoert,
            )
        }
    }

    fun slettUferdigeBrev(
        klageId: UUID,
        saksbehandler: BrukerTokenInfo,
    ) {
        runBlocking {
            try {
                val vedtaksbrev = brevApiKlient.hentVedtaksbrev(klageId, saksbehandler)
                if (vedtaksbrev?.status?.ikkeFerdigstilt() == true) {
                    brevApiKlient.slettVedtaksbrev(klageId, saksbehandler)
                }
            } catch (e: Exception) {
                logger.warn("Kunne ikke slette vedtaksbrev for klageId=$klageId", e)
            }
            try {
                val oversendelsebrev = brevApiKlient.hentOversendelsesbrev(klageId, saksbehandler)
                if (oversendelsebrev?.status?.ikkeFerdigstilt() == true) {
                    brevApiKlient.slettOversendelsesbrev(klageId, saksbehandler)
                }
            } catch (e: Exception) {
                logger.warn("Kunne ikke slette oversendelsesbrev for klageId=$klageId", e)
            }
        }
    }

    fun ferdigstillVedtaksbrev(
        klage: Klage,
        saksbehandler: Saksbehandler,
    ) {
        runBlocking {
            brevApiKlient.ferdigstillVedtaksbrev(klage.id, klage.sak.id, saksbehandler)
        }
    }

    /*
     * TODO:
     *  Burde sikre at det er INNSENDEREN sin journalpostId og bestillingId som benyttes
     * */
    private fun ferdigstillOgDistribuerBrev(
        sakId: SakId,
        brevId: Long,
        saksbehandler: Saksbehandler,
    ): Pair<Tidspunkt, String> =
        runBlocking {
            val eksisterendeInnstillingsbrev = brevApiKlient.hentBrev(sakId, brevId, saksbehandler)
            if (eksisterendeInnstillingsbrev.status.ikkeFerdigstilt()) {
                brevApiKlient.ferdigstillOversendelseBrev(sakId, brevId, saksbehandler)
            } else {
                logger.info("Brev med id=$brevId har status ${eksisterendeInnstillingsbrev.status} og er allerede ferdigstilt")
            }

            val journalpostIdJournalfoering =
                if (eksisterendeInnstillingsbrev.status.ikkeJournalfoert()) {
                    brevApiKlient.journalfoerBrev(sakId, brevId, saksbehandler).journalpostId.first()
                } else {
                    logger.info(
                        "Brev med id=$brevId har status ${eksisterendeInnstillingsbrev.status} og er allerede " +
                            "journalført på journalpostId=${eksisterendeInnstillingsbrev.mottakere.first().journalpostId}",
                    )
                    requireNotNull(eksisterendeInnstillingsbrev.mottakere.first().journalpostId) {
                        "Har et brev med id=$brevId med status=${eksisterendeInnstillingsbrev.status} som mangler journalpostId"
                    }
                }
            val tidspunktJournalfoert = Tidspunkt.now()

            if (eksisterendeInnstillingsbrev.status.ikkeDistribuert()) {
                val bestillingsIdDistribuering = brevApiKlient.distribuerBrev(sakId, brevId, saksbehandler).bestillingsId.first()
                logger.info(
                    "Distribusjon av innstillingsbrevet med id=$brevId bestilt til klagen i sak med sakId=$sakId, " +
                        "med bestillingsId $bestillingsIdDistribuering",
                )
            } else {
                logger.info(
                    "Brev med id=$brevId har status ${eksisterendeInnstillingsbrev.status} og er allerede " +
                        "distribuert med bestillingsid=${eksisterendeInnstillingsbrev.mottakere.first().bestillingId}",
                )
            }
            tidspunktJournalfoert to journalpostIdJournalfoering
        }

    private fun hentBrev(
        sakId: SakId,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev =
        runBlocking {
            brevApiKlient.hentBrev(sakId, brevId, brukerTokenInfo)
        }
}
