package no.nav.etterlatte.libs.common.behandling

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.person.VergeEllerFullmektig
import no.nav.etterlatte.libs.common.sak.Sak
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

    // potensielt en status for å markere oversendingen til Kabal
    FERDIGSTILT, // klagen er ferdig fra gjenny sin side

    ;

    companion object {
        fun kanOppdatereFormkrav(status: KlageStatus): Boolean {
            return status !== FERDIGSTILT
        }

        fun kanOppdatereUtfall(status: KlageStatus): Boolean {
            return status === FORMKRAV_OPPFYLT || status === UTFALL_VURDERT
        }
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

data class Klage(
    val id: UUID,
    val sak: Sak,
    val opprettet: Tidspunkt,
    val status: KlageStatus,
    val kabalStatus: KabalStatus?,
    val formkrav: FormkravMedBeslutter?,
    val utfall: KlageUtfall?,
    val resultat: KlageResultat?,
    val kabalResultat: BehandlingResultat?,
    val innkommendeDokument: InnkommendeKlage?,
) {
    fun oppdaterFormkrav(
        formkrav: Formkrav,
        saksbehandlerIdent: String,
    ): Klage {
        if (!this.kanOppdatereFormkrav()) {
            throw IllegalStateException(
                "Kan ikke oppdatere formkrav i klagen med id=${this.id}, på grunn av " +
                    "tilstanden til klagen: ${this.status}",
            )
        }
        return this.copy(
            formkrav =
                FormkravMedBeslutter(
                    formkrav = formkrav,
                    saksbehandler = Grunnlagsopplysning.Saksbehandler.create(saksbehandlerIdent),
                ),
            status =
                when (formkrav.erFormkraveneOppfylt) {
                    JaNei.JA -> KlageStatus.FORMKRAV_OPPFYLT
                    JaNei.NEI -> KlageStatus.FORMKRAV_IKKE_OPPFYLT
                },
        )
    }

    fun oppdaterUtfall(utfallMedBrev: KlageUtfall): Klage {
        if (!this.kanOppdatereUtfall()) {
            throw IllegalStateException(
                "Kan ikke oppdatere utfallet i klagen med id=${this.id} på grunn av statusen" +
                    "til klagen (${this.status})",
            )
        }
        val hjemmel =
            when (utfallMedBrev) {
                is KlageUtfall.StadfesteVedtak -> utfallMedBrev.innstilling.lovhjemmel
                is KlageUtfall.DelvisOmgjoering -> utfallMedBrev.innstilling.lovhjemmel
                is KlageUtfall.Omgjoering -> null
            }
        hjemmel?.let {
            require(it.kanBrukesForSaktype(this.sak.sakType)) {
                "Hjemmelen $it er ikke gyldig for saktypen ${this.sak.sakType}"
            }
        }

        return this.copy(
            utfall = utfallMedBrev,
            status = KlageStatus.UTFALL_VURDERT,
        )
    }

    fun ferdigstill(resultat: KlageResultat): Klage {
        if (!this.kanFerdigstille()) {
            throw IllegalStateException(
                "Kan ikke ferdigstille klagen med id=${this.id} med resultatet $resultat " +
                    "på grunn av status til klagen (${this.status})",
            )
        }
        val harSendtTilKabal = resultat.sendtInnstillingsbrev?.sendtKabalTidspunkt != null

        return this.copy(
            resultat = resultat,
            status = KlageStatus.FERDIGSTILT,
            kabalStatus = KabalStatus.OPPRETTET.takeIf { harSendtTilKabal },
        )
    }

    fun kanOppdatereFormkrav(): Boolean {
        return KlageStatus.kanOppdatereFormkrav(this.status)
    }

    fun kanOppdatereUtfall(): Boolean {
        return KlageStatus.kanOppdatereUtfall(this.status)
    }

    fun kanFerdigstille(): Boolean {
        return when (this.status) {
            KlageStatus.UTFALL_VURDERT -> {
                this.utfall != null
            }

            KlageStatus.FORMKRAV_IKKE_OPPFYLT -> {
                // TODO("Støtt avslag på formkrav på klage")
                false
            }

            else -> false
        }
    }

    fun tilKabalForsendelse() {
    }

    companion object {
        fun ny(
            sak: Sak,
            innkommendeDokument: InnkommendeKlage?,
        ): Klage {
            return Klage(
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
            )
        }
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "utfall")
sealed class KlageUtfall {
    abstract val saksbehandler: Grunnlagsopplysning.Saksbehandler

    @JsonTypeName("OMGJOERING")
    data class Omgjoering(
        val omgjoering: KlageOmgjoering,
        override val saksbehandler: Grunnlagsopplysning.Saksbehandler,
    ) : KlageUtfall()

    @JsonTypeName("DELVIS_OMGJOERING")
    data class DelvisOmgjoering(
        val omgjoering: KlageOmgjoering,
        val innstilling: InnstillingTilKabal,
        override val saksbehandler: Grunnlagsopplysning.Saksbehandler,
    ) : KlageUtfall()

    @JsonTypeName("STADFESTE_VEDTAK")
    data class StadfesteVedtak(
        val innstilling: InnstillingTilKabal,
        override val saksbehandler: Grunnlagsopplysning.Saksbehandler,
    ) : KlageUtfall()
}

sealed class GyldigForYtelse {
    abstract val gyldigForBarnepensjon: Boolean
    abstract val gyldigForOmstillingsstoenad: Boolean

    fun gyldigForSaktype(sakType: SakType): Boolean {
        return when (sakType) {
            SakType.BARNEPENSJON -> gyldigForBarnepensjon
            SakType.OMSTILLINGSSTOENAD -> gyldigForOmstillingsstoenad
        }
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
}

enum class KabalHjemmel(private val gyldigForYtelse: GyldigForYtelse) {
    FTRL_1_3(GyldigForYtelse.OmsOgBp),
    FTRL_1_3_A(GyldigForYtelse.OmsOgBp),
    FTRL_1_3_B(GyldigForYtelse.OmsOgBp),
    FTRL_1_5(GyldigForYtelse.OmsOgBp),

    FTRL_2_1(GyldigForYtelse.OmsOgBp),
    FTRL_2_1_A(GyldigForYtelse.OmsOgBp),
    FTRL_2_2(GyldigForYtelse.OmsOgBp),
    FTRL_2_3(GyldigForYtelse.OmsOgBp),
    FTRL_2_4(GyldigForYtelse.OmsOgBp),
    FTRL_2_5(GyldigForYtelse.OmsOgBp),
    FTRL_2_6(GyldigForYtelse.OmsOgBp),
    FTRL_2_7(GyldigForYtelse.OmsOgBp),
    FTRL_2_7_A(GyldigForYtelse.OmsOgBp),
    FTRL_2_8(GyldigForYtelse.OmsOgBp),
    FTRL_2_9(GyldigForYtelse.OmsOgBp),
    FTRL_2_10(GyldigForYtelse.OmsOgBp),
    FTRL_2_11(GyldigForYtelse.OmsOgBp),
    FTRL_2_12(GyldigForYtelse.OmsOgBp),
    FTRL_2_13(GyldigForYtelse.OmsOgBp),
    FTRL_2_14(GyldigForYtelse.OmsOgBp),
    FTRL_2_15(GyldigForYtelse.OmsOgBp),
    FTRL_2_16(GyldigForYtelse.OmsOgBp),
    FTRL_2_17(GyldigForYtelse.OmsOgBp),

    FTRL_3_1(GyldigForYtelse.OmsOgBp),
    FTRL_3_5_TRYGDETID(GyldigForYtelse.OmsOgBp),
    FTRL_3_7(GyldigForYtelse.OmsOgBp),
    FTRL_3_10(GyldigForYtelse.OmsOgBp),
    FTRL_3_13(GyldigForYtelse.OmsOgBp),
    FTRL_3_14(GyldigForYtelse.OmsOgBp),
    FTRL_3_15(GyldigForYtelse.OmsOgBp),

    FTRL_21_6(GyldigForYtelse.OmsOgBp),
    FTRL_21_7(GyldigForYtelse.OmsOgBp),
    FTRL_21_10(GyldigForYtelse.OmsOgBp),

    FTRL_22_1(GyldigForYtelse.OmsOgBp),
    FTRL_22_1_A(GyldigForYtelse.OmsOgBp),
    FTRL_22_12(GyldigForYtelse.OmsOgBp),
    FTRL_22_13_1(GyldigForYtelse.OmsOgBp),
    FTRL_22_13_3(GyldigForYtelse.OmsOgBp),
    FTRL_22_13_4_C(GyldigForYtelse.OmsOgBp),
    FTRL_22_13_7(GyldigForYtelse.OmsOgBp),
    FTRL_22_14_3(GyldigForYtelse.OmsOgBp),
    FTRL_22_15_1_1(GyldigForYtelse.OmsOgBp),
    FTRL_22_15_1_2(GyldigForYtelse.OmsOgBp),
    FTRL_22_15_2(GyldigForYtelse.OmsOgBp),
    FTRL_22_15_4(GyldigForYtelse.OmsOgBp),
    FTRL_22_15_5(GyldigForYtelse.OmsOgBp),
    FTRL_22_17A(GyldigForYtelse.OmsOgBp),
    FTRL_22_17B(GyldigForYtelse.OmsOgBp),
    FTRL_25_14(GyldigForYtelse.OmsOgBp),

    FVL_31(GyldigForYtelse.OmsOgBp),
    FVL_32(GyldigForYtelse.OmsOgBp),
    FVL_33_2(GyldigForYtelse.OmsOgBp),
    FVL_35_1_C(GyldigForYtelse.OmsOgBp),
    FVL_36(GyldigForYtelse.OmsOgBp),

    ANDRE_TRYGDEAVTALER(GyldigForYtelse.OmsOgBp),
    EOES_AVTALEN_BEREGNING(GyldigForYtelse.OmsOgBp),
    EOES_AVTALEN_MEDLEMSKAP_TRYGDETID(GyldigForYtelse.OmsOgBp),

    FTRL_18_1_A(GyldigForYtelse.KunBp),
    FTRL_18_2(GyldigForYtelse.KunBp),
    FTRL_18_3(GyldigForYtelse.KunBp),
    FTRL_18_4(GyldigForYtelse.KunBp),
    FTRL_18_5(GyldigForYtelse.KunBp),
    FTRL_18_6(GyldigForYtelse.KunBp),
    FTRL_18_7(GyldigForYtelse.KunBp),
    FTRL_18_8(GyldigForYtelse.KunBp),
    FTRL_18_9(GyldigForYtelse.KunBp),
    FTRL_18_10(GyldigForYtelse.KunBp),
    FTRL_18_11(GyldigForYtelse.KunBp),

    FTRL_17_1_A(GyldigForYtelse.KunOms),
    FTRL_17_2(GyldigForYtelse.KunOms),
    FTRL_17_3(GyldigForYtelse.KunOms),
    FTRL_17_4(GyldigForYtelse.KunOms),
    FTRL_17_5(GyldigForYtelse.KunOms),
    FTRL_17_6(GyldigForYtelse.KunOms),
    FTRL_17_7(GyldigForYtelse.KunOms),
    FTRL_17_8(GyldigForYtelse.KunOms),
    FTRL_17_9(GyldigForYtelse.KunOms),
    FTRL_17_10(GyldigForYtelse.KunOms),
    FTRL_17_11(GyldigForYtelse.KunOms),
    FTRL_17_12(GyldigForYtelse.KunOms),
    FTRL_17_13(GyldigForYtelse.KunOms),
    FTRL_17_14(GyldigForYtelse.KunOms),
    FTRL_17_15(GyldigForYtelse.KunOms),

    FTRL_17_A_1(GyldigForYtelse.KunOms),
    FTRL_17_A_2(GyldigForYtelse.KunOms),
    FTRL_17_A_3(GyldigForYtelse.KunOms),
    FTRL_17_A_4(GyldigForYtelse.KunOms),
    FTRL_17_A_5(GyldigForYtelse.KunOms),
    FTRL_17_A_6(GyldigForYtelse.KunOms),
    FTRL_17_A_7(GyldigForYtelse.KunOms),
    FTRL_17_A_8(GyldigForYtelse.KunOms),
    ;

    fun kanBrukesForSaktype(sakType: SakType): Boolean {
        return this.gyldigForYtelse.gyldigForSaktype(sakType)
    }
}

enum class GrunnForOmgjoering {
    FEIL_LOVANVENDELSE,
    FEIL_REGELVERKSFORSTAAELSE,
    FEIL_ELLER_ENDRET_FAKTA,
    PROSESSUELL_FEIL,
    SAKEN_HAR_EN_AAPEN_BEHANDLING,
    ANNET,
}

data class KlageOmgjoering(val grunnForOmgjoering: GrunnForOmgjoering, val begrunnelse: String)

class InnstillingTilKabal(val lovhjemmel: KabalHjemmel, val tekst: String, val brev: KlageBrevInnstilling)

data class InnstillingTilKabalUtenBrev(val lovhjemmel: String, val tekst: String)

data class KlageBrevInnstilling(val brevId: Long)

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
}

data class Formkrav(
    val vedtaketKlagenGjelder: VedtaketKlagenGjelder?,
    val erKlagerPartISaken: JaNei,
    val erKlagenSignert: JaNei,
    val gjelderKlagenNoeKonkretIVedtaket: JaNei,
    val erKlagenFramsattInnenFrist: JaNei,
    val erFormkraveneOppfylt: JaNei,
) {
    companion object {
        fun erFormkravKonsistente(formkrav: Formkrav): Boolean {
            val alleSvar =
                listOf(
                    formkrav.erKlagenSignert,
                    formkrav.erKlagerPartISaken,
                    formkrav.erKlagenFramsattInnenFrist,
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
    val saksbehandler: Grunnlagsopplysning.Saksbehandler,
)

enum class KlageHendelseType {
    OPPRETTET,
    FERDIGSTILT,
}

data class KlageOversendelseDto(val klage: Klage, val ekstraData: EkstradataInnstilling)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Mottaker(
    val navn: String,
    val foedselsnummer: Mottakerident?,
    val orgnummer: String? = null,
)

data class Mottakerident(
    val value: String,
)

data class EkstradataInnstilling(
    val mottakerInnstilling: Mottaker,
    val vergeEllerFullmektig: VergeEllerFullmektig?,
    val journalpostInnstillingsbrev: String,
    val journalpostKlage: String?,
    val journalpostSoeknad: String?,
    val journalpostVedtak: String?,
)
