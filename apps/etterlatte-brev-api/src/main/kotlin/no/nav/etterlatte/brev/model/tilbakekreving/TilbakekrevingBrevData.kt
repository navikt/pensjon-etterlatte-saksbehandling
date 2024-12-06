package no.nav.etterlatte.brev.model.tilbakekreving

import no.nav.etterlatte.brev.BrevDataFerdigstilling
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.tilbakekreving.JaNei
import no.nav.etterlatte.libs.common.tilbakekreving.Tilbakekreving
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingResultat
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVarsel
import no.nav.etterlatte.libs.common.tilbakekreving.kunYtelse
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate

data class TilbakekrevingBrevDTO(
    override val innhold: List<Slate.Element>,
    val sakType: SakType,
    val bosattUtland: Boolean,
    val brukerNavn: String,
    val doedsbo: Boolean,
    val varsel: TilbakekrevingVarsel,
    val datoVarselEllerVedtak: LocalDate,
    val datoTilsvarBruker: LocalDate?,
    val tilbakekreving: TilbakekrevingData,
) : BrevDataFerdigstilling {
    companion object {
        fun fra(
            redigerbart: List<Slate.Element>,
            muligTilbakekreving: Tilbakekreving?,
            sakType: SakType,
            utlandstilknytningType: UtlandstilknytningType?,
            soekerNavn: String,
        ): TilbakekrevingBrevDTO {
            val tilbakekreving =
                muligTilbakekreving
                    ?: throw BrevDataTilbakerevingHarManglerException("Vedtak mangler tilbakekreving")

            val perioderSortert = tilbakekreving.perioder.sortedBy { it.maaned }

            return TilbakekrevingBrevDTO(
                innhold = redigerbart,
                sakType = sakType,
                bosattUtland = utlandstilknytningType == UtlandstilknytningType.BOSATT_UTLAND,
                brukerNavn = soekerNavn,
                doedsbo = tilbakekreving.vurdering?.doedsbosak == JaNei.JA,
                varsel = tilbakekreving.vurdering?.forhaandsvarsel ?: throw TilbakeKrevingManglerVarsel(),
                datoVarselEllerVedtak =
                    tilbakekreving.vurdering?.forhaandsvarselDato ?: throw TilbakeKrevingManglerForhaandsvarselDatoException(),
                datoTilsvarBruker = tilbakekreving.vurdering?.tilsvar?.dato,
                tilbakekreving =
                    TilbakekrevingData(
                        fraOgMed = perioderSortert.first().maaned.atDay(1),
                        tilOgMed = perioderSortert.last().maaned.atEndOfMonth(),
                        skalTilbakekreve =
                            tilbakekreving.perioder.any {
                                it.tilbakekrevingsbeloep.kunYtelse().any { beloep ->
                                    beloep.resultat == TilbakekrevingResultat.FULL_TILBAKEKREV ||
                                        beloep.resultat == TilbakekrevingResultat.DELVIS_TILBAKEKREV
                                }
                            },
                        helTilbakekreving =
                            tilbakekreving.perioder.any {
                                it.tilbakekrevingsbeloep.kunYtelse().any { beloep ->
                                    beloep.resultat == TilbakekrevingResultat.FULL_TILBAKEKREV
                                }
                            },
                        perioder = tilbakekrevingsPerioder(tilbakekreving),
                        harRenteTillegg = sjekkOmHarRenter(tilbakekreving),
                        summer = perioderSummert(tilbakekreving),
                    ),
            )
        }

        private fun tilbakekrevingsPerioder(tilbakekreving: Tilbakekreving) =
            tilbakekreving.perioder.map { periode ->
                val beloepMedKunYtelse = periode.tilbakekrevingsbeloep.kunYtelse()

                TilbakekrevingPeriodeData(
                    maaned = periode.maaned.atDay(1),
                    beloeper =
                        periode.let { beloeper ->
                            val beregnetFeilutbetaling = beloepMedKunYtelse.sumOf { beloep -> beloep.beregnetFeilutbetaling ?: 0 }
                            val bruttoTilbakekreving = beloepMedKunYtelse.sumOf { beloep -> beloep.bruttoTilbakekreving ?: 0 }
                            val skatt = beloepMedKunYtelse.sumOf { beloep -> beloep.skatt ?: 0 }
                            val netto = beloepMedKunYtelse.sumOf { beloep -> beloep.nettoTilbakekreving ?: 0 }
                            val renteTillegg = beloepMedKunYtelse.sumOf { beloep -> beloep.rentetillegg ?: 0 }

                            TilbakekrevingBeloeperData(
                                feilutbetaling = Kroner(beregnetFeilutbetaling),
                                bruttoTilbakekreving = Kroner(bruttoTilbakekreving),
                                fradragSkatt = Kroner(skatt),
                                nettoTilbakekreving = Kroner(netto),
                                renteTillegg = Kroner(renteTillegg),
                                sumNettoRenter = Kroner(netto + renteTillegg),
                            )
                        },
                    resultat =
                        periode.tilbakekrevingsbeloep
                            .map { it.resultat ?: throw TilbakekrevingManglerResultatException("Alle perioder må ha resultat") }
                            .let {
                                TilbakekrevingResultat.hoyesteGradAvTilbakekreving(it)
                            } ?: throw TilbakekrevingManglerResultatException("Fant ingen resultat"),
                )
            }

        private fun perioderSummert(tilbakekreving: Tilbakekreving): TilbakekrevingBeloeperData {
            val perioder = tilbakekrevingsPerioder(tilbakekreving)

            val netto = perioder.sumOf { it.beloeper.nettoTilbakekreving.value }
            val renteTillegg = perioder.sumOf { it.beloeper.renteTillegg.value }
            return TilbakekrevingBeloeperData(
                feilutbetaling = Kroner(perioder.sumOf { it.beloeper.feilutbetaling.value }),
                bruttoTilbakekreving = Kroner(perioder.sumOf { it.beloeper.bruttoTilbakekreving.value }),
                fradragSkatt = Kroner(perioder.sumOf { it.beloeper.fradragSkatt.value }),
                nettoTilbakekreving = Kroner(netto),
                renteTillegg = Kroner(renteTillegg),
                sumNettoRenter = Kroner(netto + renteTillegg),
            )
        }

        private fun sjekkOmHarRenter(tilbakekreving: Tilbakekreving) =
            tilbakekreving.perioder.any { periode ->
                periode.tilbakekrevingsbeloep.any {
                    it.rentetillegg?.let { rentetillegg -> rentetillegg > 0 }
                        ?: false
                }
            }
    }
}

data class TilbakekrevingData(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val skalTilbakekreve: Boolean,
    val helTilbakekreving: Boolean,
    val harRenteTillegg: Boolean,
    val perioder: List<TilbakekrevingPeriodeData>,
    val summer: TilbakekrevingBeloeperData,
)

data class TilbakekrevingPeriodeData(
    val maaned: LocalDate,
    val beloeper: TilbakekrevingBeloeperData,
    val resultat: TilbakekrevingResultat,
)

data class TilbakekrevingBeloeperData(
    val feilutbetaling: Kroner,
    val bruttoTilbakekreving: Kroner,
    val nettoTilbakekreving: Kroner,
    val fradragSkatt: Kroner,
    val renteTillegg: Kroner,
    val sumNettoRenter: Kroner,
)

class BrevDataTilbakerevingHarManglerException(
    message: String,
) : RuntimeException(message)

class TilbakekrevingManglerResultatException(
    message: String,
) : UgyldigForespoerselException(
        code = "TILBAKEKREVING_MANGLER_RESULTAT",
        detail = message,
    )

class TilbakeKrevingManglerForhaandsvarselDatoException :
    UgyldigForespoerselException(
        code = "TILBAKEKREVING_MANGLER_VURDERING_FORHÅNDSVARSELSDATO",
        detail = "Kan ikke generere pdf uten vurdering forhaandsvarselDato av tilbakekreving",
    )

class TilbakeKrevingManglerVarsel :
    UgyldigForespoerselException(
        code = "TILBAKEKREVING_MANGLER_VURDERING_VARSEL",
        detail = "Kan ikke generere pdf uten at varsel er satt under vurdering",
    )
