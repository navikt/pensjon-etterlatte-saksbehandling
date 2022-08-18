package no.nav.etterlatte.libs.common.soeknad.dataklasser.common

import com.fasterxml.jackson.annotation.JsonValue

data class Utenlandsadresse(
    val land: Opplysning<FritekstSvar>,
    val adresse: Opplysning<FritekstSvar>
)

data class UtbetalingsInformasjon(
    val kontonummer: Opplysning<FritekstSvar>? = null,
    val utenlandskBankNavn: Opplysning<FritekstSvar>? = null,
    val utenlandskBankAdresse: Opplysning<FritekstSvar>? = null,
    val iban: Opplysning<FritekstSvar>? = null,
    val swift: Opplysning<FritekstSvar>? = null,
    val skattetrekk: BetingetOpplysning<EnumSvar<JaNeiVetIkke>, Opplysning<FritekstSvar>?>? = null
)

data class Kontaktinfo(
    val telefonnummer: Opplysning<FritekstSvar>
)

enum class Spraak(@get:JsonValue val verdi: String) { NB("nb"), NN("nn"), EN("en") }
enum class BankkontoType { NORSK, UTENLANDSK }
enum class InntektType { ARBEIDSINNTEKT, PENSJON, KAPITALINNTEKT, ANDRE_YTELSER }
enum class ForholdTilAvdoedeType { GIFT, SEPARERT, SAMBOER, SKILT, TIDLIGERE_SAMBOER }
enum class OppholdUtlandType { BODD, ARBEIDET }
enum class JobbStatusType { ARBEIDSTAKER, SELVSTENDIG, UNDER_UTDANNING, INGEN }
enum class StillingType { FAST, MIDLERTIDIG, SESONGARBEID }
enum class OmsorgspersonType { GJENLEVENDE, VERGE, ANNET }
enum class SivilstatusType { ENSLIG, EKTESKAP, SAMBOERSKAP }

data class SamboerInntekt(
    val inntektstype: Opplysning<List<EnumSvar<InntektType>>>,
    val samletBruttoinntektPrAar: Opplysning<FritekstSvar>
)
data class ForholdTilAvdoede(
    val relasjon: Opplysning<EnumSvar<ForholdTilAvdoedeType>>,
    val datoForInngaattPartnerskap: Opplysning<DatoSvar>? = null,
    val datoForInngaattSamboerskap: Opplysning<DatoSvar>? = null,
    val datoForSkilsmisse: Opplysning<DatoSvar>? = null,
    val datoForSamlivsbrudd: Opplysning<DatoSvar>? = null,
    val fellesBarn: Opplysning<EnumSvar<JaNeiVetIkke>>?,
    val samboereMedFellesBarnFoerGiftemaal: Opplysning<EnumSvar<JaNeiVetIkke>>? = null,
    val tidligereGift: Opplysning<EnumSvar<JaNeiVetIkke>>? = null,
    val omsorgForBarn: Opplysning<EnumSvar<JaNeiVetIkke>>? = null,
    val mottokBidrag: Opplysning<EnumSvar<JaNeiVetIkke>>? = null, // Finner ikke igjen
    val mottokEktefelleBidrag: Opplysning<EnumSvar<JaNeiVetIkke>>? = null // Finner ikke igjen?
)

data class Utenlandsopphold(
    val land: Opplysning<FritekstSvar>,
    val fraDato: Opplysning<DatoSvar>?,
    val tilDato: Opplysning<DatoSvar>?,
    val oppholdsType: Opplysning<List<EnumSvar<OppholdUtlandType>>>,
    val medlemFolketrygd: Opplysning<EnumSvar<JaNeiVetIkke>>,
    val pensjonsutbetaling: Opplysning<FritekstSvar>?
)

data class Naeringsinntekt(
    val naeringsinntektPrAarFoerDoedsfall: Opplysning<FritekstSvar>?,
    val naeringsinntektVedDoedsfall: Opplysning<EnumSvar<JaNeiVetIkke>>?
)

typealias AarstallForMilitaerTjeneste = FritekstSvar

data class ArbeidOgUtdanning(
    val dinSituasjon: Opplysning<List<EnumSvar<JobbStatusType>>>,
    val arbeidsforhold: Opplysning<List<Arbeidstaker>>?,
    val selvstendig: Opplysning<List<SelvstendigNaeringsdrivende>>?,
    val utdanning: Opplysning<Utdanning>?,
    val annet: Opplysning<FritekstSvar>?
)

data class Utdanning(
    val navn: Opplysning<FritekstSvar>,
    val startDato: Opplysning<DatoSvar>,
    val sluttDato: Opplysning<DatoSvar>
)

typealias AnnenUtdanning = FritekstSvar

enum class HoeyesteUtdanning {
    GRUNNSKOLE,
    VIDEREGAAENDE,
    FAGBREV,
    UNIVERSITET_OPPTIL_4_AAR,
    UNIVERSITET_OVER_4_AAR,
    INGEN,
    ANNEN
}

typealias EndretInntektBegrunnelse = FritekstSvar

data class SelvstendigNaeringsdrivende(
    val firmanavn: Opplysning<FritekstSvar>,
    val orgnr: Opplysning<FritekstSvar>,
    val endretInntekt: BetingetOpplysning<EnumSvar<JaNeiVetIkke>, Opplysning<EndretInntektBegrunnelse>>
)

data class Arbeidstaker(
    val arbeidsgiver: Opplysning<FritekstSvar>,
    val ansettelsesforhold: Opplysning<EnumSvar<StillingType>>,
    val stillingsprosent: Opplysning<FritekstSvar>,
    val endretInntekt: BetingetOpplysning<EnumSvar<JaNeiVetIkke>, Opplysning<EndretInntektBegrunnelse>>
)

enum class Ytelser {
    DAGPENGER,
    SYKEPENGER,
    PLEIEPENGER,
    SVANGERSKAPSPENGER,
    FORELDREPENGER,
    ARBEIDSAVKLARINGSPENGER,
    KVALIFISERINGSSTOENAD,
    KOMMUNAL_OMSORGSSTONAD,
    FOSTERHJEMSGODTGJOERING,
    OMSORGSPENGER,
    OPPLAERINGSPENGER
}

typealias Pensjonsordning = FritekstSvar

data class AndreYtelser(
    val kravOmAnnenStonad: BetingetOpplysning<EnumSvar<JaNeiVetIkke>, Opplysning<EnumSvar<Ytelser>>?>,
    val annenPensjon: BetingetOpplysning<EnumSvar<JaNeiVetIkke>, Opplysning<Pensjonsordning>?>,
    val pensjonUtland: BetingetOpplysning<EnumSvar<JaNeiVetIkke>, PensjonUtland?>
)

data class PensjonUtland(
    val pensjonsType: Opplysning<FritekstSvar>?,
    val land: Opplysning<FritekstSvar>?,
    val bruttobeloepPrAar: Opplysning<FritekstSvar>?
)

data class OppholdUtland(
    val land: Opplysning<FritekstSvar>,
    val medlemFolketrygd: Opplysning<EnumSvar<JaNeiVetIkke>>
)