package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.utbetaling.common.UUID30
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class VedtakId(
    val value: Long,
)

data class SakId(
    val value: Long,
)

data class BehandlingId(
    val value: UUID,
    val shortValue: UUID30,
)

data class UtbetalingslinjeId(
    val value: Long,
)

data class Foedselsnummer(
    val value: String,
)

data class NavIdent(
    val value: String,
)

enum class UtbetalingStatus {
    GODKJENT,
    GODKJENT_MED_FEIL,
    AVVIST,
    FEILET,
    SENDT,
    MOTTATT,
}

enum class Utbetalingslinjetype {
    OPPHOER,
    UTBETALING,
}

data class PeriodeForUtbetaling(
    val fra: LocalDate,
    val til: LocalDate? = null,
)

data class Kvittering(
    val oppdrag: Oppdrag,
    val alvorlighetsgrad: String,
    val beskrivelse: String? = null,
    val kode: String? = null,
)

data class Utbetaling(
    val id: UUID,
    val sakId: SakId,
    val sakType: Saktype,
    val behandlingId: BehandlingId,
    val vedtakId: VedtakId,
    val opprettet: Tidspunkt,
    val endret: Tidspunkt,
    val avstemmingsnoekkel: Tidspunkt,
    val stoenadsmottaker: Foedselsnummer,
    val saksbehandler: NavIdent,
    val saksbehandlerEnhet: Enhetsnummer? = null,
    val attestant: NavIdent,
    val attestantEnhet: Enhetsnummer? = null,
    val vedtak: Utbetalingsvedtak,
    val oppdrag: Oppdrag? = null,
    val kvittering: Kvittering? = null,
    val utbetalingslinjer: List<Utbetalingslinje>,
    val utbetalingshendelser: List<Utbetalingshendelse>,
) {
    fun status() = utbetalingshendelser.minByOrNull { it.status }?.status ?: UtbetalingStatus.MOTTATT
}

data class Utbetalingslinje(
    val id: UtbetalingslinjeId,
    val type: Utbetalingslinjetype,
    val utbetalingId: UUID,
    val erstatterId: UtbetalingslinjeId? = null,
    val opprettet: Tidspunkt,
    val sakId: SakId,
    val periode: PeriodeForUtbetaling,
    val beloep: BigDecimal? = null,
    val klassifikasjonskode: OppdragKlassifikasjonskode,
    val kjoereplan: Kjoereplan,
)

/**
 * Samme som [Utbetaling], men noen felt er ekskludert da de ikke benyttes
 * av konsistensavstemmingen og krever mye prosessering for å lastes.
 */
data class UtbetalingForKonsistensavstemming(
    val id: UUID,
    val sakId: SakId,
    val sakType: Saktype,
    val behandlingId: BehandlingId,
    val vedtakId: VedtakId,
    val opprettet: Tidspunkt,
    val endret: Tidspunkt,
    val avstemmingsnoekkel: Tidspunkt,
    val stoenadsmottaker: Foedselsnummer,
    val saksbehandler: NavIdent,
    val saksbehandlerEnhet: Enhetsnummer? = null,
    val attestant: NavIdent,
    val attestantEnhet: Enhetsnummer? = null,
    val utbetalingslinjer: List<Utbetalingslinje>,
    val utbetalingshendelser: List<Utbetalingshendelse>,
)

enum class Kjoereplan(
    private val oppdragVerdi: String,
) {
    // ref. https://confluence.adeo.no/display/OKSY/Inputdata+fra+fagrutinen+til+Oppdragssystemet:
    // "Bruk-kjoreplan gjør det mulig å velge om delytelsen skal beregnes/utbetales i henhold til kjøreplanen eller om
    // dette skal skje idag. Verdien 'N' medfører at beregningen kjøres idag. Beregningen vil bare gjelde
    // beregningsperioder som allerede er forfalt."
    NESTE_PLANLAGTE_UTBETALING("J"),
    MED_EN_GANG("N"),
    ;

    override fun toString(): String = oppdragVerdi

    companion object {
        fun fraKode(kode: String): Kjoereplan =
            when (kode.trim()) {
                "J", "j" -> NESTE_PLANLAGTE_UTBETALING
                "N", "n" -> MED_EN_GANG
                else -> throw IllegalArgumentException("kode $kode er ikke en gjenkjent verdi for bruk_kjoereplan")
            }
    }
}

/*
    Noen klassekoder:
    https://confluence.adeo.no/pages/viewpage.action?pageId=329267485
 */
enum class OppdragKlassifikasjonskode(
    private val oppdragVerdi: String,
    val tekniskArt: Boolean = false,
) {
    BARNEPENSJON_OPTP("BARNEPENSJON-OPTP"),
    BARNEPEFOER2024_OPTP("BARNEPEFØR2024-OPTP"),
    OMSTILLINGSTOENAD_OPTP("OMSTILLINGOR"),
    FORSKUDDSSKATT("FSKTSKAT"),
    FEILKONTO("KL_KODE_FEIL_PEN"),
    FEILKONTO_OMSTILLINGSTOENAD("KL_KODE_FEIL_OMSTILL"),
    MOTPOSTERING("TBMOTOBS", tekniskArt = true),
    BFRISKAT("BFRISKAT"),
    PENSPKBPE01("PENSPKBPE01"), // Statens pensjonskasse Barnepensjon
    BSKTKRED("BSKTKRED"), // Kreditor disponerer - dette er mest sannsynlig verge
    KREDKRED("KREDKRED"), // Bøtetrekk, erstatningskrav, husleietrekk, eksternt kreditortrekk eller krav på offentlige avgifter
    TBTREKK("TBTREKK"), // Innbetalinger feilutbetalt stønad
    ;

    override fun toString(): String = oppdragVerdi

    companion object {
        fun fraString(string: String): OppdragKlassifikasjonskode =
            when (string) {
                "BARNEPENSJON-OPTP" -> BARNEPENSJON_OPTP
                "BARNEPENSJON_OPTP" -> BARNEPENSJON_OPTP
                "BARNEPEFØR2024-OPTP" -> BARNEPEFOER2024_OPTP
                "BARNEPEFØR2024_OPTP" -> BARNEPEFOER2024_OPTP
                "OMSTILLINGOR" -> OMSTILLINGSTOENAD_OPTP
                "FSKTSKAT" -> FORSKUDDSSKATT
                "KL_KODE_FEIL_PEN" -> FEILKONTO
                "KL_KODE_FEIL_OMSTILL" -> FEILKONTO_OMSTILLINGSTOENAD
                "TBMOTOBS" -> MOTPOSTERING
                "BFRISKAT" -> BFRISKAT
                "BSKTKRED" -> BSKTKRED
                "KREDKRED" -> KREDKRED
                "PENSPKBPE01" -> PENSPKBPE01
                "TBTREKK" -> TBTREKK
                else -> throw IllegalArgumentException("$string er ikke en støttet OppdragKlassifikasjonskode!")
            }
    }
}

data class Utbetalingshendelse(
    val id: UUID = UUID.randomUUID(),
    val utbetalingId: UUID,
    val tidspunkt: Tidspunkt = Tidspunkt.now(),
    val status: UtbetalingStatus,
)
