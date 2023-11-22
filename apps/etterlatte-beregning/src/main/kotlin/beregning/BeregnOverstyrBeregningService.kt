package no.nav.etterlatte.beregning

import beregning.regler.finnAnvendtGrunnbeloep
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagService
import no.nav.etterlatte.beregning.grunnlag.OverstyrBeregningGrunnlag
import no.nav.etterlatte.beregning.grunnlag.PeriodisertBeregningGrunnlag
import no.nav.etterlatte.beregning.grunnlag.mapVerdier
import no.nav.etterlatte.beregning.regler.overstyr.PeriodisertOverstyrGrunnlag
import no.nav.etterlatte.beregning.regler.overstyr.beregnOverstyrRegel
import no.nav.etterlatte.beregning.regler.overstyr.grunnbeloep
import no.nav.etterlatte.klienter.GrunnlagKlient
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.RegelkjoeringResultat
import no.nav.etterlatte.libs.regler.eksekver
import no.nav.etterlatte.libs.regler.finnAnvendteRegler
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class BeregnOverstyrBeregningService(
    private val grunnlagKlient: GrunnlagKlient,
    private val beregningsGrunnlagService: BeregningsGrunnlagService,
) {
    private val logger = LoggerFactory.getLogger(BeregnOverstyrBeregningService::class.java)

    suspend fun beregn(
        behandling: DetaljertBehandling,
        overstyrBeregning: OverstyrBeregning,
        brukerTokenInfo: BrukerTokenInfo,
    ): Beregning {
        val grunnlag = grunnlagKlient.hentGrunnlag(behandling.id, brukerTokenInfo)
        val behandlingType = behandling.behandlingType
        val virkningstidspunkt =
            requireNotNull(behandling.virkningstidspunkt?.dato) { "Behandling ${behandling.id} mangler virkningstidspunkt" }

        val beregningsGrunnlag =
            requireNotNull(
                beregningsGrunnlagService.hentOverstyrBeregningGrunnlag(behandling.id),
            ) { "Behandling ${behandling.id} mangler overstyr beregningsgrunnlag" }

        val beregningsType =
            when (behandling.sakType) {
                SakType.BARNEPENSJON -> Beregningstype.BP
                SakType.OMSTILLINGSSTOENAD -> Beregningstype.OMS
            }

        val overstyrGrunnlag =
            opprettOverstyrGrunnlag(
                beregningsGrunnlag,
                virkningstidspunkt.atDay(1),
            )

        return when (behandlingType) {
            BehandlingType.FØRSTEGANGSBEHANDLING ->
                beregnOverstyr(
                    behandlingId = behandling.id,
                    beregningsType = beregningsType,
                    grunnlag = grunnlag,
                    beregningsGrunnlag = overstyrGrunnlag,
                    virkningstidspunkt = virkningstidspunkt,
                    overstyrBeregning = overstyrBeregning,
                )

            else ->
                throw UnsupportedOperationException("Overstyrt beregning er ikke støttet for $behandlingType for ${behandling.id}")
        }
    }

    private fun opprettOverstyrGrunnlag(
        beregningsGrunnlag: OverstyrBeregningGrunnlag,
        fom: LocalDate,
    ) = PeriodisertOverstyrGrunnlag(
        overstyrGrunnlag =
            PeriodisertBeregningGrunnlag.lagKomplettPeriodisertGrunnlag(
                perioder =
                    beregningsGrunnlag.perioder.mapVerdier { overstyrBeregningGrunnlag ->
                        FaktumNode(
                            verdi = overstyrBeregningGrunnlag,
                            kilde = beregningsGrunnlag.kilde,
                            beskrivelse = "Overstyr beregning grunnlag",
                        )
                    },
                fom = fom,
                tom = null,
            ),
    )

    private fun beregnOverstyr(
        behandlingId: UUID,
        beregningsType: Beregningstype,
        grunnlag: Grunnlag,
        beregningsGrunnlag: PeriodisertOverstyrGrunnlag,
        virkningstidspunkt: YearMonth,
        overstyrBeregning: OverstyrBeregning,
    ): Beregning {
        val resultat =
            beregnOverstyrRegel.eksekver(
                grunnlag = beregningsGrunnlag,
                periode = RegelPeriode(virkningstidspunkt.atDay(1)),
            )

        val beregnetDato = Tidspunkt.now()

        return when (resultat) {
            is RegelkjoeringResultat.Suksess ->
                Beregning(
                    beregningId = UUID.randomUUID(),
                    behandlingId = behandlingId,
                    type = beregningsType,
                    beregnetDato = beregnetDato,
                    grunnlagMetadata = grunnlag.metadata,
                    beregningsperioder =
                        resultat.periodiserteResultater.map { periodisertResultat ->
                            logger.info(
                                "Beregnet overstyr for periode fra={} til={} og beløp={} med regler={}",
                                periodisertResultat.periode.fraDato,
                                periodisertResultat.periode.tilDato,
                                periodisertResultat.resultat.verdi,
                                periodisertResultat.resultat.finnAnvendteRegler()
                                    .map { "${it.regelReferanse.id} (${it.beskrivelse})" }.toSet(),
                            )

                            val grunnbeloep =
                                requireNotNull(periodisertResultat.resultat.finnAnvendtGrunnbeloep(grunnbeloep)) {
                                    "Anvendt grunnbeløp ikke funnet for perioden"
                                }

                            val broek =
                                IntBroek.fra(
                                    Pair(
                                        periodisertResultat.resultat.verdi.prorataBroekTeller?.toInt(),
                                        periodisertResultat.resultat.verdi.prorataBroekNevner?.toInt(),
                                    ),
                                )

                            val broekVerdi = broek?.let { it.teller.toDouble() / it.nevner.toDouble() } ?: 1.0

                            val trygdetid = (periodisertResultat.resultat.verdi.trygdetid * broekVerdi).toInt()

                            Beregningsperiode(
                                datoFOM = YearMonth.from(periodisertResultat.periode.fraDato),
                                datoTOM = periodisertResultat.periode.tilDato?.let { YearMonth.from(it) },
                                utbetaltBeloep = periodisertResultat.resultat.verdi.utbetaltBeloep.toInt(),
                                institusjonsopphold = null,
                                grunnbelopMnd = grunnbeloep.grunnbeloepPerMaaned,
                                grunnbelop = grunnbeloep.grunnbeloep,
                                trygdetid = trygdetid,
                                beregningsMetode =
                                    when (broek) {
                                        null -> BeregningsMetode.NASJONAL
                                        else -> BeregningsMetode.PRORATA
                                    },
                                samletNorskTrygdetid =
                                    periodisertResultat.resultat.verdi.trygdetid.takeIf { broek == null }
                                        ?.toInt(),
                                samletTeoretiskTrygdetid =
                                    periodisertResultat.resultat.verdi.trygdetid.takeIf { broek != null }
                                        ?.toInt(),
                                broek = broek,
                                regelResultat = objectMapper.valueToTree(periodisertResultat),
                                regelVersjon = periodisertResultat.reglerVersjon,
                                trygdetidForIdent = null,
                                kilde =
                                    Grunnlagsopplysning.RegelKilde(
                                        navn = beregnOverstyrRegel.regelReferanse.id,
                                        ts = beregnetDato,
                                        versjon = periodisertResultat.reglerVersjon,
                                    ),
                            )
                        },
                    overstyrBeregning = overstyrBeregning,
                )

            is RegelkjoeringResultat.UgyldigPeriode -> throw RuntimeException(
                "Ugyldig regler for periode: ${resultat.ugyldigeReglerForPeriode}",
            )
        }
    }
}
