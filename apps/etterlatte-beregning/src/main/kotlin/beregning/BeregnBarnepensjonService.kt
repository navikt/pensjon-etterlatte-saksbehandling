package no.nav.etterlatte.beregning

import beregning.regler.finnAnvendtGrunnbeloep
import beregning.regler.finnAnvendtTrygdetid
import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.beregning.BeregnBarnepensjonServiceFeatureToggle.BrukInstitusjonsoppholdIBeregning
import no.nav.etterlatte.beregning.BeregnBarnepensjonServiceFeatureToggle.BrukNyttRegelverkIBeregning
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagService
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.PeriodisertBeregningGrunnlag
import no.nav.etterlatte.beregning.grunnlag.kombinerOverlappendePerioder
import no.nav.etterlatte.beregning.grunnlag.mapVerdier
import no.nav.etterlatte.beregning.regler.AnvendtTrygdetid
import no.nav.etterlatte.beregning.regler.barnepensjon.PeriodisertBarnepensjonGrunnlag
import no.nav.etterlatte.beregning.regler.barnepensjon.kroneavrundetBarnepensjonRegel
import no.nav.etterlatte.beregning.regler.barnepensjon.kroneavrundetBarnepensjonRegelMedInstitusjon
import no.nav.etterlatte.beregning.regler.barnepensjon.sats.aktuelleBarnepensjonSatsRegler
import no.nav.etterlatte.beregning.regler.barnepensjon.sats.barnepensjonSatsRegel1967
import no.nav.etterlatte.beregning.regler.barnepensjon.sats.barnepensjonSatsRegel2024
import no.nav.etterlatte.beregning.regler.barnepensjon.sats.grunnbeloep
import no.nav.etterlatte.beregning.regler.barnepensjon.trygdetidsfaktor.TrygdetidGrunnlag
import no.nav.etterlatte.beregning.regler.barnepensjon.trygdetidsfaktor.anvendtTrygdetidRegel
import no.nav.etterlatte.beregning.regler.barnepensjon.trygdetidsfaktor.trygdetidBruktRegel
import no.nav.etterlatte.beregning.regler.toSamlet
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnbeloep.GrunnbeloepRepository
import no.nav.etterlatte.klienter.GrunnlagKlient
import no.nav.etterlatte.klienter.TrygdetidKlient
import no.nav.etterlatte.klienter.VilkaarsvurderingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.KonstantGrunnlag
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.RegelkjoeringResultat
import no.nav.etterlatte.libs.regler.eksekver
import no.nav.etterlatte.libs.regler.finnAnvendteRegler
import no.nav.etterlatte.regler.Beregningstall
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

enum class BeregnBarnepensjonServiceFeatureToggle(private val key: String) : FeatureToggle {
    BrukFaktiskTrygdetid("pensjon-etterlatte.bp-bruk-faktisk-trygdetid"),
    BrukInstitusjonsoppholdIBeregning("pensjon-etterlatte.bp-bruk-institusjonsopphold-i-beregning"),
    BrukNyttRegelverkIBeregning("pensjon-etterlatte.bp-bruk-nytt-regelverk-i-beregning"),
    ;

    override fun key() = key
}

class BeregnBarnepensjonService(
    private val grunnlagKlient: GrunnlagKlient,
    private val vilkaarsvurderingKlient: VilkaarsvurderingKlient,
    private val grunnbeloepRepository: GrunnbeloepRepository = GrunnbeloepRepository,
    private val beregningsGrunnlagService: BeregningsGrunnlagService,
    private val trygdetidKlient: TrygdetidKlient,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(BeregnBarnepensjonService::class.java)

    private val nyttRegelverkAktivert = featureToggleService.isEnabled(BrukNyttRegelverkIBeregning, false)

    init {
        aktuelleBarnepensjonSatsRegler.clear()
        aktuelleBarnepensjonSatsRegler.add(barnepensjonSatsRegel1967)
        if (nyttRegelverkAktivert) {
            aktuelleBarnepensjonSatsRegler.add(barnepensjonSatsRegel2024)
        }
    }

    suspend fun beregn(
        behandling: DetaljertBehandling,
        brukerTokenInfo: BrukerTokenInfo,
    ): Beregning {
        val grunnlag = grunnlagKlient.hentGrunnlag(behandling.sak, behandling.id, brukerTokenInfo)
        val behandlingType = behandling.behandlingType
        val virkningstidspunkt =
            requireNotNull(behandling.virkningstidspunkt?.dato) { "Behandling ${behandling.id} mangler virkningstidspunkt" }

        val beregningsGrunnlag =
            requireNotNull(
                beregningsGrunnlagService.hentBarnepensjonBeregningsGrunnlag(behandling.id, brukerTokenInfo),
            ) { "Behandling ${behandling.id} mangler beregningsgrunnlag" }

        val trygdetid =
            if (featureToggleService.isEnabled(BeregnBarnepensjonServiceFeatureToggle.BrukFaktiskTrygdetid, false)) {
                try {
                    trygdetidKlient.hentTrygdetid(behandling.id, brukerTokenInfo)
                } catch (e: Exception) {
                    logger.warn(
                        "Kunne ikke hente ut trygdetid for behandlingen med id=${behandling.id}. " +
                            "Dette er ikke kritisk siden vi ikke har krav om trygdetid enda.",
                    )
                    null
                }
            } else {
                null
            }

        val anvendtTrygdetidPerioder =
            trygdetid?.let { finnAnvendtTrygdetidPerioder(virkningstidspunkt, it, beregningsGrunnlag) }
                ?: listOf(
                    GrunnlagMedPeriode(
                        data =
                            listOf(
                                AnvendtTrygdetid(
                                    beregningsMetode = BeregningsMetode.NASJONAL,
                                    trygdetid = Beregningstall(FASTSATT_TRYGDETID_I_PILOT),
                                    avdoed = grunnlag.hentAvdoed().hentFoedselsnummer()?.verdi?.value,
                                ),
                            ),
                        fom = virkningstidspunkt.atDay(1), tom = null,
                    ),
                )

        val barnepensjonGrunnlag =
            opprettBeregningsgrunnlag(
                beregningsGrunnlag,
                anvendtTrygdetidPerioder,
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
                        )

                    VilkaarsvurderingUtfall.IKKE_OPPFYLT -> opphoer(behandling.id, grunnlag, virkningstidspunkt)
                }
            }

            BehandlingType.MANUELT_OPPHOER -> opphoer(behandling.id, grunnlag, virkningstidspunkt)
        }
    }

    private fun finnAnvendtTrygdetidPerioder(
        virkningstidspunkt: YearMonth,
        trygdetider: List<TrygdetidDto>,
        beregningsGrunnlag: BeregningsGrunnlag,
    ): List<GrunnlagMedPeriode<List<AnvendtTrygdetid>>> {
        // modellere dette mer med klasser som "pakker inn" jobben?
        // lol koble sammen greier her?
        val sammenkoblet =
            beregningsGrunnlag.avdoedeBeregningmetode.map { beregningsmetodeForAvdoedPeriode ->
                val data = beregningsmetodeForAvdoedPeriode.data
                val avdoed = data.avdoed
                val beregningsMetode = data.beregningsMetode.beregningsMetode

                val trygdetidForAvdoed =
                    trygdetider.find { it.ident == beregningsmetodeForAvdoedPeriode.data.avdoed }
                        ?: throw InternfeilException("Whoops, vi klarer ikke å koble sammen")
                val kobletSammen =
                    requireNotNull(trygdetidForAvdoed.beregnetTrygdetid?.resultat?.toSamlet(beregningsMetode, avdoed)) {
                        "Vi har ikke beregnet trygdetid for avdød"
                    }
                return@map GrunnlagMedPeriode(
                    data = kobletSammen,
                    fom = beregningsmetodeForAvdoedPeriode.fom,
                    tom = beregningsmetodeForAvdoedPeriode.tom,
                )
            }

        val periodisertTrygdetider =
            sammenkoblet.map {
                anvendtTrygdetidRegel.eksekver(
                    KonstantGrunnlag(
                        TrygdetidGrunnlag(
                            FaktumNode(it.data, kilde = "Trygdetid", beskrivelse = "Beregnet trygdetid for avdød"),
                        ),
                    ),
                    RegelPeriode(it.fom, it.tom),
                )
            }.map {
                when (it) {
                    is RegelkjoeringResultat.Suksess -> {
                        val aktueltResultat = it.periodiserteResultater.single()
                        GrunnlagMedPeriode(
                            data = aktueltResultat.resultat.verdi,
                            fom = aktueltResultat.periode.fraDato,
                            tom = aktueltResultat.periode.tilDato,
                        )
                    }

                    is RegelkjoeringResultat.UgyldigPeriode -> throw InternfeilException("Oida")
                }
            }
        return periodisertTrygdetider.kombinerOverlappendePerioder()
    }

    private fun beregnBarnepensjon(
        behandlingId: UUID,
        grunnlag: Grunnlag,
        beregningsgrunnlag: PeriodisertBarnepensjonGrunnlag,
        virkningstidspunkt: YearMonth,
    ): Beregning {
        val resultat =
            if (featureToggleService.isEnabled(BrukInstitusjonsoppholdIBeregning, false)) {
                kroneavrundetBarnepensjonRegelMedInstitusjon.eksekver(
                    grunnlag = beregningsgrunnlag,
                    periode = RegelPeriode(virkningstidspunkt.atDay(1)),
                )
            } else {
                kroneavrundetBarnepensjonRegel.eksekver(
                    grunnlag = beregningsgrunnlag,
                    periode = RegelPeriode(virkningstidspunkt.atDay(1)),
                )
            }

        return when (resultat) {
            is RegelkjoeringResultat.Suksess ->
                beregning(
                    behandlingId = behandlingId,
                    grunnlagMetadata = grunnlag.metadata,
                    beregningsperioder =
                        resultat.periodiserteResultater.map { periodisertResultat ->
                            logger.info(
                                "Beregnet barnepensjon for periode fra={} til={} og beløp={} med regler={}",
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

                            val trygdetid =
                                requireNotNull(periodisertResultat.resultat.finnAnvendtTrygdetid(trygdetidBruktRegel)) {
                                    "Anvendt trygdetid ikke funnet for perioden"
                                }

//                        val trygdetidGrunnlagForPeriode =
//                            beregningsgrunnlag.avdoedesTrygdetid.finnGrunnlagForPeriode(
//                                periodisertResultat.periode.fraDato,
//                            ).verdi

                            Beregningsperiode(
                                datoFOM = YearMonth.from(periodisertResultat.periode.fraDato),
                                datoTOM = periodisertResultat.periode.tilDato?.let { YearMonth.from(it) },
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
                                beregningsMetode = trygdetid.beregningsMetode,
                                // TODO FIX DISSE
//                            samletNorskTrygdetid = trygdetidGrunnlagForPeriode.samletTrygdetidNorge?.toInteger(),
//                            samletTeoretiskTrygdetid = trygdetidGrunnlagForPeriode.samletTrygdetidTeoretisk?.toInteger(),
//                            broek = trygdetidGrunnlagForPeriode.prorataBroek,
                                regelResultat = objectMapper.valueToTree(periodisertResultat),
                                regelVersjon = periodisertResultat.reglerVersjon,
                            )
                        },
                )

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
    )

    private fun opprettBeregningsgrunnlag(
        beregningsGrunnlag: BeregningsGrunnlag,
        trygdetid: List<GrunnlagMedPeriode<List<AnvendtTrygdetid>>>?,
        fom: LocalDate,
        tom: LocalDate?,
        grunnlag: Grunnlag,
    ) = PeriodisertBarnepensjonGrunnlag(
        soeskenKull =
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
            ),
        avdoedesTrygdetid =
            trygdetid?.let {
                PeriodisertBeregningGrunnlag.lagKomplettPeriodisertGrunnlag(
                    trygdetid.mapVerdier { anvendtTrygdetider ->
                        FaktumNode(
                            verdi = anvendtTrygdetider,
                            kilde = beregningsGrunnlag.kilde,
                            beskrivelse = "Anvendte trygdetider",
                        )
                    },
                    fom,
                    tom,
                )
            } ?: KonstantGrunnlag(
                FaktumNode(
                    verdi =
                        listOf(
                            AnvendtTrygdetid(
                                beregningsMetode = BeregningsMetode.NASJONAL,
                                trygdetid =
                                    Beregningstall(
                                        FASTSATT_TRYGDETID_I_PILOT,
                                    ),
                                avdoed = null, // TODO
                            ),
                        ),
                    kilde = Grunnlagsopplysning.RegelKilde("MVP hardkodet trygdetid", Tidspunkt.now(), "1"),
                    beskrivelse = "Trygdetid avdød forelder",
                ),
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
        brukNyttRegelverk = featureToggleService.isEnabled(BrukNyttRegelverkIBeregning, false),
    )

    companion object {
        private const val FASTSATT_TRYGDETID_I_PILOT = 40
    }

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
