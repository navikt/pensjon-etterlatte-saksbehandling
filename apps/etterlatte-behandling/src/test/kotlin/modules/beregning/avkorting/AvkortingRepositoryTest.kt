package no.nav.etterlatte.modules.beregning.avkorting

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.libs.common.beregning.InntektsjusteringAvkortingInfoRequest
import no.nav.etterlatte.libs.common.beregning.Sanksjon
import no.nav.etterlatte.libs.common.beregning.SanksjonType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.modules.beregning.aarsoppgjoer
import no.nav.etterlatte.modules.beregning.avkortetYtelse
import no.nav.etterlatte.modules.beregning.avkortinggrunnlag
import no.nav.etterlatte.modules.beregning.avkortingsperiode
import no.nav.etterlatte.modules.beregning.regler.avkortinggrunnlagLagreDto
import no.nav.etterlatte.modules.beregning.regler.beregning
import no.nav.etterlatte.modules.beregning.regler.beregningsperiode
import no.nav.etterlatte.modules.beregning.regler.bruker
import no.nav.etterlatte.modules.beregning.regler.lagreSanksjon
import no.nav.etterlatte.modules.beregning.sanksjon.SanksjonRepository
import no.nav.etterlatte.modules.beregning.ytelseFoerAvkorting
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AvkortingRepositoryTest(
    ds: DataSource,
) {
    private val avkortingRepository = AvkortingRepository(ds)
    private val sanksjonRepository = SanksjonRepository(ds)

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `bevarer beregnet avkorting med flere aar restanse og sanksjoner ved lagring`(brukNyeReglerAvkorting: Boolean) {
        val behandlingId = UUID.randomUUID()
        val sakId = randomSakId()
        val sanksjoner = opprettSanksjoner(behandlingId, sakId)
        val beregnetAvkorting = beregnAvkorting(sanksjoner, brukNyeReglerAvkorting)
        val dtoFoerLagring = beregnetAvkorting.toDto()

        dtoFoerLagring.avkortingGrunnlag.size shouldBe 4
        dtoFoerLagring.avkortetYtelse.any { it.restanse != 0 } shouldBe true
        dtoFoerLagring.avkortetYtelse.mapNotNull { it.sanksjon?.sanksjonType }.toSet() shouldBe
            setOf(SanksjonType.STANS, SanksjonType.BORTFALL)

        avkortingRepository.lagreAvkorting(behandlingId, sakId, beregnetAvkorting)

        val lagretAvkorting = requireNotNull(avkortingRepository.hentAvkorting(behandlingId))
        lagretAvkorting.toDto() shouldBe dtoFoerLagring
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `bevarer beregnet etteroppgjoer og faktisk inntekt ved lagring`(brukNyeReglerAvkorting: Boolean) {
        val forbehandlingId = UUID.randomUUID()
        val sakId = randomSakId()
        val sanksjoner = opprettSanksjoner(forbehandlingId, sakId)
        val avkorting = beregnAvkorting(sanksjoner, brukNyeReglerAvkorting)
        val beregnetAvkorting =
            Avkorting(listOf(avkorting.aarsoppgjoer.first())).beregnEtteroppgjoer(
                brukerTokenInfo = bruker,
                aar = 2024,
                loennsinntekt = 350000,
                afp = 10000,
                naeringsinntekt = 20000,
                utland = 30000,
                sanksjoner = sanksjoner,
                spesifikasjon = "Faktisk inntekt",
                innvilgetPeriodeIEtteroppgjoersAar = Periode(YearMonth.of(2024, 1), YearMonth.of(2024, 12)),
                opphoerFom = null,
                brukNyeReglerAvkorting = brukNyeReglerAvkorting,
            )
        val etteroppgjoer = beregnetAvkorting.aarsoppgjoer.single().shouldBeInstanceOf<Etteroppgjoer>()

        avkortingRepository.lagreAvkorting(forbehandlingId, sakId, beregnetAvkorting)

        val lagretAvkorting = requireNotNull(avkortingRepository.hentAvkorting(forbehandlingId))
        val lagretInntekt = requireNotNull(avkortingRepository.hentFaktiskInntekt(etteroppgjoer.id))
        lagretAvkorting.toDto() shouldBe beregnetAvkorting.toDto()
        lagretInntekt.toDto() shouldBe etteroppgjoer.inntekt.toDto()
        lagretInntekt.maanederInnvilget shouldBe etteroppgjoer.inntekt.maanederInnvilget
    }

    @Test
    fun `skal returnere true hvis bruker har inntekt for aar`() {
        val behandlingId: UUID = UUID.randomUUID()
        val aarsoppgjoer = nyAvkorting(2024)
        val sakId = randomSakId()

        avkortingRepository.harSakInntektForAar(
            InntektsjusteringAvkortingInfoRequest(
                sakId,
                aarsoppgjoer.aar,
                sisteBehandling = UUID.randomUUID(),
            ),
        ) shouldBe false
        avkortingRepository.lagreAvkorting(
            behandlingId,
            sakId,
            Avkorting(
                aarsoppgjoer = listOf(aarsoppgjoer),
            ),
        )

        avkortingRepository.harSakInntektForAar(
            InntektsjusteringAvkortingInfoRequest(
                sakId,
                aarsoppgjoer.aar,
                sisteBehandling = behandlingId,
            ),
        ) shouldBe true
    }

    @Test
    fun `skal returnere null hvis mangler avkorting`() {
        avkortingRepository.hentAvkorting(UUID.randomUUID()) shouldBe null
    }

    @Test
    fun `Skal lagre og oppdatere avkorting`() {
        val behandlingId: UUID = UUID.randomUUID()
        val aarsoppgjoer = nyAvkorting(2024)
        val sakId = randomSakId()

        avkortingRepository.lagreAvkorting(
            behandlingId,
            sakId,
            Avkorting(
                aarsoppgjoer = listOf(aarsoppgjoer),
            ),
        )

        val endretAarsoppgjoer =
            aarsoppgjoer(
                ytelseFoerAvkorting = aarsoppgjoer.ytelseFoerAvkorting.map { it.copy(beregning = 333) },
                inntektsavkorting =
                    aarsoppgjoer.inntektsavkorting.map { inntektsavkorting ->
                        Inntektsavkorting(
                            grunnlag = inntektsavkorting.grunnlag.copy(spesifikasjon = "Endret"),
                            avkortingsperioder = inntektsavkorting.avkortingsperioder.map { it.copy(avkorting = 333) },
                            avkortetYtelseForventetInntekt =
                                inntektsavkorting.avkortetYtelseForventetInntekt.map {
                                    it.copy(
                                        avkortingsbeloep = 444,
                                    )
                                },
                        )
                    },
                avkortetYtelse = aarsoppgjoer.avkortetYtelse.map { it.copy(avkortingsbeloep = 444) },
            )

        val nyttArsoppgjoer = nyAvkorting(2025)

        avkortingRepository.lagreAvkorting(
            behandlingId,
            sakId,
            Avkorting(
                aarsoppgjoer =
                    listOf(
                        endretAarsoppgjoer,
                        nyttArsoppgjoer,
                    ),
            ),
        )
        val avkorting = avkortingRepository.hentAvkorting(behandlingId)

        with(avkorting!!.aarsoppgjoer[0] as AarsoppgjoerLoepende) {
            aar shouldBe 2024
            ytelseFoerAvkorting.asClue {
                it.size shouldBe 1
                it shouldBe endretAarsoppgjoer.ytelseFoerAvkorting
            }
            inntektsavkorting.asClue {
                it.size shouldBe 2
                it[0].asClue { avkorting ->
                    avkorting.grunnlag shouldBe endretAarsoppgjoer.inntektsavkorting.first().grunnlag
                    avkorting.avkortingsperioder shouldBe endretAarsoppgjoer.inntektsavkorting.first().avkortingsperioder
                    avkorting.avkortetYtelseForventetInntekt shouldBe
                        endretAarsoppgjoer.inntektsavkorting.first().avkortetYtelseForventetInntekt
                }
            }
            avkortetYtelse.asClue {
                it.size shouldBe 2
                it shouldBe endretAarsoppgjoer.avkortetYtelse
            }
        }

        with(avkorting.aarsoppgjoer[1] as AarsoppgjoerLoepende) {
            aar shouldBe 2025
            ytelseFoerAvkorting.asClue {
                it.size shouldBe 1
                it shouldBe nyttArsoppgjoer.ytelseFoerAvkorting
            }
            inntektsavkorting.asClue {
                it.size shouldBe 2
                it[0].asClue { avkorting ->
                    avkorting.grunnlag shouldBe nyttArsoppgjoer.inntektsavkorting.first().grunnlag
                    avkorting.avkortingsperioder shouldBe nyttArsoppgjoer.inntektsavkorting.first().avkortingsperioder
                    avkorting.avkortetYtelseForventetInntekt shouldBe
                        nyttArsoppgjoer.inntektsavkorting.first().avkortetYtelseForventetInntekt
                }
            }
            avkortetYtelse.asClue {
                it.size shouldBe 2
                it shouldBe nyttArsoppgjoer.avkortetYtelse
            }
        }
    }

    @Test
    fun `hent alle aarsoppgjoer`() {
        val sakId = randomSakId()
        avkortingRepository.lagreAvkorting(
            UUID.randomUUID(),
            sakId,
            Avkorting(
                aarsoppgjoer = listOf(nyAvkorting(2024), nyAvkorting(2025)),
            ),
        )

        val alle = avkortingRepository.hentAlleAarsoppgjoer(sakId)

        alle.size shouldBe 2
        alle[0].aar shouldBe 2024
        alle[1].aar shouldBe 2025
    }

    @Test
    fun `hent alle aarsoppgjoer for behandlinger`() {
        val sakId = randomSakId()
        val behandlingId = UUID.randomUUID()
        avkortingRepository.lagreAvkorting(
            behandlingId,
            sakId,
            Avkorting(
                aarsoppgjoer = listOf(nyAvkorting(2024), nyAvkorting(2025)),
            ),
        )

        val alle = avkortingRepository.hentAlleAarsoppgjoer(listOf(behandlingId))

        alle.size shouldBe 2
        alle[0].aar shouldBe 2024
        alle[1].aar shouldBe 2025
    }

    @Test
    fun `Skal lagre og oppdatere avkorting med etteroppgjoer`() {
        val behandlingId: UUID = UUID.randomUUID()
        val etteroppgjoer = nyAvkortingEtteroppgjoer(2024)
        val sakId = randomSakId()

        avkortingRepository.lagreAvkorting(
            behandlingId,
            sakId,
            Avkorting(
                aarsoppgjoer = listOf(etteroppgjoer),
            ),
        )

        val endretEtteroppgjoer =
            etteroppgjoer.copy(
                ytelseFoerAvkorting = etteroppgjoer.ytelseFoerAvkorting.map { it.copy(beregning = 333) },
                inntekt =
                    etteroppgjoer.inntekt.copy(
                        loennsinntekt = 170000,
                    ),
                avkortingsperioder = etteroppgjoer.avkortingsperioder.map { it.copy(avkorting = 333) },
                avkortetYtelse = etteroppgjoer.avkortetYtelse.map { it.copy(avkortingsbeloep = 444) },
            )

        val nyttArsoppgjoer = nyAvkorting(2025)

        avkortingRepository.lagreAvkorting(
            behandlingId,
            sakId,
            Avkorting(
                aarsoppgjoer =
                    listOf(
                        endretEtteroppgjoer,
                        nyttArsoppgjoer,
                    ),
            ),
        )
        val avkorting = avkortingRepository.hentAvkorting(behandlingId)

        with(avkorting!!.aarsoppgjoer[0] as Etteroppgjoer) {
            aar shouldBe 2024
            ytelseFoerAvkorting.asClue {
                it.size shouldBe 1
                it shouldBe endretEtteroppgjoer.ytelseFoerAvkorting
            }
            inntekt shouldBe endretEtteroppgjoer.inntekt
            avkortingsperioder shouldBe endretEtteroppgjoer.avkortingsperioder
            avkortetYtelse.asClue {
                it.size shouldBe 2
                it shouldBe endretEtteroppgjoer.avkortetYtelse
            }
        }

        with(avkorting.aarsoppgjoer[1] as AarsoppgjoerLoepende) {
            aar shouldBe 2025
            ytelseFoerAvkorting.asClue {
                it.size shouldBe 1
                it shouldBe nyttArsoppgjoer.ytelseFoerAvkorting
            }
            inntektsavkorting.asClue {
                it.size shouldBe 2
                it[0].asClue { avkorting ->
                    avkorting.grunnlag shouldBe nyttArsoppgjoer.inntektsavkorting.first().grunnlag
                    avkorting.avkortingsperioder shouldBe nyttArsoppgjoer.inntektsavkorting.first().avkortingsperioder
                    avkorting.avkortetYtelseForventetInntekt shouldBe
                        nyttArsoppgjoer.inntektsavkorting.first().avkortetYtelseForventetInntekt
                }
            }
            avkortetYtelse.asClue {
                it.size shouldBe 2
                it shouldBe nyttArsoppgjoer.avkortetYtelse
            }
        }
    }

    private fun opprettSanksjoner(
        behandlingId: UUID,
        sakId: SakId,
    ): List<Sanksjon> {
        listOf(
            lagreSanksjon(
                sakId = sakId,
                type = SanksjonType.STANS,
                fom = YearMonth.of(2024, 8).atDay(1),
                tom = YearMonth.of(2024, 8).atEndOfMonth(),
            ),
            lagreSanksjon(
                sakId = sakId,
                type = SanksjonType.BORTFALL,
                fom = YearMonth.of(2025, 2).atDay(1),
                tom = YearMonth.of(2025, 2).atEndOfMonth(),
            ),
        ).forEach { sanksjon ->
            sanksjonRepository.opprettSanksjon(behandlingId, sakId, bruker.ident(), sanksjon)
        }
        return sanksjonRepository.hentSanksjon(behandlingId)
    }

    private fun beregnAvkorting(
        sanksjoner: List<Sanksjon>,
        brukNyeReglerAvkorting: Boolean,
    ): Avkorting {
        val beregning =
            beregning().copy(
                beregningsperioder =
                    listOf(
                        beregningsperiode(datoFOM = YearMonth.of(2024, 1), datoTOM = YearMonth.of(2024, 4), utbetaltBeloep = 20000),
                        beregningsperiode(datoFOM = YearMonth.of(2024, 5), datoTOM = YearMonth.of(2025, 4), utbetaltBeloep = 22000),
                        beregningsperiode(datoFOM = YearMonth.of(2025, 5), datoTOM = YearMonth.of(2025, 12), utbetaltBeloep = 24000),
                    ),
            )
        val opprinneligAvkorting =
            Avkorting().beregnAvkortingMedNyeGrunnlag(
                nyttGrunnlag = (2024..2025).map { avkortinggrunnlagLagreDto(aarsinntekt = 500000, fom = YearMonth.of(it, 1)) },
                bruker = bruker,
                beregning = beregning,
                sanksjoner = sanksjoner,
                opphoerFom = YearMonth.of(2026, 1),
                brukNyeReglerAvkorting = brukNyeReglerAvkorting,
            )
        return opprinneligAvkorting.beregnAvkortingMedNyeGrunnlag(
            nyttGrunnlag = (2024..2025).map { avkortinggrunnlagLagreDto(aarsinntekt = 300000, fom = YearMonth.of(it, 7)) },
            bruker = bruker,
            beregning = beregning,
            sanksjoner = sanksjoner,
            opphoerFom = YearMonth.of(2026, 1),
            brukNyeReglerAvkorting = brukNyeReglerAvkorting,
        )
    }

    private fun nyAvkorting(
        aar: Int,
        forventaInnvilgaMaaneder: Int = 12,
    ): AarsoppgjoerLoepende {
        val inntektEn =
            avkortinggrunnlag(
                innvilgaMaaneder = forventaInnvilgaMaaneder,
                periode = Periode(fom = YearMonth.of(aar, 1), tom = YearMonth.of(aar, 3)),
                overstyrtInnvilgaMaanederAarsak = OverstyrtInnvilgaMaanederAarsak.ANNEN,
                overstyrtInnvilgaMaanederBegrunnelse = "overstyrtInnvilgaMaanederBegrunnelse",
            )
        val inntektsavkortingEn =
            Inntektsavkorting(
                grunnlag = inntektEn,
                avkortingsperioder =
                    listOf(
                        avkortingsperiode(
                            inntektsgrunnlag = inntektEn.id,
                            fom = YearMonth.of(aar, 1),
                        ),
                    ),
                avkortetYtelseForventetInntekt =
                    listOf(
                        avkortetYtelse(
                            type = AvkortetYtelseType.FORVENTET_INNTEKT,
                            inntektsgrunnlag = inntektEn.id,
                            periode = Periode(fom = YearMonth.of(aar, 1), tom = null),
                        ),
                    ),
            )

        val inntektTo =
            avkortinggrunnlag(
                innvilgaMaaneder = forventaInnvilgaMaaneder,
                periode = Periode(fom = YearMonth.of(aar, 4), tom = null),
            )
        val inntektsavkortingTo =
            Inntektsavkorting(
                grunnlag = inntektTo,
                avkortingsperioder =
                    listOf(
                        avkortingsperiode(
                            inntektsgrunnlag = inntektTo.id,
                            fom = YearMonth.of(aar, 4),
                        ),
                    ),
                avkortetYtelseForventetInntekt =
                    listOf(
                        avkortetYtelse(
                            type = AvkortetYtelseType.FORVENTET_INNTEKT,
                            inntektsgrunnlag = inntektTo.id,
                            periode = Periode(fom = YearMonth.of(aar, 4), tom = null),
                        ),
                    ),
            )

        return AarsoppgjoerLoepende(
            id = UUID.randomUUID(),
            aar = aar,
            fom = YearMonth.of(aar, 12),
            ytelseFoerAvkorting =
                listOf(
                    ytelseFoerAvkorting(
                        periode =
                            Periode(
                                fom = YearMonth.of(aar, 1),
                                tom = null,
                            ),
                    ),
                ),
            inntektsavkorting =
                listOf(
                    inntektsavkortingEn,
                    inntektsavkortingTo,
                ),
            avkortetYtelse =
                listOf(
                    avkortetYtelse(
                        type = AvkortetYtelseType.AARSOPPGJOER,
                        periode = Periode(fom = YearMonth.of(aar, 1), tom = YearMonth.of(aar, 3)),
                    ),
                    avkortetYtelse(
                        type = AvkortetYtelseType.AARSOPPGJOER,
                        periode = Periode(fom = YearMonth.of(aar, 4), tom = null),
                    ),
                ),
        )
    }

    private fun nyAvkortingEtteroppgjoer(
        aar: Int,
        forventaInnvilgaMaaneder: Int = 12,
    ): Etteroppgjoer {
        val inntekt =
            FaktiskInntekt(
                id = UUID.randomUUID(),
                periode = Periode(fom = YearMonth.of(aar, 1), tom = YearMonth.of(aar, 12)),
                innvilgaMaaneder = forventaInnvilgaMaaneder,
                loennsinntekt = 100000,
                naeringsinntekt = 30000,
                afp = 10000,
                utlandsinntekt = 10000,
                kilde = Grunnlagsopplysning.Saksbehandler.create("lok"),
                spesifikasjon = "Spesifikasjon",
                inntektInnvilgetPeriode =
                    BenyttetInntektInnvilgetPeriode(
                        verdi = 150000,
                        tidspunkt = Tidspunkt.now(),
                        regelResultat = "".toJsonNode(),
                        kilde =
                            Grunnlagsopplysning.RegelKilde(
                                navn = "",
                                ts = Tidspunkt.now(),
                                versjon = "",
                            ),
                    ),
                maanederInnvilget = (1..12).map { MaanedInnvilget(maaned = YearMonth.of(2024, it), innvilget = true) },
                maanederInnvilgetRegelResultat = null,
            )

        return Etteroppgjoer(
            id = UUID.randomUUID(),
            aar = aar,
            fom = YearMonth.of(aar, 12),
            ytelseFoerAvkorting =
                listOf(
                    ytelseFoerAvkorting(
                        periode =
                            Periode(
                                fom = YearMonth.of(aar, 1),
                                tom = null,
                            ),
                    ),
                ),
            inntekt = inntekt,
            avkortingsperioder =
                listOf(
                    avkortingsperiode(
                        inntektsgrunnlag = inntekt.id,
                        fom = YearMonth.of(aar, 1),
                    ),
                ),
            avkortetYtelse =
                listOf(
                    avkortetYtelse(
                        type = AvkortetYtelseType.ETTEROPPGJOER,
                        periode = Periode(fom = YearMonth.of(aar, 1), tom = YearMonth.of(aar, 3)),
                        restanse = null,
                    ),
                    avkortetYtelse(
                        type = AvkortetYtelseType.ETTEROPPGJOER,
                        periode = Periode(fom = YearMonth.of(aar, 4), tom = null),
                        restanse = null,
                    ),
                ),
        )
    }
}
