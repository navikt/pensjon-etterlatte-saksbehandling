package no.nav.etterlatte.beregning

import beregning.regler.finnAnvendtGrunnbeloep
import no.nav.etterlatte.beregning.grunnbeloep.GrunnbeloepRepository
import no.nav.etterlatte.beregning.klienter.GrunnlagKlient
import no.nav.etterlatte.beregning.klienter.TrygdetidKlient
import no.nav.etterlatte.beregning.klienter.VilkaarsvurderingKlient
import no.nav.etterlatte.beregning.regler.Beregningstall
import no.nav.etterlatte.beregning.regler.omstillingstoenad.Avdoed
import no.nav.etterlatte.beregning.regler.omstillingstoenad.OmstillingstoenadGrunnlag
import no.nav.etterlatte.beregning.regler.omstillingstoenad.kroneavrundetOmstillingsstoenadRegel
import no.nav.etterlatte.beregning.regler.omstillingstoenad.sats.grunnbeloep
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.RegelkjoeringResultat
import no.nav.etterlatte.libs.regler.eksekver
import no.nav.etterlatte.libs.regler.finnAnvendteRegler
import no.nav.etterlatte.token.Bruker
import org.slf4j.LoggerFactory
import java.time.YearMonth
import java.util.*
import java.util.UUID.randomUUID

class BeregnOmstillingsstoenadService(
    private val grunnlagKlient: GrunnlagKlient,
    private val vilkaarsvurderingKlient: VilkaarsvurderingKlient,
    private val trygdetidKlient: TrygdetidKlient,
    private val grunnbeloepRepository: GrunnbeloepRepository = GrunnbeloepRepository
) {
    private val logger = LoggerFactory.getLogger(BeregnOmstillingsstoenadService::class.java)

    suspend fun beregn(behandling: DetaljertBehandling, bruker: Bruker): Beregning {
        val grunnlag = grunnlagKlient.hentGrunnlag(behandling.sak, bruker)
        val trygdetid = trygdetidKlient.hentTrygdetid(behandling.id, bruker)
        val behandlingType = behandling.behandlingType
        val virkningstidspunkt = requireNotNull(behandling.virkningstidspunkt?.dato)
        val beregningsgrunnlag = opprettBeregningsgrunnlagOmstillingsstoenad(trygdetid)

        logger.info("Beregner omstillingsstønad for behandlingId=${behandling.id} med behandlingType=$behandlingType")

        return when (behandlingType) {
            BehandlingType.FØRSTEGANGSBEHANDLING ->
                beregnOmstillingsstoenad(behandling.id, grunnlag, beregningsgrunnlag, virkningstidspunkt)

            BehandlingType.REVURDERING -> {
                val vilkaarsvurderingUtfall = vilkaarsvurderingKlient.hentVilkaarsvurdering(behandling.id, bruker)
                    .resultat?.utfall
                    ?: throw Exception("Forventa å ha vilkårsvurderingsresultat for behandlingId=${behandling.id}")

                when (vilkaarsvurderingUtfall) {
                    VilkaarsvurderingUtfall.OPPFYLT ->
                        beregnOmstillingsstoenad(behandling.id, grunnlag, beregningsgrunnlag, virkningstidspunkt)

                    VilkaarsvurderingUtfall.IKKE_OPPFYLT ->
                        opphoer(behandling.id, grunnlag, virkningstidspunkt)
                }
            }

            BehandlingType.MANUELT_OPPHOER -> opphoer(behandling.id, grunnlag, virkningstidspunkt)
        }
    }

    private fun beregnOmstillingsstoenad(
        behandlingId: UUID,
        grunnlag: Grunnlag,
        beregningsgrunnlag: OmstillingstoenadGrunnlag,
        virkningstidspunkt: YearMonth
    ): Beregning {
        val resultat = kroneavrundetOmstillingsstoenadRegel.eksekver(
            grunnlag = beregningsgrunnlag,
            periode = RegelPeriode(virkningstidspunkt.atDay(1))
        )

        return when (resultat) {
            is RegelkjoeringResultat.Suksess ->
                Beregning(
                    beregningId = randomUUID(),
                    behandlingId = behandlingId,
                    type = Beregningstype.OMS,
                    beregnetDato = Tidspunkt.now(),
                    grunnlagMetadata = grunnlag.metadata,
                    beregningsperioder = resultat.periodiserteResultater.map { periodisertResultat ->
                        logger.info(
                            "Beregnet omstillingsstønad for periode fra={} til={} og beløp={} med regler={}",
                            periodisertResultat.periode.fraDato,
                            periodisertResultat.periode.tilDato,
                            periodisertResultat.resultat.verdi,
                            periodisertResultat.resultat.finnAnvendteRegler()
                                .map { "${it.regelReferanse.id} (${it.beskrivelse})" }.toSet()
                        )

                        val grunnbeloep =
                            requireNotNull(periodisertResultat.resultat.finnAnvendtGrunnbeloep(grunnbeloep)) {
                                "Anvendt grunnbeløp ikke funnet for perioden"
                            }

                        Beregningsperiode(
                            datoFOM = YearMonth.from(periodisertResultat.periode.fraDato),
                            datoTOM = periodisertResultat.periode.tilDato?.let { YearMonth.from(it) },
                            utbetaltBeloep = periodisertResultat.resultat.verdi,
                            grunnbelopMnd = grunnbeloep.grunnbeloepPerMaaned,
                            grunnbelop = grunnbeloep.grunnbeloep,
                            trygdetid = beregningsgrunnlag.avdoed.verdi.trygdetid.toInteger(),
                            regelResultat = objectMapper.valueToTree(periodisertResultat),
                            regelVersjon = periodisertResultat.reglerVersjon
                        )
                    }
                )

            is RegelkjoeringResultat.UgyldigPeriode ->
                throw RuntimeException("Ugyldig regler for periode: ${resultat.ugyldigeReglerForPeriode}")
        }
    }

    private fun opphoer(
        behandlingId: UUID,
        grunnlag: Grunnlag,
        virkningstidspunkt: YearMonth
    ): Beregning {
        val grunnbeloep = grunnbeloepRepository.hentGjeldendeGrunnbeloep(virkningstidspunkt)

        return Beregning(
            beregningId = randomUUID(),
            behandlingId = behandlingId,
            type = Beregningstype.OMS,
            beregnetDato = Tidspunkt.now(),
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

    private fun opprettBeregningsgrunnlagOmstillingsstoenad(trygdetid: TrygdetidDto): OmstillingstoenadGrunnlag {
        val totalTrygdetid = requireNotNull(trygdetid.beregnetTrygdetid?.total) {
            "Total trygdetid ikke satt for behandling ${trygdetid.behandlingId}"
        }

        return OmstillingstoenadGrunnlag(
            avdoed = FaktumNode(
                verdi = Avdoed(Beregningstall(totalTrygdetid)),
                kilde = Grunnlagsopplysning.RegelKilde(
                    "Trygdetid fastsatt av saksbehandler",
                    trygdetid.beregnetTrygdetid?.tidspunkt
                        ?: throw Exception("Trygdetid mangler tidspunkt på beregnet trygdetid"),
                    "1"
                ),
                beskrivelse = "Trygdetid avdød ektefelle"
            )
        )
    }
}