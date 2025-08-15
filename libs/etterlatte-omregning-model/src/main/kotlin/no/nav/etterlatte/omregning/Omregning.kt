package no.nav.etterlatte.omregning

import com.fasterxml.jackson.module.kotlin.treeToValue
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.EventnameHendelseType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.inntektsjustering.MottattInntektsjustering
import java.time.LocalDate
import java.util.UUID

/*
* En hendelseflyt som automatisk revurderer en sak.
* Flyt benyttes fra flere hold blant annet Regulering, Inntektsjsutering, etc.
*
*/
enum class OmregningHendelseType : EventnameHendelseType {
    KLAR_FOR_OMREGNING,
    BEHANDLING_OPPRETTA,
    VILKAARSVURDERT,
    TRYGDETID_KOPIERT,
    BEREGNA,
    ;

    override fun lagEventnameForType(): String = "OMREGNING:${this.name}"
}

enum class UtbetalingVerifikasjon {
    INGEN,
    SIMULERING,
    SIMULERING_AVBRYT_ETTERBETALING_ELLER_TILBAKEKREVING,
}

/*
* Verdier til omregninghendelse vil tilføres underveis i omregningsløpet og flere felter er derfor nødt ti å være mutable.
* Derimot er det ønskelig at feltene er immutable og non null etter de blir satt.
* Av den grunn er det er feltene tilgjengeliggjort gjennom "hent-" og "endre-" metoder.
*
* OBS! For å ta dette objektet i bruk i en melding er du nødt til å bruke metode "toPacket" for å få alle feltene tilgjengelig.
*/
data class OmregningData(
    val kjoering: String,
    val sakId: SakId,
    val revurderingaarsak: Revurderingaarsak,
    private var fradato: LocalDate? = null,
    private var sakType: SakType? = null,
    private var behandlingId: UUID? = null,
    private var forrigeBehandlingId: UUID? = null,
    val utbetalingVerifikasjon: UtbetalingVerifikasjon = UtbetalingVerifikasjon.INGEN,
    private var inntektsjustering: MottattInntektsjustering? = null,
) {
    fun toPacket() =
        OmregningDataPacket(
            kjoering,
            sakId,
            revurderingaarsak,
            fradato,
            sakType,
            behandlingId,
            forrigeBehandlingId,
            utbetalingVerifikasjon,
            inntektsjustering,
        )

    fun hentFraDato(): LocalDate = fradato ?: throw OmregningshendelseHarFeilTilstand(OmregningData::fradato.name)

    fun endreFraDato(value: LocalDate) {
        if (fradato != null) {
            throw OmregningshendelseSkalIkkeMuteres(OmregningData::fradato.name)
        }
        fradato = value
    }

    fun hentSakType(): SakType = sakType ?: throw OmregningshendelseHarFeilTilstand(OmregningData::sakType.name)

    fun endreSakType(value: SakType) {
        if (sakType != null) {
            throw OmregningshendelseSkalIkkeMuteres(OmregningData::sakType.name)
        }
        sakType = value
    }

    fun hentBehandlingId() = behandlingId ?: throw OmregningshendelseHarFeilTilstand(OmregningData::behandlingId.name)

    fun endreBehandlingId(value: UUID) {
        if (behandlingId != null) {
            throw OmregningshendelseSkalIkkeMuteres(OmregningData::behandlingId.name)
        }
        behandlingId = value
    }

    fun hentForrigeBehandlingid() = forrigeBehandlingId ?: throw OmregningshendelseHarFeilTilstand(OmregningData::forrigeBehandlingId.name)

    fun endreForrigeBehandlingid(value: UUID) {
        if (forrigeBehandlingId != null) {
            throw OmregningshendelseSkalIkkeMuteres(OmregningData::forrigeBehandlingId.name)
        }
        forrigeBehandlingId = value
    }

    fun hentInntektsjustering() = inntektsjustering ?: throw OmregningshendelseHarFeilTilstand(OmregningData::inntektsjustering.name)

    fun endreInntektsjustering(value: MottattInntektsjustering) {
        if (inntektsjustering != null) {
            throw OmregningshendelseSkalIkkeMuteres(OmregningData::inntektsjustering.name)
        }
        inntektsjustering = value
    }
}

data class OmregningDataPacket(
    val kjoering: String,
    val sakId: SakId,
    val revurderingaarsak: Revurderingaarsak,
    val fradato: LocalDate?,
    val sakType: SakType?,
    val behandlingId: UUID?,
    val forrigeBehandlingId: UUID?,
    val utbetalingVerifikasjon: UtbetalingVerifikasjon,
    val inntektsjustering: MottattInntektsjustering?,
) {
    companion object KEYS {
        val KEY = "hendelse_data"
        val KJOERING = "$KEY.${OmregningDataPacket::kjoering.name}"
        val SAK_ID = "$KEY.${OmregningDataPacket::sakId.name}"
        val SAK_TYPE = "$KEY.${OmregningDataPacket::sakType.name}"
        val FRA_DATO = "$KEY.${OmregningDataPacket::fradato.name}"
        val BEHANDLING_ID = "$KEY.${OmregningDataPacket::behandlingId.name}"
        val FORRIGE_BEHANDLING_ID = "$KEY.${OmregningDataPacket::forrigeBehandlingId.name}"
        val REV_AARSAK = "$KEY.${OmregningDataPacket::revurderingaarsak.name}"
        val INNTEKTSJUSTERING = "$KEY.${OmregningDataPacket::inntektsjustering.name}"
    }
}

data class OmregningInntektsjustering(
    val inntekt: Int,
    val inntektUtland: Int,
)

var JsonMessage.omregningData: OmregningData
    get() = objectMapper.treeToValue(this[OmregningDataPacket.KEY])
    set(name) {
        this[OmregningDataPacket.KEY] = name.toPacket()
    }

class OmregningshendelseHarFeilTilstand(
    felt: String,
) : InternfeilException("${OmregningData::class.simpleName} krever på dette stadiet $felt")

class OmregningshendelseSkalIkkeMuteres(
    felt: String,
) : InternfeilException("${OmregningData::class.simpleName}.$felt skal ikke kunne endres etter det er satt.")
