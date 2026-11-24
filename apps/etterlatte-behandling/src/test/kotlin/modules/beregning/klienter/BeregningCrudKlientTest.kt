package no.nav.etterlatte.modules.beregning.klienter

import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.jackson3.JacksonConverter
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.OverstyrBeregningGrunnlagDao
import no.nav.etterlatte.beregning.grunnlag.Vedtaksperiode
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType
import no.nav.etterlatte.libs.common.beregning.InntektsjusteringAvkortingInfoRequest
import no.nav.etterlatte.libs.common.beregning.OverstyrtBeregningKategori
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.PeriodisertResultat
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.finnFaktumIGrunnlag
import no.nav.etterlatte.modules.beregning.OverstyrBeregning
import no.nav.etterlatte.modules.beregning.OverstyrBeregningStatus
import no.nav.etterlatte.modules.beregning.anvendttrygdetid.AnvendtTrygdetid
import no.nav.etterlatte.modules.beregning.anvendttrygdetid.AnvendtTrygdetidPeriodeUtrekning
import no.nav.etterlatte.modules.beregning.avkorting.Avkorting
import no.nav.etterlatte.modules.beregning.avkorting.IngenInntektInnvilgetPeriode
import no.nav.etterlatte.modules.beregning.avkorting.Inntektsavkorting
import no.nav.etterlatte.modules.beregning.avkorting.Restanse
import no.nav.etterlatte.modules.beregning.etteroppgjoer.BeregnetEtteroppgjoerResultat
import no.nav.etterlatte.modules.beregning.etteroppgjoer.EtteroppgjoerGrense
import no.nav.etterlatte.modules.beregning.etteroppgjoer.EtteroppgjoerRettsgebyr
import no.nav.etterlatte.modules.beregning.etteroppgjoer.ReferanseEtteroppgjoer
import no.nav.etterlatte.modules.beregning.regler.aarsoppgjoer
import no.nav.etterlatte.modules.beregning.regler.avkortetYtelse
import no.nav.etterlatte.modules.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.modules.beregning.regler.beregning
import no.nav.etterlatte.modules.beregning.regler.etteroppgjoer
import no.nav.etterlatte.modules.beregning.regler.lagreSanksjon
import no.nav.etterlatte.modules.beregning.regler.sanksjon
import no.nav.etterlatte.modules.beregning.regler.toGrunnlag
import no.nav.etterlatte.regler.Beregningstall
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class BeregningCrudKlientTest {
    private val behandlingId = UUID.randomUUID()
    private val sakId = SakId(123)
    private val kilde = Grunnlagsopplysning.Saksbehandler.create("Z123456")

    @Test
    fun `henter og lagrer beregning uten aa endre data`() {
        val beregning = beregning().copy(behandlingId = behandlingId)
        medKlient(HttpMethod.Get, "/beregning/$behandlingId", svar = beregning) {
            it.hent(behandlingId) shouldBe beregning
        }
        medKlient(HttpMethod.Put, "/beregning", beregning, beregning) {
            it.lagreEllerOppdaterBeregning(beregning) shouldBe beregning
        }
        medKlient(HttpMethod.Delete, "/beregning/$behandlingId/overstyrt-grunnlag", status = HttpStatusCode.NoContent) {
            it.slettOverstyrtBeregningsgrunnlag(behandlingId)
        }
    }

    @Test
    fun `henter lagrer og deaktiverer overstyring paa sak`() {
        val overstyring =
            OverstyrBeregning(sakId, "Manuelt beregnet", Tidspunkt.now(), kategori = OverstyrtBeregningKategori.UKJENT_AVDOED)
        medKlient(HttpMethod.Get, "/beregning/overstyring/123", svar = overstyring) {
            it.hentOverstyrBeregning(sakId) shouldBe overstyring
        }
        medKlient(HttpMethod.Put, "/beregning/overstyring", overstyring, overstyring) {
            it.opprettOverstyrBeregning(overstyring) shouldBe overstyring
        }
        val inaktiv = overstyring.copy(status = OverstyrBeregningStatus.IKKE_AKTIV)
        medKlient(HttpMethod.Put, "/beregning/overstyring", inaktiv, status = HttpStatusCode.NoContent) {
            it.opprettOverstyrBeregning(inaktiv) shouldBe null
        }
        medKlient(HttpMethod.Delete, "/beregning/overstyring/123", status = HttpStatusCode.NoContent) {
            it.deaktiverOverstyrtBeregning(sakId)
        }
    }

    @Test
    fun `henter og lagrer beregningsgrunnlag med vedtaksperioder`() {
        val grunnlag =
            BeregningsGrunnlag(
                behandlingId = behandlingId,
                soeskenMedIBeregning = emptyList(),
                institusjonsopphold = emptyList(),
                beregningsMetode = BeregningsMetode.NASJONAL.toGrunnlag(),
                kilde = kilde,
                vedtaksperioder = listOf(Vedtaksperiode(YearMonth.of(2024, 1), null)),
            )
        medKlient(HttpMethod.Get, "/beregningsgrunnlag/$behandlingId", svar = grunnlag) {
            it.finnBeregningsGrunnlag(behandlingId) shouldBe grunnlag
        }
        medKlient(HttpMethod.Put, "/beregningsgrunnlag", grunnlag, true) {
            it.lagreBeregningsGrunnlag(grunnlag) shouldBe true
        }
        medKlient(HttpMethod.Put, "/beregningsgrunnlag", grunnlag, false) {
            it.lagreBeregningsGrunnlag(grunnlag) shouldBe false
        }
    }

    @Test
    fun `henter og erstatter overstyrt grunnlag med null i valgfrie felter`() {
        val grunnlag =
            listOf(
                OverstyrBeregningGrunnlagDao(
                    id = UUID.randomUUID(),
                    behandlingId = behandlingId,
                    datoFOM = LocalDate.of(2024, 1, 1),
                    datoTOM = null,
                    utbetaltBeloep = 5000,
                    foreldreloessats = false,
                    trygdetid = 40,
                    trygdetidForIdent = null,
                    prorataBroekTeller = null,
                    prorataBroekNevner = null,
                    sakId = sakId,
                    beskrivelse = "Manuelt beregnet",
                    aarsak = "ANNET",
                    kilde = kilde,
                    reguleringRegelresultat = null,
                ),
            )
        medKlient(HttpMethod.Get, "/beregningsgrunnlag/$behandlingId/overstyrt", svar = grunnlag) {
            it.finnOverstyrBeregningGrunnlagForBehandling(behandlingId) shouldBe grunnlag
        }
        medKlient(HttpMethod.Put, "/beregningsgrunnlag/$behandlingId/overstyrt", grunnlag, status = HttpStatusCode.NoContent) {
            it.lagreOverstyrBeregningGrunnlagForBehandling(behandlingId, grunnlag)
        }
        medKlient(HttpMethod.Put, "/beregningsgrunnlag/$behandlingId/overstyrt", emptyList<Any>(), status = HttpStatusCode.NoContent) {
            it.lagreOverstyrBeregningGrunnlagForBehandling(behandlingId, emptyList())
        }
    }

    @Test
    fun `lagrer anvendt trygdetid med regeltraer som JSON og returnerer antall rader`() {
        val fom = LocalDate.of(2024, 1, 1)
        val tom = LocalDate.of(2024, 12, 31)
        val trygdetid = AnvendtTrygdetid(BeregningsMetode.NASJONAL, Beregningstall(40), "test-ident")
        val regel =
            finnFaktumIGrunnlag<AnvendtTrygdetid, AnvendtTrygdetid, AnvendtTrygdetid>(
                gjelderFra = fom,
                beskrivelse = "Trygdetid",
                finnFaktum = { FaktumNode(it, "test", "Trygdetid") },
                finnFelt = { it },
            )
        val periode = RegelPeriode(fom, tom)
        val utregning =
            AnvendtTrygdetidPeriodeUtrekning(
                anvendt = listOf(GrunnlagMedPeriode(listOf(trygdetid), fom, tom)),
                utrekning = listOf(PeriodisertResultat(periode, regel.anvend(trygdetid, periode), "1")),
            )
        medKlient(HttpMethod.Post, "/anvendt-trygdetid/$behandlingId", utregning, 1, HttpStatusCode.Created) {
            it.lagreAnvendtTrygdetid(behandlingId, utregning) shouldBe 1
        }
    }

    @Test
    fun `henter og lagrer full avkorting med begge typer aarsoppgjoer og inntekt`() {
        val restanse = Restanse(UUID.randomUUID(), 100, 25, Tidspunkt.now(), null, kilde)
        val grunnlag =
            avkortinggrunnlag(
                periode = Periode(YearMonth.of(2024, 1), YearMonth.of(2024, 6)),
                innvilgaMaaneder = 6,
            ).copy(maanederInnvilgetRegelResultat = null)
        val avkorting =
            Avkorting(
                listOf(
                    aarsoppgjoer(
                        inntektsavkorting =
                            listOf(
                                Inntektsavkorting(grunnlag, emptyList(), emptyList()),
                                Inntektsavkorting(
                                    grunnlag.copy(
                                        id = UUID.randomUUID(),
                                        periode = Periode(YearMonth.of(2024, 7), YearMonth.of(2024, 12)),
                                        inntektInnvilgetPeriode = IngenInntektInnvilgetPeriode,
                                    ),
                                ),
                            ),
                        avkortetYtelse = listOf(avkortetYtelse().copy(restanse = restanse)),
                    ),
                    etteroppgjoer(aar = 2025),
                ),
            )
        medKlient(HttpMethod.Get, "/avkorting/$behandlingId", svar = avkorting) {
            it.hentAvkorting(behandlingId) shouldBe avkorting
        }
        medKlient(
            HttpMethod.Put,
            "/avkorting/$behandlingId",
            mapOf("sakId" to sakId, "avkorting" to avkorting),
            status = HttpStatusCode.NoContent,
        ) {
            it.lagreAvkorting(behandlingId, sakId, avkorting)
        }
        medKlient(HttpMethod.Delete, "/avkorting/$behandlingId", status = HttpStatusCode.NoContent) {
            it.slettForBehandling(behandlingId)
        }
    }

    @Test
    fun `henter faktisk inntekt`() {
        val aarsoppgjoer = etteroppgjoer()
        medKlient(HttpMethod.Get, "/avkorting/faktisk-inntekt/${aarsoppgjoer.id}", svar = aarsoppgjoer.inntekt) {
            it.hentFaktiskInntekt(aarsoppgjoer.id) shouldBe aarsoppgjoer.inntekt
        }
    }

    @Test
    fun `henter aarsoppgjoer og behandlingsreferanser uten aa flate ut historikk`() {
        val aarsoppgjoer = listOf(aarsoppgjoer())
        val behandlinger = listOf(behandlingId, UUID.randomUUID())
        medKlient(HttpMethod.Get, "/avkorting/sak/123/aarsoppgjoer", svar = aarsoppgjoer) {
            it.hentAlleAarsoppgjoer(sakId) shouldBe aarsoppgjoer
        }
        medKlient(HttpMethod.Get, "/avkorting/sak/123/behandlinger", svar = behandlinger) {
            it.hentAlleBehandlingerMedAarsoppgjoer(sakId) shouldBe behandlinger
        }
        medKlient(HttpMethod.Post, "/avkorting/aarsoppgjoer", behandlinger, aarsoppgjoer) {
            it.hentAlleAarsoppgjoer(behandlinger) shouldBe aarsoppgjoer
        }
    }

    @Test
    fun `sjekker inntekt for aar med CRUD-endepunktet`() {
        val request = InntektsjusteringAvkortingInfoRequest(sakId, 2024, behandlingId)
        listOf(true, false).forEach { harInntekt ->
            medKlient(HttpMethod.Post, "/avkorting/inntekt-for-aar", request, harInntekt) {
                it.harSakInntektForAar(request) shouldBe harInntekt
            }
        }
    }

    @Test
    fun `henter og lagrer etteroppgjoersresultat med referanser og regelresultat`() {
        val resultat =
            BeregnetEtteroppgjoerResultat(
                id = UUID.randomUUID(),
                aar = 2024,
                forbehandlingId = behandlingId,
                sisteIverksatteBehandlingId = UUID.randomUUID(),
                utbetaltStoenad = 10000,
                nyBruttoStoenad = 9000,
                differanse = -1000,
                grense =
                    EtteroppgjoerGrense(
                        Beregningstall(100),
                        Beregningstall(200),
                        EtteroppgjoerRettsgebyr(LocalDate.of(2024, 1, 1), Beregningstall(1000)),
                    ),
                resultatType = EtteroppgjoerResultatType.TILBAKEKREVING,
                harIngenInntekt = false,
                tidspunkt = Tidspunkt.now(),
                regelResultat = mapOf("regel" to "resultat").toJsonNode(),
                kilde = kilde,
                referanseAvkorting = ReferanseEtteroppgjoer(UUID.randomUUID(), UUID.randomUUID(), listOf(123L)),
            )
        medKlient(
            HttpMethod.Get,
            "/etteroppgjoer/resultat/$behandlingId/2024/${resultat.sisteIverksatteBehandlingId}",
            svar = resultat,
        ) {
            it.hentEtteroppgjoerResultat(2024, behandlingId, resultat.sisteIverksatteBehandlingId) shouldBe resultat
        }
        medKlient(HttpMethod.Put, "/etteroppgjoer/resultat", resultat, status = HttpStatusCode.NoContent) {
            it.lagreEtteroppgjoerResultat(resultat)
        }
        val avkorting = Avkorting(listOf(etteroppgjoer()))
        val samletResultat =
            resultat.copy(
                referanseAvkorting = resultat.referanseAvkorting.copy(avkortingForbehandling = avkorting.aarsoppgjoer.single().id),
            )
        medKlient(
            HttpMethod.Put,
            "/etteroppgjoer/$behandlingId",
            mapOf("sakId" to sakId, "avkorting" to avkorting, "resultat" to samletResultat),
            status = HttpStatusCode.NoContent,
        ) {
            it.lagreAvkortingOgEtteroppgjoerResultat(sakId, avkorting, samletResultat)
        }
    }

    @Test
    fun `henter oppretter kopierer oppdaterer og sletter sanksjoner`() {
        val sanksjon = sanksjon(behandlingId = behandlingId, sakId = sakId)
        val sanksjonId = requireNotNull(sanksjon.id)
        val lagre = lagreSanksjon(id = sanksjonId, sakId = sakId)
        medKlient(HttpMethod.Get, "/sanksjon/behandling/$behandlingId", svar = listOf(sanksjon)) {
            it.hentSanksjon(behandlingId) shouldBe listOf(sanksjon)
        }
        medKlient(HttpMethod.Get, "/sanksjon/$sanksjonId", svar = sanksjon) {
            it.hentSanksjonMedId(sanksjonId) shouldBe sanksjon
        }
        medKlient(
            HttpMethod.Post,
            "/sanksjon/behandling/$behandlingId",
            mapOf("sakId" to sakId, "saksbehandlerIdent" to "Z123456", "sanksjon" to lagre),
            status = HttpStatusCode.Created,
        ) {
            it.opprettSanksjon(behandlingId, sakId, "Z123456", lagre)
        }
        medKlient(
            HttpMethod.Post,
            "/sanksjon/behandling/$behandlingId/kopi",
            mapOf("sakId" to sakId, "sanksjon" to sanksjon),
            status = HttpStatusCode.Created,
        ) {
            it.opprettSanksjonFraKopi(behandlingId, sakId, sanksjon)
        }
        medKlient(
            HttpMethod.Put,
            "/sanksjon",
            mapOf("saksbehandlerIdent" to "Z123456", "sanksjon" to lagre),
            status = HttpStatusCode.NoContent,
        ) {
            it.oppdaterSanksjon(lagre, "Z123456")
        }
        medKlient(HttpMethod.Delete, "/sanksjon/$sanksjonId", svar = 1) {
            it.slettSanksjon(sanksjonId) shouldBe 1
        }
    }

    @Test
    fun `returnerer null bare for manglende enkeltressurser`() {
        val operasjoner =
            listOf<Pair<String, (BeregningCrudKlient) -> Any?>>(
                "/beregning/$behandlingId" to { it.hent(behandlingId) },
                "/beregning/overstyring/123" to { it.hentOverstyrBeregning(sakId) },
                "/beregningsgrunnlag/$behandlingId" to { it.finnBeregningsGrunnlag(behandlingId) },
                "/avkorting/$behandlingId" to { it.hentAvkorting(behandlingId) },
                "/avkorting/faktisk-inntekt/$behandlingId" to { it.hentFaktiskInntekt(behandlingId) },
                "/etteroppgjoer/resultat/$behandlingId/2024/$behandlingId" to {
                    it.hentEtteroppgjoerResultat(2024, behandlingId, behandlingId)
                },
                "/sanksjon/$behandlingId" to { it.hentSanksjonMedId(behandlingId) },
            )
        operasjoner.forEach { (sti, operasjon) ->
            medKlient(HttpMethod.Get, sti, status = HttpStatusCode.NotFound) { operasjon(it) shouldBe null }
        }
    }

    @Test
    fun `bevarer tomme lister og null i beregningsperioder`() {
        medKlient(HttpMethod.Get, "/sanksjon/behandling/$behandlingId", svar = emptyList<Any>()) {
            it.hentSanksjon(behandlingId) shouldBe emptyList()
        }
        medKlient(HttpMethod.Get, "/avkorting/sak/123/aarsoppgjoer", svar = emptyList<Any>()) {
            it.hentAlleAarsoppgjoer(sakId) shouldBe emptyList()
        }
        val beregning = beregning().let { it.copy(beregningsperioder = it.beregningsperioder.map { it.copy(regelResultat = null) }) }
        medKlient(HttpMethod.Get, "/beregning/$behandlingId", svar = beregning) {
            it.hent(behandlingId) shouldBe beregning
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [400, 401, 403, 500, 503])
    fun `feil ved lesing blir ikke tolket som manglende data`(statuskode: Int) {
        medKlient(HttpMethod.Get, "/beregning/$behandlingId", status = HttpStatusCode.fromValue(statuskode)) {
            assertThrows(InternfeilException::class.java) { it.hent(behandlingId) }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [200, 400, 401, 403, 404, 500, 503])
    fun `skrivefeil og uventede statuser blir ikke tolket som vellykket lagring`(statuskode: Int) {
        val avkorting = Avkorting()
        medKlient(
            HttpMethod.Put,
            "/avkorting/$behandlingId",
            mapOf("sakId" to sakId, "avkorting" to avkorting),
            status = HttpStatusCode.fromValue(statuskode),
        ) {
            assertThrows(InternfeilException::class.java) { it.lagreAvkorting(behandlingId, sakId, avkorting) }
        }
    }

    @Test
    fun `404 paa listeendepunkt er en feil og ikke en tom liste`() {
        medKlient(HttpMethod.Get, "/sanksjon/behandling/$behandlingId", status = HttpStatusCode.NotFound) {
            assertThrows(InternfeilException::class.java) { it.hentSanksjon(behandlingId) }
        }
    }

    @Test
    fun `ugyldig JSON i et vellykket svar gir feil`() {
        medKlient(HttpMethod.Get, "/beregning/$behandlingId", svar = "ugyldig beregning") {
            assertThrows(Exception::class.java) { it.hent(behandlingId) }
        }
    }

    private fun medKlient(
        metode: HttpMethod,
        sti: String,
        data: Any? = null,
        svar: Any? = null,
        status: HttpStatusCode = HttpStatusCode.OK,
        test: (BeregningCrudKlient) -> Unit,
    ) {
        val engine =
            MockEngine { request ->
                request.method shouldBe metode
                request.url.host shouldBe "beregning"
                request.url.fullPath shouldBe "/api/beregning/intern$sti"
                if (data != null) {
                    request.body.contentType shouldBe ContentType.Application.Json
                    objectMapper.readTree(request.body.toByteArray()) shouldBe objectMapper.readTree(data.toJson())
                }
                respond(svar?.toJson() ?: "", status, headersOf("Content-Type", "application/json"))
            }
        HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(objectMapper))
            }
        }.use { client ->
            test(BeregningCrudKlient(client, "http://beregning/"))
            engine.requestHistory.size shouldBe 1
        }
    }
}
