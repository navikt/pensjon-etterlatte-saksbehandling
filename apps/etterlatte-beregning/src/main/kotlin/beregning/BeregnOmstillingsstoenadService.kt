package no.nav.etterlatte.beregning

import beregning.regler.finnAnvendtGrunnbeloep
import no.nav.etterlatte.beregning.grunnbeloep.GrunnbeloepRepository
import no.nav.etterlatte.beregning.klienter.GrunnlagKlient
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
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.RegelkjoeringResultat
import no.nav.etterlatte.libs.regler.eksekver
import no.nav.etterlatte.libs.regler.finnAnvendteRegler
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.YearMonth
import java.util.UUID.randomUUID

class BeregnOmstillingsstoenadService(
    private val grunnlagKlient: GrunnlagKlient,
    private val vilkaarsvurderingKlient: VilkaarsvurderingKlient,
    private val grunnbeloepRepository: GrunnbeloepRepository = GrunnbeloepRepository
) {
    private val logger = LoggerFactory.getLogger(BeregnOmstillingsstoenadService::class.java)

    suspend fun beregn(behandling: DetaljertBehandling, accessToken: String): Beregning {
        val grunnlag = grunnlagKlient.hentGrunnlag(behandling.sak, accessToken)
        val behandlingType = behandling.behandlingType
        val virkningstidspunkt = requireNotNull(behandling.virkningstidspunkt?.dato)
        val beregningsgrunnlag = opprettBeregningsgrunnlagOmstillingsstoenad(FASTSATT_TRYGDETID_I_PILOT)

        logger.info("Beregner omstillingsstønad for behandlingId=${behandling.id} med behandlingType=$behandlingType")

        return when (behandlingType) {
            BehandlingType.FØRSTEGANGSBEHANDLING, BehandlingType.OMREGNING ->
                beregnOmstillingsstoenad(behandling, grunnlag, beregningsgrunnlag, virkningstidspunkt)

            BehandlingType.REVURDERING -> {
                val vilkaarsvurderingUtfall = vilkaarsvurderingKlient.hentVilkaarsvurdering(behandling.id, accessToken)
                    .resultat?.utfall
                    ?: throw Exception("Forventa å ha vilkårsvurderingsresultat for behandlingId=${behandling.id}")

                when (vilkaarsvurderingUtfall) {
                    VilkaarsvurderingUtfall.OPPFYLT ->
                        beregnOmstillingsstoenad(behandling, grunnlag, beregningsgrunnlag, virkningstidspunkt)

                    VilkaarsvurderingUtfall.IKKE_OPPFYLT ->
                        opphoer(behandling, grunnlag, virkningstidspunkt)
                }
            }

            BehandlingType.MANUELT_OPPHOER -> opphoer(behandling, grunnlag, virkningstidspunkt)
        }
    }

    private fun beregnOmstillingsstoenad(
        behandling: DetaljertBehandling,
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
                    behandlingId = behandling.id,
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
        behandling: DetaljertBehandling,
        grunnlag: Grunnlag,
        virkningstidspunkt: YearMonth
    ): Beregning {
        val grunnbeloep = grunnbeloepRepository.hentGjeldendeGrunnbeloep(virkningstidspunkt)

        return Beregning(
            beregningId = randomUUID(),
            behandlingId = behandling.id,
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

    private fun opprettBeregningsgrunnlagOmstillingsstoenad(trygdetid: Int) = OmstillingstoenadGrunnlag(
        avdoed = FaktumNode(
            verdi = Avdoed(Beregningstall(trygdetid)),
            kilde = Grunnlagsopplysning.RegelKilde("MVP hardkodet trygdetid", Instant.now(), "1"),
            beskrivelse = "Trygdetid avdøed ektefelle"
        )
    )

    private companion object {
        private const val FASTSATT_TRYGDETID_I_PILOT = 40
    }
}