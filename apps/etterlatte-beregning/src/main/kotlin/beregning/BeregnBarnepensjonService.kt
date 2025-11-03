package no.nav.etterlatte.beregning

import no.nav.etterlatte.beregning.BarnepensjonAnvendtTrygdetidPerioder.finnForAvdoed
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagService
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.PeriodisertBeregningGrunnlag
import no.nav.etterlatte.beregning.grunnlag.mapVerdier
import no.nav.etterlatte.beregning.regler.AnvendtTrygdetid
import no.nav.etterlatte.beregning.regler.barnepensjon.PeriodisertBarnepensjonGrunnlag
import no.nav.etterlatte.beregning.regler.barnepensjon.kroneavrundetBarnepensjonRegelMedInstitusjon
import no.nav.etterlatte.beregning.regler.barnepensjon.sats.avdodeForeldre2024
import no.nav.etterlatte.beregning.regler.barnepensjon.sats.grunnbeloep
import no.nav.etterlatte.beregning.regler.barnepensjon.sats.skalHaForeldreloesSats2024
import no.nav.etterlatte.beregning.regler.barnepensjon.trygdetidsfaktor.trygdetidBruktRegel
import no.nav.etterlatte.beregning.regler.finnAnvendtGrunnbeloep
import no.nav.etterlatte.beregning.regler.finnAnvendtRegelverkBarnepensjon
import no.nav.etterlatte.beregning.regler.finnAnvendtTrygdetid
import no.nav.etterlatte.beregning.regler.finnAvdodeForeldre
import no.nav.etterlatte.beregning.regler.finnHarForeldreloessats
import no.nav.etterlatte.grunnbeloep.GrunnbeloepRepository
import no.nav.etterlatte.klienter.GrunnlagKlient
import no.nav.etterlatte.klienter.TrygdetidKlient
import no.nav.etterlatte.klienter.VilkaarsvurderingKlient
import no.nav.etterlatte.libs.common.behandling.AnnenForelder.AnnenForelderVurdering.KUN_EN_REGISTRERT_JURIDISK_FORELDER
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.virkningstidspunkt
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
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
    private val anvendtTrygdetidRepository: AnvendtTrygdetidRepository,
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
        if (beregningsGrunnlag.behandlingId != behandling.id) {
            throw BeregningsgrunnlagMangler(behandling.id)
        }

        validerKunEnJuridiskForelder(behandling, beregningsGrunnlag, grunnlag)

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

        if (trygdetidListe.isEmpty()) {
            throw TrygdetidIkkeOpprettet()
        }

        val anvendtTrygdetider =
            BarnepensjonAnvendtTrygdetidPerioder
                .finnAnvendtTrygdetidPerioder(trygdetidListe, beregningsGrunnlag)
                .also { anvendtTrygdetidRepository.lagreAnvendtTrygdetid(behandling.id, it) }

        val barnepensjonGrunnlag =
            opprettBeregningsgrunnlag(
                beregningsGrunnlag,
                trygdetidListe,
                anvendtTrygdetider.anvendt,
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

    data class PeriodisertVilkaarsvurdering(
        val periode: Periode,
        val vilkaarsvurderingResultat: VilkaarsvurderingResultat
    )

    private fun beregnBarnepensjon(
        behandlingId: UUID,
        grunnlag: Grunnlag,
        beregningsgrunnlag: PeriodisertBarnepensjonGrunnlag,
        trygdetider: List<TrygdetidDto>,
        virkningstidspunkt: YearMonth,
        kunGammeltRegelverk: Boolean = false,
        tilDato: LocalDate? = null,
        vilkaarsperioder: List<PeriodisertVilkaarsvurdering>
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

        val perioderViSkalBeregneOver: List<RegelPeriode> = emptyList()

        perioderViSkalBeregneOver.map {

        }


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

                            val regelverk =
                                periodisertResultat.resultat.finnAnvendtRegelverkBarnepensjon()
                                    ?: throw InternfeilException("Kunne ikke finne anvendt regelverk for barnepensjon")

                            val anvendtTrygdetid =
                                periodisertResultat.resultat.finnAnvendtTrygdetid(trygdetidBruktRegel)
                                    ?: throw AnvendtTrygdetidIkkeFunnet(
                                        periodisertResultat.periode.fraDato,
                                        periodisertResultat.periode.tilDato,
                                    )

                            val anvendtTrygdetidId = anvendtTrygdetid.ident

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
                                avdoedeForeldre = periodisertResultat.resultat.finnAvdodeForeldre(avdodeForeldre2024),
                                kunEnJuridiskForelder =
                                    beregningsgrunnlag.kunEnJuridiskForelder
                                        .finnGrunnlagForPeriode(
                                            periodisertResultat.periode.fraDato,
                                        ).verdi,
                                regelResultat = objectMapper.valueToTree(periodisertResultat),
                                regelVersjon = periodisertResultat.reglerVersjon,
                                regelverk = regelverk,
                                harForeldreloessats =
                                    periodisertResultat.resultat.finnHarForeldreloessats(
                                        skalHaForeldreloesSats2024,
                                    ),
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
        virkFom: LocalDate,
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
                    virkFom,
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
                            BarnepensjonAnvendtTrygdetidPerioder
                                .finnKonstantTrygdetidPerioder(
                                    trygdetider,
                                    beregningsGrunnlag,
                                    virkFom,
                                ).also { anvendtTrygdetidRepository.lagreAnvendtTrygdetid(beregningsGrunnlag.behandlingId, it) }
                                .anvendt
                                .first()
                                .data,
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
                    virkFom,
                    tom,
                )
            },
        institusjonsopphold =
            PeriodisertBeregningGrunnlag.lagPotensieltTomtGrunnlagMedDefaultUtenforPerioder(
                beregningsGrunnlag.institusjonsopphold.mapVerdier
                    { institusjonsopphold ->
                        FaktumNode(
                            verdi = institusjonsopphold,
                            kilde = beregningsGrunnlag.kilde,
                            beskrivelse = "Institusjonsopphold",
                        )
                    },
            ) { _, _, _ -> FaktumNode(null, beregningsGrunnlag.kilde, "Institusjonsopphold") },
        kunEnJuridiskForelder =
            PeriodisertBeregningGrunnlag.lagPotensieltTomtGrunnlagMedDefaultUtenforPerioder(
                beregningsGrunnlag.kunEnJuridiskForelder?.let { kunEnJuridiskForelder ->
                    listOf(kunEnJuridiskForelder).mapVerdier { _ ->
                        FaktumNode(true, beregningsGrunnlag.kilde, "")
                    }
                } ?: emptyList(),
            ) { _, _, _ -> FaktumNode(false, beregningsGrunnlag.kilde, "Kun en registrert juridisk forelder") },
    )

    private fun validerKunEnJuridiskForelder(
        behandling: DetaljertBehandling,
        beregningsGrunnlag: BeregningsGrunnlag,
        grunnlag: Grunnlag,
    ) {
        val harBeregningsgrunnlagMedKunEnJuridisk = beregningsGrunnlag.kunEnJuridiskForelder != null
        val persongalleriHarKunEnJuridiskForelder =
            grunnlag.hentAnnenForelder()?.vurdering == KUN_EN_REGISTRERT_JURIDISK_FORELDER

        if (harBeregningsgrunnlagMedKunEnJuridisk && !persongalleriHarKunEnJuridiskForelder) {
            throw BPBeregningsgrunnlagKunEnJuridiskForelderFinnesIkkeIPersongalleri(behandlingId = behandling.id)
        }
        if (persongalleriHarKunEnJuridiskForelder && !harBeregningsgrunnlagMedKunEnJuridisk) {
            throw BPKunEnJuridiskForelderManglerIBeregningsgrunnlag(behandlingId = behandling.id)
        }
        val startdatoKunEnJuridisk = beregningsGrunnlag.kunEnJuridiskForelder?.fom
        val virkningstidspunkt =
            krevIkkeNull(behandling.virkningstidspunkt) {
                "Behandling mangler virkningstidspunkt, kan ikke beregne (sakId=${behandling.sak}, behandlingId=${behandling.id})"
            }.dato
        if (startdatoKunEnJuridisk != null && startdatoKunEnJuridisk.isAfter(virkningstidspunkt.atDay(1))) {
            throw BPKunEnJuridiskForelderMaaGjeldeFraVirkningstidspunkt(behandlingId = behandling.id)
        }
    }
}

class BPBeregningsgrunnlagKunEnJuridiskForelderFinnesIkkeIPersongalleri(
    behandlingId: UUID,
) : UgyldigForespoerselException(
        code = "BP_BEREGNING_KUN_EN_JURIDISK_FORELDER_MANGLER_I_PERSONGALLERI",
        detail =
            "Kun én juridisk forelder er registrert i beregningsgrunnlaget, men ikke i familieforhold. " +
                "Du må lagre trygdetid i beregningen for å få med endringen.",
        meta = mapOf("behandlingId" to behandlingId),
    )

class BPKunEnJuridiskForelderManglerIBeregningsgrunnlag(
    behandlingId: UUID,
) : UgyldigForespoerselException(
        code = "BP_BEREGNING_KUN_EN_JURIDISK_FORELDER_MANGLER_I_BEREGNINGSGRUNNLAG",
        detail =
            "Kun én juridisk forelder er registrert i familieforhold, " +
                "men beregningsgrunnlaget er ikke oppdatert. Du må lagre trygdetid i beregningen.",
        meta = mapOf("behandlingId" to behandlingId),
    )

class BPKunEnJuridiskForelderMaaGjeldeFraVirkningstidspunkt(
    behandlingId: UUID,
) : UgyldigForespoerselException(
        code = "BP_BEREGNING_KUN_EN_JURIDISK_FORELDER_MAA_GJELDE_FRA_VIRKNINGSTIDSPUNKT",
        detail = "Dato fom på kun en juridisk forelder i beregningsgrunnlaget må være lik eller etter virkningstidspunkt",
        meta = mapOf("behandlingId" to behandlingId),
    )
