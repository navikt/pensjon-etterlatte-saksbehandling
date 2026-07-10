package no.nav.etterlatte.brev

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.behandling.klage.KlageDao
import no.nav.etterlatte.behandling.klienter.VedtakInternalService
import no.nav.etterlatte.brev.behandling.hentGjenlevende
import no.nav.etterlatte.brev.behandling.mapAvdoede
import no.nav.etterlatte.brev.behandling.mapInnsender
import no.nav.etterlatte.brev.behandling.mapSoeker
import no.nav.etterlatte.brev.behandling.mapSpraak
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.klage.AvvistKlageBrevDataInnholdData
import no.nav.etterlatte.brev.model.klage.AvvistKlageBrevInnholdDataNy
import no.nav.etterlatte.brev.model.klage.AvvistKlageBrevRedigerbarInnholdData
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import java.util.UUID

class KlageAvvistBrevService(
    private val sakService: SakService,
    private val brevKlient: BrevKlient,
    private val brevApiKlient: no.nav.etterlatte.behandling.klienter.BrevApiKlient,
    private val grunnlagService: GrunnlagService,
    private val klageDao: KlageDao,
    private val vedtakInternalService: VedtakInternalService,
    private val oppgaveService: OppgaveService,
) {
    suspend fun opprettVedtaksbrev(
        behandlingId: UUID,
        sakId: SakId,
        bruker: BrukerTokenInfo,
    ): Brev {
        val eksisterende = brevApiKlient.hentVedtaksbrev(behandlingId, bruker)
        if (eksisterende != null) {
            return eksisterende
        }

        val brevRequest = utledBrevRequest(bruker, behandlingId, sakId)
        return brevKlient.opprettStrukturertBrev(behandlingId, brevRequest, bruker)
    }

    suspend fun genererPdf(
        brevID: BrevID,
        behandlingId: UUID,
        sakId: SakId,
        bruker: BrukerTokenInfo,
        skalLagres: Boolean,
    ): Pdf {
        val brevRequest = utledBrevRequest(bruker, behandlingId, sakId, skalLagres)
        return brevKlient.genererPdf(brevID, behandlingId, brevRequest, bruker)
    }

    suspend fun ferdigstillVedtaksbrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        brevKlient.ferdigstillStrukturertBrev(behandlingId, Brevtype.VEDTAK, brukerTokenInfo)
    }

    suspend fun tilbakestillVedtaksbrev(
        brevID: BrevID,
        behandlingId: UUID,
        sakId: SakId,
        bruker: BrukerTokenInfo,
    ): BrevPayload {
        val brevRequest = utledBrevRequest(bruker, behandlingId, sakId)
        return brevKlient.tilbakestillStrukturertBrev(brevID, behandlingId, brevRequest, bruker)
    }

    suspend fun hentVedtaksbrev(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ): Brev? = brevApiKlient.hentVedtaksbrev(behandlingId, bruker)

    private suspend fun utledBrevRequest(
        bruker: BrukerTokenInfo,
        behandlingId: UUID,
        sakId: SakId,
        skalLagres: Boolean = false,
    ): BrevRequest =
        coroutineScope {
            val klage =
                klageDao.hentKlage(behandlingId)
                    ?: throw IkkeFunnetException("KLAGE_IKKE_FUNNET", "Klage med id=$behandlingId finnes ikke")
            val sak =
                sakService.finnSak(sakId)
                    ?: throw InternfeilException("Fant ikke sak med id=$sakId")

            val vedtakDeferred = async { vedtakInternalService.hentVedtak(behandlingId, bruker) }
            val grunnlag =
                grunnlagService.hentOpplysningsgrunnlagForSak(sak.id)
                    ?: throw InternfeilException("Fant ikke grunnlag for sakId=$sakId")

            val vedtak =
                vedtakDeferred.await()
                    ?: throw InternfeilException("Kan ikke lage vedtaksbrev for avvist klage uten vedtak behandlingId=$behandlingId")

            val verge = hentVergeForSak(sak.sakType, null, grunnlag)
            val soeker = grunnlag.mapSoeker(null)
            val oppgaveForKlage =
                oppgaveService
                    .hentOppgaverForReferanse(behandlingId.toString())
                    .singleOrNull { it.type == OppgaveType.KLAGE }
            val (saksbehandlerIdent, attestantIdent) =
                hentSaksbehandlerOgAttestantForVedtak(
                    vedtakDto = vedtak,
                    oppgaveForBehandling = oppgaveForKlage,
                    brukerTokenInfo = bruker,
                )

            BrevRequest(
                sak = sak,
                innsender = grunnlag.mapInnsender(),
                soeker = soeker,
                avdoede = grunnlag.mapAvdoede(),
                verge = verge,
                gjenlevende = grunnlag.hentGjenlevende(),
                spraak = grunnlag.mapSpraak(),
                saksbehandlerIdent = saksbehandlerIdent,
                attestantIdent = attestantIdent,
                skalLagre = skalLagres,
                brevFastInnholdData =
                    AvvistKlageBrevInnholdDataNy(
                        data =
                            AvvistKlageBrevDataInnholdData(
                                sakType = sak.sakType,
                                bosattUtland = false,
                                klageDato =
                                    klage.innkommendeDokument?.mottattDato
                                        ?: klage.opprettet.toLocalDate(),
                                datoForVedtaketKlagenGjelder =
                                    klage.formkrav
                                        ?.formkrav
                                        ?.vedtaketKlagenGjelder
                                        ?.datoAttestert
                                        ?.toLocalDate(),
                            ),
                    ),
                brevRedigerbarInnholdData =
                    AvvistKlageBrevRedigerbarInnholdData(
                        sakType = sak.sakType,
                        bosattUtland = false,
                        klageDato =
                            klage.innkommendeDokument?.mottattDato
                                ?: klage.opprettet.toLocalDate(),
                        datoForVedtaketKlagenGjelder =
                            klage.formkrav
                                ?.formkrav
                                ?.vedtaketKlagenGjelder
                                ?.datoAttestert
                                ?.toLocalDate(),
                    ),
                brevVedleggData = emptyList(),
            )
        }
}
