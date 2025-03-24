package no.nav.etterlatte.behandling.etteroppgjoer.forbehandling

import io.ktor.server.plugins.NotFoundException
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerForbehandling
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerOpplysninger
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import no.nav.etterlatte.behandling.etteroppgjoer.ForbehandlingDto
import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.InntektskomponentService
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SigrunKlient
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.inTransaction
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
import java.time.YearMonth
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
    suspend fun hentEtteroppgjoer(
        brukerTokenInfo: BrukerTokenInfo,
        behandlingId: UUID,
    ): ForbehandlingDto {
        val forbehandling =
            inTransaction { dao.hentForbehandling(behandlingId) } ?: throw FantIkkeForbehandling(behandlingId)

        val sisteIverksatteBehandling =
            inTransaction {
                behandlingService.hentSisteIverksatte(forbehandling.sak.id)
                    ?: throw InternfeilException("Fant ikke siste iverksatte")
            }

        val avkorting =
            beregningKlient.hentAvkortingForForbehandlingEtteroppgjoer(
                EtteroppgjoerBeregnetAvkortingRequest(
                    forbehandling = behandlingId,
                    sisteIverksatteBehandling = sisteIverksatteBehandling.id,
                    aar = forbehandling.aar,
                ),
                brukerTokenInfo,
            )

        return inTransaction {
            val pensjonsgivendeInntekt = dao.hentPensjonsgivendeInntekt(behandlingId)
            val aInntekt = dao.hentAInntekt(behandlingId)

            if (pensjonsgivendeInntekt == null || aInntekt == null) {
                throw InternfeilException(
                    "Mangler ${if (pensjonsgivendeInntekt == null) "pensjonsgivendeInntekt" else "aInntekt"} for behandlingId=$behandlingId",
                )
            }

            ForbehandlingDto(
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
    }

    suspend fun opprettEtteroppgjoer(
        sakId: SakId,
        inntektsaar: Int,
    ): EtteroppgjoerOgOppgave {
        val sak =
            inTransaction {
                sakDao.hentSak(sakId) ?: throw NotFoundException("Fant ikke sak med id=$sakId")
            }

        val pensjonsgivendeInntekt = sigrunKlient.hentPensjonsgivendeInntekt(sak.ident, inntektsaar)
        val aInntekt = inntektskomponentService.hentInntektFraAInntekt(sak.ident, inntektsaar)

        return inTransaction {
            val nyForbehandling =
                EtteroppgjoerForbehandling(
                    id = UUID.randomUUID(),
                    hendelseId = UUID.randomUUID(),
                    sak = sak,
                    status = "opprettet",
                    aar = inntektsaar,
                    opprettet = Tidspunkt.now(),
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

            EtteroppgjoerOgOppgave(
                etteroppgjoerBehandling = nyForbehandling,
                oppgave = oppgave,
            )
        }
    }

    suspend fun fastsettFaktiskInntekt(
        behandlingId: UUID,
        request: FastsettFaktiskInntektRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val request =
            inTransaction {
                val forbehandling = dao.hentForbehandling(behandlingId) ?: throw FantIkkeForbehandling(behandlingId)

                val sisteIverksatteBehandling =
                    behandlingService.hentSisteIverksatte(forbehandling.sak.id)
                        ?: throw InternfeilException("Fant ikke siste iverksatte")

                EtteroppgjoerBeregnFaktiskInntektRequest(
                    sakId = forbehandling.sak.id,
                    forbehandlingId = forbehandling.id,
                    sisteIverksatteBehandling = sisteIverksatteBehandling.id,
                    fraOgMed = YearMonth.of(2024, 1), // TODO utled rikig fom
                    tilOgMed = YearMonth.of(2024, 12), // TODO utled rikig tom
                    loennsinntekt = request.loennsinntekt,
                    naeringsinntekt = request.naeringsinntekt,
                    afp = request.afp,
                    utland = request.utland,
                )
            }
        beregningKlient.beregnAvkortingFaktiskInntekt(request, brukerTokenInfo)
    }
}

data class EtteroppgjoerOgOppgave(
    val etteroppgjoerBehandling: EtteroppgjoerForbehandling,
    val oppgave: OppgaveIntern,
)

data class FastsettFaktiskInntektRequest(
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
