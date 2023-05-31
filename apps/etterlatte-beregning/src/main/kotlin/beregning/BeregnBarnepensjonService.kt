package no.nav.etterlatte.beregning

import beregning.regler.finnAnvendtGrunnbeloep
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagService
import no.nav.etterlatte.beregning.grunnlag.PeriodisertBeregningGrunnlag
import no.nav.etterlatte.beregning.grunnlag.mapVerdier
import no.nav.etterlatte.beregning.regler.barnepensjon.AvdoedForelder
import no.nav.etterlatte.beregning.regler.barnepensjon.PeriodisertBarnepensjonGrunnlag
import no.nav.etterlatte.beregning.regler.barnepensjon.kroneavrundetBarnepensjonRegel
import no.nav.etterlatte.beregning.regler.barnepensjon.sats.grunnbeloep
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnbeloep.GrunnbeloepRepository
import no.nav.etterlatte.klienter.GrunnlagKlient
import no.nav.etterlatte.klienter.TrygdetidKlient
import no.nav.etterlatte.klienter.VilkaarsvurderingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.KonstantGrunnlag
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.RegelkjoeringResultat
import no.nav.etterlatte.libs.regler.eksekver
import no.nav.etterlatte.libs.regler.finnAnvendteRegler
import no.nav.etterlatte.regler.Beregningstall
import no.nav.etterlatte.token.Bruker
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

enum class BeregnBarnepensjonServiceFeatureToggle(private val key: String) : FeatureToggle {
    BrukFaktiskTrygdetid("pensjon-etterlatte.bp-bruk-faktisk-trygdetid");

    override fun key() = key
}

class BeregnBarnepensjonService(
    private val grunnlagKlient: GrunnlagKlient,
    private val vilkaarsvurderingKlient: VilkaarsvurderingKlient,
    private val grunnbeloepRepository: GrunnbeloepRepository = GrunnbeloepRepository,
    private val beregningsGrunnlagService: BeregningsGrunnlagService,
    private val trygdetidKlient: TrygdetidKlient,
    private val featureToggleService: FeatureToggleService
) {
    private val logger = LoggerFactory.getLogger(BeregnBarnepensjonService::class.java)

    suspend fun beregn(behandling: DetaljertBehandling, bruker: Bruker): Beregning {
        val grunnlag = grunnlagKlient.hentGrunnlag(behandling.sak, bruker)
        val behandlingType = behandling.behandlingType
        val virkningstidspunkt = requireNotNull(behandling.virkningstidspunkt?.dato)

        val beregningsGrunnlag = requireNotNull(
            beregningsGrunnlagService.hentBarnepensjonBeregningsGrunnlag(behandling.id, bruker)
        )

        val trygdetid = trygdetidKlient.hentTrygdetid(behandling.id, bruker)

        val barnepensjonGrunnlag = opprettBeregningsgrunnlag(
            beregningsGrunnlag,
            trygdetid.hvisKanBrukes(),
            virkningstidspunkt.atDay(1),
            null
        )

        logger.info("Beregner barnepensjon for behandlingId=${behandling.id} med behandlingType=$behandlingType")

        return when (behandlingType) {
            BehandlingType.FØRSTEGANGSBEHANDLING -> beregnBarnepensjon(
                behandling.id,
                grunnlag,
                barnepensjonGrunnlag,
                virkningstidspunkt
            )

            BehandlingType.REVURDERING -> {
                val vilkaarsvurderingUtfall =
                    vilkaarsvurderingKlient.hentVilkaarsvurdering(behandling.id, bruker).resultat?.utfall
                        ?: throw RuntimeException("Forventa å ha resultat for behandling ${behandling.id}")

                when (vilkaarsvurderingUtfall) {
                    VilkaarsvurderingUtfall.OPPFYLT -> beregnBarnepensjon(
                        behandling.id,
                        grunnlag,
                        barnepensjonGrunnlag,
                        virkningstidspunkt
                    )

                    VilkaarsvurderingUtfall.IKKE_OPPFYLT -> opphoer(behandling.id, grunnlag, virkningstidspunkt)
                }
            }

            BehandlingType.MANUELT_OPPHOER -> opphoer(behandling.id, grunnlag, virkningstidspunkt)
        }
    }

    private fun beregnBarnepensjon(
        behandlingId: UUID,
        grunnlag: Grunnlag,
        beregningsgrunnlag: PeriodisertBarnepensjonGrunnlag,
        virkningstidspunkt: YearMonth
    ): Beregning {
        val resultat = kroneavrundetBarnepensjonRegel.eksekver(
            grunnlag = beregningsgrunnlag,
            periode = RegelPeriode(virkningstidspunkt.atDay(1))
        )

        return when (resultat) {
            is RegelkjoeringResultat.Suksess -> beregning(
                behandlingId = behandlingId,
                grunnlagMetadata = grunnlag.metadata,
                beregningsperioder = resultat.periodiserteResultater.map { periodisertResultat ->
                    logger.info(
                        "Beregnet barnepensjon for periode fra={} til={} og beløp={} med regler={}",
                        periodisertResultat.periode.fraDato,
                        periodisertResultat.periode.tilDato,
                        periodisertResultat.resultat.verdi,
                        periodisertResultat.resultat.finnAnvendteRegler()
                            .map { "${it.regelReferanse.id} (${it.beskrivelse})" }.toSet()
                    )

                    val grunnbeloep = requireNotNull(periodisertResultat.resultat.finnAnvendtGrunnbeloep(grunnbeloep)) {
                        "Anvendt grunnbeløp ikke funnet for perioden"
                    }

                    Beregningsperiode(
                        datoFOM = YearMonth.from(periodisertResultat.periode.fraDato),
                        datoTOM = periodisertResultat.periode.tilDato?.let { YearMonth.from(it) },
                        utbetaltBeloep = periodisertResultat.resultat.verdi,
                        soeskenFlokk = beregningsgrunnlag.soeskenKull.finnGrunnlagForPeriode(
                            periodisertResultat.periode.fraDato
                        ).verdi.map {
                            it.value
                        },
                        grunnbelopMnd = grunnbeloep.grunnbeloepPerMaaned,
                        grunnbelop = grunnbeloep.grunnbeloep,
                        trygdetid = beregningsgrunnlag.avdoedForelder.finnGrunnlagForPeriode(
                            periodisertResultat.periode.fraDato
                        ).verdi.trygdetid.toInteger(),
                        regelResultat = objectMapper.valueToTree(periodisertResultat),
                        regelVersjon = periodisertResultat.reglerVersjon
                    )
                }
            )

            is RegelkjoeringResultat.UgyldigPeriode -> throw RuntimeException(
                "Ugyldig regler for periode: ${resultat.ugyldigeReglerForPeriode}"
            )
        }
    }

    private fun opphoer(
        behandlingId: UUID,
        grunnlag: Grunnlag,
        virkningstidspunkt: YearMonth
    ): Beregning {
        val grunnbeloep = grunnbeloepRepository.hentGjeldendeGrunnbeloep(virkningstidspunkt)

        return beregning(
            behandlingId = behandlingId,
            grunnlagMetadata = grunnlag.metadata,
            beregningsperioder = listOf(
                Beregningsperiode(
                    datoFOM = virkningstidspunkt,
                    datoTOM = null,
                    utbetaltBeloep = 0,
                    soeskenFlokk = null,
                    grunnbelopMnd = grunnbeloep.grunnbeloepPerMaaned,
                    grunnbelop = grunnbeloep.grunnbeloep,
                    trygdetid = 0
                )
            )
        )
    }

    private fun beregning(
        behandlingId: UUID,
        grunnlagMetadata: Metadata,
        beregningsperioder: List<Beregningsperiode>
    ) = Beregning(
        beregningId = UUID.randomUUID(),
        behandlingId = behandlingId,
        type = Beregningstype.BP,
        beregningsperioder = beregningsperioder,
        beregnetDato = Tidspunkt.now(),
        grunnlagMetadata = grunnlagMetadata
    )

    private fun opprettBeregningsgrunnlag(
        beregningsGrunnlag: BeregningsGrunnlag,
        trygdetid: TrygdetidDto?,
        fom: LocalDate,
        tom: LocalDate?
    ) = PeriodisertBarnepensjonGrunnlag(
        soeskenKull = PeriodisertBeregningGrunnlag.lagKomplettPeriodisertGrunnlag(
            beregningsGrunnlag.soeskenMedIBeregning.mapVerdier { soeskenMedIBeregning ->
                FaktumNode(
                    verdi = soeskenMedIBeregning.filter { it.skalBrukes }.map { it.foedselsnummer },
                    kilde = beregningsGrunnlag.kilde,
                    beskrivelse = "Søsken i kullet"
                )
            },
            fom,
            tom
        ),
        avdoedForelder = trygdetid?.beregnetTrygdetid?.total.let { trygdetidTotal ->
            if (trygdetidTotal != null) {
                KonstantGrunnlag(
                    FaktumNode(
                        verdi = AvdoedForelder(Beregningstall(trygdetidTotal)),
                        kilde = Grunnlagsopplysning.RegelKilde(
                            "Trygdetid fastsatt av saksbehandler",
                            trygdetid?.beregnetTrygdetid?.tidspunkt
                                ?: throw Exception("Trygdetid mangler tidspunkt på beregnet trygdetid"),
                            "1"
                        ),
                        beskrivelse = "Trygdetid avdød forelder"
                    )
                )
            } else {
                KonstantGrunnlag(
                    FaktumNode(
                        verdi = AvdoedForelder(Beregningstall(FASTSATT_TRYGDETID_I_PILOT)),
                        kilde = Grunnlagsopplysning.RegelKilde("MVP hardkodet trygdetid", Tidspunkt.now(), "1"),
                        beskrivelse = "Trygdetid avdød forelder"
                    )
                )
            }
        }
    )

    private fun TrygdetidDto?.hvisKanBrukes() = this.takeIf {
        featureToggleService.isEnabled(BeregnBarnepensjonServiceFeatureToggle.BrukFaktiskTrygdetid, false) ||
            it?.beregnetTrygdetid?.total == FASTSATT_TRYGDETID_I_PILOT
    }

    companion object {
        private const val FASTSATT_TRYGDETID_I_PILOT = 40
    }
}