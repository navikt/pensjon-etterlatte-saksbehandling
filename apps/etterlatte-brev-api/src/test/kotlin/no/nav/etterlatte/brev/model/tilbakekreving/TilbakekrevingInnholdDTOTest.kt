package no.nav.etterlatte.brev.model.tilbakekreving

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.Innsender
import no.nav.etterlatte.brev.behandling.PersonerISak
import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.brev.brevbaker.formaterNavn
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingPeriode
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingResultat
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVurdering
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import no.nav.pensjon.brevbaker.api.model.Kroner
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class TilbakekrevingInnholdDTOTest {
    @Test
    fun `skal inneholde saktype`() {
        TilbakekrevingBrevDTO
            .fra(
                emptyList(),
                brevData().forenkletVedtak?.tilbakekreving,
                brevData().sak.sakType,
                brevData().utlandstilknytning?.type,
                brevData().personerISak.soeker.formaterNavn(),
            ).sakType shouldBe SakType.OMSTILLINGSSTOENAD
    }

    @Test
    fun `skal ha beloep og resultat for alle perioder`() {
        val brevData =
            brevData(
                perioder =
                    listOf(
                        tilbakekrevingperiode(
                            beregnetFeilutbetaling = 100,
                            bruttoTilbakekreving = 200,
                            nettoTilbakekreving = 300,
                            skatt = 400,
                            renteTilleg = 500,
                            resultat = TilbakekrevingResultat.FULL_TILBAKEKREV,
                        ),
                    ),
            )

        val data =
            TilbakekrevingBrevDTO.fra(
                emptyList(),
                brevData.forenkletVedtak?.tilbakekreving,
                brevData.sak.sakType,
                brevData.utlandstilknytning?.type,
                brevData.personerISak.soeker.formaterNavn(),
            )

        data.tilbakekreving.perioder.size shouldBe 1
        with(data.tilbakekreving.perioder[0]) {
            maaned shouldBe YearMonth.of(2023, 1).atDay(1)
            beloeper shouldBe
                TilbakekrevingBeloeperData(
                    feilutbetaling = Kroner(100),
                    bruttoTilbakekreving = Kroner(200),
                    nettoTilbakekreving = Kroner(300),
                    fradragSkatt = Kroner(400),
                    renteTillegg = Kroner(500),
                    sumNettoRenter = Kroner(800),
                )
            resultat shouldBe TilbakekrevingResultat.FULL_TILBAKEKREV
        }
    }

    @Test
    fun `skal inneholde summering av alle perioder`() {
        val brevData =
            brevData(
                perioder =
                    listOf(
                        tilbakekrevingperiode(
                            beregnetFeilutbetaling = 100,
                            bruttoTilbakekreving = 200,
                            nettoTilbakekreving = 300,
                            skatt = 400,
                            renteTilleg = 500,
                        ),
                        tilbakekrevingperiode(
                            beregnetFeilutbetaling = 100,
                            bruttoTilbakekreving = 200,
                            nettoTilbakekreving = 300,
                            skatt = 400,
                            renteTilleg = 500,
                        ),
                    ),
            )
        TilbakekrevingBrevDTO
            .fra(
                emptyList(),
                brevData.forenkletVedtak?.tilbakekreving,
                brevData.sak.sakType,
                brevData.utlandstilknytning?.type,
                brevData.personerISak.soeker.formaterNavn(),
            ).tilbakekreving.summer shouldBe
            TilbakekrevingBeloeperData(
                feilutbetaling = Kroner(200),
                bruttoTilbakekreving = Kroner(400),
                nettoTilbakekreving = Kroner(600),
                fradragSkatt = Kroner(800),
                renteTillegg = Kroner(1000),
                sumNettoRenter = Kroner(1600),
            )
    }

    companion object {
        fun brevData(
            perioder: List<TilbakekrevingPeriode> = listOf(tilbakekrevingperiode()),
            vurdering: TilbakekrevingVurdering = tilbakekrevingvurdering(),
        ) = GenerellBrevData(
            sak = Sak("12345612345", SakType.OMSTILLINGSSTOENAD, randomSakId(), Enheter.PORSGRUNN.enhetNr),
            personerISak =
                PersonerISak(
                    Innsender(Foedselsnummer("11057523044")),
                    Soeker("GRØNN", "MELLOMNAVN", "KOPP", Foedselsnummer("12345612345")),
                    listOf(Avdoed(Foedselsnummer(""), "DØD TESTPERSON", LocalDate.now().minusMonths(1))),
                    verge = null,
                ),
            behandlingId = UUID.randomUUID(),
            forenkletVedtak =
                ForenkletVedtak(
                    1,
                    VedtakStatus.FATTET_VEDTAK,
                    VedtakType.TILBAKEKREVING,
                    Enheter.PORSGRUNN.enhetNr,
                    "saksbehandler",
                    attestantIdent = null,
                    vedtaksdato = null,
                    virkningstidspunkt = YearMonth.now(),
                    tilbakekreving =
                        tilbakekreving(
                            vurdering = vurdering,
                            perioder = perioder,
                        ),
                ),
            spraak = Spraak.NB,
            systemkilde = Vedtaksloesning.GJENNY,
        )
    }
}
