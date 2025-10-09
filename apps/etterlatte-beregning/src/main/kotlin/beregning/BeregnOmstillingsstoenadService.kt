package no.nav.etterlatte.beregning

import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagService
import no.nav.etterlatte.beregning.grunnlag.PeriodisertBeregningGrunnlag
import no.nav.etterlatte.beregning.grunnlag.mapVerdier
import no.nav.etterlatte.beregning.regler.finnAnvendtGrunnbeloep
import no.nav.etterlatte.beregning.regler.finnAnvendtTrygdetid
import no.nav.etterlatte.beregning.regler.omstillingstoenad.Avdoed
import no.nav.etterlatte.beregning.regler.omstillingstoenad.PeriodisertOmstillingstoenadGrunnlag
import no.nav.etterlatte.beregning.regler.omstillingstoenad.kroneavrundetOmstillingstoenadRegelMedInstitusjon
import no.nav.etterlatte.beregning.regler.omstillingstoenad.sats.grunnbeloep
import no.nav.etterlatte.beregning.regler.omstillingstoenad.trygdetidsfaktor.trygdetidBruktRegel
import no.nav.etterlatte.beregning.regler.toSamlet
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnbeloep.GrunnbeloepRepository
import no.nav.etterlatte.klienter.GrunnlagKlient
import no.nav.etterlatte.klienter.TrygdetidKlient
import no.nav.etterlatte.klienter.VilkaarsvurderingKlient
import no.nav.etterlatte.libs.common.Regelverk
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.virkningstidspunkt
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
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
import java.time.YearMonth
import java.util.UUID
import java.util.UUID.randomUUID

enum class BeregningToggles(
    val value: String,
) : FeatureToggle {
    BEREGNING_BRUK_NYE_BEREGNINGSREGLER("beregning_bruk_nye_beregningsregler"),
    ;

    override fun key(): String = this.value
}

class BeregnOmstillingsstoenadService(
    private val grunnlagKlient: GrunnlagKlient,
    private val vilkaarsvurderingKlient: VilkaarsvurderingKlient,
    private val trygdetidKlient: TrygdetidKlient,
    private val beregningsGrunnlagService: BeregningsGrunnlagService,
    private val grunnbeloepRepository: GrunnbeloepRepository = GrunnbeloepRepository,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(BeregnOmstillingsstoenadService::class.java)

    suspend fun beregn(
        behandling: DetaljertBehandling,
        brukerTokenInfo: BrukerTokenInfo,
        tilDato: LocalDate? = null,
    ): Beregning {
        val grunnlag = grunnlagKlient.hentGrunnlag(behandling.id, brukerTokenInfo)

        val trygdetid =
            try {
                val trygdetidListe = trygdetidKlient.hentTrygdetid(behandling.id, brukerTokenInfo)

                when (trygdetidListe.size) {
                    0 -> throw TrygdetidMangler(behandling.id)
                    1 -> trygdetidListe.first()
                    else -> throw ForeldreloesTrygdetid(behandling.id)
                }
            } catch (e: Exception) {
                throw TrygdetidMangler(behandling.id)
            }

        val behandlingType = behandling.behandlingType
        val virkningstidspunkt = behandling.virkningstidspunkt().dato
        val beregningsgrunnlag =
            beregningsGrunnlagService.hentBeregningsGrunnlag(behandling.id, brukerTokenInfo)
                ?: throw BeregningsgrunnlagMangler(behandling.id)
        if (beregningsgrunnlag.behandlingId != behandling.id) {
            throw BeregningsgrunnlagMangler(behandling.id)
        }

        logger.info("Beregner omstillingsstønad for behandlingId=${behandling.id} med behandlingType=$behandlingType")

        val omstillingstoenadGrunnlag =
            opprettBeregningsgrunnlag(
                trygdetid,
                beregningsgrunnlag,
            )
        return when (behandlingType) {
            BehandlingType.FØRSTEGANGSBEHANDLING ->
                beregnOmstillingsstoenad(behandling.id, grunnlag, omstillingstoenadGrunnlag, virkningstidspunkt, tilDato)

            BehandlingType.REVURDERING -> {
                val vilkaarsvurderingUtfall =
                    vilkaarsvurderingKlient
                        .hentVilkaarsvurdering(
                            behandling.id,
                            brukerTokenInfo,
                        ).resultat
                        ?.utfall
                        ?: throw Exception("Forventa å ha vilkårsvurderingsresultat for behandlingId=${behandling.id}")

                when (vilkaarsvurderingUtfall) {
                    VilkaarsvurderingUtfall.OPPFYLT ->
                        beregnOmstillingsstoenad(behandling.id, grunnlag, omstillingstoenadGrunnlag, virkningstidspunkt, tilDato)

                    VilkaarsvurderingUtfall.IKKE_OPPFYLT ->
                        opphoer(behandling.id, grunnlag, virkningstidspunkt)
                }
            }
        }
    }

    private fun beregnOmstillingsstoenad(
        behandlingId: UUID,
        grunnlag: Grunnlag,
        beregningsgrunnlag: PeriodisertOmstillingstoenadGrunnlag,
        virkningstidspunkt: YearMonth,
        tilDato: LocalDate? = null,
    ): Beregning {
        val skalBrukeNyeBeregningsregler = featureToggleService.isEnabled(BeregningToggles.BEREGNING_BRUK_NYE_BEREGNINGSREGLER, false)

        val resultat =
            if (skalBrukeNyeBeregningsregler) {
                kroneavrundetOmstillingstoenadRegelMedInstitusjon.eksekver(
                    grunnlag = beregningsgrunnlag,
                    periode = RegelPeriode(fraDato = virkningstidspunkt.atDay(1), tilDato = tilDato),
                )
            } else {
                logger.info("Beregner omstillingsstønad med nye beregningsregler")
                kroneavrundetOmstillingstoenadRegelMedInstitusjon.eksekver(
                    grunnlag = beregningsgrunnlag,
                    periode = RegelPeriode(fraDato = virkningstidspunkt.atDay(1), tilDato = tilDato),
                )
            }

        val beregnetDato = Tidspunkt.now()
        return when (resultat) {
            is RegelkjoeringResultat.Suksess ->
                Beregning(
                    beregningId = randomUUID(),
                    behandlingId = behandlingId,
                    type = Beregningstype.OMS,
                    beregnetDato = beregnetDato,
                    grunnlagMetadata = grunnlag.metadata,
                    beregningsperioder =
                        resultat.periodiserteResultater.map { periodisertResultat ->
                            logger.info(
                                "Beregnet omstillingsstønad for periode fra={} til={} og beløp={} med regler={}",
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

                            val regelverk = Regelverk.REGELVERK_FOM_JAN_2024

                            val trygdetid =
                                periodisertResultat.resultat.finnAnvendtTrygdetid(trygdetidBruktRegel)
                                    ?: throw AnvendtTrygdetidIkkeFunnet(
                                        periodisertResultat.periode.fraDato,
                                        periodisertResultat.periode.tilDato,
                                    )

                            val trygdetidGrunnlagForPeriode =
                                beregningsgrunnlag.avdoed
                                    .finnGrunnlagForPeriode(
                                        periodisertResultat.periode.fraDato,
                                    ).verdi.trygdetid

                            Beregningsperiode(
                                datoFOM = YearMonth.from(periodisertResultat.periode.fraDato),
                                datoTOM = periodisertResultat.periode.tilDato?.let { YearMonth.from(it) },
                                utbetaltBeloep = periodisertResultat.resultat.verdi,
                                institusjonsopphold =
                                    beregningsgrunnlag.institusjonsopphold
                                        .finnGrunnlagForPeriode(
                                            periodisertResultat.periode.fraDato,
                                        ).verdi,
                                grunnbelopMnd = grunnbeloep.grunnbeloepPerMaaned,
                                grunnbelop = grunnbeloep.grunnbeloep,
                                trygdetid = trygdetid.trygdetid.toInteger(),
                                beregningsMetode = trygdetid.beregningsMetode,
                                samletNorskTrygdetid = trygdetidGrunnlagForPeriode.samletTrygdetidNorge?.toInteger(),
                                samletTeoretiskTrygdetid = trygdetidGrunnlagForPeriode.samletTrygdetidTeoretisk?.toInteger(),
                                broek = trygdetidGrunnlagForPeriode.prorataBroek,
                                regelResultat = objectMapper.valueToTree(periodisertResultat),
                                regelVersjon = periodisertResultat.reglerVersjon,
                                regelverk = regelverk,
                                trygdetidForIdent = trygdetidGrunnlagForPeriode.ident,
                                kilde =
                                    Grunnlagsopplysning.RegelKilde(
                                        navn = kroneavrundetOmstillingstoenadRegelMedInstitusjon.regelReferanse.id,
                                        ts = beregnetDato,
                                        versjon = periodisertResultat.reglerVersjon,
                                    ),
                            )
                        },
                    overstyrBeregning = null,
                )

            is RegelkjoeringResultat.UgyldigPeriode ->
                throw RuntimeException("Ugyldig regler for periode: ${resultat.ugyldigeReglerForPeriode}")
        }
    }

    private fun opphoer(
        behandlingId: UUID,
        grunnlag: Grunnlag,
        virkningstidspunkt: YearMonth,
    ): Beregning {
        val grunnbeloep = grunnbeloepRepository.hentGjeldendeGrunnbeloep(virkningstidspunkt)

        return Beregning(
            beregningId = randomUUID(),
            behandlingId = behandlingId,
            type = Beregningstype.OMS,
            beregnetDato = Tidspunkt.now(),
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
            overstyrBeregning = null,
        )
    }

    private fun opprettBeregningsgrunnlag(
        trygdetid: TrygdetidDto,
        beregningsgrunnlag: BeregningsGrunnlag,
    ): PeriodisertOmstillingstoenadGrunnlag {
        val samletTrygdetid =
            trygdetid.toSamlet(beregningsgrunnlag.beregningsMetode.beregningsMetode)
                ?: throw TrygdetidMangler(trygdetid.behandlingId)

        return PeriodisertOmstillingstoenadGrunnlag(
            avdoed =
                KonstantGrunnlag(
                    FaktumNode(
                        verdi = Avdoed(samletTrygdetid),
                        kilde =
                            Grunnlagsopplysning.RegelKilde(
                                "Trygdetid fastsatt av saksbehandler",
                                trygdetid.beregnetTrygdetid?.tidspunkt
                                    ?: throw Exception("Trygdetid mangler tidspunkt på beregnet trygdetid"),
                                "1",
                            ),
                        beskrivelse = "Trygdetid avdød ektefelle",
                    ),
                ),
            institusjonsopphold =
                PeriodisertBeregningGrunnlag.lagPotensieltTomtGrunnlagMedDefaultUtenforPerioder(
                    beregningsgrunnlag.institusjonsopphold.mapVerdier { institusjonsopphold ->
                        FaktumNode(
                            verdi = institusjonsopphold,
                            kilde = beregningsgrunnlag.kilde,
                            beskrivelse = "Institusjonsopphold",
                        )
                    },
                ) { _, _, _ -> FaktumNode(null, beregningsgrunnlag.kilde, "Institusjonsopphold") },
        )
    }
}
