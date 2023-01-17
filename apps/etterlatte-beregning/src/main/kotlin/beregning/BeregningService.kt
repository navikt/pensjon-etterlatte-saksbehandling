package no.nav.etterlatte.beregning

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.beregning.klienter.BehandlingKlient
import no.nav.etterlatte.beregning.klienter.GrunnlagKlient
import no.nav.etterlatte.beregning.klienter.VilkaarsvurderingKlient
import no.nav.etterlatte.beregning.model.G
import no.nav.etterlatte.beregning.model.Grunnbeloep
import no.nav.etterlatte.beregning.regler.AvdoedForelder
import no.nav.etterlatte.beregning.regler.BarnepensjonGrunnlag
import no.nav.etterlatte.beregning.regler.kroneavrundetBarnepensjonRegel
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstyper
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.hentSoeskenjustering
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Beregningsgrunnlag
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.RegelkjoeringResultat
import no.nav.etterlatte.libs.regler.eksekver
import org.slf4j.LoggerFactory
import java.time.YearMonth
import java.util.*
import java.util.UUID.randomUUID

class BeregningService(
    private val beregningRepository: BeregningRepository,
    private val vilkaarsvurderingKlient: VilkaarsvurderingKlient,
    private val grunnlagKlient: GrunnlagKlient,
    private val behandlingKlient: BehandlingKlient
) {
    private val logger = LoggerFactory.getLogger(BeregningService::class.java)

    fun hentBeregning(behandlingId: UUID): Beregning? = beregningRepository.hent(behandlingId)

    suspend fun lagreBeregning(behandlingId: UUID, accessToken: String): Beregning {
        logger.info("Oppretter barnepensjonberegning for behandlingId=$behandlingId")
        return tilstandssjekkFoerKjoerning(behandlingId, accessToken) {
            coroutineScope {
                val behandling = async { behandlingKlient.hentBehandling(behandlingId, accessToken) }
                val grunnlag = async { grunnlagKlient.hentGrunnlag(behandling.await().sak, accessToken) }
                val vilkaarsvurdering =
                    async { vilkaarsvurderingKlient.hentVilkaarsvurdering(behandlingId, accessToken) }

                val beregning = beregnBarnepensjon(grunnlag.await(), behandling.await(), vilkaarsvurdering.await())

                beregningRepository.lagreEllerOppdaterBeregning(beregning).also {
                    behandlingKlient.beregn(behandlingId, accessToken, true)
                }
            }
        }
    }

    fun beregnBarnepensjon(
        grunnlag: Grunnlag,
        behandling: DetaljertBehandling,
        vilkaarsvurdering: VilkaarsvurderingDto
    ): Beregning {
        val behandlingType = requireNotNull(behandling.behandlingType)
        val virkningstidspunkt = requireNotNull(behandling.virkningstidspunkt?.dato)
        val vilkaarsvurderingUtfall = requireNotNull(vilkaarsvurdering.resultat?.utfall)
        val grunnbeloep = Grunnbeloep.hentGjeldendeG(virkningstidspunkt)
        val beregningsgrunnlag =
            opprettBeregningsgrunnlag(grunnbeloep, requireNotNull(grunnlag.sak.hentSoeskenjustering()))

        logger.info("Beregner barnepensjon for behandlingId=${behandling.id} med behandlingType=$behandlingType")

        return when (behandlingType) {
            BehandlingType.FØRSTEGANGSBEHANDLING ->
                beregn(behandling, grunnlag, beregningsgrunnlag, virkningstidspunkt, grunnbeloep)
            BehandlingType.REVURDERING ->
                when (vilkaarsvurderingUtfall) {
                    VilkaarsvurderingUtfall.OPPFYLT -> beregn(
                        behandling,
                        grunnlag,
                        beregningsgrunnlag,
                        virkningstidspunkt,
                        grunnbeloep
                    )
                    VilkaarsvurderingUtfall.IKKE_OPPFYLT ->
                        beregnOpphoer(behandling, grunnlag, beregningsgrunnlag, virkningstidspunkt, grunnbeloep)
                }
            BehandlingType.MANUELT_OPPHOER -> {
                beregnOpphoer(behandling, grunnlag, beregningsgrunnlag, virkningstidspunkt, grunnbeloep)
            }
        }
    }

    private fun beregn(
        behandling: DetaljertBehandling,
        grunnlag: Grunnlag,
        beregningsgrunnlag: BarnepensjonGrunnlag,
        virkningstidspunkt: YearMonth,
        grunnbeloep: G
    ): Beregning {
        val resultat = kroneavrundetBarnepensjonRegel.eksekver(
            grunnlag = beregningsgrunnlag,
            periode = RegelPeriode(virkningstidspunkt.atDay(1))
        )

        return when (resultat) {
            is RegelkjoeringResultat.Suksess ->
                beregning(
                    behandling = behandling,
                    grunnlag = grunnlag,
                    beregningsperioder = resultat.periodiserteResultater.map { periodisertResultat ->
                        beregningsperiode(
                            datoFOM = YearMonth.from(periodisertResultat.periode.fraDato),
                            datoTOM = periodisertResultat.periode.tilDato?.let { YearMonth.from(it) },
                            beloep = periodisertResultat.resultat.verdi,
                            grunnbeloep = grunnbeloep,
                            beregningsgrunnlag = beregningsgrunnlag
                        )
                    }
                )
            is RegelkjoeringResultat.UgyldigPeriode ->
                throw RuntimeException("Ugyldig regler for periode: ${resultat.ugyldigeReglerForPeriode}")
        }
    }

    private fun beregnOpphoer(
        behandling: DetaljertBehandling,
        grunnlag: Grunnlag,
        beregningsgrunnlag: BarnepensjonGrunnlag,
        virkningstidspunkt: YearMonth,
        grunnbeloep: G
    ) = beregning(
        behandling = behandling,
        grunnlag = grunnlag,
        beregningsperioder = listOf(
            beregningsperiode(
                datoFOM = virkningstidspunkt,
                datoTOM = null,
                beloep = 0,
                grunnbeloep = grunnbeloep,
                beregningsgrunnlag = beregningsgrunnlag
            )
        )
    )

    private fun beregning(
        behandling: DetaljertBehandling,
        grunnlag: Grunnlag,
        beregningsperioder: List<Beregningsperiode>
    ) = Beregning(
        beregningId = randomUUID(),
        behandlingId = behandling.id,
        beregningsperioder = beregningsperioder,
        beregnetDato = Tidspunkt.now(),
        grunnlagMetadata = grunnlag.metadata
    )

    private fun beregningsperiode(
        datoFOM: YearMonth,
        datoTOM: YearMonth? = null,
        beloep: Int,
        grunnbeloep: G,
        beregningsgrunnlag: BarnepensjonGrunnlag
    ) = Beregningsperiode(
        delytelsesId = "BP",
        type = Beregningstyper.GP,
        datoFOM = datoFOM,
        datoTOM = datoTOM,
        utbetaltBeloep = beloep,
        soeskenFlokk = beregningsgrunnlag.soeskenKull.verdi.map { it.value },
        grunnbelopMnd = grunnbeloep.grunnbeløpPerMåned,
        grunnbelop = grunnbeloep.grunnbeløp,
        trygdetid = beregningsgrunnlag.avdoedForelder.verdi.trygdetid.toInt()
    )

    private fun opprettBeregningsgrunnlag(
        grunnbeloep: G,
        soeskenJustering: Opplysning.Konstant<Beregningsgrunnlag>
    ) = BarnepensjonGrunnlag(
        grunnbeloep = FaktumNode(
            verdi = grunnbeloep.grunnbeløpPerMåned.toBigDecimal(),
            kilde = "System",
            beskrivelse = "Grunnbeløp"
        ),
        antallSoeskenIKullet = FaktumNode(
            verdi = soeskenJustering.verdi.beregningsgrunnlag.filter { it.skalBrukes }.size,
            kilde = soeskenJustering.kilde,
            beskrivelse = "Antall søsken i kullet"
        ),
        soeskenKull = FaktumNode(
            verdi = soeskenJustering.verdi.beregningsgrunnlag.filter { it.skalBrukes }.map { it.foedselsnummer },
            kilde = soeskenJustering.kilde,
            beskrivelse = "Søsken i kullet"
        ),
        avdoedForelder = FaktumNode(
            verdi = AvdoedForelder(40.0.toBigDecimal()),
            kilde = "System",
            beskrivelse = "Trygdetid avdøed forelder"
        )
    )

    private suspend fun tilstandssjekkFoerKjoerning(
        behandlingId: UUID,
        accessToken: String,
        block: suspend () -> Beregning
    ): Beregning {
        val kanBeregne = behandlingKlient.beregn(behandlingId, accessToken, false)

        if (!kanBeregne) {
            throw IllegalStateException("Kunne ikke beregne, er i feil state")
        }

        return block()
    }
}