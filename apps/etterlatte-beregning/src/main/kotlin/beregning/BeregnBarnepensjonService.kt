package no.nav.etterlatte.beregning

import no.nav.etterlatte.beregning.AnvendtTrygdetidPerioder.finnForAvdoed
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagService
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.PeriodisertBeregningGrunnlag
import no.nav.etterlatte.beregning.grunnlag.kombinerOverlappendePerioder
import no.nav.etterlatte.beregning.grunnlag.mapVerdier
import no.nav.etterlatte.beregning.regler.AnvendtTrygdetid
import no.nav.etterlatte.beregning.regler.barnepensjon.PeriodisertBarnepensjonGrunnlag
import no.nav.etterlatte.beregning.regler.barnepensjon.kroneavrundetBarnepensjonRegelMedInstitusjon
import no.nav.etterlatte.beregning.regler.barnepensjon.sats.avdodeForeldre2024
import no.nav.etterlatte.beregning.regler.barnepensjon.sats.grunnbeloep
import no.nav.etterlatte.beregning.regler.barnepensjon.trygdetidsfaktor.TrygdetidGrunnlag
import no.nav.etterlatte.beregning.regler.barnepensjon.trygdetidsfaktor.anvendtTrygdetidRegel
import no.nav.etterlatte.beregning.regler.barnepensjon.trygdetidsfaktor.trygdetidBruktRegel
import no.nav.etterlatte.beregning.regler.finnAnvendtGrunnbeloep
import no.nav.etterlatte.beregning.regler.finnAnvendtTrygdetid
import no.nav.etterlatte.beregning.regler.finnAvdodeForeldre
import no.nav.etterlatte.beregning.regler.toSamlet
import no.nav.etterlatte.config.BeregningFeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnbeloep.GrunnbeloepRepository
import no.nav.etterlatte.klienter.GrunnlagKlient
import no.nav.etterlatte.klienter.TrygdetidKlient
import no.nav.etterlatte.klienter.VilkaarsvurderingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.virkningstidspunkt
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.beregning.SamletTrygdetidMedBeregningsMetode
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.KonstantGrunnlag
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.RegelkjoeringResultat
import no.nav.etterlatte.libs.regler.eksekver
import no.nav.etterlatte.libs.regler.finnAnvendteRegler
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID

class BeregnBarnepensjonService(
    private val grunnlagKlient: GrunnlagKlient,
    private val vilkaarsvurderingKlient: VilkaarsvurderingKlient,
    private val grunnbeloepRepository: GrunnbeloepRepository = GrunnbeloepRepository,
    private val beregningsGrunnlagService: BeregningsGrunnlagService,
    private val trygdetidKlient: TrygdetidKlient,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(BeregnBarnepensjonService::class.java)

    suspend fun beregn(
        behandling: DetaljertBehandling,
        brukerTokenInfo: BrukerTokenInfo,
        tilDato: LocalDate? = null,
    ): Beregning {
        val grunnlag = grunnlagKlient.hentGrunnlag(behandling.id, brukerTokenInfo)
        val behandlingType = behandling.behandlingType
        val virkningstidspunkt = behandling.virkningstidspunkt().dato

        val beregningsGrunnlag =
            beregningsGrunnlagService.hentBeregningsGrunnlag(behandling.id, brukerTokenInfo)
                ?: throw BeregningsgrunnlagMangler(behandling.id)

        val foreldreloesFlag = featureToggleService.isEnabled(BeregningFeatureToggle.Foreldreloes, false)

        val trygdetidListe =
            try {
                trygdetidKlient.hentTrygdetid(behandling.id, brukerTokenInfo)
            } catch (e: Exception) {
                logger.warn(
                    "Kunne ikke hente ut trygdetid for behandlingen med id=${behandling.id}. " +
                        "Dette er ikke kritisk siden vi ikke har krav om trygdetid enda.",
                )
                emptyList()
            }

        if (trygdetidListe.size > 1 && !foreldreloesFlag) {
            throw ForeldreloesTrygdetid(behandling.id)
        }

        if (trygdetidListe.isEmpty()) {
            throw TrygdetidIkkeOpprettet()
        }

        val anvendtTrygdetider =
            AnvendtTrygdetidPerioder.finnAnvendtTrygdetidPerioder(trygdetidListe, beregningsGrunnlag)

        val barnepensjonGrunnlag =
            opprettBeregningsgrunnlag(
                beregningsGrunnlag,
                trygdetidListe,
                anvendtTrygdetider,
                virkningstidspunkt.atDay(1),
                null,
            )

        logger.info("Beregner barnepensjon for behandlingId=${behandling.id} med behandlingType=$behandlingType")

        return when (behandlingType) {
            BehandlingType.FØRSTEGANGSBEHANDLING ->
                beregnBarnepensjon(
                    behandling.id,
                    grunnlag,
                    barnepensjonGrunnlag,
                    trygdetidListe,
                    virkningstidspunkt,
                    tilDato = tilDato,
                )

            BehandlingType.REVURDERING -> {
                val vilkaarsvurderingUtfall =
                    vilkaarsvurderingKlient.hentVilkaarsvurdering(behandling.id, brukerTokenInfo).resultat?.utfall
                        ?: throw RuntimeException("Forventa å ha resultat for behandling ${behandling.id}")

                when (vilkaarsvurderingUtfall) {
                    VilkaarsvurderingUtfall.OPPFYLT ->
                        beregnBarnepensjon(
                            behandling.id,
                            grunnlag,
                            barnepensjonGrunnlag,
                            trygdetidListe,
                            virkningstidspunkt,
                            tilDato = tilDato,
                        )

                    VilkaarsvurderingUtfall.IKKE_OPPFYLT -> opphoer(behandling.id, grunnlag, virkningstidspunkt)
                }
            }
        }
    }

    private fun beregnBarnepensjon(
        behandlingId: UUID,
        grunnlag: Grunnlag,
        beregningsgrunnlag: PeriodisertBarnepensjonGrunnlag,
        trygdetider: List<TrygdetidDto>,
        virkningstidspunkt: YearMonth,
        kunGammeltRegelverk: Boolean = false,
        tilDato: LocalDate? = null,
    ): Beregning {
        val beregningTom =
            when (kunGammeltRegelverk) {
                true -> YearMonth.of(2023, Month.DECEMBER)
                false -> tilDato?.let { YearMonth.from(it) }
            }

        val resultat =
            kroneavrundetBarnepensjonRegelMedInstitusjon.eksekver(
                grunnlag = beregningsgrunnlag,
                periode = RegelPeriode(virkningstidspunkt.atDay(1), beregningTom?.atEndOfMonth()),
            )

        return when (resultat) {
            is RegelkjoeringResultat.Suksess -> {
                val antallPerioder = resultat.periodiserteResultater.size
                beregning(
                    behandlingId = behandlingId,
                    grunnlagMetadata = grunnlag.metadata,
                    beregningsperioder =
                        resultat.periodiserteResultater.mapIndexed { index, periodisertResultat ->
                            logger.info(
                                "Beregnet barnepensjon for periode fra={} til={} og beløp={} med regler={}",
                                periodisertResultat.periode.fraDato,
                                periodisertResultat.periode.tilDato,
                                periodisertResultat.resultat.verdi,
                                periodisertResultat.resultat
                                    .finnAnvendteRegler()
                                    .map { "${it.regelReferanse.id} (${it.beskrivelse})" }
                                    .toSet(),
                            )

                            val grunnbeloep =
                                periodisertResultat.resultat.finnAnvendtGrunnbeloep(grunnbeloep)
                                    ?: throw AnvendtGrunnbeloepIkkeFunnet()

                            val anvendtTrygdetid =
                                periodisertResultat.resultat.finnAnvendtTrygdetid(trygdetidBruktRegel)
                                    ?: throw AnvendtTrygdetidIkkeFunnet(
                                        periodisertResultat.periode.fraDato,
                                        periodisertResultat.periode.tilDato,
                                    )

                            val anvendtTrygdetidId = anvendtTrygdetid.ident ?: throw AnvendtTrygdetidIdentIkkeFunnet()

                            val tom = periodisertResultat.periode.tilDato?.let { YearMonth.from(it) }
                            val overstyrtTom =
                                if (index == antallPerioder - 1) {
                                    null
                                } else {
                                    tom
                                }

                            val trygdetidForAvdoed =
                                trygdetider.finnForAvdoed(anvendtTrygdetidId).beregnetTrygdetid?.resultat

                            Beregningsperiode(
                                datoFOM = YearMonth.from(periodisertResultat.periode.fraDato),
                                datoTOM =
                                    when (kunGammeltRegelverk) {
                                        true -> overstyrtTom
                                        false -> tom
                                    },
                                utbetaltBeloep = periodisertResultat.resultat.verdi,
                                soeskenFlokk =
                                    beregningsgrunnlag.soeskenKull
                                        .finnGrunnlagForPeriode(
                                            periodisertResultat.periode.fraDato,
                                        ).verdi
                                        .map {
                                            it.value
                                        },
                                institusjonsopphold =
                                    beregningsgrunnlag.institusjonsopphold
                                        .finnGrunnlagForPeriode(
                                            periodisertResultat.periode.fraDato,
                                        ).verdi,
                                grunnbelopMnd = grunnbeloep.grunnbeloepPerMaaned,
                                grunnbelop = grunnbeloep.grunnbeloep,
                                trygdetid = anvendtTrygdetid.trygdetid.toInteger(),
                                trygdetidForIdent = anvendtTrygdetidId,
                                beregningsMetode = anvendtTrygdetid.beregningsMetode,
                                samletNorskTrygdetid = trygdetidForAvdoed?.samletTrygdetidNorge,
                                samletTeoretiskTrygdetid = trygdetidForAvdoed?.samletTrygdetidTeoretisk,
                                broek = trygdetidForAvdoed?.prorataBroek,
                                avdodeForeldre = periodisertResultat.resultat.finnAvdodeForeldre(avdodeForeldre2024),
                                regelResultat = objectMapper.valueToTree(periodisertResultat),
                                regelVersjon = periodisertResultat.reglerVersjon,
                            )
                        },
                )
            }

            is RegelkjoeringResultat.UgyldigPeriode -> throw RuntimeException(
                "Ugyldig regler for periode: ${resultat.ugyldigeReglerForPeriode}",
            )
        }
    }

    private fun opphoer(
        behandlingId: UUID,
        grunnlag: Grunnlag,
        virkningstidspunkt: YearMonth,
    ): Beregning {
        val grunnbeloep = grunnbeloepRepository.hentGjeldendeGrunnbeloep(virkningstidspunkt)

        return beregning(
            behandlingId = behandlingId,
            grunnlagMetadata = grunnlag.metadata,
            beregningsperioder =
                listOf(
                    Beregningsperiode(
                        datoFOM = virkningstidspunkt,
                        datoTOM = null,
                        utbetaltBeloep = 0,
                        soeskenFlokk = null,
                        grunnbelopMnd = grunnbeloep.grunnbeloepPerMaaned,
                        grunnbelop = grunnbeloep.grunnbeloep,
                        trygdetid = 0,
                    ),
                ),
        )
    }

    private fun beregning(
        behandlingId: UUID,
        grunnlagMetadata: Metadata,
        beregningsperioder: List<Beregningsperiode>,
    ) = Beregning(
        beregningId = UUID.randomUUID(),
        behandlingId = behandlingId,
        type = Beregningstype.BP,
        beregningsperioder = beregningsperioder,
        beregnetDato = Tidspunkt.now(),
        grunnlagMetadata = grunnlagMetadata,
        overstyrBeregning = null,
    )

    private fun opprettBeregningsgrunnlag(
        beregningsGrunnlag: BeregningsGrunnlag,
        trygdetider: List<TrygdetidDto>,
        anvendtTrygdetider: List<GrunnlagMedPeriode<List<AnvendtTrygdetid>>>,
        fom: LocalDate,
        tom: LocalDate?,
    ) = PeriodisertBarnepensjonGrunnlag(
        soeskenKull =
            if (beregningsGrunnlag.soeskenMedIBeregning.isNotEmpty()) {
                PeriodisertBeregningGrunnlag.lagKomplettPeriodisertGrunnlag(
                    beregningsGrunnlag.soeskenMedIBeregning.mapVerdier { soeskenMedIBeregning ->
                        FaktumNode(
                            verdi = soeskenMedIBeregning.filter { it.skalBrukes }.map { it.foedselsnummer },
                            kilde = beregningsGrunnlag.kilde,
                            beskrivelse = "Søsken i kullet",
                        )
                    },
                    fom,
                    tom,
                )
            } else {
                KonstantGrunnlag(FaktumNode(emptyList(), beregningsGrunnlag.kilde, "Ingen søsken i kullet"))
            },
        avdoedesTrygdetid =
            if (anvendtTrygdetider.isEmpty()) {
                KonstantGrunnlag(
                    FaktumNode(
                        verdi =
                            AnvendtTrygdetidPerioder.finnKonstantTrygdetidPerioder(
                                trygdetider,
                                beregningsGrunnlag,
                                fom,
                            ),
                        kilde = beregningsGrunnlag.kilde,
                        beskrivelse = "Konstant anvendt trygdetider",
                    ),
                )
            } else {
                PeriodisertBeregningGrunnlag.lagKomplettPeriodisertGrunnlag(
                    anvendtTrygdetider.mapVerdier {
                        FaktumNode(
                            verdi = it,
                            kilde = beregningsGrunnlag.kilde,
                            beskrivelse = "Anvendte trygdetider",
                        )
                    },
                    fom,
                    tom,
                )
            },
        institusjonsopphold =
            PeriodisertBeregningGrunnlag.lagPotensieltTomtGrunnlagMedDefaultUtenforPerioder(
                beregningsGrunnlag.institusjonsoppholdBeregningsgrunnlag.mapVerdier
                    { institusjonsopphold ->
                        FaktumNode(
                            verdi = institusjonsopphold,
                            kilde = beregningsGrunnlag.kilde,
                            beskrivelse = "Institusjonsopphold",
                        )
                    },
            ) { _, _, _ -> FaktumNode(null, beregningsGrunnlag.kilde, "Institusjonsopphold") },
    )
}

object AnvendtTrygdetidPerioder {
    private val logger = LoggerFactory.getLogger(AnvendtTrygdetidPerioder::class.java)

    fun finnAnvendtTrygdetidPerioder(
        trygdetider: List<TrygdetidDto>,
        beregningsGrunnlag: BeregningsGrunnlag,
    ) = anvendtPerioder(beregningsGrunnlag.finnMuligeTrygdetidPerioder(trygdetider))

    fun finnKonstantTrygdetidPerioder(
        trygdetider: List<TrygdetidDto>,
        beregningsGrunnlag: BeregningsGrunnlag,
        fom: LocalDate,
    ): List<AnvendtTrygdetid> {
        if (trygdetider.size != 1) {
            throw UgyldigForespoerselException(
                code = "FEIL_ANTALL_TRYGDETIDER",
                detail = "Fant flere trygdetider - men fikk bare en beregningsgrunnlag",
            )
        }

        return trygdetider.first().toSamlet(beregningsGrunnlag.beregningsMetode.beregningsMetode)?.let {
            anvendtPerioder(
                listOf(
                    GrunnlagMedPeriode(it, fom, null),
                ),
            ).first().data
        } ?: throw TrygdetidIkkeOpprettet()
    }

    private fun anvendtPerioder(muligePerioder: List<GrunnlagMedPeriode<SamletTrygdetidMedBeregningsMetode>>) =
        muligePerioder
            .map {
                anvendtTrygdetidRegel.eksekver(
                    KonstantGrunnlag(
                        TrygdetidGrunnlag(
                            FaktumNode(it.data, kilde = "Trygdetid", beskrivelse = "Beregnet trygdetid for avdød"),
                        ),
                    ),
                    RegelPeriode(it.fom, it.tom),
                )
            }.map {
                when (it) {
                    is RegelkjoeringResultat.Suksess -> {
                        val aktueltResultat = it.periodiserteResultater.single()
                        GrunnlagMedPeriode(
                            data = aktueltResultat.resultat.verdi,
                            fom = aktueltResultat.periode.fraDato,
                            tom = aktueltResultat.periode.tilDato,
                        )
                    }

                    is RegelkjoeringResultat.UgyldigPeriode -> throw InternfeilException(
                        "Ugyldig regler for periode: ${it.ugyldigeReglerForPeriode}",
                    )
                }
            }.kombinerOverlappendePerioder()

    private fun BeregningsGrunnlag.finnMuligeTrygdetidPerioder(trygdetider: List<TrygdetidDto>) =
        begegningsmetodeFlereAvdoede.map { beregningsmetodeForAvdoedPeriode ->
            GrunnlagMedPeriode(
                data =
                    trygdetider
                        .finnForAvdoed(
                            beregningsmetodeForAvdoedPeriode.data.avdoed,
                        ).toSamlet(
                            beregningsmetodeForAvdoedPeriode.data.beregningsMetode.beregningsMetode,
                        ) ?: throw InternfeilException("Kunne ikke samle trygdetid for avdoed").also {
                        logger.warn("Kunne ikke samle trygdetid for avdoed - se sikkerlogg")
                        sikkerlogger().warn("Kunne ikke samle trygdetid for avdoed ${beregningsmetodeForAvdoedPeriode.data.avdoed}")
                    },
                fom = beregningsmetodeForAvdoedPeriode.fom,
                tom = beregningsmetodeForAvdoedPeriode.tom,
            )
        }

    fun List<TrygdetidDto>.finnForAvdoed(avdoed: String) =
        this.find { it.ident == avdoed }
            ?: throw InternfeilException("Manglende trygdetid for avdoed").also {
                logger.warn("Fant ikke trygdetid for avdoed - se sikkerlogg")
                sikkerlogger().warn("Fant ikke trygdetid for avdoed $avdoed i $this")
            }
}
