package no.nav.etterlatte.beregning

import beregning.regler.finnAnvendtGrunnbeloep
import no.nav.etterlatte.beregning.grunnbeloep.GrunnbeloepRepository
import no.nav.etterlatte.beregning.klienter.GrunnlagKlient
import no.nav.etterlatte.beregning.klienter.VilkaarsvurderingKlient
import no.nav.etterlatte.beregning.regler.Beregningstall
import no.nav.etterlatte.beregning.regler.barnepensjon.AvdoedForelder
import no.nav.etterlatte.beregning.regler.barnepensjon.BarnepensjonGrunnlag
import no.nav.etterlatte.beregning.regler.barnepensjon.kroneavrundetBarnepensjonRegel
import no.nav.etterlatte.beregning.regler.barnepensjon.sats.grunnbeloep
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.hentSoeskenjustering
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Beregningsgrunnlag
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
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

class BeregnBarnepensjonService(
    private val grunnlagKlient: GrunnlagKlient,
    private val vilkaarsvurderingKlient: VilkaarsvurderingKlient,
    private val grunnbeloepRepository: GrunnbeloepRepository = GrunnbeloepRepository
) {
    private val logger = LoggerFactory.getLogger(BeregnBarnepensjonService::class.java)

    suspend fun beregn(behandling: DetaljertBehandling, bruker: Bruker): Beregning {
        val grunnlag = grunnlagKlient.hentGrunnlag(behandling.sak, bruker)
        val behandlingType = behandling.behandlingType
        val virkningstidspunkt = requireNotNull(behandling.virkningstidspunkt?.dato)
        val beregningsgrunnlag = opprettBeregningsgrunnlag(requireNotNull(grunnlag.sak.hentSoeskenjustering()))

        logger.info("Beregner barnepensjon for behandlingId=${behandling.id} med behandlingType=$behandlingType")

        return when (behandlingType) {
            BehandlingType.FØRSTEGANGSBEHANDLING, BehandlingType.OMREGNING ->
                beregnBarnepensjon(behandling.id, grunnlag, beregningsgrunnlag, virkningstidspunkt)

            BehandlingType.REVURDERING -> {
                val vilkaarsvurderingUtfall = vilkaarsvurderingKlient.hentVilkaarsvurdering(behandling.id, bruker)
                    .resultat?.utfall
                    ?: throw RuntimeException("Forventa å ha resultat for behandling ${behandling.id}")

                when (vilkaarsvurderingUtfall) {
                    VilkaarsvurderingUtfall.OPPFYLT ->
                        beregnBarnepensjon(behandling.id, grunnlag, beregningsgrunnlag, virkningstidspunkt)
                    VilkaarsvurderingUtfall.IKKE_OPPFYLT ->
                        opphoer(behandling.id, grunnlag, virkningstidspunkt)
                }
            }

            BehandlingType.MANUELT_OPPHOER -> opphoer(behandling.id, grunnlag, virkningstidspunkt)
        }
    }

    private fun beregnBarnepensjon(
        behandlingId: UUID,
        grunnlag: Grunnlag,
        beregningsgrunnlag: BarnepensjonGrunnlag,
        virkningstidspunkt: YearMonth
    ): Beregning {
        val resultat = kroneavrundetBarnepensjonRegel.eksekver(
            grunnlag = beregningsgrunnlag,
            periode = RegelPeriode(virkningstidspunkt.atDay(1))
        )

        return when (resultat) {
            is RegelkjoeringResultat.Suksess ->
                beregning(
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

                        val grunnbeloep =
                            requireNotNull(periodisertResultat.resultat.finnAnvendtGrunnbeloep(grunnbeloep)) {
                                "Anvendt grunnbeløp ikke funnet for perioden"
                            }

                        Beregningsperiode(
                            datoFOM = YearMonth.from(periodisertResultat.periode.fraDato),
                            datoTOM = periodisertResultat.periode.tilDato?.let { YearMonth.from(it) },
                            utbetaltBeloep = periodisertResultat.resultat.verdi,
                            soeskenFlokk = beregningsgrunnlag.soeskenKull.verdi.map { it.value },
                            grunnbelopMnd = grunnbeloep.grunnbeloepPerMaaned,
                            grunnbelop = grunnbeloep.grunnbeloep,
                            trygdetid = beregningsgrunnlag.avdoedForelder.verdi.trygdetid.toInteger(),
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
        soeskenJustering: Opplysning.Konstant<Beregningsgrunnlag>
    ) = BarnepensjonGrunnlag(
        soeskenKull = FaktumNode(
            verdi = soeskenJustering.verdi.beregningsgrunnlag.filter { it.skalBrukes }.map { it.foedselsnummer },
            kilde = soeskenJustering.kilde,
            beskrivelse = "Søsken i kullet"
        ),
        avdoedForelder = FaktumNode(
            verdi = AvdoedForelder(Beregningstall(FASTSATT_TRYGDETID_I_PILOT)),
            kilde = Grunnlagsopplysning.RegelKilde("MVP hardkodet trygdetid", Tidspunkt.now(), "1"),
            beskrivelse = "Trygdetid avdøed forelder"
        )
    )

    companion object {
        private const val FASTSATT_TRYGDETID_I_PILOT = 40
    }
}