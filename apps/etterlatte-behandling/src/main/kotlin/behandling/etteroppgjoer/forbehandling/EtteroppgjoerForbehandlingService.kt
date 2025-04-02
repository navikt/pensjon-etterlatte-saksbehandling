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
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.beregning.BeregnetEtteroppgjoerResultatDto
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnFaktiskInntektRequest
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkortingRequest
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.logger
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakLesDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Month
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
    private val vedtakKlient: VedtakKlient,
) {
    private val logger: Logger = LoggerFactory.getLogger(EtteroppgjoerForbehandlingService::class.java)

    // opprett forbehandling for etteroppgjør med status MOTTATT_SKATTEOPPGJOER
    suspend fun startOpprettForbehandlingKjoering() {
        logger.info("Opprette forbehandling for etteroppgjør med mottatt skatteoppgjør")
        val etteroppgjoerListe = etteroppgjoerService.hentEtteroppgjoerForStatus(EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER)

        for (etteroppgjoer in etteroppgjoerListe) {
            opprettEtteroppgjoerForbehandling(etteroppgjoer.sakId, etteroppgjoer.inntektsaar, HardkodaSystembruker.etteroppgjoer)
        }
    }

    fun hentEtteroppgjoerForbehandling(behandlingId: UUID): EtteroppgjoerForbehandling =
        dao.hentForbehandling(behandlingId) ?: throw FantIkkeForbehandling(behandlingId)

    fun hentEtteroppgjoerForbehandlinger(sakId: SakId): List<EtteroppgjoerForbehandling> = inTransaction { dao.hentForbehandlinger(sakId) }

    suspend fun opprettEtteroppgjoerForbehandling(
        sakId: SakId,
        inntektsaar: Int,
        brukerTokenInfo: BrukerTokenInfo,
    ): EtteroppgjoerForbehandlingOgOppgave {
        logger.info("Oppretter etteroppgjør forbehandling for sakId=$sakId")

        if (!kanOppretteEtteroppgjoerForbehandling(sakId, inntektsaar)) {
            throw InternfeilException("Kan ikke opprette forbehandling ... ???")
        }

        val sak =
            inTransaction {
                sakDao.hentSak(sakId) ?: throw NotFoundException("Fant ikke sak med id=$sakId")
            }

        val pensjonsgivendeInntekt = sigrunKlient.hentPensjonsgivendeInntekt(sak.ident, inntektsaar)
        val aInntekt = inntektskomponentService.hentInntektFraAInntekt(sak.ident, inntektsaar)

        val virkOgOpphoer = vedtakKlient.hentFoersteVirkOgOppoerTilSak(sakId, brukerTokenInfo)

        return inTransaction {
            val nyForbehandling =
                EtteroppgjoerForbehandling(
                    id = UUID.randomUUID(),
                    hendelseId = UUID.randomUUID(),
                    sak = sak,
                    status = "opprettet",
                    aar = inntektsaar,
                    innvilgetPeriode =
                        Periode(
                            fom =
                                if (virkOgOpphoer.foersteVirk.year == inntektsaar) {
                                    virkOgOpphoer.foersteVirk
                                } else {
                                    YearMonth.of(inntektsaar, Month.JANUARY)
                                },
                            tom =
                                if (virkOgOpphoer.opphoer != null && virkOgOpphoer!!.opphoer!!.year == inntektsaar) {
                                    virkOgOpphoer.opphoer
                                } else {
                                    YearMonth.of(inntektsaar, Month.DECEMBER)
                                },
                        ),
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

            EtteroppgjoerForbehandlingOgOppgave(
                etteroppgjoerForbehandling = nyForbehandling,
                oppgave = oppgave,
            )
        }
    }

    fun hentForbehandlingForFrontend(
        brukerTokenInfo: BrukerTokenInfo,
        behandlingId: UUID,
    ): ForbehandlingDto {
        val forbehandling = hentEtteroppgjoerForbehandling(behandlingId)

        val sisteIverksatteBehandling =
            behandlingService.hentSisteIverksatte(forbehandling.sak.id)
                ?: throw InternfeilException("Fant ikke siste iverksatt behandling")

        val request =
            EtteroppgjoerBeregnetAvkortingRequest(
                forbehandling = behandlingId,
                sisteIverksatteBehandling = sisteIverksatteBehandling.id,
                aar = forbehandling.aar,
            )
        logger.info("Henter avkorting for forbehandling: $request")
        val avkorting =
            runBlocking {
                beregningKlient.hentAvkortingForForbehandlingEtteroppgjoer(
                    request,
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

    suspend fun beregnFaktiskInntekt(
        behandlingId: UUID,
        request: BeregnFaktiskInntektRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregnetEtteroppgjoerResultatDto {
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
                    aar = forbehandling.aar,
                    loennsinntekt = request.loennsinntekt,
                    naeringsinntekt = request.naeringsinntekt,
                    afp = request.afp,
                    utland = request.utland,
                )
            }
        return beregningKlient.beregnAvkortingFaktiskInntekt(request, brukerTokenInfo)
    }

    private fun kanOppretteEtteroppgjoerForbehandling(
        sakId: SakId,
        inntektsaar: Int,
    ): Boolean {
        val etteroppgjoer = etteroppgjoerService.hentEtteroppgjoer(sakId, inntektsaar) ?: return false

        // TODO: hva mer må vi sjekke?
        // finnes en forbehandling fra før?
        //

        return when (etteroppgjoer.status) {
            EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER -> true
            else -> false
        }
    }
}

data class EtteroppgjoerForbehandlingOgOppgave(
    val etteroppgjoerForbehandling: EtteroppgjoerForbehandling,
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
