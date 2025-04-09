package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.brev.BrevFastInnholdData
import no.nav.etterlatte.brev.BrevRedigerbarInnholdData
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.oms.EtteroppgjoerBrevData.Forhaandsvarsel
import no.nav.etterlatte.brev.model.oms.EtteroppgjoerBrevData.ForhaandsvarselInnhold
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.BeregnetEtteroppgjoerResultatDto
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.pensjon.brevbaker.api.model.Kroner
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
    val faktiskInntekt: FaktiskInntekt?,
    val avkortingFaktiskInntekt: AvkortingDto?,
    val beregnetEtteroppgjoerResultat: BeregnetEtteroppgjoerResultatDto,
)

enum class EtteroppgjoerForbehandlingStatus {
    OPPRETTET,
    UNDER_BEHANDLING,
    VARSELBREV_SENDT,
    SVAR_MOTTATT,
    INGEN_SVAR_INNEN_TIDSFRIST,
}

data class EtteroppgjoerForbehandling(
    val id: UUID,
    val hendelseId: UUID,
    val status: EtteroppgjoerForbehandlingStatus,
    val sak: Sak,
    val aar: Int,
    val innvilgetPeriode: Periode,
    val opprettet: Tidspunkt,
    val brevId: Long?,
) {
    companion object {
        fun opprett(
            sak: Sak,
            innvilgetPeriode: Periode,
        ) = EtteroppgjoerForbehandling(
            id = UUID.randomUUID(),
            hendelseId = UUID.randomUUID(),
            sak = sak,
            status = EtteroppgjoerForbehandlingStatus.OPPRETTET,
            aar = innvilgetPeriode.fom.year,
            innvilgetPeriode = innvilgetPeriode,
            opprettet = Tidspunkt.now(),
            brevId = null,
        )
    }

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

data class EtteroppgjoerBrevRequestData(
    val redigerbar: BrevRedigerbarInnholdData,
    val innhold: BrevFastInnholdData,
    val data: DetaljertForbehandlingDto,
)

object EtteroppgjoerBrevDataMapper {
    fun fra(data: DetaljertForbehandlingDto) =
        EtteroppgjoerBrevRequestData(
            redigerbar =
                ForhaandsvarselInnhold(
                    sak = data.behandling.sak,
                ),
            innhold =
                Forhaandsvarsel(
                    bosattUtland = false, // TODO
                    norskInntekt = false, // TODO
                    etteroppgjoersAar = data.behandling.aar,
                    rettsgebyrBeloep = Kroner(1234), // TODO hent faktisk rettsgebyr
                    resultatType = data.beregnetEtteroppgjoerResultat.resultatType,
                    inntekt = Kroner(data.beregnetEtteroppgjoerResultat.utbetaltStoenad.toInt()), // TODO long
                    faktiskInntekt = Kroner(data.beregnetEtteroppgjoerResultat.nyBruttoStoenad.toInt()), // TODO long
                    avviksBeloep = Kroner(data.beregnetEtteroppgjoerResultat.differanse.toInt()), // TODO long
                ),
            data = data,
        )
}

data class FaktiskInntekt(
    val loennsinntekt: Long,
    val afp: Long,
    val naeringsinntekt: Long,
    val utland: Long,
    val spesifikasjon: String,
)
