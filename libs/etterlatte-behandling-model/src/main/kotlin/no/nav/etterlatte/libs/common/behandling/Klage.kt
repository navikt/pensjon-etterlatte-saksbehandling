package no.nav.etterlatte.libs.common.behandling

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.feilhaandtering.sjekkIkkeNull
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.PDFMal
import no.nav.etterlatte.libs.common.klage.AarsakTilAvbrytelse
import no.nav.etterlatte.libs.common.person.VergeEllerFullmektig
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Tenkt flyt mellom statuser i Gjenny:
 *
 *                      ┌┬────────────────┐           ┌┬───────────────┐
 *                 ┌────►│FORMKRAV_OPPFYLT├───────────►│UTFALL_VURDERT ├─┐
 *                 │    └┴────────────────┘           └┴───────────────┘ │
 *  ┌┬──────────┐  │                                                     │  ┌┬─────────────┐
 *  ││OPPRETTET ├──┤    ┌┬─────────────────────┐                        ┌┴──►│FERDIGSTILT  │
 *  └┴──────────┘  └────►│FORMKRAV_IKKE_OPPFYLT├────────────────────────┘   └┴─────────────┘
 *                      └┴─────────────────────┘
 *
 */
enum class KlageStatus {
    OPPRETTET, // klagen er opprettet i gjenny
    FORMKRAV_OPPFYLT, //
    FORMKRAV_IKKE_OPPFYLT,
    UTFALL_VURDERT,
    FATTET_VEDTAK,
    RETURNERT,

    // potensielt en status for å markere oversendingen til Kabal
    FERDIGSTILT, // klagen er ferdig fra gjenny sin side

    AVBRUTT,

    ;

    companion object {
        fun kanOppdatereFormkrav(status: KlageStatus): Boolean = status !in listOf(FERDIGSTILT, AVBRUTT)

        fun kanOppdatereUtfall(status: KlageStatus): Boolean = status in listOf(FORMKRAV_IKKE_OPPFYLT, FORMKRAV_OPPFYLT, UTFALL_VURDERT)

        fun kanAvbryte(status: KlageStatus): Boolean = status !in listOf(FERDIGSTILT, AVBRUTT)

        fun kanEndres(status: KlageStatus): Boolean = status in listOf(OPPRETTET, FORMKRAV_OPPFYLT, UTFALL_VURDERT)

        fun kanFatteVedtak(status: KlageStatus): Boolean = status in listOf(UTFALL_VURDERT, RETURNERT)

        fun kanAttestereVedtak(status: KlageStatus): Boolean = status == FATTET_VEDTAK

        fun kanUnderkjenneVedtak(status: KlageStatus): Boolean = status == FATTET_VEDTAK
    }
}

enum class KabalStatus {
    OPPRETTET,
    UTREDES,
    VENTER,
    FERDIGSTILT,
}

enum class BehandlingResultat {
    MEDHOLD,
    IKKE_MEDHOLD,
    IKKE_MEDHOLD_FORMKRAV_AVVIST,
    IKKE_SATT,
    HENLAGT,
    HEVET,
}

data class Kabalrespons(
    val kabalStatus: KabalStatus,
    val resultat: BehandlingResultat,
)

data class SendtInnstillingsbrev(
    val journalfoerTidspunkt: Tidspunkt,
    val sendtKabalTidspunkt: Tidspunkt,
    val brevId: Long,
    val journalpostId: String,
)

data class KlageResultat(
    val opprettetOppgaveOmgjoeringId: UUID?,
    val sendtInnstillingsbrev: SendtInnstillingsbrev?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InnkommendeKlage(
    val mottattDato: LocalDate,
    val journalpostId: String,
    val innsender: String?,
)

data class InitieltUtfallMedBegrunnelseOgSaksbehandler(
    val utfallMedBegrunnelse: InitieltUtfallMedBegrunnelseDto,
    val saksbehandler: String,
    val tidspunkt: Tidspunkt,
)

data class InitieltUtfallMedBegrunnelseDto(
    val utfall: KlageUtfall,
    val begrunnelse: String?,
)

enum class KlageUtfall {
    OMGJOERING,
    DELVIS_OMGJOERING,
    STADFESTE_VEDTAK,
    AVVIST,
    AVVIST_MED_OMGJOERING,
}

@ConsistentCopyVisibility
data class Klage private constructor(
    val id: UUID,
    val sak: Sak,
    val opprettet: Tidspunkt,
    val status: KlageStatus,
    val kabalStatus: KabalStatus?,
    val formkrav: FormkravMedBeslutter?,
    val initieltUtfall: InitieltUtfallMedBegrunnelseOgSaksbehandler?,
    val utfall: KlageUtfallMedData?,
    val resultat: KlageResultat?,
    val kabalResultat: BehandlingResultat?,
    val innkommendeDokument: InnkommendeKlage?,
    val aarsakTilAvbrytelse: AarsakTilAvbrytelse?,
) {
    fun oppdaterMottattDato(nyDato: LocalDate): Klage {
        if (!KlageStatus.kanEndres(this.status)) {
            throw KlageKanIkkeEndres(
                "Kan ikke oppdatere mottatt dato for klagen med id=$id, " +
                    "siden klagen har status=$status",
            )
        }
        if (this.innkommendeDokument == null) {
            throw InternfeilException(
                "Klagen med id=$id har ikke satt noe innkommende dokument, og da har vi ikke " +
                    "noen dato å endre på. Klagen burde ikke blitt opprettet uten det innkommende dokumentet.",
            )
        }
        val nyttInnkommendeDokument =
            this.innkommendeDokument.copy(
                mottattDato = nyDato,
            )
        return this.copy(innkommendeDokument = nyttInnkommendeDokument)
    }

    fun oppdaterFormkrav(
        formkrav: Formkrav,
        saksbehandlerIdent: String,
    ): Klage {
        if (!this.kanOppdatereFormkrav()) {
            throw UgyldigTilstandForKlage(this, "Form")
        }
        val utfallFortsattGyldig =
            this.erFormkraveneOppfylt() == formkrav.erFormkraveneOppfylt &&
                this.erKlagenFramsattInnenFrist() == formkrav.erKlagenFramsattInnenFrist

        return this.copy(
            formkrav =
                FormkravMedBeslutter(
                    formkrav = formkrav,
                    saksbehandler = Grunnlagsopplysning.Saksbehandler.create(saksbehandlerIdent),
                    klagerHarIkkeSvartVurdering =
                        this.formkrav?.klagerHarIkkeSvartVurdering?.takeIf {
                            formkrav.erFormkraveneOppfylt ==
                                JaNei.NEI
                        },
                ),
            status =
                when (formkrav.erFormkraveneOppfylt) {
                    JaNei.JA -> KlageStatus.FORMKRAV_OPPFYLT
                    JaNei.NEI -> KlageStatus.FORMKRAV_IKKE_OPPFYLT
                },
            utfall = this.utfall.takeIf { utfallFortsattGyldig },
            initieltUtfall = this.initieltUtfall.takeIf { utfallFortsattGyldig },
        )
    }

    fun oppdaterKlagerIkkeSvartBegrunnelse(
        begrunnelse: String,
        saksbehandlerIdent: String,
    ): Klage {
        if (this.status != KlageStatus.FORMKRAV_IKKE_OPPFYLT) {
            throw UgyldigTilstandForKlage(
                this,
                "Begrunnelse for at klager ikke har svart kan kun " +
                    "oppdateres når klagen har vurdert formkrav som ikke gyldige.",
            )
        }
        sjekkIkkeNull(this.formkrav) {
            "Formkrav må være vurdert før vurdering for at bruker ikke har svart kan lagres"
        }
        val klagerHarIkkeSvartBegrunnelse =
            KlagerHarIkkeSvartVurdering(
                begrunnelse = begrunnelse,
                saksbehandler = Grunnlagsopplysning.Saksbehandler.create(saksbehandlerIdent),
            )
        return this.copy(
            formkrav =
                this.formkrav.copy(
                    klagerHarIkkeSvartVurdering = klagerHarIkkeSvartBegrunnelse.takeIf { begrunnelse.isNotBlank() },
                ),
        )
    }

    fun oppdaterUtfall(utfallMedBrev: KlageUtfallMedData): Klage {
        if (!this.kanOppdatereUtfall()) {
            throw UgyldigTilstandForKlage(this)
        }
        if (initieltUtfall == null && !utfallMedBrev.erAvvisning) {
            throw UgyldigTilstandForKlage(this, "Initielt utfall må lagres før utfall kan lagres")
        }
        val hjemmel =
            when (utfallMedBrev) {
                is KlageUtfallMedData.StadfesteVedtak -> utfallMedBrev.innstilling.lovhjemmel
                is KlageUtfallMedData.DelvisOmgjoering -> utfallMedBrev.innstilling.lovhjemmel
                is KlageUtfallMedData.Omgjoering -> null
                is KlageUtfallMedData.Avvist -> null
                is KlageUtfallMedData.AvvistMedOmgjoering -> null
            }
        hjemmel?.let {
            krev(it.kanBrukesForSaktype(this.sak.sakType)) {
                "Hjemmelen $it er ikke gyldig for saktypen ${this.sak.sakType}"
            }
        }

        return this.copy(
            utfall = utfallMedBrev,
            status = KlageStatus.UTFALL_VURDERT,
        )
    }

    fun oppdaterInitieltUtfallMedBegrunnelse(
        utfall: InitieltUtfallMedBegrunnelseDto,
        saksbehandlerIdent: String,
    ): Klage {
        if (!this.kanOppdatereUtfall()) {
            throw UgyldigTilstandForKlage(this)
        } else {
            return this.copy(
                initieltUtfall =
                    InitieltUtfallMedBegrunnelseOgSaksbehandler(
                        utfall,
                        saksbehandlerIdent,
                        Tidspunkt.now(),
                    ),
            )
        }
    }

    fun ferdigstill(resultat: KlageResultat): Klage {
        if (!this.kanFerdigstille()) {
            throw UgyldigTilstandForKlage(this)
        }
        val harSendtTilKabal = resultat.sendtInnstillingsbrev?.sendtKabalTidspunkt != null

        return this.copy(
            resultat = resultat,
            status = KlageStatus.FERDIGSTILT,
            kabalStatus = KabalStatus.OPPRETTET.takeIf { harSendtTilKabal },
        )
    }

    fun avbryt(aarsak: AarsakTilAvbrytelse): Klage {
        if (!this.kanAvbryte()) {
            throw UgyldigTilstandForKlage(this)
        }
        return this.copy(
            status = KlageStatus.AVBRUTT,
            aarsakTilAvbrytelse = aarsak,
        )
    }

    private fun kanOppdatereFormkrav(): Boolean = KlageStatus.kanOppdatereFormkrav(this.status)

    private fun kanOppdatereUtfall(): Boolean = KlageStatus.kanOppdatereUtfall(this.status)

    fun kanFerdigstille(): Boolean =
        when (this.status) {
            KlageStatus.UTFALL_VURDERT -> {
                this.utfall != null
            }

            else -> {
                false
            }
        }

    private fun erFormkraveneOppfylt() = this.formkrav?.formkrav?.erFormkraveneOppfylt

    private fun erKlagenFramsattInnenFrist() = this.formkrav?.formkrav?.erKlagenFramsattInnenFrist

    fun kanAvbryte(): Boolean = KlageStatus.kanAvbryte(this.status)

    fun tilPdfgenDTO(
        soekerFnr: String,
        soekerNavn: String,
    ): KlageBlankettPdfgenDTO {
        if (this.utfall !is KlageUtfallMedData.StadfesteVedtak) {
            throw IllegalStateException("Kan ikke lage en oversendelsesblankett for noe som ikke er innstilling")
        }
        val innstilling = this.utfall.innstilling
        val formkrav =
            this.formkrav?.formkrav ?: throw IllegalStateException(
                "Fant ikke definerte formkrav for det vi skal lage blankett for",
            )

        return KlageBlankettPdfgenDTO(
            formkrav =
                FormkravPdfgen(
                    vedtaketKlagenGjelder =
                        VedtakKlagenGjelderPdfgen(
                            datoAttestert = formkrav.vedtaketKlagenGjelder!!.datoAttestert!!.toLocalDate(),
                            vedtakType = formkrav.vedtaketKlagenGjelder.vedtakType!!,
                        ),
                    erKlagenSignert = formkrav.erKlagenSignert == JaNei.JA,
                    gjelderKlagenNoeKonkretIVedtaket = formkrav.gjelderKlagenNoeKonkretIVedtaket == JaNei.JA,
                    erKlagerPartISaken = formkrav.gjelderKlagenNoeKonkretIVedtaket == JaNei.JA,
                    erKlagenFramsattInnenFrist = formkrav.erKlagenFramsattInnenFrist == JaNei.JA,
                    erFormkraveneOppfylt = formkrav.erFormkraveneOppfylt == JaNei.JA,
                    begrunnelse = formkrav.begrunnelse,
                ),
            hjemmel = innstilling.lovhjemmel.lesbarTekst(),
            sakId = this.sak.id,
            sakType = this.sak.sakType,
            sakGjelder = "$soekerNavn ($soekerFnr)",
            internKommentar = innstilling.internKommentar,
            ovesendelseTekst = innstilling.innstillingTekst,
            klager = this.innkommendeDokument?.innsender ?: "",
            klageDato = this.innkommendeDokument?.mottattDato ?: this.opprettet.toLocalDate(),
        )
    }

    fun fattVedtak(): Klage {
        if (!KlageStatus.kanFatteVedtak(this.status)) {
            throw UgyldigTilstandForKlage(this)
        }
        return this.copy(status = KlageStatus.FATTET_VEDTAK)
    }

    fun attesterVedtak(): Klage {
        if (!KlageStatus.kanAttestereVedtak(this.status)) {
            throw UgyldigTilstandForKlage(this)
        }
        return this.copy(status = KlageStatus.FERDIGSTILT)
    }

    fun underkjennVedtak(): Klage {
        if (!KlageStatus.kanUnderkjenneVedtak(this.status)) {
            throw UgyldigTilstandForKlage(this)
        }
        return this.copy(status = KlageStatus.RETURNERT)
    }

    companion object {
        fun ny(
            sak: Sak,
            innkommendeDokument: InnkommendeKlage?,
        ): Klage =
            Klage(
                id = UUID.randomUUID(),
                sak = sak,
                opprettet = Tidspunkt.now(),
                status = KlageStatus.OPPRETTET,
                kabalStatus = null,
                formkrav = null,
                utfall = null,
                resultat = null,
                kabalResultat = null,
                innkommendeDokument = innkommendeDokument,
                aarsakTilAvbrytelse = null,
                initieltUtfall = null,
            )

        /**
         * Skal kun brukes for å opprette et klage-objekt fra uthenting fra databasen
         */
        fun fraResultSet(
            id: UUID,
            sak: Sak,
            opprettet: Tidspunkt,
            status: KlageStatus,
            kabalStatus: KabalStatus?,
            formkrav: FormkravMedBeslutter?,
            initieltUtfall: InitieltUtfallMedBegrunnelseOgSaksbehandler?,
            utfall: KlageUtfallMedData?,
            resultat: KlageResultat?,
            kabalResultat: BehandlingResultat?,
            innkommendeDokument: InnkommendeKlage?,
            aarsakTilAvbrytelse: AarsakTilAvbrytelse?,
        ): Klage =
            Klage(
                id = id,
                sak = sak,
                opprettet = opprettet,
                status = status,
                kabalStatus = kabalStatus,
                formkrav = formkrav,
                initieltUtfall = initieltUtfall,
                utfall = utfall,
                resultat = resultat,
                kabalResultat = kabalResultat,
                innkommendeDokument = innkommendeDokument,
                aarsakTilAvbrytelse = aarsakTilAvbrytelse,
            )
    }

    fun datoVedtakOmgjoering(): LocalDate? =
        this.formkrav
            ?.formkrav
            ?.vedtaketKlagenGjelder
            ?.datoAttestert
            ?.toLocalDate()
}

/**
 * DTO-en brukes i PDFGEN og må ikke endres uten å kontrollere mot koden i PDFGEN.
 * PDFGEN kaster ikke feil dersom en verdi får nytt navn eller mangler.
 **/
data class KlageBlankettPdfgenDTO(
    val formkrav: FormkravPdfgen,
    val hjemmel: String,
    val sakId: SakId,
    val sakType: SakType,
    val sakGjelder: String,
    val internKommentar: String?,
    val ovesendelseTekst: String,
    val klager: String,
    val klageDato: LocalDate,
) : PDFMal {
    val innhold: List<String> = emptyList()

    @JsonProperty
    @Suppress("unused")
    val sakTypeFormatert =
        when (sakType) {
            SakType.BARNEPENSJON -> "Barnepensjon"
            SakType.OMSTILLINGSSTOENAD -> "Omstillingsstønad"
        }

    @JsonProperty
    @Suppress("unused")
    val vedtakTypeFormatert =
        when (formkrav.vedtaketKlagenGjelder.vedtakType) {
            VedtakType.INNVILGELSE -> "Innvilgelse"
            VedtakType.OPPHOER -> "Opphør"
            VedtakType.AVSLAG -> "Avslag på søknad"
            VedtakType.ENDRING -> "Revurdering"
            VedtakType.TILBAKEKREVING -> "Tilbakekreving"
            VedtakType.AVVIST_KLAGE -> "Avvist klage"
            VedtakType.INGEN_ENDRING -> "Ingen endring"
        }
}

data class FormkravPdfgen(
    val vedtaketKlagenGjelder: VedtakKlagenGjelderPdfgen,
    val erKlagenSignert: Boolean,
    val gjelderKlagenNoeKonkretIVedtaket: Boolean,
    val erKlagerPartISaken: Boolean,
    val erKlagenFramsattInnenFrist: Boolean,
    val erFormkraveneOppfylt: Boolean,
    val begrunnelse: String?,
)

data class VedtakKlagenGjelderPdfgen(
    val datoAttestert: LocalDate,
    val vedtakType: VedtakType,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "utfall")
sealed class KlageUtfallMedData {
    abstract val saksbehandler: Grunnlagsopplysning.Saksbehandler
    abstract val erAvvisning: Boolean

    abstract fun harSammeUtfall(other: KlageUtfallUtenBrev): Boolean

    @JsonTypeName("OMGJOERING")
    data class Omgjoering(
        val omgjoering: KlageOmgjoering,
        override val saksbehandler: Grunnlagsopplysning.Saksbehandler,
    ) : KlageUtfallMedData() {
        override fun harSammeUtfall(other: KlageUtfallUtenBrev) = other is KlageUtfallUtenBrev.Omgjoering

        @JsonIgnore
        override val erAvvisning: Boolean = false
    }

    @JsonTypeName("DELVIS_OMGJOERING")
    data class DelvisOmgjoering(
        val omgjoering: KlageOmgjoering,
        val innstilling: InnstillingTilKabal,
        override val saksbehandler: Grunnlagsopplysning.Saksbehandler,
    ) : KlageUtfallMedData() {
        override fun harSammeUtfall(other: KlageUtfallUtenBrev): Boolean = other is KlageUtfallUtenBrev.DelvisOmgjoering

        @JsonIgnore
        override val erAvvisning: Boolean = false
    }

    @JsonTypeName("STADFESTE_VEDTAK")
    data class StadfesteVedtak(
        val innstilling: InnstillingTilKabal,
        override val saksbehandler: Grunnlagsopplysning.Saksbehandler,
    ) : KlageUtfallMedData() {
        override fun harSammeUtfall(other: KlageUtfallUtenBrev): Boolean = other is KlageUtfallUtenBrev.StadfesteVedtak

        @JsonIgnore
        override val erAvvisning: Boolean = false
    }

    @JsonTypeName("AVVIST")
    data class Avvist(
        override val saksbehandler: Grunnlagsopplysning.Saksbehandler,
        val vedtak: KlageVedtak,
        val brev: KlageVedtaksbrev,
    ) : KlageUtfallMedData() {
        override fun harSammeUtfall(other: KlageUtfallUtenBrev): Boolean = other is KlageUtfallUtenBrev.Avvist

        @JsonIgnore
        override val erAvvisning: Boolean = true
    }

    @JsonTypeName("AVVIST_MED_OMGJOERING")
    data class AvvistMedOmgjoering(
        val omgjoering: KlageOmgjoering,
        override val saksbehandler: Grunnlagsopplysning.Saksbehandler,
    ) : KlageUtfallMedData() {
        override fun harSammeUtfall(other: KlageUtfallUtenBrev): Boolean = other is KlageUtfallUtenBrev.AvvistMedOmgjoering

        @JsonIgnore
        override val erAvvisning: Boolean = true
    }

    fun innstilling(): InnstillingTilKabal? =
        when (this) {
            is StadfesteVedtak -> innstilling
            is DelvisOmgjoering -> innstilling
            is Omgjoering -> null
            is Avvist -> null
            is AvvistMedOmgjoering -> null
        }

    fun omgjoering(): KlageOmgjoering? =
        when (this) {
            is StadfesteVedtak -> null
            is DelvisOmgjoering -> omgjoering
            is Omgjoering -> omgjoering
            is Avvist -> null
            is AvvistMedOmgjoering -> omgjoering
        }
}

sealed class GyldigForYtelse {
    abstract val gyldigForBarnepensjon: Boolean
    abstract val gyldigForOmstillingsstoenad: Boolean

    fun gyldigForSaktype(sakType: SakType): Boolean =
        when (sakType) {
            SakType.BARNEPENSJON -> gyldigForBarnepensjon
            SakType.OMSTILLINGSSTOENAD -> gyldigForOmstillingsstoenad
        }

    data object OmsOgBp : GyldigForYtelse() {
        override val gyldigForBarnepensjon = true
        override val gyldigForOmstillingsstoenad = true
    }

    data object KunOms : GyldigForYtelse() {
        override val gyldigForBarnepensjon = false
        override val gyldigForOmstillingsstoenad = true
    }

    data object KunBp : GyldigForYtelse() {
        override val gyldigForBarnepensjon = true
        override val gyldigForOmstillingsstoenad = false
    }

    data object Utdatert : GyldigForYtelse() {
        override val gyldigForOmstillingsstoenad: Boolean = false
        override val gyldigForBarnepensjon: Boolean = false
    }
}

enum class KabalHjemmel(
    private val gyldigForYtelse: GyldigForYtelse,
    private val leseligTekst: String,
) {
    FTRL_1_3(GyldigForYtelse.OmsOgBp, "Ftrl. § 1-3"),
    FTRL_1_3_A(GyldigForYtelse.OmsOgBp, "Ftrl. § 1-3-A"),
    FTRL_1_3_B(GyldigForYtelse.OmsOgBp, "Ftrl. § 1-3-B"),
    FTRL_1_5(GyldigForYtelse.OmsOgBp, "Ftrl. § 1-5"),

    FTRL_2_1(GyldigForYtelse.OmsOgBp, "Ftrl. § 2-1"),
    FTRL_2_1_A(GyldigForYtelse.OmsOgBp, "Ftrl. § 2-1-A"),
    FTRL_2_2(GyldigForYtelse.OmsOgBp, "Ftrl. § 2-2"),
    FTRL_2_3(GyldigForYtelse.OmsOgBp, "Ftrl. § 2-3"),
    FTRL_2_4(GyldigForYtelse.OmsOgBp, "Ftrl. § 2-4"),
    FTRL_2_5(GyldigForYtelse.OmsOgBp, "Ftrl. § 2-5"),
    FTRL_2_6(GyldigForYtelse.OmsOgBp, "Ftrl. § 2-6"),
    FTRL_2_7(GyldigForYtelse.OmsOgBp, "Ftrl. § 2-7"),
    FTRL_2_7_A(GyldigForYtelse.OmsOgBp, "Ftrl. § 2-7-A"),
    FTRL_2_8(GyldigForYtelse.OmsOgBp, "Ftrl. § 2-8"),
    FTRL_2_9(GyldigForYtelse.OmsOgBp, "Ftrl. § 2-9"),
    FTRL_2_10(GyldigForYtelse.OmsOgBp, "Ftrl. § 2-10"),
    FTRL_2_11(GyldigForYtelse.OmsOgBp, "Ftrl. § 2-11"),
    FTRL_2_12(GyldigForYtelse.OmsOgBp, "Ftrl. § 2-12"),
    FTRL_2_13(GyldigForYtelse.OmsOgBp, "Ftrl. § 2-13"),
    FTRL_2_14(GyldigForYtelse.OmsOgBp, "Ftrl. § 2-14"),
    FTRL_2_15(GyldigForYtelse.OmsOgBp, "Ftrl. § 2-15"),
    FTRL_2_16(GyldigForYtelse.OmsOgBp, "Ftrl. § 2-16"),
    FTRL_2_17(GyldigForYtelse.OmsOgBp, "Ftrl. § 2-17"),

    FTRL_3_1(GyldigForYtelse.OmsOgBp, "Ftrl. § 3-1"),
    FTRL_3_5_TRYGDETID(GyldigForYtelse.OmsOgBp, "Ftrl. § 3-5 (trygdetid)"),
    FTRL_3_7(GyldigForYtelse.OmsOgBp, "Ftrl. § 3-7"),
    FTRL_3_10(GyldigForYtelse.OmsOgBp, "Ftrl. § 3-10"),
    FTRL_3_13(GyldigForYtelse.OmsOgBp, "Ftrl. § 3-13"),
    FTRL_3_14(GyldigForYtelse.OmsOgBp, "Ftrl. § 3-14"),
    FTRL_3_15(GyldigForYtelse.OmsOgBp, "Ftrl. § 3-15"),

    FTRL_21_6(GyldigForYtelse.OmsOgBp, "Ftrl. § 21-6"),
    FTRL_21_7(GyldigForYtelse.OmsOgBp, "Ftrl. § 21-7"),
    FTRL_21_10(GyldigForYtelse.OmsOgBp, "Ftrl. § 21-10"),

    FTRL_22_1(GyldigForYtelse.OmsOgBp, "Ftrl. § 22-1"),
    FTRL_22_1_A(GyldigForYtelse.OmsOgBp, "Ftrl. § 22-1 A"),
    FTRL_22_12(GyldigForYtelse.OmsOgBp, "Ftrl. § 22-12"),
    FTRL_22_13_1(GyldigForYtelse.OmsOgBp, "Ftrl. § 22-13 1. ledd"),
    FTRL_22_13_3(GyldigForYtelse.OmsOgBp, "Ftrl. § 22-13 3. ledd"),
    FTRL_22_13_4_C(GyldigForYtelse.OmsOgBp, "Ftrl. § 22-13 4. ledd c)"),
    FTRL_22_13_7(GyldigForYtelse.OmsOgBp, "Ftrl. § 22-13 7. ledd"),
    FTRL_22_14_3(GyldigForYtelse.OmsOgBp, "Ftrl. § 22-14 3. ledd"),
    FTRL_22_15_1_1(GyldigForYtelse.OmsOgBp, "Ftrl. § 22-15 1. ledd 1. pkt."),
    FTRL_22_15_1_2(GyldigForYtelse.OmsOgBp, "Ftrl. § 22-15 1. ledd 2. pkt."),
    FTRL_22_15_2(GyldigForYtelse.OmsOgBp, "Ftrl. § 22-15 2. ledd"),
    FTRL_22_15_4(GyldigForYtelse.OmsOgBp, "Ftrl. § 22-15 4. ledd"),
    FTRL_22_15_5(GyldigForYtelse.OmsOgBp, "Ftrl. § 22-15 5. ledd"),
    FTRL_22_17A(GyldigForYtelse.OmsOgBp, "Ftrl. § 22-17 a"),
    FTRL_22_17B(GyldigForYtelse.OmsOgBp, "Ftrl. § 22-17 b"),
    FTRL_25_14(GyldigForYtelse.OmsOgBp, "Ftrl. § 25-14"),

    FVL_31(GyldigForYtelse.OmsOgBp, "Fvl. § 31"),
    FVL_32(GyldigForYtelse.OmsOgBp, "Fvl. § 32"),
    FVL_33_2(GyldigForYtelse.OmsOgBp, "Fvl. § 33-2"),
    FVL_35_1_C(GyldigForYtelse.OmsOgBp, "Fvl. § 35, 1.ledd c)"),
    FVL_36(GyldigForYtelse.OmsOgBp, "Fvl. § 36"),

    ANDRE_TRYGDEAVTALER(GyldigForYtelse.OmsOgBp, "Andre trygdeavtaler"),
    EOES_AVTALEN_BEREGNING(GyldigForYtelse.OmsOgBp, "EØS - beregning"),
    EOES_AVTALEN_MEDLEMSKAP_TRYGDETID(GyldigForYtelse.OmsOgBp, "EØS - medlemskap / trygdetid"),

    FTRL_18_1_A(GyldigForYtelse.KunBp, "Ftrl. § 18-1 A"),
    FTRL_18_2(GyldigForYtelse.KunBp, "Ftrl. § 18-2"),
    FTRL_18_3(GyldigForYtelse.KunBp, "Ftrl. § 18-3"),
    FTRL_18_4(GyldigForYtelse.KunBp, "Ftrl. § 18-4"),
    FTRL_18_5(GyldigForYtelse.KunBp, "Ftrl. § 18-5"),
    FTRL_18_6(GyldigForYtelse.KunBp, "Ftrl. § 18-6"),
    FTRL_18_7(GyldigForYtelse.KunBp, "Ftrl. § 18-7"),
    FTRL_18_8(GyldigForYtelse.KunBp, "Ftrl. § 18-8"),
    FTRL_18_9(GyldigForYtelse.KunBp, "Ftrl. § 18-9"),
    FTRL_18_10(GyldigForYtelse.KunBp, "Ftrl. § 18-10"),
    FTRL_18_11(GyldigForYtelse.KunBp, "Ftrl. § 18-11"),

    FTRL_17_1_A(GyldigForYtelse.KunOms, "Ftrl. § 17-1 A"),
    FTRL_17_2(GyldigForYtelse.KunOms, "Ftrl. § 17-2"),
    FTRL_17_3(GyldigForYtelse.KunOms, "Ftrl. § 17-3"),

    // Utdaterte hjemler for OMS
    FTRL_17_4(GyldigForYtelse.Utdatert, "Ftrl. § 17-4 før 2024"),
    FTRL_17_5(GyldigForYtelse.Utdatert, "Ftrl. § 17-5 før 2024"),
    FTRL_17_6(GyldigForYtelse.Utdatert, "Ftrl. § 17-6 før 2024"),
    FTRL_17_7(GyldigForYtelse.Utdatert, "Ftrl. § 17-7 før 2024"),
    FTRL_17_8(GyldigForYtelse.Utdatert, "Ftrl. § 17-8 før 2024"),
    FTRL_17_9(GyldigForYtelse.Utdatert, "Ftrl. § 17-9 før 2024"),

    FTRL_17_4_1(GyldigForYtelse.KunOms, "Ftrl. § 17-4 1. ledd"),
    FTRL_17_4_2(GyldigForYtelse.KunOms, "Ftrl. § 17-4 2. ledd"),
    FTRL_17_5_1(GyldigForYtelse.KunOms, "Ftrl. § 17-5 1. ledd"),
    FTRL_17_5_2(GyldigForYtelse.KunOms, "Ftrl. § 17-5 2. ledd"),
    FTRL_17_5_3(GyldigForYtelse.KunOms, "Ftrl. § 17-5 3. ledd"),
    FTRL_17_6_NY(GyldigForYtelse.KunOms, "Ftrl. § 17-6"),
    FTRL_17_7_NY(GyldigForYtelse.KunOms, "Ftrl. § 17-7"),
    FTRL_17_8_NY(GyldigForYtelse.KunOms, "Ftrl. § 17-8"),
    FTRL_17_9_NY(GyldigForYtelse.KunOms, "Ftrl. § 17-9"),

    FTRL_17_10(GyldigForYtelse.KunOms, "Ftrl. § 17-10"),
    FTRL_17_11(GyldigForYtelse.KunOms, "Ftrl. § 17-11"),
    FTRL_17_12(GyldigForYtelse.KunOms, "Ftrl. § 17-12"),
    FTRL_17_13(GyldigForYtelse.KunOms, "Ftrl. § 17-13"),
    FTRL_17_14(GyldigForYtelse.KunOms, "Ftrl. § 17-14"),
    FTRL_17_15(GyldigForYtelse.KunOms, "Ftrl. § 17-15"),

    FTRL_17_A_1(GyldigForYtelse.KunOms, "Ftrl. § 17 A-1"),
    FTRL_17_A_2(GyldigForYtelse.KunOms, "Ftrl. § 17 A-2"),
    FTRL_17_A_3(GyldigForYtelse.KunOms, "Ftrl. § 17 A-3"),
    FTRL_17_A_4(GyldigForYtelse.KunOms, "Ftrl. § 17 A-4"),
    FTRL_17_A_5(GyldigForYtelse.KunOms, "Ftrl. § 17 A-5"),
    FTRL_17_A_6(GyldigForYtelse.KunOms, "Ftrl. § 17 A-6"),
    FTRL_17_A_7(GyldigForYtelse.KunOms, "Ftrl. § 17 A-7"),
    FTRL_17_A_8(GyldigForYtelse.KunOms, "Ftrl. § 17 A-8"),
    ;

    fun kanBrukesForSaktype(sakType: SakType): Boolean = this.gyldigForYtelse.gyldigForSaktype(sakType)

    fun lesbarTekst(): String = this.leseligTekst
}

enum class GrunnForOmgjoering {
    FEIL_LOVANVENDELSE,
    FEIL_REGELVERKSFORSTAAELSE,
    FEIL_ELLER_ENDRET_FAKTA,
    PROSESSUELL_FEIL,
    SAKEN_HAR_EN_AAPEN_BEHANDLING,
    ANNET,
}

data class KlageOmgjoering(
    val grunnForOmgjoering: GrunnForOmgjoering,
    val begrunnelse: String,
)

class InnstillingTilKabal(
    val lovhjemmel: KabalHjemmel,
    val internKommentar: String?,
    val innstillingTekst: String,
    val brev: KlageOversendelsebrev,
)

class InnstillingTilKabalUtenBrev(
    val lovhjemmel: String,
    internKommentar: String?,
    val innstillingTekst: String,
) {
    val internKommentar: String? = internKommentar
        get() = if (field.isNullOrBlank()) null else field
}

data class KlageOversendelsebrev(
    val brevId: Long,
)

data class KlageVedtaksbrev(
    val brevId: Long,
)

data class KlageVedtak(
    val vedtakId: Long,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "utfall")
sealed class KlageUtfallUtenBrev {
    @JsonTypeName("OMGJOERING")
    data class Omgjoering(
        val omgjoering: KlageOmgjoering,
    ) : KlageUtfallUtenBrev()

    @JsonTypeName("DELVIS_OMGJOERING")
    data class DelvisOmgjoering(
        val omgjoering: KlageOmgjoering,
        val innstilling: InnstillingTilKabalUtenBrev,
    ) : KlageUtfallUtenBrev()

    @JsonTypeName("STADFESTE_VEDTAK")
    data class StadfesteVedtak(
        val innstilling: InnstillingTilKabalUtenBrev,
    ) : KlageUtfallUtenBrev()

    @JsonTypeName("AVVIST")
    class Avvist : KlageUtfallUtenBrev()

    @JsonTypeName("AVVIST_MED_OMGJOERING")
    data class AvvistMedOmgjoering(
        val omgjoering: KlageOmgjoering,
    ) : KlageUtfallUtenBrev()
}

data class Formkrav(
    val vedtaketKlagenGjelder: VedtaketKlagenGjelder?,
    val erKlagerPartISaken: JaNei,
    val erKlagenSignert: JaNei,
    val gjelderKlagenNoeKonkretIVedtaket: JaNei,
    val erKlagenFramsattInnenFrist: JaNei,
    val erFormkraveneOppfylt: JaNei,
    val begrunnelse: String? = null,
) {
    companion object {
        fun erFormkravKonsistente(formkrav: Formkrav): Boolean {
            val alleSvar =
                listOf(
                    formkrav.erKlagenSignert,
                    formkrav.erKlagerPartISaken,
                    formkrav.gjelderKlagenNoeKonkretIVedtaket,
                )

            return if (formkrav.erFormkraveneOppfylt == JaNei.JA) {
                formkrav.vedtaketKlagenGjelder != null && alleSvar.all { it == JaNei.JA }
            } else {
                formkrav.vedtaketKlagenGjelder == null || alleSvar.any { it == JaNei.NEI }
            }
        }
    }
}

data class VedtaketKlagenGjelder(
    val id: String,
    val behandlingId: String,
    val datoAttestert: ZonedDateTime?,
    val vedtakType: VedtakType?,
)

data class FormkravMedBeslutter(
    val formkrav: Formkrav,
    val klagerHarIkkeSvartVurdering: KlagerHarIkkeSvartVurdering?,
    val saksbehandler: Grunnlagsopplysning.Saksbehandler,
)

data class KlagerHarIkkeSvartVurdering(
    val begrunnelse: String,
    val saksbehandler: Grunnlagsopplysning.Saksbehandler,
)

data class KlageOversendelseDto(
    val klage: Klage,
    val ekstraData: EkstradataInnstilling,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KlageMottaker(
    val navn: String,
    val foedselsnummer: Mottakerident?,
    val orgnummer: String? = null,
)

data class Mottakerident(
    val value: String,
)

data class EkstradataInnstilling(
    val mottakerInnstilling: KlageMottaker,
    val vergeEllerFullmektig: VergeEllerFullmektig?,
    val journalpostInnstillingsbrev: String,
    val journalpostKlage: String?,
    val journalpostSoeknad: String?,
    val journalpostVedtak: String?,
)

class KlageKanIkkeEndres(
    override val detail: String,
) : UgyldigForespoerselException(code = "KLAGE_KAN_IKKE_ENDRES", detail = detail)

class UgyldigTilstandForKlage(
    klage: Klage,
    begrunnelse: String = "",
) : UgyldigForespoerselException(
        code = "KLAGE_HAR_FEIL_TILSTAND",
        detail = "Klagen med id=${klage.id} og status ${klage.status} kan ikke oppdateres. $begrunnelse",
    )
