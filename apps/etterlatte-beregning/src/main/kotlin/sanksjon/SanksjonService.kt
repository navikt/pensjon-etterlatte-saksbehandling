package no.nav.etterlatte.sanksjon

import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.erGrunnlagLiktFoerEnDato
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.beregning.Sanksjon
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.time.YearMonth
import java.util.UUID

class SanksjonService(
    private val behandlingKlient: BehandlingKlient,
    private val sanksjonRepository: SanksjonRepository,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentSanksjon(behandlingId: UUID): List<Sanksjon>? {
        logger.info("Henter sanksjoner med behandlingID=$behandlingId")
        return sanksjonRepository.hentSanksjon(behandlingId)?.sortedBy { it.fom }
    }

    /**
     * Kopierer inn sanksjoner fra forrige iverksatte behandling til denne hvis:
     *
     *  1. denne behandlingen er en revurdering
     *  2. denne behandlingen har ingen sanksjoner knyttet til seg allerede
     *  3. den forrige iverksatte behandlingen i saken har sanksjoner
     *
     *  Dette fungerer bra nok til at denne metoden kan kjøres "ofte" -- som i dag
     *  er hver gang vi beregner.
     *
     *  Men det fungerer ikke så bra hvis det man vil gjøre er å slette en sanksjon som
     *  er lagt inn feil (en omgjøring pga klage for eksempel). Da må bruken av denne
     *  funksjonen justeres.
     */
    suspend fun kopierSanksjon(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
        if (behandling.behandlingType != BehandlingType.REVURDERING) {
            return
        }

        val forrigeBehandlingId = behandlingKlient.hentSisteIverksatteBehandling(behandling.sak, brukerTokenInfo).id

        val sanksjonerIDenneBehandlingen = sanksjonRepository.hentSanksjon(behandlingId)
        val sanksjonerIForrigeBehandling = sanksjonRepository.hentSanksjon(forrigeBehandlingId)

        if (sanksjonerIDenneBehandlingen.isNullOrEmpty()) {
            sanksjonerIForrigeBehandling?.forEach {
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

    suspend fun opprettEllerOppdaterSanksjon(
        behandlingId: UUID,
        sanksjon: LagreSanksjon,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)

        if (!behandling.status.kanEndres()) throw BehandlingKanIkkeEndres()

        if (behandling.virkningstidspunkt == null) throw ManglerVirkningstidspunktException()
        if (sanksjon.sakId != behandling.sak) throw SakidTilhoererIkkeBehandlingException()

        val normalisertFom = YearMonth.from(sanksjon.fom)
        val normalisertTom = sanksjon.tom?.let { YearMonth.from(it) }
        if (normalisertTom != null && normalisertTom.isBefore(normalisertFom)) throw TomErFoerFomException()

        if (!sanksjonerErLikeFoerVirk(behandling, sanksjon, brukerTokenInfo)) {
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

    private suspend fun sanksjonerErLikeFoerVirk(
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
            krevIkkeNull(behandling.virkningstidspunkt) {
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
