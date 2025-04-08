package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.BeregnetEtteroppgjoerResultatDto
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

data class Etteroppgjoer(
    val sakId: SakId,
    val inntektsaar: Int,
    val status: EtteroppgjoerStatus,
)

enum class EtteroppgjoerStatus {
    VENTER_PAA_SKATTEOPPGJOER,
    MOTTATT_SKATTEOPPGJOER,
    UNDER_FORBEHANDLING,
    UNDER_REVURDERING,
}

// TODO falte ut behandling..
data class DetaljertForbehandlingDto(
    val behandling: EtteroppgjoerForbehandling,
    val opplysninger: EtteroppgjoerOpplysninger,
    val avkortingFaktiskInntekt: AvkortingDto?,
    val beregnetEtteroppgjoerResultat: BeregnetEtteroppgjoerResultatDto,
)

data class EtteroppgjoerForbehandling(
    val id: UUID,
    val hendelseId: UUID,
    val status: String, // TODO enum
    val sak: Sak,
    val aar: Int,
    val innvilgetPeriode: Periode,
    val opprettet: Tidspunkt,
    val brevId: Long?,
) {
    fun medBrev(opprettetBrev: Brev): EtteroppgjoerForbehandling = this.copy(brevId = opprettetBrev.id)
}

data class EtteroppgjoerOpplysninger(
    val skatt: PensjonsgivendeInntektFraSkatt,
    val ainntekt: AInntekt,
    val tidligereAvkorting: AvkortingDto,
)

data class PensjonsgivendeInntektFraSkatt(
    val inntektsaar: Int,
    val inntekter: List<PensjonsgivendeInntekt>,
) {
    init {
        require(inntekter.all { it.inntektsaar == inntektsaar }) {
            "Alle inntekter må ha inntektsår = $inntektsaar, men fant: ${inntekter.map { it.inntektsaar }}"
        }
    }

    companion object {
        fun stub(
            aar: Int = 2024,
            aarsinntekt: Int = 300000,
        ) = PensjonsgivendeInntektFraSkatt(
            inntektsaar = aar,
            inntekter =
                listOf(
                    PensjonsgivendeInntekt(
                        skatteordning = "FASTLAND",
                        loensinntekt = aarsinntekt,
                        naeringsinntekt = 0,
                        fiskeFangstFamiliebarnehage = 0,
                        inntektsaar = aar,
                    ),
                    PensjonsgivendeInntekt(
                        skatteordning = "SVALBARD",
                        loensinntekt = aarsinntekt,
                        naeringsinntekt = 0,
                        fiskeFangstFamiliebarnehage = 0,
                        inntektsaar = aar,
                    ),
                ),
        )
    }
}

data class PensjonsgivendeInntekt(
    val inntektsaar: Int,
    val skatteordning: String,
    val loensinntekt: Int,
    val naeringsinntekt: Int,
    val fiskeFangstFamiliebarnehage: Int,
)

data class AInntekt(
    val aar: Int,
    val inntektsmaaneder: List<AInntektMaaned>,
) {
    init {
        require(inntektsmaaneder.all { it.maaned.year == aar }) {
            "Alle inntektsmaaneder må ha inntektsår = $aar, men fant: ${inntektsmaaneder.map { it.maaned.year }}"
        }
    }

    companion object {
        fun stub(
            aar: Int = 2024,
            aarsinntekt: Int = 300000,
        ) = AInntekt(
            aar = 2024,
            // inntektsmaaneder = listOf("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12").map {
            inntektsmaaneder =
                listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12).map {
                    AInntektMaaned(
                        // maaned = "$aar-$it",
                        maaned = YearMonth.of(aar, it),
                        inntekter =
                            listOf(
                                Inntekt(
                                    beloep = BigDecimal(aarsinntekt / 12),
                                    beskrivelse = "En månedsinntekt",
                                ),
                            ),
                        summertBeloep = BigDecimal(aarsinntekt / 12),
                    )
                },
        )
    }
}

data class AInntektMaaned(
    val maaned: YearMonth,
    val inntekter: List<Inntekt>,
    val summertBeloep: BigDecimal,
)

data class Inntekt(
    val beloep: BigDecimal,
    val beskrivelse: String,
)

data class HendelseslisteFraSkatt(
    val hendelser: List<SkatteoppgjoerHendelser>,
) {
    companion object {
        fun stub(
            startSekvensnummer: Long = 0,
            antall: Int = 10,
            aar: Int = 2024,
        ): HendelseslisteFraSkatt {
            val hendelser =
                List(antall) { index ->
                    SkatteoppgjoerHendelser(
                        gjelderPeriode = aar.toString(),
                        hendelsetype = "NY", // TODO
                        identifikator = index.toString(), // TODO
                        sekvensnummer = startSekvensnummer + index,
                        somAktoerid = false,
                    )
                }
            return HendelseslisteFraSkatt(hendelser)
        }
    }
}

data class SkatteoppgjoerHendelser(
    val gjelderPeriode: String, // inntektsaar
    val hendelsetype: String,
    val identifikator: String,
    val sekvensnummer: Long,
    val somAktoerid: Boolean,
)

/* TODO: bruke hvis vi tar i bruk redigerbart innhold
data class EtteroppgjoerBrevRequestData(
    val redigerbar: BrevRedigerbarInnholdData?,
    val innhold: BrevFastInnholdData,
)

object EtteroppgjoerBrevMapper {
    // TODO: mappe til riktige brevvarianter avhengig av data i forbehandlingen
    fun fra(forbehandling: EtteroppgjoerForbehandling): EtteroppgjoerBrevRequestData =
        EtteroppgjoerBrevRequestData(
            redigerbar = null,
            innhold = EtteroppgjoerBrevData.ForhaandsvarselBrevData(forbehandling.sak),
        )
}
 */
