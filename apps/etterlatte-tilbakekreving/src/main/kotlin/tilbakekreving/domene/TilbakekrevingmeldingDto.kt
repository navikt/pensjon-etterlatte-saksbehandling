package no.nav.etterlatte.tilbakekreving.domene

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRootName

sealed class TilbakekrevingsmeldingDto

@JsonRootName("endringKravOgVedtakstatus")
internal data class KravgrunnlagStatusendringRootDto(
    @field:JsonProperty(value = "kravOgVedtakstatus")
    val endringKravOgVedtakstatus: KravgrunnlagStatusendringDto,
) : TilbakekrevingsmeldingDto()

internal data class KravgrunnlagStatusendringDto(
    @field:JsonProperty(value = "vedtakId")
    val vedtakId: String,
    @field:JsonProperty(value = "kodeStatusKrav")
    val kodeStatusKrav: String,
    @field:JsonProperty(value = "kodeFagomraade")
    val kodeFagområde: String,
    @field:JsonProperty(value = "fagsystemId")
    val fagsystemId: String,
    @field:JsonProperty(value = "vedtakGjelderId")
    val vedtakGjelderId: String,
    @field:JsonProperty(value = "typeGjelderId")
    val idTypeGjelder: String,
)

/**
 * Melding som oppdrag legger på køen er wrappet i en "dobbel" root.
 * Kan hende det finnes noen triks for å strippe bort rot-noden i xml.
 */
@JsonRootName(value = "detaljertKravgrunnlagMelding")
internal data class KravgrunnlagRootDto(
    @field:JsonProperty(value = "detaljertKravgrunnlag")
    val kravgrunnlagDto: KravgrunnlagDto,
) : TilbakekrevingsmeldingDto()

/**
 * Representerer kravmeldingen (XML) som oppdrag legger på kravgrunnlagskøen.
 * Bruker kun String/nullable i deserialiseringssteget, slik at det kun kan feile på formfeil.
 * Ytteligere validering gjøres i domenet og/eller i mapping til domenet.
 * Se under avsnittet 'Grensesnitt Vedtak - Grunnlagsinformasjon': https://confluence.adeo.no/display/OKSY/Detaljer+om+de+enkelte+ID-koder
 *
 * Feltene er dokumenter som: Nr - Datafelt - Format - Kommentar
 */
internal data class KravgrunnlagDto(
    /**
     * 1 - Kravgrunnlag-id - 9(10) - Identifikasjon av tilbakekrevingsgrunnlaget
     *
     * F.eks. 250227, 250241 og 8631869768112589191 (eksemplene er fra permittering i preprod)
     */
    @field:JsonProperty(value = "kravgrunnlagId")
    val kravgrunnlagId: String,

    /**
     * 2 - Vedtak-id - 9(10) - Identifikasjon av tilbakekrevingsvedtaket opprettet av Tilbakekrevingskomponenten
     *
     * F.eks. 348005
     */
    @field:JsonProperty(value = "vedtakId")
    val vedtakId: String,

    /** 3 - Kode-status-krav - X(04) - Status på kravgrunnlaget:
     * - ANNU - Kravgrunnlag annulert
     * - ANOM - Kravgrunnlag annulert ved omg
     * - AVSL - Avsluttet kravgrunnlag
     * - BEHA - Kravgrunnlag ferdigbehandlet
     * - ENDR - Endret kravgrunnlag
     * - FEIL - Feil på kravgrunnlag
     * - MANU - Manuell behandling
     * - NY   - Nytt kravgrunnlag
     * - SPER - Kravgrunnlag sperret */
    @field:JsonProperty(value = "kodeStatusKrav")
    val kodeStatusKrav: String,

    /**
     * 4 - Kode-fagomraade - X(08) - Fagområdet på feilutbetalingen
     *
     * F.eks. SUUFORE
     */
    @field:JsonProperty(value = "kodeFagomraade")
    val kodeFagområde: String,

    /**
     * 5 - Fagsystem-id - X(30) - Fagsystemets identifikasjon av vedtaket som har feilutbetaling.
     *
     * Dette vil tilsvare saksnummer i SU: F.eks. 2021
     */
    @field:JsonProperty(value = "fagsystemId")
    val fagsystemId: String,

    /**
     * 6 - Dato-vedtak-fagsystem - X(08) - Dette er fagsystemets vedtak for vedtaket
     *
     * TODO jah: Finn ut om dette feltet inngår i meldingen.
     */
    @field:JsonProperty(value = "datoVedtakFagsystem")
    val datoVedtakFagsystem: String?,

    /**
     * 7 - Vedtak-id-omgjort - 9(10) - Dette er henvisning til forrige gyldige vedtak. Feltet vil bare være utfylt i forbindelse med omgjøring
     *
     * F.eks. 0
     */
    @field:JsonProperty(value = "vedtakIdOmgjort")
    val vedtakIdOmgjort: String?,

    /**
     * 8 - Vedtak-gjelder-id - X(11) - Vanligvis stønadsmottaker (fnr/orgnr) i feilutbetalingen. Dette er Oppdrag-gjelder-id i Oppdragssystemet for feilutbetalingen
     *
     * F.eks. 12345678910
     */
    @field:JsonProperty(value = "vedtakGjelderId")
    val vedtakGjelderId: String,

    /**
     * 9 - Id-type-gjelder - X(20) - Angir om Vedtak-gjelder-id er fnr, orgnr, TSS-nr etc
     *
     * F.eks. PERSON
     */
    @field:JsonProperty(value = "typeGjelderId")
    val idTypeGjelder: String,

    /**
     * 10 - Utbetales-til-id - X(11) - Mottaker av pengene i feilutbetalingen. Dette er utbetales-til-id i Oppdragssystemet for feilutbetalingen
     *
     * F.eks. 12345678910
     */
    @field:JsonProperty(value = "utbetalesTilId")
    val utbetalesTilId: String,

    /**
     * 11 - Id-type-utbet - X(20) - Angir om Utbetales-til-id er fnr, orgnr, TSS-nr etc
     *
     * F.eks. PERSON
     */
    @field:JsonProperty(value = "typeUtbetId")
    val idTypeUtbet: String,

    /**
     * 12 - Kode-hjemmel - X(20) - Lovhjemmel for tilbakekrevingsvedtaket
     *
     * TODO jah: Finn ut om dette feltet inngår i meldingen.
     */
    @field:JsonProperty(value = "kodeHjemmel")
    val kodeHjemmel: String?,

    /**
     * 13 - Renter-beregnes - X(01) - 'J' dersom det skal beregnes renter på kravet (red. anm.: antar det er 'N' i de andre tilfellene)
     *
     * TODO jah: Finn ut om dette feltet inngår i meldingen.
     */
    @field:JsonProperty(value = "renterBeregnes")
    val renterBeregnes: String?,

    /**
     * 14 - Enhet-ansvarlig - X(13) - Ansvarlig enhet utledes av parametre som er oppsettsdata og genereres i Tilbakekrevingskomponenten
     *
     * F.eks. 8020
     */
    @field:JsonProperty(value = "enhetAnsvarlig")
    val enhetAnsvarlig: String,

    /**
     * 15 - Enhet-bosted - X(13) - Bostedsenhet, hentet fra feilutbetalingen
     *
     * F.eks. 8020
     * */
    @field:JsonProperty(value = "enhetBosted")
    val enhetBosted: String,

    /**
     * 16 - Enhet-behandl - X(13) - Behandlende enhet, hentet fra feilutbetalingen
     *
     * F.eks. 8020
     */
    @field:JsonProperty(value = "enhetBehandl")
    val enhetBehandl: String,

    /**
     * 17 - Kontrollfelt - X(26) - Brukes ved innsending av tilbakekrevingsvedtak for å kontrollere at kravgrunnlaget ikke er blitt endret i mellomtiden
     *
     * F.eks. 2021-04-27-18.51.06.913218
     */
    @field:JsonProperty(value = "kontrollfelt")
    val kontrollfelt: String,

    /**
     * 18 - Saksbeh-id - X(08) - Saksbehandler
     *
     * F.eks. K231B433
     */
    @field:JsonProperty(value = "saksbehId")
    val saksbehId: String,

    /**
     * 19 - Referanse - X(30) - Henvisning fra nyeste oppdragslinje
     *
     * F.eks. 01F49912SX9SRRVGT0J5R4WYFR
     *
     * En referanse til utbetalingId (vår) som førte til opprettelse/endring av dette kravgrunnlaget
     * @see [no.nav.su.se.bakover.client.oppdrag.utbetaling.UtbetalingRequest.Oppdragslinje.utbetalingId]
     */
    @field:JsonProperty(value = "referanse")
    val utbetalingId: String,

    @field:JsonProperty(value = "tilbakekrevingsPeriode")
    val tilbakekrevingsperioder: List<Tilbakekrevingsperiode>,
) {
    data class Tilbakekrevingsperiode(
        @field:JsonProperty(value = "periode")
        val periode: Periode,

        /** F.eks. 1027.00 */
        @field:JsonProperty(value = "belopSkattMnd")
        val skattebeløpPerMåned: String,

        @field:JsonProperty(value = "tilbakekrevingsBelop")
        val tilbakekrevingsbeløp: List<Tilbakekrevingsbeløp>,
    ) {
        data class Periode(
            /** Kommer på isoformat (YYYY-mm-DD) f.eks. 2020-01-01 */
            @field:JsonProperty(value = "fom")
            val fraOgMed: String,

            /** Kommer på isoformat (YYYY-mm-DD) f.eks. 2020-01-31 */
            @field:JsonProperty(value = "tom")
            val tilOgMed: String,
        )

        data class Tilbakekrevingsbeløp(
            /** F.eks. SUUFORE eller KL_KODE_FEIL_INNT */
            @field:JsonProperty(value = "kodeKlasse")
            val kodeKlasse: String,

            /** F.eks. YTEL eller FEIL */
            @field:JsonProperty(value = "typeKlasse")
            val typeKlasse: String,

            /** F.eks. 0.00 eller 15209.00 */
            @field:JsonProperty(value = "belopOpprUtbet")
            val belopOpprUtbet: String,

            /** F.eks. 2282.00 eller 12927.00 */
            @field:JsonProperty(value = "belopNy")
            val belopNy: String,

            /** F.eks. 0.00 eller 2282.00 */
            @field:JsonProperty(value = "belopTilbakekreves")
            val belopTilbakekreves: String,

            /** F.eks. 0.00 */
            @field:JsonProperty(value = "belopUinnkrevd")
            val belopUinnkrevd: String,

            /** F.eks. 0.0000 eller 44.0000 */
            @field:JsonProperty(value = "skattProsent")
            val skattProsent: String,
        )
    }
}
