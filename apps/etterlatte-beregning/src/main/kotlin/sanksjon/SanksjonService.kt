package no.nav.etterlatte.sanksjon

import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.erGrunnlagLiktFoerEnDato
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.time.YearMonth
import java.util.UUID

enum class SanksjonToggles(
    val value: String,
) : FeatureToggle {
    SANKSJON("sanksjon"),
    ;

    override fun key(): String = this.value
}

class SanksjonService(
    private val behandlingKlient: BehandlingKlient,
    private val sanksjonRepository: SanksjonRepository,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(SanksjonService::class.java)

    fun hentSanksjon(behandlingId: UUID): List<Sanksjon>? {
        if (!featureToggleService.isEnabled(SanksjonToggles.SANKSJON, defaultValue = false)) {
            return null
        }
        logger.info("Henter sanksjoner med behandlingID=$behandlingId")
        return sanksjonRepository.hentSanksjon(behandlingId)?.sortedBy { it.fom }
    }

    suspend fun kopierSanksjon(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        logger.info("Kopierer sanksjoner fra forrige behandling med behandlingID=$behandlingId")
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
        val forrigeBehandlingId = behandlingKlient.hentSisteIverksatteBehandling(behandling.sak, brukerTokenInfo).id

        val sanksjoner = sanksjonRepository.hentSanksjon(forrigeBehandlingId)
        if (sanksjoner != null) {
            sanksjoner.forEach {
                sanksjonRepository.opprettSanksjonFraKopi(
                    behandlingId,
                    it.sakId,
                    it,
                )
            }
        }
    }

    suspend fun opprettEllerOppdaterSanksjon(
        behandlingId: UUID,
        sanksjon: LagreSanksjon,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)

        if (!behandling.status.kanEndres()) throw BehandlingKanIkkeEndres()

        if (behandling.virkningstidspunkt == null) throw ManglerVirkningstidspunktException()
        if (sanksjon.sakId != behandling.sak) throw SakidTilhoererIkkeBehandlingException()

        if (sanksjon.tom != null && sanksjon.tom.isBefore(sanksjon.fom)) throw TomErFoerFomException()
        if (!sanksjonErLikeFoerVirk(behandling, sanksjon, brukerTokenInfo)) {
            throw SanksjonEndresFoerVirkException()
        }

        if (sanksjon.id === null) {
            logger.info("Oppretter sanksjon med behandlingID=$behandlingId")
            settBehandlingTilBeregnetStatus(behandlingId, brukerTokenInfo)
            return sanksjonRepository.opprettSanksjon(behandlingId, behandling.sak, brukerTokenInfo.ident(), sanksjon)
        }

        logger.info("Oppdaterer sanksjon med behandlingID=$behandlingId")
        // TODO: vi vil kanskje ikke tvinge noen avkortings-reberegning her hvis det er kun beskrivelse som endrer seg
        settBehandlingTilBeregnetStatus(behandlingId, brukerTokenInfo)
        return sanksjonRepository.oppdaterSanksjon(sanksjon, brukerTokenInfo.ident())
    }

    private suspend fun sanksjonErLikeFoerVirk(
        behandling: DetaljertBehandling,
        sanksjon: LagreSanksjon,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean {
        if (behandling.behandlingType == BehandlingType.FØRSTEGANGSBEHANDLING) {
            if (YearMonth
                    .of(
                        sanksjon.fom.year,
                        sanksjon.fom.month,
                    ) < behandling.virkningstidspunkt!!.dato
            ) {
                throw FomErFoerVirkningstidpunktException()
            }
            return true
        }
        val forrigeIverksatteBehandling =
            behandlingKlient.hentSisteIverksatteBehandling(behandling.sak, brukerTokenInfo)
        val sanksjonerIForrigeBehandling =
            sanksjonRepository.hentSanksjon(forrigeIverksatteBehandling.id) ?: emptyList()
        val sanksjonerIBehandling = sanksjonRepository.hentSanksjon(behandling.id) ?: emptyList()

        val forrigeBehandlingSanksjoner =
            sanksjonerIForrigeBehandling.map {
                GrunnlagMedPeriode(
                    data = it.type,
                    fom = it.fom.atDay(1),
                    tom = it.tom?.atEndOfMonth(),
                )
            }
        val naavaerendeBehandlingSanksjoner =
            sanksjonerIBehandling
                .filter {
                    // Filtrerer bort den sanksjonen vi endrer (hvis vi endrer noe)
                    it.id != sanksjon.id
                }.map {
                    GrunnlagMedPeriode(
                        data = it.type,
                        fom = it.fom.atDay(1),
                        tom = it.tom?.atEndOfMonth(),
                    )
                } +
                listOf(
                    GrunnlagMedPeriode(
                        data = sanksjon.type,
                        fom = sanksjon.fom,
                        tom = sanksjon.tom,
                    ),
                )

        return erGrunnlagLiktFoerEnDato(
            forrigeBehandlingSanksjoner,
            naavaerendeBehandlingSanksjoner,
            behandling.virkningstidspunkt!!.dato.atDay(1),
        )
    }

    suspend fun slettSanksjon(
        behandlingId: UUID,
        sanksjonId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
        if (!behandling.status.kanEndres()) {
            throw BehandlingKanIkkeEndres()
        }

        val sanksjonSomSkalSlettes = sanksjonRepository.hentSanksjonMedId(sanksjonId) ?: return
        if (sanksjonSomSkalSlettes.behandlingId != behandling.id) {
            throw UgyldigForespoerselException(
                "SANKSJON_TILHOERER_IKKE_BEHANDLING",
                "Den angitte sanksjonsId'en hører ikke til den angitte behandlingId'en",
            )
        }

        val virkningstidspunkt =
            checkNotNull(behandling.virkningstidspunkt) {
                "Behandling (id=$behandlingId) man prøver å slette sanksjon (id=$sanksjonId) i har ikke virkningstidspunkt"
            }.dato
        if (sanksjonSomSkalSlettes.fom < virkningstidspunkt) {
            // Siden vi forbyr å legge til sanksjoner med fom < virk, er dette en kopiert sanksjon fra forrige
            // behandling, og vi skal da ikke kunnne slette den.
            throw SanksjonEndresFoerVirkException()
        }

        settBehandlingTilBeregnetStatus(behandlingId, brukerTokenInfo)
        logger.info("Sletter sanksjon med sanksjonID=$sanksjonId")
        sanksjonRepository.slettSanksjon(sanksjonId)
    }

    private suspend fun settBehandlingTilBeregnetStatus(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        // Setter behandlingen til status "beregnet", slik at avkorting fanger opp at den må regnes ut på nytt
        if (!behandlingKlient.kanBeregnes(behandlingId, brukerTokenInfo, true)) {
            throw BehandlingKanIkkeEndres()
        }
    }
}

class FomErFoerVirkningstidpunktException :
    UgyldigForespoerselException(
        code = "FOM_ER_FOER_VIRKNINGSTIDSPUNKT",
        detail = "Fra og med dato kan ikke være før virkningstidspunkt",
    )

class TomErFoerFomException :
    UgyldigForespoerselException(
        code = "TOM_ER_FOER_FOM",
        detail = "Til og med dato er kan ikke være før fra og med dato",
    )

class SakidTilhoererIkkeBehandlingException :
    UgyldigForespoerselException(
        code = "SAK_ID_TILHOERER_IKKE_BEHANDLING",
        detail = "Sak id stemmer ikke over ens med behandling",
    )

class ManglerVirkningstidspunktException :
    UgyldigForespoerselException(
        code = "MANGLER_VIRK",
        detail = "Behandling mangler virkningstidspunkt",
    )

class BehandlingKanIkkeEndres :
    IkkeTillattException(
        code = "BEHANDLINGEN_KAN_IKKE_ENDRES",
        detail = "Behandlingen kan ikke endres",
    )

class SanksjonEndresFoerVirkException :
    UgyldigForespoerselException(
        code = "SANKSJON_ENDRES_FOER_VIRK",
        detail =
            "Sanksjoner kan ikke endres før virkningstidspunkt. Hvis sanksjonen skal endres tidligere " +
                "må virkningstidspunktet flyttes tidligere.",
    )
