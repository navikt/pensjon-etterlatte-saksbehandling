package no.nav.etterlatte.modules.beregning.grunnlag

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BehandlingStatusService
import no.nav.etterlatte.behandling.domain.Behandling.BehandlingStoetterIkkeStatusEndringException
import no.nav.etterlatte.behandling.domain.TilstandException
import no.nav.etterlatte.behandling.vedtaksvurdering.InnvilgetPeriode
import no.nav.etterlatte.behandling.vedtaksvurdering.Vedtak
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtaksvurderingService
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.LagreBeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.OverstyrBeregningGrunnlag
import no.nav.etterlatte.beregning.grunnlag.OverstyrBeregningGrunnlagDTO
import no.nav.etterlatte.beregning.grunnlag.OverstyrBeregningGrunnlagDao
import no.nav.etterlatte.beregning.grunnlag.OverstyrBeregningGrunnlagData
import no.nav.etterlatte.beregning.grunnlag.Vedtaksperiode
import no.nav.etterlatte.beregning.grunnlag.erGrunnlagLiktFoerEnDato
import no.nav.etterlatte.beregning.grunnlag.mapVerdier
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.inTransactionIfNeeded
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.virkningstidspunkt
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning.Companion.automatiskSaksbehandler
import no.nav.etterlatte.libs.common.grunnlag.hentAvdoedesbarn
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.route.BehandlingTilgangsSjekk
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.modules.beregning.BeregningRepository
import no.nav.etterlatte.modules.beregning.sanksjon.SanksjonService
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

val REFORM_TIDSPUNKT_BP = YearMonth.of(2024, 1)

class ManglerVirkningstidspunktBP :
    UgyldigForespoerselException(
        code = "MANGLER_VIRK_BP",
        detail = "Mangler virkningstidspunkt for barnepensjon.",
    )

class ManglerForrigeGrunnlag :
    UgyldigForespoerselException(
        code = "MANGLER_FORRIGE_GRUNNLAG",
        detail = "Mangler forrige grunnlag for revurdering",
    )

class UgyldigBeregningsgrunnlag :
    UgyldigForespoerselException(
        code = "UGYLDIG_BEREGNINGSGRUNNLAG",
        detail = "Beregningsgrunnlaget er ikke gyldig",
    )

class BeregningsGrunnlagService(
    private val beregningsGrunnlagRepository: BeregningsGrunnlagRepository,
    private val beregningRepository: BeregningRepository,
    private val behandlingService: BehandlingService,
    private val vedtaksvurderingService: VedtaksvurderingService,
    private val grunnlagService: GrunnlagService,
    private val sanksjonService: SanksjonService,
    private val behandlingStatusService: BehandlingStatusService,
    private val behandlingTilgangsSjekk: BehandlingTilgangsSjekk,
) {
    private val logger = LoggerFactory.getLogger(BeregningsGrunnlagService::class.java)

    fun lagreBeregningsGrunnlag(
        behandlingId: UUID,
        beregningsGrunnlag: LagreBeregningsGrunnlag,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregningsGrunnlag? {
        if (!kanSetteStatusTrygdetidOppdatert(behandlingId, brukerTokenInfo)) {
            return null
        }
        logger.info("Lagrer beregningsgrunnlag for behandling $behandlingId")

        val behandling =
            inTransactionIfNeeded { behandlingService.hentDetaljertBehandling(behandlingId, brukerTokenInfo) }
                ?: throw InternfeilException("Klarte ikke hente behandling med behandlingId=$behandlingId")
        val grunnlag =
            grunnlagService.hentOpplysningsgrunnlag(behandlingId)
                ?: throw InternfeilException("Mangler opplysningsgrunnlag for behandlingId=$behandlingId")
        val virkningstidspunkt =
            behandling.virkningstidspunkt ?: throw InternfeilException("Virkningstidspunkt ikke satt for behandling $behandlingId")

        if (behandling.sakType == SakType.BARNEPENSJON && virkningstidspunkt.dato.isBefore(REFORM_TIDSPUNKT_BP)) {
            validerSoeskenMedIBeregning(
                behandlingId,
                beregningsGrunnlag,
                grunnlag,
            )
        } else {
            logger.info("Virkningstidspunktet er $virkningstidspunkt, trenger ikke å validere søsken i beregningsgrunnlaget")
        }

        val kanLagreDetteGrunnlaget =
            if (behandling.behandlingType == BehandlingType.REVURDERING) {
                // Her vil vi sjekke opp om det vi lagrer ned ikke er modifisert før virk på revurderingen
                val sisteIverksatteBehandling =
                    inTransactionIfNeeded { vedtaksvurderingService.hentIverksatteVedtakISak(behandling.sak) }
                        .sortedByDescending { it.vedtakFattet?.tidspunkt }
                        .first { it.type != VedtakType.OPPHOER }

                logger.info("Siste iverksatte behandling er ${sisteIverksatteBehandling.behandlingId}")

                grunnlagErIkkeEndretFoerVirk(
                    behandling,
                    sisteIverksatteBehandling.behandlingId,
                    beregningsGrunnlag,
                )
            } else {
                true
            }

        val soeskenMedIBeregning =
            if (behandling.sakType == SakType.BARNEPENSJON) {
                beregningsGrunnlag.soeskenMedIBeregning.ifEmpty {
                    when (val virk = behandling.virkningstidspunkt) {
                        null -> {
                            throw ManglerVirkningstidspunktBP()
                        }

                        else -> {
                            if (virk.dato.isBefore(REFORM_TIDSPUNKT_BP)) {
                                // Her burde man egentlig ha en sjekk på om avdøde har noen barn.
                                // Hvis avdøde har barn og vi er før reformtidspunkt må disse barnene søskenjusteres
                                listOf(
                                    GrunnlagMedPeriode(
                                        data = emptyList(),
                                        fom = virk.dato.atDay(1),
                                        tom = null,
                                    ),
                                )
                            } else {
                                emptyList()
                            }
                        }
                    }
                }
            } else {
                emptyList()
            }

        return if (kanLagreDetteGrunnlaget) {
            logger.info("Beregningsgrunnlaget er gyldig for behandling $behandlingId - lagrer")
            beregningsGrunnlagRepository.lagreBeregningsGrunnlag(
                BeregningsGrunnlag(
                    behandlingId = behandlingId,
                    kilde = Grunnlagsopplysning.Saksbehandler.create(brukerTokenInfo.ident()),
                    soeskenMedIBeregning = soeskenMedIBeregning,
                    institusjonsopphold =
                        beregningsGrunnlag.institusjonsopphold ?: emptyList(),
                    beregningsMetode = beregningsGrunnlag.beregningsMetode,
                    beregningsMetodeFlereAvdoede =
                        beregningsGrunnlag.beregningsMetodeFlereAvdoede
                            ?: emptyList(),
                    kunEnJuridiskForelder = beregningsGrunnlag.kunEnJuridiskForelder,
                ),
            )

            inTransactionIfNeeded { behandlingStatusService.settTrygdetidOppdatert(behandlingId, brukerTokenInfo, dryRun = false) }
            beregningsGrunnlagRepository.finnBeregningsGrunnlag(behandlingId)
        } else {
            logger.info("Beregningsgrunnlaget er ikke gyldig for behandling $behandlingId")
            sikkerlogger().info(
                "Beregningsgrunnlaget er ikke gyldig for behandling $behandlingId. Beregningsgrunnlag: ${beregningsGrunnlag.toJson()}",
            )
            throw UgyldigBeregningsgrunnlag()
        }
    }

    private fun validerSoeskenMedIBeregning(
        behandlingId: UUID,
        barnepensjonBeregningsGrunnlag: LagreBeregningsGrunnlag,
        grunnlag: Grunnlag,
    ) {
        val soeskensFoedselsnummere =
            barnepensjonBeregningsGrunnlag.soeskenMedIBeregning
                .flatMap { soeskenGrunnlag -> soeskenGrunnlag.data }
                .map { it.foedselsnummer.value }

        if (soeskensFoedselsnummere.isNotEmpty()) {
            val avdoede = grunnlag.hentAvdoede()

            // Gammelt regelverk i Gjenyn har ikke støtte for flere avdøde. Dette skal beregnes med andre regler enn vanlig søskenjustering
            if (avdoede.size > 1) {
                throw BPBeregningsgrunnlagStoetterIkkeFlereAvdoedeVedSoeskenJusteringException(behandlingId)
            }

            val avdoed = avdoede.first().hentAvdoedesbarn()!!
            val avdoedesBarn = avdoed.verdi.avdoedesBarn!!.associateBy({ it.foedselsnummer.value }, { it })

            val alleSoeskenFinnes = soeskensFoedselsnummere.all { fnr -> avdoedesBarn.contains(fnr) }
            if (!alleSoeskenFinnes) {
                throw BPBeregningsgrunnlagSoeskenIkkeAvdoedesBarnException(behandlingId)
            }

            val alleSoeskenIberegningenErlevende =
                soeskensFoedselsnummere.all { fnr -> avdoedesBarn[fnr]?.doedsdato === null }
            if (!alleSoeskenIberegningenErlevende) {
                throw BPBeregningsgrunnlagSoeskenMarkertDoedException(behandlingId)
            }
        }
    }

    private fun grunnlagErIkkeEndretFoerVirk(
        revurdering: DetaljertBehandling,
        forrigeIverksatteBehandlingId: UUID,
        beregningsGrunnlag: LagreBeregningsGrunnlag,
    ): Boolean {
        logger.info("Kontrollerer at beregningsgrunnlag ikke er endret før virkningstidspunkt")
        val forrigeGrunnlag =
            beregningsGrunnlagRepository.finnBeregningsGrunnlag(
                forrigeIverksatteBehandlingId,
            )

        // hvis forrigeGrunnlag er null, kan aarsaken være at tidligere behandling er manuelt overstyrt
        if (forrigeGrunnlag == null) {
            val overstyrtBeregningsGrunnlag =
                beregningsGrunnlagRepository.finnOverstyrBeregningGrunnlagForBehandling(
                    forrigeIverksatteBehandlingId,
                )
            if (overstyrtBeregningsGrunnlag.isNotEmpty()) {
                // i tilfelle hvor tidligere behandling er manuelt overstyrt  (mangler beregningsgrunnlag)
                // kan vi returnere True ettersom vi ikke har noe å sammenligne med
                return true
            } else {
                // i tilfelle hvor tidligere behandling ikke er overstyrt beregnet
                throw ManglerForrigeGrunnlag()
            }
        }

        val revurderingVirk = revurdering.virkningstidspunkt().dato.atDay(1)
        val soeskenjusteringErLiktFoerVirk =
            if (revurdering.sakType == SakType.BARNEPENSJON) {
                erGrunnlagLiktFoerEnDato(
                    beregningsGrunnlag.soeskenMedIBeregning,
                    forrigeGrunnlag.soeskenMedIBeregning,
                    revurderingVirk,
                )
            } else {
                true
            }

        val institusjonsoppholdErLiktFoerVirk =
            erGrunnlagLiktFoerEnDato(
                beregningsGrunnlag.institusjonsopphold ?: emptyList(),
                forrigeGrunnlag.institusjonsopphold,
                revurderingVirk,
            )

        return soeskenjusteringErLiktFoerVirk && institusjonsoppholdErLiktFoerVirk
    }

    fun hentBeregningsGrunnlag(behandlingId: UUID): BeregningsGrunnlag? {
        logger.info("Henter grunnlag $behandlingId")
        val grunnlag = beregningsGrunnlagRepository.finnBeregningsGrunnlag(behandlingId)
        return grunnlag
    }

    /** Henter ordinært beregningsgrunnlag. Hvis det ikke finnes så kopieres det fra forrige behandling og lagres.
     * Returnerer det lagrede grunnlaget. */
    suspend fun hentEllerKopierBeregningsGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregningsGrunnlag? {
        logger.info("Henter eller kopierer grunnlag $behandlingId")
        val grunnlag = hentBeregningsGrunnlag(behandlingId)
        if (grunnlag != null ||
            !kanOppdatereBeregningsGrunnlag(behandlingId, brukerTokenInfo)
        ) {
            return grunnlag
        }

        // Det kan hende behandlingen er en revurdering, og da må vi finne forrige grunnlag for saken
        val forrigeIverksatte = forrigeIverksatteBehandling(behandlingId, brukerTokenInfo)
        return if (forrigeIverksatte != null) {
            dupliserOrdinaertBeregningsGrunnlag(
                behandlingId,
                forrigeIverksatte.behandlingId,
                brukerTokenInfo,
            )
        } else {
            null
        }
    }

    private fun dupliserOrdinaertBeregningsGrunnlag(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregningsGrunnlag? {
        logger.info("Dupliser ordinært grunnlag for $behandlingId fra $forrigeBehandlingId")

        krev(beregningsGrunnlagRepository.finnBeregningsGrunnlag(behandlingId) == null) {
            "Eksisterende grunnlag funnet for $behandlingId"
        }
        val forrigeGrunnlag =
            beregningsGrunnlagRepository.finnBeregningsGrunnlag(forrigeBehandlingId)
                ?: return null

        val behandling =
            inTransactionIfNeeded { behandlingService.hentDetaljertBehandling(behandlingId, brukerTokenInfo) }
                ?: throw InternfeilException("Klarte ikke hente behandling med behandlingId=$behandlingId")

        val vedtaksperioder = hentVedtaksperioderForSak(behandling.sak, brukerTokenInfo)

        val nyttGrunnlag =
            forrigeGrunnlag.copy(
                behandlingId = behandlingId,
                vedtaksperioder = vedtaksperioder.takeIf { it.isNotEmpty() },
            )
        beregningsGrunnlagRepository.lagreBeregningsGrunnlag(nyttGrunnlag)

        if (behandling.sakType == SakType.OMSTILLINGSSTOENAD && behandling.behandlingType == BehandlingType.REVURDERING) {
            sanksjonService.kopierSanksjon(behandlingId, forrigeBehandlingId, brukerTokenInfo)
        }
        return nyttGrunnlag
    }

    fun dupliserBeregningsGrunnlag(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        logger.info("Dupliserer grunnlag for $behandlingId fra $forrigeBehandlingId")

        val behandling =
            inTransactionIfNeeded { behandlingService.hentDetaljertBehandling(behandlingId, brukerTokenInfo) }
                ?: throw InternfeilException("Klarte ikke hente behandling med behandlingId=$behandlingId")

        if (beregningRepository.hentOverstyrBeregning(behandling.sak) != null) {
            dupliserOverstyrBeregningGrunnlag(behandlingId, forrigeBehandlingId)
        } else {
            dupliserOrdinaertBeregningsGrunnlag(behandlingId, forrigeBehandlingId, brukerTokenInfo)
        }
    }

    private fun dupliserOverstyrBeregningGrunnlag(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
    ): List<OverstyrBeregningGrunnlagDao> {
        krev(
            beregningsGrunnlagRepository
                .finnOverstyrBeregningGrunnlagForBehandling(behandlingId)
                .isEmpty(),
        ) {
            "Eksisterende overstyrt grunnlag funnet for $behandlingId"
        }
        val nyttGrunnlag =
            beregningsGrunnlagRepository
                .finnOverstyrBeregningGrunnlagForBehandling(forrigeBehandlingId)
                .map {
                    it.copy(
                        id = UUID.randomUUID(),
                        behandlingId = behandlingId,
                    )
                }
        if (nyttGrunnlag.isNotEmpty()) {
            beregningsGrunnlagRepository.lagreOverstyrBeregningGrunnlagForBehandling(
                behandlingId,
                nyttGrunnlag,
            )
        }
        return nyttGrunnlag
    }

    fun hentOverstyrBeregningGrunnlag(behandlingId: UUID): OverstyrBeregningGrunnlag =
        beregningsGrunnlagRepository
            .finnOverstyrBeregningGrunnlagForBehandling(
                behandlingId,
            ).let { overstyrBeregningGrunnlagDaoListe ->
                toOverstyrBeregningGrunnlag(overstyrBeregningGrunnlagDaoListe)
            }

    /** Henter overstyrt beregningsgrunnlag. Hvis det ikke finnes så kopieres det fra forrige behandling og lagres.
     * Returnerer det lagrede grunnlaget. */
    suspend fun hentEllerKopierOverstyrBeregningGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): OverstyrBeregningGrunnlag {
        logger.info("Henter overstyrt beregningsgrunnlag $behandlingId")
        val overstyrtePerioder = hentOverstyrBeregningGrunnlag(behandlingId)
        val behandling =
            inTransactionIfNeeded { behandlingService.hentDetaljertBehandling(behandlingId, brukerTokenInfo) }
                ?: throw InternfeilException("Klarte ikke hente behandling med behandlingId=$behandlingId")
        val skalOverstyres = beregningRepository.hentOverstyrBeregning(behandling.sak) != null

        if (overstyrtePerioder.perioder.isNotEmpty() ||
            !kanOppdatereBeregningsGrunnlag(behandlingId, brukerTokenInfo) ||
            !skalOverstyres
        ) {
            return overstyrtePerioder
        }
        // Henter forrige beregningsgrunnlag
        val forrigeIverksatte = forrigeIverksatteBehandling(behandlingId, brukerTokenInfo) ?: return overstyrtePerioder

        val nyttGrunnlag = dupliserOverstyrBeregningGrunnlag(behandlingId, forrigeIverksatte.behandlingId)

        return toOverstyrBeregningGrunnlag(nyttGrunnlag)
    }

    private fun forrigeIverksatteBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Vedtak? {
        val behandling =
            inTransactionIfNeeded { behandlingService.hentDetaljertBehandling(behandlingId, brukerTokenInfo) }
                ?: throw InternfeilException("Klarte ikke hente behandling med behandlingId=$behandlingId")
        return if (behandling.behandlingType == BehandlingType.REVURDERING) {
            inTransactionIfNeeded { vedtaksvurderingService.hentIverksatteVedtakISak(behandling.sak) }
                .sortedByDescending { it.vedtakFattet?.tidspunkt }
                .first { it.type != VedtakType.OPPHOER } // Opphør har ikke beregningsgrunnlag
        } else {
            null
        }
    }

    fun lagreOverstyrBeregningGrunnlag(
        behandlingId: UUID,
        data: OverstyrBeregningGrunnlagDTO,
        brukerTokenInfo: BrukerTokenInfo,
    ): OverstyrBeregningGrunnlag {
        logger.info("Lagre overstyr beregning grunnlag $behandlingId")

        val behandling =
            inTransactionIfNeeded { behandlingService.hentDetaljertBehandling(behandlingId, brukerTokenInfo) }
                ?: throw InternfeilException("Klarte ikke hente behandling med behandlingId=$behandlingId")

        if (!kanSetteStatusTrygdetidOppdatert(behandlingId, brukerTokenInfo)) {
            throw OverstyrtBeregningFeilBehandlingStatusException(behandlingId, behandling.status)
        }
        beregningsGrunnlagRepository.lagreOverstyrBeregningGrunnlagForBehandling(
            behandlingId,
            data.perioder.map {
                val gyldigTrygdetid = it.data.trygdetid in 0..40
                if (!gyldigTrygdetid) {
                    throw OverstyrtBeregningUgyldigTrygdetid(behandlingId)
                }

                // TODO her burde det sikkert være mer validering av hva saksbehandler kan sende inn

                OverstyrBeregningGrunnlagDao(
                    id = UUID.randomUUID(),
                    behandlingId = behandlingId,
                    datoFOM = it.fom,
                    datoTOM = it.tom,
                    utbetaltBeloep = it.data.utbetaltBeloep,
                    foreldreloessats = it.data.foreldreloessats,
                    trygdetid = it.data.trygdetid,
                    trygdetidForIdent = it.data.trygdetidForIdent,
                    prorataBroekTeller = it.data.prorataBroekTeller,
                    prorataBroekNevner = it.data.prorataBroekNevner,
                    sakId = behandling.sak,
                    beskrivelse = it.data.beskrivelse,
                    aarsak = it.data.aarsak,
                    kilde = Grunnlagsopplysning.Saksbehandler.create(brukerTokenInfo.ident()),
                )
            },
        )
        inTransactionIfNeeded { behandlingStatusService.settTrygdetidOppdatert(behandlingId, brukerTokenInfo, dryRun = false) }
        return hentOverstyrBeregningGrunnlag(behandlingId)
    }

    fun tilpassOverstyrtBeregningsgrunnlagForRegulering(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        beregningsGrunnlagRepository.finnOverstyrBeregningGrunnlagForBehandling(behandlingId).let { grunnlag ->
            if (grunnlag.isNotEmpty()) {
                val behandling =
                    inTransactionIfNeeded { behandlingService.hentDetaljertBehandling(behandlingId, brukerTokenInfo) }
                        ?: throw InternfeilException("Klarte ikke hente behandling med behandlingId=$behandlingId")

                val reguleringsmaaned = LocalDate.of(behandling.virkningstidspunkt().dato.year, 5, 1)

                val nyePerioder = mutableListOf<OverstyrBeregningGrunnlagDao>()
                grunnlag.forEach {
                    val erFoerRegulering = it.datoTOM != null && it.datoTOM!! < reguleringsmaaned
                    val erOverRegulering =
                        it.datoFOM < reguleringsmaaned && (it.datoTOM == null || it.datoTOM!! > reguleringsmaaned)

                    if (erFoerRegulering) {
                        nyePerioder.add(it)
                    } else if (erOverRegulering) {
                        val forrigeMaaned = reguleringsmaaned.minusMonths(1)
                        val eksisterende =
                            it.copy(
                                datoTOM =
                                    LocalDate.of(
                                        reguleringsmaaned.year,
                                        forrigeMaaned.month,
                                        forrigeMaaned.lengthOfMonth(),
                                    ),
                            )
                        val nyPeriode =
                            tilpassOverstyrtBeregningsgrunnlagForRegulering(
                                YearMonth.from(reguleringsmaaned),
                                fom = reguleringsmaaned,
                                it,
                                behandlingId,
                            )
                        nyePerioder.add(eksisterende)
                        nyePerioder.add(nyPeriode)
                    } else {
                        nyePerioder.add(
                            tilpassOverstyrtBeregningsgrunnlagForRegulering(
                                YearMonth.from(reguleringsmaaned),
                                fom = it.datoFOM,
                                it,
                                behandlingId,
                            ),
                        )
                    }
                }

                beregningsGrunnlagRepository.lagreOverstyrBeregningGrunnlagForBehandling(
                    behandlingId,
                    nyePerioder,
                )
            }
        }
    }

    fun sjekkOmOverstyrtGrunnlagErLiktFoerVirk(
        behandlingId: UUID,
        virkningstidspunkt: YearMonth,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val forrigeIverksatteBehandling = forrigeIverksatteBehandling(behandlingId, brukerTokenInfo)
        if (forrigeIverksatteBehandling != null) {
            // har vi overstyrt beregning for denne behandlingen?
            val forrigeOverstyrBeregningGrunnlagPerioder =
                hentOverstyrBeregningGrunnlag(forrigeIverksatteBehandling.behandlingId)
                    .perioder
                    .mapVerdier(OverstyrBeregningGrunnlagData::tilSammenligningsperiode)
            if (forrigeOverstyrBeregningGrunnlagPerioder.isEmpty()) {
                return
            }
            val naavaerendeGrunnlagPerioder =
                hentOverstyrBeregningGrunnlag(behandlingId)
                    .perioder
                    .mapVerdier(OverstyrBeregningGrunnlagData::tilSammenligningsperiode)
            if (!erGrunnlagLiktFoerEnDato(
                    naavaerendeGrunnlagPerioder.sortedBy { it.fom },
                    forrigeOverstyrBeregningGrunnlagPerioder.sortedBy { it.fom },
                    virkningstidspunkt.atDay(1),
                )
            ) {
                throw OverstyrtBeregningsgrunnlagEndresFoerVirkException(
                    behandlingId = behandlingId,
                    forrigeBehandlingId = forrigeIverksatteBehandling.behandlingId,
                )
            }
        }
    }

    private fun toOverstyrBeregningGrunnlag(
        overstyrBeregningGrunnlagDaoListe: List<OverstyrBeregningGrunnlagDao>,
    ): OverstyrBeregningGrunnlag =
        OverstyrBeregningGrunnlag(
            perioder =
                overstyrBeregningGrunnlagDaoListe.map(OverstyrBeregningGrunnlagDao::tilGrunnlagMedPeriode),
            kilde = overstyrBeregningGrunnlagDaoListe.firstOrNull()?.kilde ?: automatiskSaksbehandler,
        )

    private fun hentVedtaksperioderForSak(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<Vedtaksperiode> {
        logger.info("Henter vedtaksperioder for sak $sakId")
        val innvilgedePerioder = inTransactionIfNeeded { vedtaksvurderingService.hentInnvilgedePerioder(sakId) }
        return innvilgedePerioder.map { it.tilVedtaksperiode() }
    }

    private fun InnvilgetPeriode.tilVedtaksperiode(): Vedtaksperiode =
        Vedtaksperiode(
            fraOgMed = this.periode.fom,
            tilOgMed = this.periode.tom,
        )

    private fun kanSetteStatusTrygdetidOppdatert(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean {
        try {
            inTransactionIfNeeded { behandlingStatusService.settTrygdetidOppdatert(behandlingId, brukerTokenInfo, dryRun = true) }
        } catch (e: Exception) {
            when (e) {
                is TilstandException.UgyldigTilstand,
                is TilstandException.IkkeFyltUt,
                is TilstandException.KanIkkeRedigere,
                is BehandlingStoetterIkkeStatusEndringException,
                -> {
                    logger.info("Kan ikke sette status trygdetid oppdatert for behandlingId=$behandlingId", e)
                    return false
                }

                else -> {
                    throw e
                }
            }
        }
        return true
    }

    private suspend fun kanOppdatereBeregningsGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean {
        if (!kanSetteStatusTrygdetidOppdatert(behandlingId, brukerTokenInfo)) {
            return false
        }
        return when (brukerTokenInfo) {
            is Saksbehandler -> {
                behandlingTilgangsSjekk.harTilgangTilBehandling(
                    behandlingId = behandlingId,
                    skrivetilgang = true,
                    bruker = brukerTokenInfo,
                )
            }

            else -> {
                true
            }
        }
    }
}

class OverstyrtBeregningFeilBehandlingStatusException(
    behandlingId: UUID,
    behandlingStatus: BehandlingStatus,
) : UgyldigForespoerselException(
        code = "OVERSTYRT_BEREGNING_FEIL_BEHANDLINGSSTATUS",
        detail = "Kunne ikke lagre overstyrt beregningsgrunnlag fordi behandlingen er i feil status",
        meta = mapOf("behandlingId" to behandlingId, "behandlingStatus" to behandlingStatus),
    )

class OverstyrtBeregningUgyldigTrygdetid(
    behandlingId: UUID,
) : UgyldigForespoerselException(
        code = "OVERSTYRT_BEREGNING_UGYLDIG_TRYGDETID",
        detail = "Anvendt trygdetid må være mellom 0 og 40 år",
        meta = mapOf("behandlingId" to behandlingId),
    )

class BPBeregningsgrunnlagSoeskenIkkeAvdoedesBarnException(
    behandlingId: UUID,
) : UgyldigForespoerselException(
        code = "BP_BEREGNING_SOESKEN_IKKE_AVDOEDES_BARN",
        detail = "Barnepensjon beregningsgrunnlag har søsken fnr som ikke er avdødes barn",
        meta = mapOf("behandlingId" to behandlingId),
    )

class BPBeregningsgrunnlagStoetterIkkeFlereAvdoedeVedSoeskenJusteringException(
    behandlingId: UUID,
) : UgyldigForespoerselException(
        code = "BP_BEREGNING_STOETTER_IKKE_FLERE_AVDOEDE",
        detail =
            "Behandlingsgrunnlaget inneholder flere avdøde, dette er ikke støttet ved søskenjustering " +
                "og gammelt regelverk. Vurder manuell overstyring av beregning..",
        meta = mapOf("behandlingId" to behandlingId),
    )

class BPBeregningsgrunnlagSoeskenMarkertDoedException(
    behandlingId: UUID,
) : UgyldigForespoerselException(
        code = "BP_BEREGNING_SOESKEN_MARKERT_DOED",
        detail = "Barnpensjon beregningsgrunnlag bruker søsken som er døde i beregningen",
        meta = mapOf("behandlingId" to behandlingId),
    )

class OverstyrtBeregningsgrunnlagEndresFoerVirkException(
    behandlingId: UUID,
    forrigeBehandlingId: UUID,
) : UgyldigForespoerselException(
        code = "OVERSTYRT_GRUNNLAG_ENDRET_FOER_VIRK",
        detail =
            "De overstyrte beregningsperiodene er forskjellige fra forrige " +
                "vedtak (id=$forrigeBehandlingId) før virkningstidpunktet i denne behandlingen (id=$behandlingId). " +
                "Endringer skal kun skje etter virkningstidspunktet.",
    )
