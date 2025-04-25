package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.brev.BrevFastInnholdData
import no.nav.etterlatte.brev.BrevRedigerbarInnholdData
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.oms.EtteroppgjoerBrevData
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.BeregnetEtteroppgjoerResultatDto
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
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
    FERDIGSTILT_FORBEHANDLING,
    UNDER_REVURDERING,
    FERDIGSTILT_REVURDERING,
}

data class EtteroppgjoerForbehandling(
    val id: UUID,
    val hendelseId: UUID,
    val opprettet: Tidspunkt,
    val status: EtteroppgjoerForbehandlingStatus,
    val relatertForbehandlingId: UUID? = null,
    val sak: Sak,
    val aar: Int,
    val innvilgetPeriode: Periode,
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

    fun tilBeregnet(): EtteroppgjoerForbehandling {
        if (status in listOf(EtteroppgjoerForbehandlingStatus.OPPRETTET, EtteroppgjoerForbehandlingStatus.BEREGNET)) {
            return copy(status = EtteroppgjoerForbehandlingStatus.BEREGNET)
        } else {
            throw InternfeilException("Kunne ikke endre status fra $status til ${EtteroppgjoerForbehandlingStatus.BEREGNET}")
        }
    }

    fun tilFerdigstilt(): EtteroppgjoerForbehandling {
        if (status == EtteroppgjoerForbehandlingStatus.BEREGNET) {
            return copy(status = EtteroppgjoerForbehandlingStatus.FERDIGSTILT)
        } else {
            throw InternfeilException("Kunne ikke endre status fra $status til ${EtteroppgjoerForbehandlingStatus.FERDIGSTILT}")
        }
    }

    fun medBrev(opprettetBrev: Brev): EtteroppgjoerForbehandling = this.copy(brevId = opprettetBrev.id)

    fun erUnderBehandling() =
        status in
            listOf(
                EtteroppgjoerForbehandlingStatus.OPPRETTET,
                EtteroppgjoerForbehandlingStatus.BEREGNET,
            )

    fun erFerdigstilt() =
        status in
            listOf(
                EtteroppgjoerForbehandlingStatus.FERDIGSTILT,
            )
}

enum class EtteroppgjoerForbehandlingStatus {
    OPPRETTET,
    BEREGNET,
    FERDIGSTILT,
}

data class DetaljertForbehandlingDto(
    val behandling: EtteroppgjoerForbehandling,
    val sisteIverksatteBehandling: UUID,
    val opplysninger: EtteroppgjoerOpplysninger,
    val faktiskInntekt: FaktiskInntekt?,
    val avkortingFaktiskInntekt: AvkortingDto?,
    val beregnetEtteroppgjoerResultat: BeregnetEtteroppgjoerResultatDto?,
)

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
    fun fra(
        data: DetaljertForbehandlingDto,
        sisteIverksatteBehandling: Behandling,
        pensjonsgivendeInntekt: PensjonsgivendeInntektFraSkatt?,
    ): EtteroppgjoerBrevRequestData {
        krevIkkeNull(data.beregnetEtteroppgjoerResultat) {
            "Beregnet etteroppgjoer resultat er null og kan ikke vises i brev"
        }

        val bosattUtland = sisteIverksatteBehandling.utlandstilknytning?.type == UtlandstilknytningType.BOSATT_UTLAND

        // TODO: usikker om dette blir rett, følge opp ifm testing
        val norskInntekt = pensjonsgivendeInntekt != null && pensjonsgivendeInntekt.inntekter.isNotEmpty()

        return EtteroppgjoerBrevRequestData(
            redigerbar =
                EtteroppgjoerBrevData.ForhaandsvarselInnhold(
                    sak = data.behandling.sak,
                ),
            innhold =
                EtteroppgjoerBrevData.Forhaandsvarsel(
                    bosattUtland = bosattUtland,
                    norskInntekt = norskInntekt,
                    etteroppgjoersAar = data.behandling.aar,
                    rettsgebyrBeloep = Kroner(data.beregnetEtteroppgjoerResultat.grense.rettsgebyr),
                    resultatType = data.beregnetEtteroppgjoerResultat.resultatType,
                    inntekt = Kroner(data.beregnetEtteroppgjoerResultat.utbetaltStoenad.toInt()),
                    faktiskInntekt = Kroner(data.beregnetEtteroppgjoerResultat.nyBruttoStoenad.toInt()),
                    avviksBeloep = Kroner(data.beregnetEtteroppgjoerResultat.differanse.toInt()),
                ),
            data = data,
        )
    }
}

data class FaktiskInntekt(
    val loennsinntekt: Long,
    val afp: Long,
    val naeringsinntekt: Long,
    val utland: Long,
    val spesifikasjon: String,
)
