package no.nav.etterlatte.beregning

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagService
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.PeriodisertBeregningGrunnlag
import no.nav.etterlatte.beregning.grunnlag.mapVerdier
import no.nav.etterlatte.beregning.regler.barnepensjon.PeriodisertBarnepensjonGrunnlag
import no.nav.etterlatte.beregning.regler.barnepensjon.kroneavrundetBarnepensjonRegelMedInstitusjon
import no.nav.etterlatte.beregning.regler.barnepensjon.sats.grunnbeloep
import no.nav.etterlatte.beregning.regler.barnepensjon.trygdetidsfaktor.trygdetidBruktRegel
import no.nav.etterlatte.beregning.regler.finnAnvendtGrunnbeloep
import no.nav.etterlatte.beregning.regler.finnAnvendtTrygdetid
import no.nav.etterlatte.beregning.regler.toSamlet
import no.nav.etterlatte.config.BeregningFeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnbeloep.GrunnbeloepRepository
import no.nav.etterlatte.klienter.GrunnlagKlient
import no.nav.etterlatte.klienter.TrygdetidKlient
import no.nav.etterlatte.klienter.VilkaarsvurderingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.virkningstidspunkt
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.UKJENT_AVDOED
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.KonstantGrunnlag
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.RegelkjoeringResultat
import no.nav.etterlatte.libs.regler.eksekver
import no.nav.etterlatte.libs.regler.finnAnvendteRegler
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID

class BeregnBarnepensjonService(
    private val grunnlagKlient: GrunnlagKlient,
    private val vilkaarsvurderingKlient: VilkaarsvurderingKlient,
    private val grunnbeloepRepository: GrunnbeloepRepository = GrunnbeloepRepository,
    private val beregningsGrunnlagService: BeregningsGrunnlagService,
    private val trygdetidKlient: TrygdetidKlient,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(BeregnBarnepensjonService::class.java)

    suspend fun beregn(
        behandling: DetaljertBehandling,
        brukerTokenInfo: BrukerTokenInfo,
        tilDato: LocalDate? = null,
    ): Beregning {
        val grunnlag = grunnlagKlient.hentGrunnlag(behandling.id, brukerTokenInfo)
        val behandlingType = behandling.behandlingType
        val virkningstidspunkt = behandling.virkningstidspunkt().dato

        val beregningsGrunnlag =
            beregningsGrunnlagService.hentBarnepensjonBeregningsGrunnlag(behandling.id, brukerTokenInfo)
                ?: throw BeregningsgrunnlagMangler(behandling.id)

        val foreldreloesFlag = featureToggleService.isEnabled(BeregningFeatureToggle.Foreldreloes, false)

        val trygdetidListe =
            try {
                trygdetidKlient.hentTrygdetid(behandling.id, brukerTokenInfo)
            } catch (e: Exception) {
                logger.warn(
                    "Kunne ikke hente ut trygdetid for behandlingen med id=${behandling.id}. " +
                        "Dette er ikke kritisk siden vi ikke har krav om trygdetid enda.",
                )
                emptyList()
            }

        val trygdetid =
            when (foreldreloesFlag) {
                true -> {
                    // Frem til vi endre beregning til å bruke hele liste
                    trygdetidListe.firstOrNull()
                }

                false -> {
                    if (trygdetidListe.size > 1) {
                        throw ForeldreloesTrygdetid(behandling.id)
                    }

                    trygdetidListe.firstOrNull()
                }
            }

        val barnepensjonGrunnlag =
            opprettBeregningsgrunnlag(
                beregningsGrunnlag,
                trygdetid,
                virkningstidspunkt.atDay(1),
                null,
                grunnlag,
            )

        logger.info("Beregner barnepensjon for behandlingId=${behandling.id} med behandlingType=$behandlingType")

        return when (behandlingType) {
            BehandlingType.FØRSTEGANGSBEHANDLING ->
                beregnBarnepensjon(
                    behandling.id,
                    grunnlag,
                    barnepensjonGrunnlag,
                    virkningstidspunkt,
                    tilDato = tilDato,
                )

            BehandlingType.REVURDERING -> {
                val vilkaarsvurderingUtfall =
                    vilkaarsvurderingKlient.hentVilkaarsvurdering(behandling.id, brukerTokenInfo).resultat?.utfall
                        ?: throw RuntimeException("Forventa å ha resultat for behandling ${behandling.id}")

                when (vilkaarsvurderingUtfall) {
                    VilkaarsvurderingUtfall.OPPFYLT ->
                        beregnBarnepensjon(
                            behandling.id,
                            grunnlag,
                            barnepensjonGrunnlag,
                            virkningstidspunkt,
                            tilDato = tilDato,
                        )

                    VilkaarsvurderingUtfall.IKKE_OPPFYLT -> opphoer(behandling.id, grunnlag, virkningstidspunkt)
                }
            }
        }
    }

    private fun beregnBarnepensjon(
        behandlingId: UUID,
        grunnlag: Grunnlag,
        beregningsgrunnlag: PeriodisertBarnepensjonGrunnlag,
        virkningstidspunkt: YearMonth,
        kunGammeltRegelverk: Boolean = false,
        tilDato: LocalDate? = null,
    ): Beregning {
        val beregningTom =
            when (kunGammeltRegelverk) {
                true -> YearMonth.of(2023, Month.DECEMBER)
                false -> tilDato?.let { YearMonth.from(it) }
            }

        val resultat =
            kroneavrundetBarnepensjonRegelMedInstitusjon.eksekver(
                grunnlag = beregningsgrunnlag,
                periode = RegelPeriode(virkningstidspunkt.atDay(1), beregningTom?.atEndOfMonth()),
            )

        return when (resultat) {
            is RegelkjoeringResultat.Suksess -> {
                val antallPerioder = resultat.periodiserteResultater.size
                beregning(
                    behandlingId = behandlingId,
                    grunnlagMetadata = grunnlag.metadata,
                    beregningsperioder =
                        resultat.periodiserteResultater.mapIndexed { index, periodisertResultat ->
                            logger.info(
                                "Beregnet barnepensjon for periode fra={} til={} og beløp={} med regler={}",
                                periodisertResultat.periode.fraDato,
                                periodisertResultat.periode.tilDato,
                                periodisertResultat.resultat.verdi,
                                periodisertResultat.resultat.finnAnvendteRegler()
                                    .map { "${it.regelReferanse.id} (${it.beskrivelse})" }.toSet(),
                            )

                            val grunnbeloep =
                                periodisertResultat.resultat.finnAnvendtGrunnbeloep(grunnbeloep)
                                    ?: throw AnvendtGrunnbeloepIkkeFunnet()

                            val trygdetid =
                                periodisertResultat.resultat.finnAnvendtTrygdetid(trygdetidBruktRegel)
                                    ?: throw AnvendtTrygdetidIkkeFunnet()

                            val trygdetidGrunnlagForPeriode =
                                beregningsgrunnlag.avdoedesTrygdetid.finnGrunnlagForPeriode(
                                    periodisertResultat.periode.fraDato,
                                ).verdi

                            val tom = periodisertResultat.periode.tilDato?.let { YearMonth.from(it) }
                            val overstyrtTom =
                                if (index == antallPerioder - 1) {
                                    null
                                } else {
                                    tom
                                }

                            Beregningsperiode(
                                datoFOM = YearMonth.from(periodisertResultat.periode.fraDato),
                                datoTOM =
                                    when (kunGammeltRegelverk) {
                                        true -> overstyrtTom
                                        false -> tom
                                    },
                                utbetaltBeloep = periodisertResultat.resultat.verdi,
                                soeskenFlokk =
                                    beregningsgrunnlag.soeskenKull.finnGrunnlagForPeriode(
                                        periodisertResultat.periode.fraDato,
                                    ).verdi.map {
                                        it.value
                                    },
                                institusjonsopphold =
                                    beregningsgrunnlag.institusjonsopphold.finnGrunnlagForPeriode(
                                        periodisertResultat.periode.fraDato,
                                    ).verdi,
                                grunnbelopMnd = grunnbeloep.grunnbeloepPerMaaned,
                                grunnbelop = grunnbeloep.grunnbeloep,
                                trygdetid = trygdetid.trygdetid.toInteger(),
                                trygdetidForIdent =
                                    beregningsgrunnlag.avdoedesTrygdetid.finnGrunnlagForPeriode(
                                        periodisertResultat.periode.fraDato,
                                    ).verdi.ident,
                                beregningsMetode = trygdetid.beregningsMetode,
                                samletNorskTrygdetid = trygdetidGrunnlagForPeriode.samletTrygdetidNorge?.toInteger(),
                                samletTeoretiskTrygdetid = trygdetidGrunnlagForPeriode.samletTrygdetidTeoretisk?.toInteger(),
                                broek = trygdetidGrunnlagForPeriode.prorataBroek,
                                regelResultat = objectMapper.valueToTree(periodisertResultat),
                                regelVersjon = periodisertResultat.reglerVersjon,
                            )
                        },
                )
            }

            is RegelkjoeringResultat.UgyldigPeriode -> throw RuntimeException(
                "Ugyldig regler for periode: ${resultat.ugyldigeReglerForPeriode}",
            )
        }
    }

    private fun opphoer(
        behandlingId: UUID,
        grunnlag: Grunnlag,
        virkningstidspunkt: YearMonth,
    ): Beregning {
        val grunnbeloep = grunnbeloepRepository.hentGjeldendeGrunnbeloep(virkningstidspunkt)

        return beregning(
            behandlingId = behandlingId,
            grunnlagMetadata = grunnlag.metadata,
            beregningsperioder =
                listOf(
                    Beregningsperiode(
                        datoFOM = virkningstidspunkt,
                        datoTOM = null,
                        utbetaltBeloep = 0,
                        soeskenFlokk = null,
                        grunnbelopMnd = grunnbeloep.grunnbeloepPerMaaned,
                        grunnbelop = grunnbeloep.grunnbeloep,
                        trygdetid = 0,
                    ),
                ),
        )
    }

    private fun beregning(
        behandlingId: UUID,
        grunnlagMetadata: Metadata,
        beregningsperioder: List<Beregningsperiode>,
    ) = Beregning(
        beregningId = UUID.randomUUID(),
        behandlingId = behandlingId,
        type = Beregningstype.BP,
        beregningsperioder = beregningsperioder,
        beregnetDato = Tidspunkt.now(),
        grunnlagMetadata = grunnlagMetadata,
        overstyrBeregning = null,
    )

    private fun opprettBeregningsgrunnlag(
        beregningsGrunnlag: BeregningsGrunnlag,
        trygdetid: TrygdetidDto?,
        fom: LocalDate,
        tom: LocalDate?,
        grunnlag: Grunnlag,
    ) = PeriodisertBarnepensjonGrunnlag(
        soeskenKull =
            if (beregningsGrunnlag.soeskenMedIBeregning.isNotEmpty()) {
                PeriodisertBeregningGrunnlag.lagKomplettPeriodisertGrunnlag(
                    beregningsGrunnlag.soeskenMedIBeregning.mapVerdier { soeskenMedIBeregning ->
                        FaktumNode(
                            verdi = soeskenMedIBeregning.filter { it.skalBrukes }.map { it.foedselsnummer },
                            kilde = beregningsGrunnlag.kilde,
                            beskrivelse = "Søsken i kullet",
                        )
                    },
                    fom,
                    tom,
                )
            } else {
                KonstantGrunnlag(FaktumNode(emptyList(), beregningsGrunnlag.kilde, "Ingen søsken i kullet"))
            },
        avdoedesTrygdetid =
            trygdetid?.toSamlet(beregningsGrunnlag.beregningsMetode.beregningsMetode)?.let {
                KonstantGrunnlag(
                    FaktumNode(
                        verdi = it,
                        kilde = beregningsGrunnlag.kilde,
                        beskrivelse = "Trygdetid avdød forelder",
                    ),
                )
            } ?: throw UgyldigForespoerselException(
                code = "MÅ_FASTSETTE_TRYGDETID",
                detail = "Mangler trygdetid, gå tilbake til trygdetidsiden for å opprette dette",
            ),
        institusjonsopphold =
            PeriodisertBeregningGrunnlag.lagPotensieltTomtGrunnlagMedDefaultUtenforPerioder(
                beregningsGrunnlag.institusjonsoppholdBeregningsgrunnlag.mapVerdier { institusjonsopphold ->
                    FaktumNode(
                        verdi = institusjonsopphold,
                        kilde = beregningsGrunnlag.kilde,
                        beskrivelse = "Institusjonsopphold",
                    )
                },
            ) { _, _, _ -> FaktumNode(null, beregningsGrunnlag.kilde, "Institusjonsopphold") },
        avdoedeForeldre =
            when (trygdetid.ident) {
                UKJENT_AVDOED ->
                    KonstantGrunnlag(
                        FaktumNode(
                            verdi = emptyList(),
                            kilde = beregningsGrunnlag.kilde,
                            beskrivelse = "Avdød er ukjent. Trygdetid er satt manuelt.",
                        ),
                    )

                else -> {
                    if (grunnlag.hentAvdoede().any { it.hentDoedsdato() == null }) {
                        KonstantGrunnlag(
                            FaktumNode(
                                verdi = emptyList(),
                                kilde = beregningsGrunnlag.kilde,
                                beskrivelse = "Avdød mangler dødsdato. Trygdetid skal være satt manuelt eller overstyrt.",
                            ),
                        )
                    } else {
                        PeriodisertBeregningGrunnlag.lagKomplettPeriodisertGrunnlag(
                            grunnlag.hentAvdoede().toPeriodisertAvdoedeGrunnlag().mapVerdier { fnrListe ->
                                FaktumNode(
                                    verdi = fnrListe,
                                    kilde = beregningsGrunnlag.kilde,
                                    beskrivelse = "Hvilke foreldre er døde",
                                )
                            },
                            fom,
                            tom,
                        )
                    }
                }
            },
    )

    private fun List<Grunnlagsdata<JsonNode>>.toPeriodisertAvdoedeGrunnlag(): List<GrunnlagMedPeriode<List<Folkeregisteridentifikator>>> {
        val doede = mutableListOf<Folkeregisteridentifikator>()
        val resultat = mutableListOf<GrunnlagMedPeriode<List<Folkeregisteridentifikator>>>()

        val avdoedeSortert =
            this.distinctBy { it.hentFoedselsnummer() }
                .sortedBy { it.hentDoedsdato()!!.verdi!! }
        val iterator = avdoedeSortert.listIterator()
        while (iterator.hasNext()) {
            val dennePeriode = iterator.next()
            doede.add(Folkeregisteridentifikator.of(dennePeriode.hentFoedselsnummer()!!.verdi.value))

            val nestePeriode = if (iterator.hasNext()) avdoedeSortert[iterator.nextIndex()] else null
            val datoFOM = virkAvDoedsfall(dennePeriode.hentDoedsdato()!!.verdi)!!
            val datoTOM = virkAvDoedsfall(nestePeriode?.hentDoedsdato()?.verdi)?.minusDays(1)

            resultat.add(GrunnlagMedPeriode(doede.toList(), datoFOM, datoTOM))
        }
        return resultat
    }

    private fun virkAvDoedsfall(dato: LocalDate?) = dato?.plusMonths(1)?.withDayOfMonth(1)
}
