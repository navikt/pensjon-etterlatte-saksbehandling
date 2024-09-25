package no.nav.etterlatte.rapidsandrivers

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.EventnameHendelseType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.helse.rapids_rivers.JsonMessage
import java.time.LocalDate
import java.util.UUID

/*
* En hendelseflyt som automatisk revurderer en sak.
* Flyt benyttes fra flere hold blant annet Regulering, Inntektsjsutering, etc.
*
* TODO omregning med vedtaksbrev støttes ikke enda
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

/*
* Verdier til omregninghendelse vil tilføres underveis i omregningsløpet og flere felter er derfor nødt ti å være mutable.
* Derimot er det ønskelig at feltene er immutable og non null etter de blir satt.
* Av den grunn er det er feltene tilgjengeliggjort gjennom "hent-" og "endre-" metoder.
*/
data class Omregningshendelse(
    val sakId: SakId,
    val fradato: LocalDate,
    val revurderingaarsak: Revurderingaarsak,
    private var sakType: SakType? = null,
    private var behandlingId: UUID? = null,
    private var forrigeBehandlingId: UUID? = null,
) {
    fun toPacket() =
        OmregningshendelsePacket(
            sakId,
            fradato,
            revurderingaarsak,
            sakType,
            behandlingId,
            forrigeBehandlingId,
        )

    fun hentSakType(): SakType = sakType ?: throw OmregningshendelseHarFeilTilstand(Omregningshendelse::sakType.name)

    fun endreSakType(value: SakType) {
        if (sakType != null) {
            throw OmregningshendelseSkalIkkeMuteres(Omregningshendelse::sakType.name)
        }
        sakType = value
    }

    fun hentBehandlingId() = behandlingId ?: throw OmregningshendelseHarFeilTilstand(Omregningshendelse::behandlingId.name)

    fun endreBehandlingId(value: UUID) {
        if (behandlingId != null) {
            throw OmregningshendelseSkalIkkeMuteres(Omregningshendelse::behandlingId.name)
        }
        behandlingId = value
    }

    fun hentForrigeBehandlingid() =
        forrigeBehandlingId ?: throw OmregningshendelseHarFeilTilstand(Omregningshendelse::forrigeBehandlingId.name)

    fun endreForrigeBehandlingid(value: UUID) {
        if (forrigeBehandlingId != null) {
            throw OmregningshendelseSkalIkkeMuteres(Omregningshendelse::forrigeBehandlingId.name)
        }
        forrigeBehandlingId = value
    }
}

data class OmregningshendelsePacket(
    val sakId: SakId,
    val fradato: LocalDate,
    val revurderingaarsak: Revurderingaarsak,
    val sakType: SakType?,
    val behandlingId: UUID?,
    val forrigeBehandlingId: UUID?,
) {
    companion object KEYS {
        const val SAKTYPE = "$HENDELSE_DATA_KEY.sakType"
        const val BEHANDLING_ID = "$HENDELSE_DATA_KEY.behandlingId"
        const val FORRIGE_BEHANDLING_ID = "$HENDELSE_DATA_KEY.forrigeBehandlingId"
    }
}

var JsonMessage.omregninshendelse: Omregningshendelse
    get() = objectMapper.treeToValue(this[HENDELSE_DATA_KEY])
    set(name) {
        this[HENDELSE_DATA_KEY] = name.toPacket()
    }

data class OmregningshendelseHarFeilTilstand(
    val felt: String,
) : IllegalStateException("Omregninghendelse krever på dette stadiet $felt")

data class OmregningshendelseSkalIkkeMuteres(
    val felt: String,
) : IllegalStateException("${Omregningshendelse::class.simpleName}.$felt skal ikke kunne endres etter det er satt.")

enum class ReguleringHendelseType : EventnameHendelseType {
    FINN_SAKER_TIL_REGULERING,
    REGULERING_STARTA,
    SAK_FUNNET,
    LOEPENDE_YTELSE_FUNNET,
    YTELSE_IKKE_LOEPENDE,
    ;

    override fun lagEventnameForType(): String = "REGULERING:${this.name}"
}

/*
* TODO
* Ubrukt enn så lenge men tanken er at dette vil bli starten på en flyt
* til en jobb hvor flere saker gjennomfører omregning
*/
enum class MasseOmregningHendelseType : EventnameHendelseType {
    START_MASSE_OMREGNING,
    ;

    override fun lagEventnameForType(): String = "MASSE_OMREGNING:${this.name}"
}
