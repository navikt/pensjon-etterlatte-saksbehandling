package no.nav.etterlatte.brev.model.tilbakekreving

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingResultat
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import no.nav.pensjon.brevbaker.api.model.Kroner
import org.junit.jupiter.api.Test
import java.time.YearMonth

class TilbakekrevingInnholdDTOTest {
    @Test
    fun `skal inneholde saktype`() {
        TilbakekrevingBrevDTO
            .fra(
                muligTilbakekreving =
                    tilbakekreving(
                        vurdering = tilbakekrevingvurdering(),
                        perioder = listOf(tilbakekrevingperiode()),
                    ),
                sakType = SakType.OMSTILLINGSSTOENAD,
                utlandstilknytning = null,
                soeker = Soeker("GRØNN", "MELLOMNAVN", "KOPP", Foedselsnummer("12345612345")),
                emptyList(),
            ).sakType shouldBe SakType.OMSTILLINGSSTOENAD
    }

    @Test
    fun `skal ha beloep og resultat for alle perioder`() {
        val data =
            TilbakekrevingBrevDTO.fra(
                muligTilbakekreving =
                    tilbakekreving(
                        vurdering = tilbakekrevingvurdering(),
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
                    ),
                sakType = SakType.OMSTILLINGSSTOENAD,
                utlandstilknytning = null,
                soeker = Soeker("GRØNN", "MELLOMNAVN", "KOPP", Foedselsnummer("12345612345")),
                emptyList(),
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
        TilbakekrevingBrevDTO
            .fra(
                muligTilbakekreving =
                    tilbakekreving(
                        vurdering = tilbakekrevingvurdering(),
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
                    ),
                sakType = SakType.OMSTILLINGSSTOENAD,
                utlandstilknytning = null,
                soeker = Soeker("GRØNN", "MELLOMNAVN", "KOPP", Foedselsnummer("12345612345")),
                emptyList(),
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
}
