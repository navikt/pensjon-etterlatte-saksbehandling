package no.nav.etterlatte.behandling.etteroppgjoer.forbehandling

import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerForbehandling
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerOpplysninger
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import no.nav.etterlatte.behandling.etteroppgjoer.ForbehandlingDto
import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.InntektskomponentService
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SigrunKlient
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnFaktiskInntektRequest
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkortingRequest
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakLesDao
import java.util.UUID

class EtteroppgjoerForbehandlingService(
    private val dao: EtteroppgjoerForbehandlingDao,
    private val sakDao: SakLesDao,
    private val etteroppgjoerService: EtteroppgjoerService,
    private val oppgaveService: OppgaveService,
    private val inntektskomponentService: InntektskomponentService,
    private val sigrunKlient: SigrunKlient,
    private val beregningKlient: BeregningKlient,
    private val behandlingService: BehandlingService,
) {
    fun hentForbehandling(
        brukerTokenInfo: BrukerTokenInfo,
        behandlingId: UUID,
    ): ForbehandlingDto {
        val forbehandling = dao.hentForbehandling(behandlingId) ?: throw FantIkkeForbehandling(behandlingId)

        val sisteIverksatteBehandling =
            behandlingService.hentSisteIverksatte(forbehandling.sak.id)
                ?: throw InternfeilException("Fant ikke siste iverksatte")

        val avkorting =
            runBlocking {
                beregningKlient.hentAvkortingForForbehandlingEtteroppgjoer(
                    EtteroppgjoerBeregnetAvkortingRequest(
                        forbehandling = behandlingId,
                        sisteIverksatteBehandling = sisteIverksatteBehandling.id,
                        aar = forbehandling.aar,
                    ),
                    brukerTokenInfo,
                )
            }

        val pensjonsgivendeInntekt = dao.hentPensjonsgivendeInntekt(behandlingId)
        val aInntekt = dao.hentAInntekt(behandlingId)

        if (pensjonsgivendeInntekt == null || aInntekt == null) {
            throw InternfeilException(
                "Mangler ${if (pensjonsgivendeInntekt == null) "pensjonsgivendeInntekt" else "aInntekt"} for behandlingId=$behandlingId",
            )
        }

        return ForbehandlingDto(
            behandling = forbehandling,
            opplysninger =
                EtteroppgjoerOpplysninger(
                    skatt = pensjonsgivendeInntekt,
                    ainntekt = aInntekt,
                    tidligereAvkorting = avkorting.avkortingMedForventaInntekt,
                ),
            avkortingFaktiskInntekt = avkorting.avkortingMedFaktiskInntekt,
        )
    }

    fun lagreBrevreferanse(
        forbehandlingId: UUID,
        brev: Brev,
    ) {
        val forbehandling = dao.hentForbehandling(forbehandlingId) ?: throw FantIkkeForbehandling(forbehandlingId)
        dao.lagreForbehandling(forbehandling.medBrev(brev))
    }

    fun opprettEtteroppgjoer(
        sakId: SakId,
        inntektsaar: Int,
    ): EtteroppgjoerOgOppgave {
        val sak = sakDao.hentSak(sakId) ?: throw NotFoundException("Fant ikke sak med id=$sakId")

        val pensjonsgivendeInntekt = runBlocking { sigrunKlient.hentPensjonsgivendeInntekt(sak.ident, inntektsaar) }
        val aInntekt = runBlocking { inntektskomponentService.hentInntektFraAInntekt(sak.ident, inntektsaar) }

        val nyForbehandling =
            EtteroppgjoerForbehandling(
                id = UUID.randomUUID(),
                hendelseId = UUID.randomUUID(),
                sak = sak,
                status = "opprettet",
                aar = inntektsaar,
                opprettet = Tidspunkt.now(),
                brevId = null,
            )

        val oppgave =
            oppgaveService.opprettOppgave(
                referanse = nyForbehandling.id.toString(),
                sakId = sakId,
                kilde = OppgaveKilde.BEHANDLING,
                type = OppgaveType.ETTEROPPGJOER,
                merknad = null,
                frist = null,
                saksbehandler = null,
                gruppeId = null,
            )

        dao.lagreForbehandling(nyForbehandling)
        dao.lagrePensjonsgivendeInntekt(pensjonsgivendeInntekt, nyForbehandling.id)
        dao.lagreAInntekt(aInntekt, nyForbehandling.id)

        etteroppgjoerService.oppdaterStatus(sak.id, inntektsaar, EtteroppgjoerStatus.UNDER_FORBEHANDLING)

        return EtteroppgjoerOgOppgave(
            etteroppgjoerBehandling = nyForbehandling,
            oppgave = oppgave,
        )
    }

    fun beregnFaktiskInntekt(
        behandlingId: UUID,
        request: BeregnFaktiskInntektRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val forbehandling = dao.hentForbehandling(behandlingId) ?: throw FantIkkeForbehandling(behandlingId)

        val sisteIverksatteBehandling =
            behandlingService.hentSisteIverksatte(forbehandling.sak.id)
                ?: throw InternfeilException("Fant ikke siste iverksatte")

        val request =
            EtteroppgjoerBeregnFaktiskInntektRequest(
                sakId = forbehandling.sak.id,
                forbehandlingId = forbehandling.id,
                sisteIverksatteBehandling = sisteIverksatteBehandling.id,
                aar = forbehandling.aar,
                loennsinntekt = request.loennsinntekt,
                naeringsinntekt = request.naeringsinntekt,
                afp = request.afp,
                utland = request.utland,
            )

        runBlocking { beregningKlient.beregnAvkortingFaktiskInntekt(request, brukerTokenInfo) }
    }
}

data class EtteroppgjoerOgOppgave(
    val etteroppgjoerBehandling: EtteroppgjoerForbehandling,
    val oppgave: OppgaveIntern,
)

data class BeregnFaktiskInntektRequest(
    val loennsinntekt: Int,
    val afp: Int,
    val naeringsinntekt: Int,
    val utland: Int,
)

class FantIkkeForbehandling(
    val behandlingId: UUID,
) : IkkeFunnetException(
        code = "MANGLER_FORBEHANDLING_ETTEROPPGJOER",
        detail = "Fant ikke forbehandling etteroppgjør $behandlingId",
    )
