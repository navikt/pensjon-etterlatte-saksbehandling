package no.nav.etterlatte.modules.beregning.sanksjon

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BehandlingStatusService
import no.nav.etterlatte.behandling.domain.Behandling.BehandlingStoetterIkkeStatusEndringException
import no.nav.etterlatte.behandling.domain.TilstandException
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.erGrunnlagLiktFoerEnDato
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransactionIfNeeded
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.beregning.Sanksjon
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.modules.beregning.BeregningToggles
import org.slf4j.LoggerFactory
import java.time.YearMonth
import java.util.UUID

class SanksjonService(
    private val behandlingService: BehandlingService,
    private val sanksjonRepository: SanksjonRepository,
    private val featureToggleService: FeatureToggleService,
    private val behandlingStatusService: BehandlingStatusService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentSanksjon(behandlingId: UUID): List<Sanksjon> {
        logger.info("Henter sanksjoner med behandlingID=$behandlingId")
        return sanksjonRepository.hentSanksjon(behandlingId).sortedBy { it.fom }
    }

    /**
     * Kopierer inn sanksjoner fra samme kildebehandling som beregningsgrunnlaget hvis:
     *
     *  1. denne behandlingen er en revurdering
     *  2. denne behandlingen har ingen sanksjoner knyttet til seg allerede
     *  3. kildebehandlingen har sanksjoner
     *
     *  Dette fungerer bra nok til at denne metoden kan kjøres "ofte" -- som i dag
     *  er hver gang vi beregner.
     *
     *  Men det fungerer ikke så bra hvis det man vil gjøre er å slette en sanksjon som
     *  er lagt inn feil (en omgjøring pga klage for eksempel). Da må bruken av denne
     *  funksjonen justeres.
     */
    fun kopierSanksjon(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val behandling =
            inTransactionIfNeeded { behandlingService.hentDetaljertBehandling(behandlingId, brukerTokenInfo) }
                ?: throw InternfeilException("Klarte ikke hente behandling med behandlingId=$behandlingId")
        if (behandling.behandlingType != BehandlingType.REVURDERING) {
            return
        }

        val sanksjonerIDenneBehandlingen = sanksjonRepository.hentSanksjon(behandlingId)
        val sanksjonerIForrigeBehandling = sanksjonRepository.hentSanksjon(forrigeBehandlingId)

        if (sanksjonerIDenneBehandlingen.isEmpty()) {
            sanksjonerIForrigeBehandling.forEach {
                logger.info(
                    "Kopierer sanksjon [${it.id}] fra forrige behandling til behandlingen " +
                        "med behandlingID=$behandlingId, fra behandling med id=$forrigeBehandlingId",
                )
                sanksjonRepository.opprettSanksjonFraKopi(
                    behandlingId,
                    it.sakId,
                    it,
                )
            }
        }
    }

    fun opprettEllerOppdaterSanksjon(
        behandlingId: UUID,
        sanksjon: LagreSanksjon,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val behandling =
            inTransactionIfNeeded { behandlingService.hentDetaljertBehandling(behandlingId, brukerTokenInfo) }
                ?: throw InternfeilException("Klarte ikke hente behandling med behandlingId=$behandlingId")

        if (!behandling.status.kanEndres()) throw BehandlingKanIkkeEndres()

        if (behandling.virkningstidspunkt == null) throw ManglerVirkningstidspunktException()
        if (sanksjon.sakId != behandling.sak) throw SakidTilhoererIkkeBehandlingException()
        if (sanksjon.id != null && sanksjonRepository.hentSanksjonMedId(sanksjon.id)?.behandlingId != behandling.id) {
            throw SanksjonTilhoererIkkeBehandlingException()
        }

        val normalisertFom = YearMonth.from(sanksjon.fom)
        val normalisertTom = sanksjon.tom?.let { YearMonth.from(it) }
        if (normalisertTom != null && normalisertTom.isBefore(normalisertFom)) throw TomErFoerFomException()

        if (!sanksjonerErLikeFoerVirk(behandling, sanksjon, brukerTokenInfo)) {
            throw SanksjonEndresFoerVirkException()
        }

        if (sanksjon.id === null) {
            logger.info("Oppretter sanksjon med behandlingID=$behandlingId")
            oppdaterBehandlingstatusEtterOppdateringSanksjon(behandlingId, brukerTokenInfo)
            return sanksjonRepository.opprettSanksjon(behandlingId, behandling.sak, brukerTokenInfo.ident(), sanksjon)
        }

        logger.info("Oppdaterer sanksjon med behandlingID=$behandlingId")
        oppdaterBehandlingstatusEtterOppdateringSanksjon(behandlingId, brukerTokenInfo)
        return sanksjonRepository.oppdaterSanksjon(sanksjon, brukerTokenInfo.ident())
    }

    private fun sanksjonerErLikeFoerVirk(
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
            inTransactionIfNeeded { behandlingService.hentSisteIverksatteBehandling(behandling.sak) }
                ?: throw InternfeilException("Klarte ikke hente siste iverksatte behandling på sak med id=${behandling.sak}")
        val sanksjonerIForrigeBehandling =
            sanksjonRepository.hentSanksjon(forrigeIverksatteBehandling.id)
        val sanksjonerIBehandling = sanksjonRepository.hentSanksjon(behandling.id)

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
                        fom = YearMonth.from(sanksjon.fom).atDay(1),
                        tom = sanksjon.tom?.let { YearMonth.from(it).atEndOfMonth() },
                    ),
                )

        return erGrunnlagLiktFoerEnDato(
            forrigeBehandlingSanksjoner,
            naavaerendeBehandlingSanksjoner,
            behandling.virkningstidspunkt!!.dato.atDay(1),
        )
    }

    fun slettSanksjon(
        behandlingId: UUID,
        sanksjonId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val behandling =
            inTransactionIfNeeded { behandlingService.hentDetaljertBehandling(behandlingId, brukerTokenInfo) }
                ?: throw InternfeilException("Klarte ikke hente behandling med behandlingId=$behandlingId")
        if (!behandling.status.kanEndres()) {
            throw BehandlingKanIkkeEndres()
        }

        val sanksjonSomSkalSlettes = sanksjonRepository.hentSanksjonMedId(sanksjonId) ?: return
        if (sanksjonSomSkalSlettes.behandlingId != behandling.id) {
            throw SanksjonTilhoererIkkeBehandlingException()
        }

        val virkningstidspunkt =
            krevIkkeNull(behandling.virkningstidspunkt) {
                "Behandling (id=$behandlingId) man prøver å slette sanksjon (id=$sanksjonId) i har ikke virkningstidspunkt"
            }.dato
        if (sanksjonSomSkalSlettes.fom < virkningstidspunkt) {
            // Siden vi forbyr å legge til sanksjoner med fom < virk, er dette en kopiert sanksjon fra forrige
            // behandling, og vi skal da ikke kunnne slette den.
            throw SanksjonEndresFoerVirkException()
        }

        oppdaterBehandlingstatusEtterOppdateringSanksjon(behandlingId, brukerTokenInfo)
        logger.info("Sletter sanksjon med sanksjonID=$sanksjonId")
        sanksjonRepository.slettSanksjon(sanksjonId)
    }

    private fun oppdaterBehandlingstatusEtterOppdateringSanksjon(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val brukNyeReglerAvkorting = featureToggleService.isEnabled(BeregningToggles.BEREGNING_BRUK_NYE_BEREGNINGSREGLER, false)

        try {
            inTransactionIfNeeded {
                if (brukNyeReglerAvkorting) {
                    // Setter behandlingen tl status "trygdetid oppdatert", slik at vi fanger opp at beregningen må oppdateres
                    behandlingStatusService.settTrygdetidOppdatert(behandlingId, brukerTokenInfo, dryRun = false)
                } else {
                    // Setter behandlingen til status "beregnet", slik at avkorting fanger opp at den må regnes ut på nytt
                    behandlingStatusService.settBeregnet(behandlingId, brukerTokenInfo, dryRun = false)
                }
            }
        } catch (e: Exception) {
            throw when (e) {
                is TilstandException.UgyldigTilstand,
                is TilstandException.IkkeFyltUt,
                is TilstandException.KanIkkeRedigere,
                is BehandlingStoetterIkkeStatusEndringException,
                -> BehandlingKanIkkeEndres(e)

                else -> e
            }
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

class SanksjonTilhoererIkkeBehandlingException :
    UgyldigForespoerselException(
        code = "SANKSJON_TILHOERER_IKKE_BEHANDLING",
        detail = "Den angitte sanksjonsId'en hører ikke til den angitte behandlingId'en",
    )

class ManglerVirkningstidspunktException :
    UgyldigForespoerselException(
        code = "MANGLER_VIRK",
        detail = "Behandling mangler virkningstidspunkt",
    )

class BehandlingKanIkkeEndres(
    cause: Throwable? = null,
) : IkkeTillattException(
        code = "BEHANDLINGEN_KAN_IKKE_ENDRES",
        detail = "Behandlingen kan ikke endres",
        cause = cause,
    )

class SanksjonEndresFoerVirkException :
    UgyldigForespoerselException(
        code = "SANKSJON_ENDRES_FOER_VIRK",
        detail =
            "Sanksjoner kan ikke endres før virkningstidspunkt. Hvis sanksjonen skal endres tidligere " +
                "må virkningstidspunktet flyttes tidligere.",
    )
