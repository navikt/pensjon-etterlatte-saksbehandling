package no.nav.etterlatte.model

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import model.finnSoeskenperiode.FinnSoeskenPeriode
import no.nav.etterlatte.BeregningRepository
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstyper
import no.nav.etterlatte.libs.common.beregning.SoeskenPeriode
import no.nav.etterlatte.libs.common.beregning.erInklusiv
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.tidspunkt.norskTidssone
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.model.behandling.BehandlingKlient
import no.nav.etterlatte.model.grunnlag.GrunnlagKlient
import rapidsandrivers.vedlikehold.VedlikeholdService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class BeregningService(
    private val beregningRepository: BeregningRepository,
    private val vilkaarsvurderingKlient: VilkaarsvurderingKlient,
    private val grunnlagKlient: GrunnlagKlient,
    private val behandlingKlient: BehandlingKlient
) : VedlikeholdService {

    fun hentBeregning(behandlingId: UUID): Beregning? = beregningRepository.hent(behandlingId)

    suspend fun lagreBeregning(behandlingId: UUID, accessToken: String): Beregning {
        return tilstandssjekkFoerKjoerning(behandlingId, accessToken) {
            coroutineScope {
                val vilkaarsvurdering =
                    async { vilkaarsvurderingKlient.hentVilkaarsvurdering(behandlingId, accessToken) }
                val behandling = async { behandlingKlient.hentBehandling(behandlingId, accessToken) }
                val grunnlag = async { grunnlagKlient.hentGrunnlag(behandling.await().sak, accessToken) }

                val beregning = lagBeregning(
                    grunnlag.await(),
                    behandling.await().virkningstidspunkt!!.dato,
                    YearMonth.now().plusMonths(3),
                    vilkaarsvurdering.await().resultat!!.utfall,
                    behandling.await().behandlingType!!,
                    behandlingId
                )
                beregningRepository.lagreEllerOppdaterBeregning(beregning).also {
                    behandlingKlient.beregn(behandlingId, accessToken, true)
                }
            }
        }
    }

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

    private fun foersteVirkFraDoedsdato(dødsdato: LocalDate): YearMonth = YearMonth.from(dødsdato).plusMonths(1)

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

    override fun slettSak(sakId: Long) {
        beregningRepository.slettBeregningsperioderISak(sakId)
    }
}

fun beregnSisteTom(fødselsdato: LocalDate, tom: YearMonth): YearMonth? {
    val fyller18YearMonth = YearMonth.from(fødselsdato).plusYears(18)
    return if (fyller18YearMonth.isAfter(tom)) null else fyller18YearMonth
}