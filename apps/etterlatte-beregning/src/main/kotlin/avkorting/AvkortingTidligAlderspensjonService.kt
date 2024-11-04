package no.nav.etterlatte.avkorting

import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
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

    // Sjekke om Avkorting innvilga måneder overstyres på grunn av tidlig alderspensjon, og opprett oppgave om opphør
    suspend fun opprettOppgaveHvisTidligAlderspensjon(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
        logger.info("Sjekker om vi skal opprette oppgave om opphør grunnen tidlig alderspensjon for sakId=${behandling.sak.sakId}")
        val avkorting =
            avkortingRepository.hentAvkorting(behandlingId) ?: throw AvkortingFinnesIkkeException(behandlingId)

        val (overstyrtInntektsavkorting, aarsoppgjoer) =
            when (behandling.behandlingType) {
                BehandlingType.FØRSTEGANGSBEHANDLING -> {
                    val overstyrtFoersteAar =
                        avkorting.aarsoppgjoer.first().inntektsavkorting.find {
                            it.grunnlag.overstyrtInnvilgaMaanederAarsak == OverstyrtInnvilgaMaanederAarsak.TAR_UT_PENSJON_TIDLIG
                        }
                    if (overstyrtFoersteAar == null && avkorting.aarsoppgjoer.size == 2) {
                        val overstyrtAndreAar =
                            avkorting.aarsoppgjoer.last().inntektsavkorting.find {
                                it.grunnlag.overstyrtInnvilgaMaanederAarsak == OverstyrtInnvilgaMaanederAarsak.TAR_UT_PENSJON_TIDLIG
                            }
                        Pair(overstyrtAndreAar, avkorting.aarsoppgjoer.last())
                    } else {
                        Pair(overstyrtFoersteAar, avkorting.aarsoppgjoer.first())
                    }
                }

                BehandlingType.REVURDERING -> {
                    val aarsoppgjoer = avkorting.aarsoppgjoer.single { it.aar == behandling.virkningstidspunkt?.dato?.year }
                    val overstyrt =
                        aarsoppgjoer.inntektsavkorting
                            .find {
                                it.grunnlag.overstyrtInnvilgaMaanederAarsak == OverstyrtInnvilgaMaanederAarsak.TAR_UT_PENSJON_TIDLIG &&
                                    it.grunnlag.periode.fom == behandling.virkningstidspunkt?.dato
                            }
                    Pair(overstyrt, aarsoppgjoer)
                }
            }

        if (overstyrtInntektsavkorting != null) {
            logger.info("Oppretter oppgave om opphør grunnen tidlig alderspensjon for sakId=${behandling.sak.sakId}")

            // Vi trekker fra 2 måneder (2L) for å få måneden før opphør (2024.03).
            // For eksempel, hvis fom = 2024.1 og innvilgaMaaneder = 3:
            // 01 + 03 = 4 (2024.04), så 4 - 2L = 2 gir oss 2024.02 som er en månede før 2024.03.
            val innvilgaMaaneder = overstyrtInntektsavkorting.grunnlag.innvilgaMaaneder - 2L
            val oppgaveFrist =
                aarsoppgjoer.fom
                    .plusMonths(innvilgaMaaneder)
                    .atDay(1)

            behandlingKlient.opprettOppgave(
                behandling.sak,
                brukerTokenInfo,
                OppgaveType.GENERELL_OPPGAVE,
                "Opphør av ytelse på grunn av alderspensjon.",
                Tidspunkt.ofNorskTidssone(oppgaveFrist, LocalTime.now()),
                OppgaveKilde.BEHANDLING,
                behandlingId.toString(),
            )
        } else {
            logger.info("Fant ingen tidlig alderspensjon for sakId=${behandling.sak.sakId}, trenger ingen oppgave om opphør.")
        }
    }
}
