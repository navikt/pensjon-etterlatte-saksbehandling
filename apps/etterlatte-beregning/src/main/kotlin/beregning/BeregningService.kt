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
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentSoeskenjustering
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Beregningsgrunnlag
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.RegelkjoeringResultat
import no.nav.etterlatte.libs.regler.eksekver
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

class BeregningService(
    private val beregningRepository: BeregningRepository,
    private val vilkaarsvurderingKlient: VilkaarsvurderingKlient,
    private val grunnlagKlient: GrunnlagKlient,
    private val behandlingKlient: BehandlingKlient
) {

    fun hentBeregning(behandlingId: UUID): Beregning? = beregningRepository.hent(behandlingId)

    suspend fun lagreBeregning(behandlingId: UUID, accessToken: String): Beregning {
        return tilstandssjekkFoerKjoerning(behandlingId, accessToken) {
            coroutineScope {
                val vilkaarsvurdering =
                    async { vilkaarsvurderingKlient.hentVilkaarsvurdering(behandlingId, accessToken) }
                val behandling = async { behandlingKlient.hentBehandling(behandlingId, accessToken) }
                val grunnlag = async { grunnlagKlient.hentGrunnlag(behandling.await().sak, accessToken) }
                val beregning = lagBeregning(grunnlag.await(), behandling.await(), vilkaarsvurdering.await())

                beregningRepository.lagreEllerOppdaterBeregning(beregning).also {
                    behandlingKlient.beregn(behandlingId, accessToken, true)
                }
            }
        }
    }

    fun lagBeregning(
        grunnlag: Grunnlag,
        behandling: DetaljertBehandling,
        vilkaarsvurdering: VilkaarsvurderingDto
    ): Beregning {
        val behandlingType = behandling.behandlingType!!
        val virkningstidspunkt = behandling.virkningstidspunkt?.dato!!
        val vilkaarsvurderingUtfall = vilkaarsvurdering.resultat!!.utfall
        val grunnbeloep = Grunnbeloep.hentGjeldendeG(virkningstidspunkt)
        val barnepensjonBeregningsgrunnlag = opprettBeregningsgrunnlag(
            grunnbeloep = grunnbeloep,
            soeskenJustering = grunnlag.sak.hentSoeskenjustering()!!
        )

        return when (behandlingType) {
            BehandlingType.FØRSTEGANGSBEHANDLING -> beregnFoerstegangsbehandling(
                behandling = behandling,
                grunnlag = grunnlag,
                barnepensjonGrunnlag = barnepensjonBeregningsgrunnlag,
                virkningstidspunkt = virkningstidspunkt,
                grunnbeloep = grunnbeloep
            )
            BehandlingType.REVURDERING -> beregnRevurdering(
                vilkaarsvurderingUtfall = vilkaarsvurderingUtfall,
                behandling = behandling,
                grunnlag = grunnlag,
                barnepensjonGrunnlag = barnepensjonBeregningsgrunnlag,
                virkningstidspunkt = virkningstidspunkt,
                grunnbeloep = grunnbeloep
            )
            BehandlingType.MANUELT_OPPHOER -> beregnManueltOpphoer(
                behandling = behandling,
                grunnlag = grunnlag,
                barnepensjonGrunnlag = barnepensjonBeregningsgrunnlag,
                grunnbeloep = grunnbeloep
            )
        }
    }

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

    private fun beregnFoerstegangsbehandling(
        behandling: DetaljertBehandling,
        grunnlag: Grunnlag,
        barnepensjonGrunnlag: BarnepensjonGrunnlag,
        virkningstidspunkt: YearMonth,
        grunnbeloep: G
    ): Beregning {
        val resultat = kroneavrundetBarnepensjonRegel.eksekver(
            grunnlag = barnepensjonGrunnlag,
            periode = RegelPeriode(virkningstidspunkt.atDay(1))
        )

        when (resultat) {
            is RegelkjoeringResultat.Suksess -> {
                val beregningsperioder = resultat.periodiserteResultater.map { periodisertResultat ->
                    Beregningsperiode(
                        delytelsesId = "BP",
                        type = Beregningstyper.GP,
                        datoFOM = YearMonth.from(periodisertResultat.periode.fraDato),
                        datoTOM = YearMonth.from(periodisertResultat.periode.tilDato),
                        utbetaltBeloep = periodisertResultat.resultat.verdi,
                        soeskenFlokk = barnepensjonGrunnlag.soeskenKull.verdi.map { it.value },
                        grunnbelopMnd = grunnbeloep.grunnbeløpPerMåned,
                        grunnbelop = grunnbeloep.grunnbeløp,
                        trygdetid = barnepensjonGrunnlag.avdoedForelder.verdi.trygdetid.toInt()
                    )
                }

                return Beregning(
                    beregningId = UUID.randomUUID(),
                    behandlingId = behandling.id,
                    beregningsperioder = beregningsperioder,
                    beregnetDato = Tidspunkt.now(),
                    grunnlagMetadata = grunnlag.metadata
                )
            }
            is RegelkjoeringResultat.UgyldigPeriode -> {
                throw RuntimeException("Ugyldig regler for periode: ${resultat.ugyldigeReglerForPeriode}")
            }
        }
    }

    private fun beregnRevurdering(
        vilkaarsvurderingUtfall: VilkaarsvurderingUtfall,
        behandling: DetaljertBehandling,
        grunnlag: Grunnlag,
        barnepensjonGrunnlag: BarnepensjonGrunnlag,
        virkningstidspunkt: YearMonth,
        grunnbeloep: G
    ): Beregning {
        when (vilkaarsvurderingUtfall) {
            VilkaarsvurderingUtfall.OPPFYLT -> {
                val resultat = kroneavrundetBarnepensjonRegel.eksekver(
                    grunnlag = barnepensjonGrunnlag,
                    periode = RegelPeriode(virkningstidspunkt.atDay(1))
                )

                when (resultat) {
                    is RegelkjoeringResultat.Suksess -> {
                        val beregningsperioder = resultat.periodiserteResultater.map { periodisertResultat ->
                            Beregningsperiode(
                                delytelsesId = "BP",
                                type = Beregningstyper.GP,
                                datoFOM = YearMonth.from(periodisertResultat.periode.fraDato),
                                datoTOM = YearMonth.from(periodisertResultat.periode.tilDato),
                                utbetaltBeloep = periodisertResultat.resultat.verdi,
                                soeskenFlokk = barnepensjonGrunnlag.soeskenKull.verdi.map { it.value },
                                grunnbelopMnd = grunnbeloep.grunnbeløpPerMåned,
                                grunnbelop = grunnbeloep.grunnbeløp,
                                trygdetid = barnepensjonGrunnlag.avdoedForelder.verdi.trygdetid.toInt()
                            )
                        }

                        return Beregning(
                            beregningId = UUID.randomUUID(),
                            behandlingId = behandling.id,
                            beregningsperioder = beregningsperioder,
                            beregnetDato = Tidspunkt.now(),
                            grunnlagMetadata = grunnlag.metadata
                        )
                    }
                    is RegelkjoeringResultat.UgyldigPeriode -> {
                        throw RuntimeException("Ugyldig regler for periode: ${resultat.ugyldigeReglerForPeriode}")
                    }
                }
            }
            VilkaarsvurderingUtfall.IKKE_OPPFYLT -> {
                return Beregning(
                    beregningId = UUID.randomUUID(),
                    behandlingId = behandling.id,
                    beregningsperioder = listOf(
                        Beregningsperiode(
                            delytelsesId = "BP",
                            type = Beregningstyper.GP,
                            datoFOM = virkningstidspunkt,
                            datoTOM = null,
                            utbetaltBeloep = 0,
                            soeskenFlokk = emptyList(),
                            grunnbelopMnd = grunnbeloep.grunnbeløpPerMåned,
                            grunnbelop = grunnbeloep.grunnbeløp,
                            trygdetid = barnepensjonGrunnlag.avdoedForelder.verdi.trygdetid.toInt()
                        )
                    ),
                    beregnetDato = Tidspunkt.now(),
                    grunnlagMetadata = grunnlag.metadata
                )
            }
        }
    }

    private fun beregnManueltOpphoer(
        behandling: DetaljertBehandling,
        grunnlag: Grunnlag,
        barnepensjonGrunnlag: BarnepensjonGrunnlag,
        grunnbeloep: G
    ): Beregning {
        val virkningstidspunkt = virkningstidspunktFraDoedsdato(grunnlag.hentAvdoed().hentDoedsdato()?.verdi!!) // TODO
        return Beregning(
            beregningId = UUID.randomUUID(),
            behandlingId = behandling.id,
            beregningsperioder = listOf(
                Beregningsperiode(
                    delytelsesId = "BP",
                    type = Beregningstyper.GP,
                    datoFOM = virkningstidspunkt,
                    datoTOM = null,
                    utbetaltBeloep = 0,
                    soeskenFlokk = emptyList(),
                    grunnbelopMnd = grunnbeloep.grunnbeløpPerMåned,
                    grunnbelop = grunnbeloep.grunnbeløp,
                    trygdetid = barnepensjonGrunnlag.avdoedForelder.verdi.trygdetid.toInt()
                )
            ),
            beregnetDato = Tidspunkt.now(),
            grunnlagMetadata = grunnlag.metadata
        )
    }

    private fun virkningstidspunktFraDoedsdato(doedsdato: LocalDate): YearMonth = YearMonth.from(doedsdato).plusMonths(
        1
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

/*
fun lagBeregning(
    grunnlag: Grunnlag,
    virkFOM: YearMonth,
    virkTOM: YearMonth,
    vilkaarsvurderingUtfall: VilkaarsvurderingUtfall,
    behandlingType: BehandlingType,
    behandlingId: UUID
): Beregning {
    return when (behandlingType) {
        BehandlingType.FØRSTEGANGSBEHANDLING -> {
            val beregningsperioder = finnBeregningsperioder(grunnlag, virkFOM, virkTOM)
            Beregning(
                beregningId = UUID.randomUUID(),
                behandlingId = behandlingId,
                beregningsperioder = beregningsperioder,
                beregnetDato = LocalDateTime.now().toTidspunkt(norskTidssone),
                grunnlagMetadata = grunnlag.metadata
            )
        }

        BehandlingType.REVURDERING -> {
            when (vilkaarsvurderingUtfall) {
                VilkaarsvurderingUtfall.IKKE_OPPFYLT -> {
                    Beregning(
                        beregningId = UUID.randomUUID(),
                        behandlingId = behandlingId,
                        beregningsperioder = listOf(
                            Beregningsperiode(
                                delytelsesId = "BP",
                                type = Beregningstyper.GP,
                                datoFOM = virkFOM,
                                datoTOM = null,
                                utbetaltBeloep = 0,
                                soeskenFlokk = listOf(),
                                grunnbelopMnd = Grunnbeloep.hentGjeldendeG(virkFOM).grunnbeløpPerMåned,
                                grunnbelop = Grunnbeloep.hentGjeldendeG(virkFOM).grunnbeløp,
                                trygdetid = 40 // TODO: Må fikses med andresaker som IKKE har 40 års trygdetid
                            )
                        ),
                        beregnetDato = LocalDateTime.now().toTidspunkt(norskTidssone),
                        grunnlagMetadata = grunnlag.metadata
                    )
                }

                else -> {
                    val beregningsperioder = finnBeregningsperioder(grunnlag, virkFOM, virkTOM)
                    Beregning(
                        beregningId = UUID.randomUUID(),
                        behandlingId = behandlingId,
                        beregningsperioder = beregningsperioder,
                        beregnetDato = LocalDateTime.now().toTidspunkt(norskTidssone),
                        grunnlagMetadata = grunnlag.metadata
                    )
                }
            }
        }
        BehandlingType.MANUELT_OPPHOER -> {
            val datoFom = foersteVirkFraDoedsdato(grunnlag.hentAvdoed().hentDoedsdato()?.verdi!!)
            return Beregning(
                beregningId = UUID.randomUUID(),
                behandlingId = behandlingId,
                beregningsperioder = listOf(
                    Beregningsperiode(
                        delytelsesId = "BP",
                        type = Beregningstyper.GP,
                        datoFOM = datoFom,
                        datoTOM = null,
                        utbetaltBeloep = 0,
                        soeskenFlokk = listOf(),
                        grunnbelopMnd = Grunnbeloep.hentGjeldendeG(datoFom).grunnbeløpPerMåned,
                        grunnbelop = Grunnbeloep.hentGjeldendeG(datoFom).grunnbeløp,
                        trygdetid = 40 // TODO: Må fikses før vi tar imot saker som IKKE har 40 års trygdetid
                    )
                ),
                beregnetDato = LocalDateTime.now().toTidspunkt(norskTidssone),
                grunnlagMetadata = grunnlag.metadata
            )
        }
    }
}

private fun finnBeregningsperioder(
    grunnlag: Grunnlag,
    virkFOM: YearMonth,
    virkTOM: YearMonth
): List<Beregningsperiode> {
    val grunnbeloep = Grunnbeloep.hentGforPeriode(virkFOM)
    val soeskenPerioder = FinnSoeskenPeriode(grunnlag, virkFOM).hentSoeskenperioder()
    val alleFOM = (grunnbeloep.map { it.dato } + soeskenPerioder.map { it.datoFOM } + virkTOM).map {
        beregnFoersteFom(it, virkFOM)
    }.distinct().sorted().zipWithNext()
        .map { Pair(it.first, it.second.minusMonths(1)) }

    val beregningsperioder = alleFOM.mapIndexed { index, (fom, tom) ->
        val gjeldendeG = Grunnbeloep.hentGjeldendeG(fom)
        val flokkForPeriode = hentFlokkforPeriode(fom, tom, soeskenPerioder)
        val utbetaltBeloep = Soeskenjustering(flokkForPeriode.size, gjeldendeG.grunnbeløp).beloep
        val søkersFødselsdato = grunnlag.soeker.hentFoedselsdato()?.verdi

        val datoTom = if (index == alleFOM.lastIndex && søkersFødselsdato != null) {
            beregnSisteTom(søkersFødselsdato, tom)
        } else {
            tom
        }

        Beregningsperiode(
            delytelsesId = "BP",
            type = Beregningstyper.GP,
            datoFOM = fom,
            datoTOM = datoTom,
            grunnbelopMnd = gjeldendeG.grunnbeløpPerMåned,
            grunnbelop = gjeldendeG.grunnbeløp,
            soeskenFlokk = flokkForPeriode.map { it.foedselsnummer.value },
            utbetaltBeloep = utbetaltBeloep,
            trygdetid = 40 // TODO: Må fikses før vi tar imot saker som IKKE har 40 års trygdetid
        )
    }

    return beregningsperioder
}

private fun hentFlokkforPeriode(
    datoFOM: YearMonth,
    datoTOM: YearMonth,
    soeskenPeriode: List<SoeskenPeriode>
): List<Person> = soeskenPeriode.firstOrNull { it.erInklusiv(datoFOM, datoTOM) }?.soeskenFlokk ?: emptyList()

private fun beregnFoersteFom(fom: YearMonth, virkFOM: YearMonth): YearMonth =
    if (fom.isBefore(virkFOM)) virkFOM else fom


fun beregnSisteTom(fødselsdato: LocalDate, tom: YearMonth): YearMonth? {
    val fyller18YearMonth = YearMonth.from(fødselsdato).plusYears(18)
    return if (fyller18YearMonth.isAfter(tom)) null else fyller18YearMonth
}
*/