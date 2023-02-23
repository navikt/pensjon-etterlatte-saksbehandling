package no.nav.etterlatte.beregning

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.beregning.grunnbeloep.GrunnbeloepRepository
import no.nav.etterlatte.beregning.klienter.BehandlingKlient
import no.nav.etterlatte.beregning.klienter.GrunnlagKlient
import no.nav.etterlatte.beregning.klienter.VilkaarsvurderingKlient
import no.nav.etterlatte.beregning.regler.AvdoedForelder
import no.nav.etterlatte.beregning.regler.BarnepensjonGrunnlag
import no.nav.etterlatte.beregning.regler.Beregningstall
import no.nav.etterlatte.beregning.regler.finnAnvendtGrunnbeloep
import no.nav.etterlatte.beregning.regler.kroneavrundetBarnepensjonRegel
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
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

    private val FASTSATT_TRYGDETID_PILOT = 40 // Trygdetid er alltid 40 år i pilot

    fun hentBeregning(behandlingId: UUID): Beregning? = beregningRepository.hent(behandlingId)

    suspend fun lagreBeregning(behandlingId: UUID, bruker: Bruker): Beregning {
        logger.info("Oppretter barnepensjonberegning for behandlingId=$behandlingId")
        return tilstandssjekkFoerKjoerning(behandlingId, bruker) {
            val behandling = behandlingKlient.hentBehandling(behandlingId, bruker)
            val sak = behandlingKlient.hentSak(behandling.sak, bruker)

            val beregning = when (sak.sakType) {
                SakType.BARNEPENSJON -> beregnBarnepensjon(behandling, bruker)
                SakType.OMSTILLINGSSTOENAD -> beregnOmstillingstoenad(behandling, bruker)
            }

            beregningRepository.lagreEllerOppdaterBeregning(beregning).also {
                behandlingKlient.beregn(behandlingId, bruker, true)
            }
        }
    }

    private suspend fun beregnOmstillingstoenad(behandling: DetaljertBehandling, bruker: Bruker): Beregning {
        logger.info("Beregner omstillingstønad for behandlingId=${behandling.id}")
        val grunnlag = grunnlagKlient.hentGrunnlag(behandling.sak, bruker)

        beregnBarnepensjon(grunnlag, behandling) {
            runBlocking {
                vilkaarsvurderingKlient.hentVilkaarsvurdering(behandling.id, bruker)
                    .resultat?.utfall
                    ?: throw RuntimeException("Forventa å ha resultat for behandling ${behandling.id}")
            }
        }
    }

    private suspend fun beregnBarnepensjon(behandling: DetaljertBehandling, bruker: Bruker): Beregning {
        logger.info("Beregner barnepensjon for behandlingId=${behandling.id}")
        val grunnlag = grunnlagKlient.hentGrunnlag(behandling.sak, bruker)

        return when (behandling.behandlingType) {
            BehandlingType.MANUELT_OPPHOER -> beregnManueltOpphoerBarnepensjon(
                grunnlag = grunnlag,
                behandling = behandling
            )

            else -> {
                beregnBarnepensjon(grunnlag, behandling) {
                    runBlocking {
                        vilkaarsvurderingKlient.hentVilkaarsvurdering(behandling.id, bruker)
                            .resultat?.utfall
                            ?: throw RuntimeException("Forventa å ha resultat for behandling ${behandling.id}")
                    }
                }
            }
        }
    }

    fun beregnManueltOpphoerBarnepensjon(
        grunnlag: Grunnlag,
        behandling: DetaljertBehandling
    ): Beregning {
        if (behandling.behandlingType != BehandlingType.MANUELT_OPPHOER) {
            throw IllegalArgumentException(
                "Fikk en behandling med id=${behandling.id} av type ${behandling.behandlingType} " +
                    "som ikke kan beregnes som et manuelt opphør."
            )
        }
        val beregningsgrunnlag = opprettBeregningsgrunnlag(requireNotNull(grunnlag.sak.hentSoeskenjustering()))
        return beregnOpphoer(
            behandling = behandling,
            grunnlag = grunnlag,
            beregningsgrunnlag = beregningsgrunnlag,
            virkningstidspunkt = behandling.virkningstidspunkt!!.dato
        )
    }

    fun beregnBarnepensjon(
        grunnlag: Grunnlag,
        behandling: DetaljertBehandling,
        hentVilkaarsvurdering: () -> VilkaarsvurderingUtfall
    ): Beregning {
        val behandlingType = behandling.behandlingType
        val virkningstidspunkt = requireNotNull(behandling.virkningstidspunkt?.dato)
        val beregningsgrunnlag = opprettBeregningsgrunnlag(requireNotNull(grunnlag.sak.hentSoeskenjustering()))

        logger.info("Beregner barnepensjon for behandlingId=${behandling.id} med behandlingType=$behandlingType")

        return when (behandlingType) {
            BehandlingType.FØRSTEGANGSBEHANDLING, BehandlingType.OMREGNING ->
                beregn(behandling, grunnlag, beregningsgrunnlag, virkningstidspunkt)

            BehandlingType.REVURDERING -> {
                when (requireNotNull(hentVilkaarsvurdering.invoke())) {
                    VilkaarsvurderingUtfall.OPPFYLT ->
                        beregn(behandling, grunnlag, beregningsgrunnlag, virkningstidspunkt)

                    VilkaarsvurderingUtfall.IKKE_OPPFYLT ->
                        beregnOpphoer(behandling, grunnlag, beregningsgrunnlag, virkningstidspunkt)
                }
            }

            else -> throw IllegalArgumentException("Kan ikke beregne manuelt opphør med en vilkårsvurdering!")
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
                        logger.info(
                            "Beregnet barnepensjon for periode fra={} til={} og beløp={} med regler={}",
                            periodisertResultat.periode.fraDato,
                            periodisertResultat.periode.tilDato,
                            periodisertResultat.resultat.verdi,
                            periodisertResultat.resultat.finnAnvendteRegler()
                                .map { "${it.regelReferanse.id} (${it.beskrivelse})" }.toSet()
                        )

                        val grunnbeloep = requireNotNull(periodisertResultat.resultat.finnAnvendtGrunnbeloep()) {
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

    private fun beregnOpphoer(
        behandling: DetaljertBehandling,
        grunnlag: Grunnlag,
        beregningsgrunnlag: BarnepensjonGrunnlag,
        virkningstidspunkt: YearMonth
    ): Beregning {
        val grunnbeloep = GrunnbeloepRepository.hentGjeldendeGrunnbeloep(virkningstidspunkt)

        return beregning(
            behandling = behandling,
            grunnlag = grunnlag,
            beregningsperioder = listOf(
                Beregningsperiode(
                    datoFOM = virkningstidspunkt,
                    datoTOM = null,
                    utbetaltBeloep = 0,
                    soeskenFlokk = beregningsgrunnlag.soeskenKull.verdi.map { it.value },
                    grunnbelopMnd = grunnbeloep.grunnbeloepPerMaaned,
                    grunnbelop = grunnbeloep.grunnbeloep,
                    trygdetid = beregningsgrunnlag.avdoedForelder.verdi.trygdetid.toInteger()
                )
            )
        )
    }

    private fun beregning(
        behandling: DetaljertBehandling,
        grunnlag: Grunnlag,
        beregningsperioder: List<Beregningsperiode>
    ) = Beregning(
        beregningId = randomUUID(),
        behandlingId = behandling.id,
        type = Beregningstype.BP,
        beregningsperioder = beregningsperioder,
        beregnetDato = Tidspunkt.now(),
        grunnlagMetadata = grunnlag.metadata
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
            verdi = AvdoedForelder(Beregningstall(FASTSATT_TRYGDETID_PILOT)),
            kilde = Grunnlagsopplysning.RegelKilde("MVP hardkodet trygdetid", Instant.now(), "1"),
            beskrivelse = "Trygdetid avdøed forelder"
        )
    )

    private suspend fun tilstandssjekkFoerKjoerning(
        behandlingId: UUID,
        bruker: Bruker,
        block: suspend () -> Beregning
    ): Beregning {
        val kanBeregne = behandlingKlient.beregn(behandlingId, bruker, false)

        if (!kanBeregne) {
            throw IllegalStateException("Kunne ikke beregne, behandling er i feil state")
        }

        return block()
    }
}