package no.nav.etterlatte.avkorting

import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.time.LocalTime
import java.util.UUID

class AvkortingTidligAlderspensjonService(
    private val behandlingKlient: BehandlingKlient,
    private val avkortingRepository: AvkortingRepository,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /*
     * Hvis innvilga måneder overstyrespå grunn av tidlig alderspensjon,
     * skal det opprettes en egen oppgave med frist én måned før forventet uttak av
     * alderspensjon.
     *
     * Systemet håndterer automatisk overgang ved 67 år, men har ingen informasjon om
     * uttak av tidlig alderspensjon. Saksbehandler må derfor legge inn opphøret manuelt.
     */
    suspend fun opprettOppgaveHvisTidligAlderspensjon(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
        logger.info("Sjekker om vi skal opprette oppgave om opphør grunnen tidlig alderspensjon for sakId=${behandling.sak.sakId}")
        val avkorting =
            avkortingRepository.hentAvkorting(behandlingId) ?: throw AvkortingFinnesIkkeException(behandlingId)

        val (overstyrtInntektsavkorting, aarsoppgjoer) = skalHaOppgaveTidligAlderspensjon(avkorting, behandling) ?: return

        if (overstyrtInntektsavkorting != null) {
            logger.info("Oppretter oppgave om opphør grunnen tidlig alderspensjon for sakId=${behandling.sak.sakId}")

            // Vi trekker fra 2 måneder for å få måneden før stans av ytelsen
            val innvilgaMaaneder = overstyrtInntektsavkorting.grunnlag.innvilgaMaaneder - 2L
            val oppgaveFrist =
                aarsoppgjoer.fom
                    .plusMonths(innvilgaMaaneder)
                    .atDay(1)

            behandlingKlient.opprettOppgave(
                behandling.sak,
                brukerTokenInfo,
                OppgaveType.GENERELL_OPPGAVE,
                "Forventet opphør pga. tidlig alderspensjon. Opphør må legges inn manuelt når bruker faktisk tar ut pensjonen, ellers løper saken til 67 år.",
                Tidspunkt.ofNorskTidssone(oppgaveFrist, LocalTime.now()),
                OppgaveKilde.BEHANDLING,
                behandlingId.toString(),
            )
        } else {
            logger.info("Fant ingen tidlig alderspensjon for sakId=${behandling.sak.sakId}, trenger ingen oppgave om opphør.")
        }
    }

    private fun skalHaOppgaveTidligAlderspensjon(
        avkorting: Avkorting,
        behandling: DetaljertBehandling,
    ): Pair<Inntektsavkorting?, Aarsoppgjoer>? {
        val (overstyrtInntektsavkorting, aarsoppgjoer) =
            when (behandling.behandlingType) {
                BehandlingType.FØRSTEGANGSBEHANDLING -> {
                    val foersteAar =
                        avkorting.aarsoppgjoer.firstOrNull() as? AarsoppgjoerLoepende ?: run {
                            logger.info(
                                "Førstegangsbehandling=${behandling.id} inneholder etteroppgjør, skal ikke ha oppgave for tidligAlderpensjon",
                            )
                            return null
                        }
                    val overstyrtFoersteAar =
                        foersteAar.inntektsavkorting.find {
                            it.grunnlag.overstyrtInnvilgaMaanederAarsak == OverstyrtInnvilgaMaanederAarsak.TAR_UT_PENSJON_TIDLIG
                        }
                    if (overstyrtFoersteAar == null && avkorting.aarsoppgjoer.size == 2) {
                        val andreAar =
                            avkorting.aarsoppgjoer.lastOrNull() as? AarsoppgjoerLoepende ?: run {
                                logger.info(
                                    "Førstegangsbehandling=${behandling.id} inneholder etteroppgjør, skal ikke ha oppgave for tidligAlderpensjon",
                                )
                                return null
                            }
                        val overstyrtAndreAar =
                            andreAar.inntektsavkorting.find {
                                it.grunnlag.overstyrtInnvilgaMaanederAarsak == OverstyrtInnvilgaMaanederAarsak.TAR_UT_PENSJON_TIDLIG
                            }
                        Pair(overstyrtAndreAar, avkorting.aarsoppgjoer.last())
                    } else {
                        Pair(overstyrtFoersteAar, avkorting.aarsoppgjoer.first())
                    }
                }

                BehandlingType.REVURDERING -> {
                    val aarsoppgjoer =
                        avkorting.aarsoppgjoer.singleOrNull {
                            it.aar == behandling.virkningstidspunkt?.dato?.year
                        } as? AarsoppgjoerLoepende ?: run {
                            logger.info(
                                "Første revurdering=${behandling.id} i nytt år inneholder etteroppgjør, skal ikke ha oppgave for tidligAlderpensjon",
                            )
                            return null
                        }
                    val overstyrt =
                        aarsoppgjoer.inntektsavkorting.find {
                            it.grunnlag.overstyrtInnvilgaMaanederAarsak == OverstyrtInnvilgaMaanederAarsak.TAR_UT_PENSJON_TIDLIG &&
                                it.grunnlag.periode.fom == behandling.virkningstidspunkt?.dato
                        }
                    Pair(overstyrt, aarsoppgjoer)
                }
            }

        return Pair(overstyrtInntektsavkorting, aarsoppgjoer)
    }
}
