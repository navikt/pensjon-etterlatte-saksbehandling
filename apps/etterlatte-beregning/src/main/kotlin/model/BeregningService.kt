package no.nav.etterlatte.model

import model.finnSoeskenperiode.FinnSoeskenPeriode
import no.nav.etterlatte.BeregningRepository
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstyper
import no.nav.etterlatte.libs.common.beregning.SoeskenPeriode
import no.nav.etterlatte.libs.common.beregning.erInklusiv
import no.nav.etterlatte.libs.common.event.BehandlingGrunnlagEndret
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.tidspunkt.norskTidssone
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.model.behandling.BehandlingKlient
import no.nav.etterlatte.model.grunnlag.GrunnlagKlient
import no.nav.helse.rapids_rivers.JsonMessage
import rapidsandrivers.vedlikehold.VedlikeholdService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

class BeregningService(
    private val beregningRepository: BeregningRepository,
    private val vilkaarsvurderingKlient: VilkaarsvurderingKlient,
    private val grunnlagKlient: GrunnlagKlient,
    private val behandlingKlient: BehandlingKlient,
    private val sendToRapid: (String, UUID) -> Unit
) : VedlikeholdService {

    fun hentBeregning(behandlingId: UUID): Beregning = beregningRepository.hent(behandlingId)

    suspend fun lagreBeregning(behandlingId: UUID, accessToken: String): Beregning {
        val vilkaarsvurdering = vilkaarsvurderingKlient.hentVilkaarsvurdering(behandlingId, accessToken)
        val behandling = behandlingKlient.hentBehandling(behandlingId, accessToken)
        val grunnlag = grunnlagKlient.hentGrunnlag(behandling.sak, accessToken)

        val beregning = lagBeregning(
            grunnlag,
            behandling.virkningstidspunkt!!.dato,
            YearMonth.now().plusMonths(3),
            vilkaarsvurdering.resultat.utfall,
            behandling.behandlingType!!,
            behandlingId
        )

        val lagretBeregning = beregningRepository.lagreEllerOppdaterBeregning(beregning)
        val message = JsonMessage.newMessage(BehandlingGrunnlagEndret.eventName)
            .apply {
                this["vilkaarsvurdering"] = vilkaarsvurdering
                this["beregning"] = lagretBeregning.toDTO()
            }
            .apply { // trengs av lagreberegning i vedtak + beregning, fjerne?
                this["sakId"] = behandling.sak
                this["fnrSoeker"] = behandling.soeker!!
                this["behandling"] = Behandling(behandling.behandlingType!!, behandling.id)
            }
        sendToRapid(message.toJson(), behandling.id)
        return lagretBeregning
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
                                    grunnbelop = Grunnbeloep.hentGjeldendeG(virkFOM).grunnbeløp
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
                            grunnbelop = Grunnbeloep.hentGjeldendeG(datoFom).grunnbeløp
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
                soeskenFlokk = flokkForPeriode,
                utbetaltBeloep = utbetaltBeloep
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

    override fun slettSak(sakId: Long) {
        beregningRepository.slettBeregningsperioderISak(sakId)
    }
}

fun beregnSisteTom(fødselsdato: LocalDate, tom: YearMonth): YearMonth? {
    val fyller18YearMonth = YearMonth.from(fødselsdato).plusYears(18)
    return if (fyller18YearMonth.isAfter(tom)) null else fyller18YearMonth
}