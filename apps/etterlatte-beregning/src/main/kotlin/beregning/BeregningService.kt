package no.nav.etterlatte.beregning

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.beregning.klienter.BehandlingKlient
import no.nav.etterlatte.beregning.klienter.GrunnlagKlient
import no.nav.etterlatte.beregning.klienter.VilkaarsvurderingKlient
import no.nav.etterlatte.beregning.regler.AvdoedForelder
import no.nav.etterlatte.beregning.regler.BarnepensjonGrunnlag
import no.nav.etterlatte.beregning.regler.finnAnvendtGrunnbeloep
import no.nav.etterlatte.beregning.regler.kroneavrundetBarnepensjonRegel
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstyper
import no.nav.etterlatte.libs.common.beregning.DelytelseId
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
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
import java.time.Instant
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
        val beregningsgrunnlag = opprettBeregningsgrunnlag(requireNotNull(grunnlag.sak.hentSoeskenjustering()))

        logger.info("Beregner barnepensjon for behandlingId=${behandling.id} med behandlingType=$behandlingType")

        return when (behandlingType) {
            BehandlingType.FØRSTEGANGSBEHANDLING ->
                beregn(behandling, grunnlag, beregningsgrunnlag, virkningstidspunkt)
            BehandlingType.REVURDERING -> {
                when (requireNotNull(vilkaarsvurdering.resultat?.utfall)) {
                    VilkaarsvurderingUtfall.OPPFYLT ->
                        beregn(behandling, grunnlag, beregningsgrunnlag, virkningstidspunkt)
                    VilkaarsvurderingUtfall.IKKE_OPPFYLT ->
                        beregnOpphoer(behandling, grunnlag, beregningsgrunnlag, virkningstidspunkt)
                }
            }
            BehandlingType.MANUELT_OPPHOER -> {
                beregnOpphoer(behandling, grunnlag, beregningsgrunnlag, virkningstidspunkt)
            }
        }
    }

    private fun beregn(
        behandling: DetaljertBehandling,
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
                    behandling = behandling,
                    grunnlag = grunnlag,
                    beregningsperioder = resultat.periodiserteResultater.map { periodisertResultat ->
                        beregningsperiode(
                            datoFOM = YearMonth.from(periodisertResultat.periode.fraDato),
                            datoTOM = periodisertResultat.periode.tilDato?.let { YearMonth.from(it) },
                            beloep = periodisertResultat.resultat.verdi,
                            grunnbeloep = requireNotNull(periodisertResultat.resultat.finnAnvendtGrunnbeloep()) {
                                "Anvendt grunnbeløp ikke funnet for perioden"
                            },
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
        virkningstidspunkt: YearMonth
    ) = beregning(
        behandling = behandling,
        grunnlag = grunnlag,
        beregningsperioder = listOf(
            beregningsperiode(
                datoFOM = virkningstidspunkt,
                datoTOM = null,
                beloep = 0,
                grunnbeloep = 0,
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
        grunnbeloep: Int,
        beregningsgrunnlag: BarnepensjonGrunnlag
    ) = Beregningsperiode(
        delytelsesId = DelytelseId.BP,
        type = Beregningstyper.GP,
        datoFOM = datoFOM,
        datoTOM = datoTOM,
        utbetaltBeloep = beloep,
        soeskenFlokk = beregningsgrunnlag.soeskenKull.verdi.map { it.value },
        grunnbelopMnd = grunnbeloep,
        trygdetid = beregningsgrunnlag.avdoedForelder.verdi.trygdetid.toInt()
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
            verdi = AvdoedForelder(40.0.toBigDecimal()),
            kilde = Grunnlagsopplysning.RegelKilde("MVP hardkodet trygdetid", Instant.now(), "1"),
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