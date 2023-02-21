package no.nav.etterlatte.opplysningerfrasoknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Forelder
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.GjenlevendeForelderSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.InnsenderSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.AVDOED_SOEKNAD_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.UTENLANDSADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.UTENLANDSOPPHOLD
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Samtykke
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoekerBarnSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeknadMottattDato
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeknadstypeOpplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Utbetalingsinformasjon
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.UtenlandsadresseBarn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Verge
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.Utenlandsadresse
import no.nav.etterlatte.libs.common.person.UtenlandsoppholdOpplysninger
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Avdoed
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.GjenlevendeForelder
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.SoeknadType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import no.nav.etterlatte.libs.common.soeknad.dataklasser.omstillingsstoenad.Omstillingsstoenad
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.*
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Utenlandsopphold as UtenlandsoppholdOpplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.UtenlandsoppholdOpplysninger as UtenlandsoppholdOpplysningerOld

class Opplysningsuthenter {
    fun lagOpplysningsListe(jsonNode: JsonNode, type: SoeknadType): List<Grunnlagsopplysning<out Any?>> {
        return when (type) {
            SoeknadType.BARNEPENSJON -> BarnepensjonUthenter.lagOpplysningsListe(jsonNode)
            SoeknadType.OMSTILLINGSSTOENAD -> OmstillingsstoenadUthenter.lagOpplysningsListe(jsonNode)
            else -> throw Exception("Ugyldig SoeknadType")
        }
    }
}

private object BarnepensjonUthenter {

    fun lagOpplysningsListe(jsonNode: JsonNode): List<Grunnlagsopplysning<out Any?>> {
        val barnepensjonssoknad = objectMapper.treeToValue<Barnepensjon>(jsonNode)
        val kilde = Grunnlagsopplysning.Privatperson(
            barnepensjonssoknad.innsender.foedselsnummer.svar.value,
            barnepensjonssoknad.mottattDato.toInstant(ZoneOffset.UTC)
        )

        val søkerUtenlandsopphold = barnepensjonssoknad.soeker.utenlandsAdresse?.let {
            lagOpplysning(
                opplysningsType = UTENLANDSADRESSE,
                kilde = kilde,
                opplysning = Utenlandsadresse(
                    it.svar.verdi,
                    it.opplysning?.land?.svar?.innhold,
                    it.opplysning?.adresse?.svar?.innhold
                ),
                periode = null
            )
        }
        val utenlandsopphold = hentAvdoedForelder(barnepensjonssoknad)?.utenlandsopphold
        val utenlandsoppholdAvdød: List<Grunnlagsopplysning<out Any?>> = utenlandsopphold?.svar?.verdi?.let { svar ->
            when (svar) {
                JaNeiVetIkke.JA -> utenlandsopphold.opplysning?.map {
                    lagOpplysning(
                        opplysningsType = UTENLANDSOPPHOLD,
                        kilde = kilde,
                        opplysning = UtenlandsoppholdOpplysninger(
                            utenlandsopphold.svar.verdi,
                            it.land.svar.innhold,
                            it.oppholdsType.svar.map { utlandsopphold -> utlandsopphold.verdi },
                            it.medlemFolketrygd.svar.verdi,
                            it.pensjonsutbetaling?.svar?.innhold
                        ),
                        periode = it.fraDato?.svar?.innhold?.let { fom ->
                            Periode(YearMonth.from(fom), it.tilDato?.svar?.innhold?.let { tom -> YearMonth.from(tom) })
                        }
                    )
                }

                JaNeiVetIkke.NEI -> hentAvdoedForelder(barnepensjonssoknad)?.foedselsnummer?.svar?.let { avdoedFnr ->
                    listOf(
                        Grunnlagsopplysning.empty(
                            UTENLANDSOPPHOLD,
                            kilde,
                            avdoedFnr,
                            YearMonth.from(avdoedFnr.getBirthDate())
                        )
                    )
                }

                JaNeiVetIkke.VET_IKKE -> emptyList()
            }
        } ?: emptyList()

        return utenlandsoppholdAvdød + listOfNotNull(
            søkerUtenlandsopphold,
            avdoed(barnepensjonssoknad, Opplysningstype.AVDOED_SOEKNAD_V1),
            soeker(barnepensjonssoknad, Opplysningstype.SOEKER_SOEKNAD_V1),
            gjenlevendeForelder(barnepensjonssoknad, Opplysningstype.GJENLEVENDE_FORELDER_SOEKNAD_V1),
            innsender(barnepensjonssoknad, Opplysningstype.INNSENDER_SOEKNAD_V1),
            utbetalingsinformasjon(barnepensjonssoknad, Opplysningstype.UTBETALINGSINFORMASJON_V1),
            samtykke(barnepensjonssoknad, Opplysningstype.SAMTYKKE),
            spraak(barnepensjonssoknad, Opplysningstype.SPRAAK),
            soeknadMottattDato(barnepensjonssoknad, Opplysningstype.SOEKNAD_MOTTATT_DATO),
            soeknadsType(barnepensjonssoknad, Opplysningstype.SOEKNADSTYPE_V1),
            personGalleri(barnepensjonssoknad)
        )
    }

    private fun <T> setBehandlingsopplysninger(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstype,
        data: T
    ): Grunnlagsopplysning<T> {
        return Grunnlagsopplysning(
            UUID.randomUUID(),
            Grunnlagsopplysning.Privatperson(
                barnepensjon.innsender.foedselsnummer.svar.value,
                barnepensjon.mottattDato.toInstant(ZoneOffset.UTC)
            ),
            opplysningsType,
            objectMapper.createObjectNode(),
            data
        )
    }

    private fun hentAvdoedForelder(barnepensjon: Barnepensjon): Avdoed? {
        return barnepensjon.foreldre.find { it.type === PersonType.AVDOED }?.let { it as Avdoed }
    }

    private fun hentGjenlevendeForelder(barnepensjon: Barnepensjon): GjenlevendeForelder? {
        return barnepensjon.foreldre.find { it.type === PersonType.GJENLEVENDE_FORELDER }
            ?.let { it as GjenlevendeForelder }
    }

    private fun avdoed(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstype
    ): Grunnlagsopplysning<out AvdoedSoeknad>? {
        return hentAvdoedForelder(barnepensjon)?.let { avdoed ->
            setBehandlingsopplysninger(
                barnepensjon,
                opplysningsType,
                avdoedOpplysning(avdoed)
            )
        }
    }

    fun soeker(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstype
    ): Grunnlagsopplysning<out SoekerBarnSoeknad> {
        val adresse = barnepensjon.soeker.utenlandsAdresse

        return setBehandlingsopplysninger(
            barnepensjon,
            opplysningsType,
            SoekerBarnSoeknad(
                type = PersonType.BARN,
                fornavn = barnepensjon.soeker.fornavn.svar,
                etternavn = barnepensjon.soeker.etternavn.svar,
                foedselsnummer = barnepensjon.soeker.foedselsnummer.svar,
                statsborgerskap = barnepensjon.soeker.statsborgerskap.svar,
                utenlandsadresse = UtenlandsadresseBarn(
                    adresse?.svar?.verdi,
                    adresse?.opplysning?.land?.svar?.innhold,
                    adresse?.opplysning?.adresse?.svar?.innhold
                ),
                foreldre = barnepensjon.soeker.foreldre.map {
                    Forelder(
                        it.type,
                        it.fornavn.svar,
                        it.etternavn.svar,
                        it.foedselsnummer.svar
                    )
                },
                verge = Verge(
                    barnepensjon.soeker.verge?.svar?.verdi,
                    barnepensjon.soeker.verge?.opplysning?.fornavn?.svar,
                    barnepensjon.soeker.verge?.opplysning?.etternavn?.svar,
                    barnepensjon.soeker.verge?.opplysning?.foedselsnummer?.svar
                ),
                omsorgPerson = barnepensjon.soeker.dagligOmsorg?.svar?.verdi
            )
        )
    }

    fun gjenlevendeForelder(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstype
    ): Grunnlagsopplysning<out GjenlevendeForelderSoeknad>? {
        return hentGjenlevendeForelder(barnepensjon)?.let { forelder ->
            setBehandlingsopplysninger(
                barnepensjon,
                opplysningsType,
                GjenlevendeForelderSoeknad(
                    PersonType.GJENLEVENDE_FORELDER,
                    forelder.fornavn.svar,
                    forelder.etternavn.svar,
                    forelder.foedselsnummer.svar,
                    forelder.adresse.svar,
                    forelder.statsborgerskap.svar,
                    forelder.kontaktinfo.telefonnummer.svar.innhold
                )
            )
        }
    }

    fun innsender(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstype
    ): Grunnlagsopplysning<out InnsenderSoeknad> {
        return setBehandlingsopplysninger(
            barnepensjon,
            opplysningsType,
            InnsenderSoeknad(
                PersonType.INNSENDER,
                barnepensjon.innsender.fornavn.svar,
                barnepensjon.innsender.etternavn.svar,
                barnepensjon.innsender.foedselsnummer.svar
            )
        )
    }

    fun samtykke(barnepensjon: Barnepensjon, opplysningsType: Opplysningstype): Grunnlagsopplysning<out Samtykke> {
        return setBehandlingsopplysninger(barnepensjon, opplysningsType, Samtykke(barnepensjon.harSamtykket.svar))
    }

    fun spraak(barnepensjon: Barnepensjon, opplysningsType: Opplysningstype): Grunnlagsopplysning<out Spraak> =
        setBehandlingsopplysninger(barnepensjon, opplysningsType, barnepensjon.spraak)

    fun utbetalingsinformasjon(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstype
    ): Grunnlagsopplysning<out Utbetalingsinformasjon>? {
        return barnepensjon.utbetalingsInformasjon?.let {
            setBehandlingsopplysninger(
                barnepensjon,
                opplysningsType,
                Utbetalingsinformasjon(
                    it.svar.verdi,
                    it.opplysning?.kontonummer?.svar?.innhold,
                    it.opplysning?.utenlandskBankNavn?.svar?.innhold,
                    it.opplysning?.utenlandskBankAdresse?.svar?.innhold,
                    it.opplysning?.iban?.svar?.innhold,
                    it.opplysning?.swift?.svar?.innhold,
                    it.opplysning?.skattetrekk?.svar?.verdi,
                    it.opplysning?.skattetrekk?.opplysning?.svar?.innhold
                )
            )
        }
    }

    private fun soeknadMottattDato(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstype
    ): Grunnlagsopplysning<out SoeknadMottattDato> {
        return setBehandlingsopplysninger(
            barnepensjon,
            opplysningsType,
            SoeknadMottattDato(barnepensjon.mottattDato)
        )
    }

    private fun soeknadsType(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstype
    ): Grunnlagsopplysning<out SoeknadstypeOpplysning> {
        return setBehandlingsopplysninger(barnepensjon, opplysningsType, SoeknadstypeOpplysning(barnepensjon.type))
    }

    private fun personGalleri(
        barnepensjon: Barnepensjon
    ): Grunnlagsopplysning<out Persongalleri> {
        return setBehandlingsopplysninger(
            barnepensjon,
            Opplysningstype.PERSONGALLERI_V1,
            Persongalleri(
                soeker = barnepensjon.soeker.foedselsnummer.svar.value,
                innsender = barnepensjon.innsender.foedselsnummer.svar.value,
                soesken = barnepensjon.soesken.map { it.foedselsnummer.svar.value },
                avdoed = barnepensjon.foreldre
                    .filter { it.type == PersonType.AVDOED }
                    .map { it.foedselsnummer.svar.value },
                gjenlevende = barnepensjon.foreldre
                    .filter { it.type == PersonType.GJENLEVENDE_FORELDER }
                    .map { it.foedselsnummer.svar.value }
            )
        )
    }
}

private object OmstillingsstoenadUthenter {
    fun lagOpplysningsListe(jsonNode: JsonNode): List<Grunnlagsopplysning<out Any?>> {
        val barnepensjonssoknad = objectMapper.treeToValue<Omstillingsstoenad>(jsonNode)

        return listOfNotNull(
            avdoedOpplysning(barnepensjonssoknad)
            // soeker(barnepensjonssoknad, Opplysningstype.SOEKER_SOEKNAD_V1),
            // innsender(barnepensjonssoknad, Opplysningstype.INNSENDER_SOEKNAD_V1),
            // utbetalingsinformasjon(barnepensjonssoknad, Opplysningstype.UTBETALINGSINFORMASJON_V1),
            // søkerUtenlandsopphold,
            // utenlandsoppholdAvdød,
            // samtykke(barnepensjonssoknad, Opplysningstype.SAMTYKKE),
            // spraak(barnepensjonssoknad, Opplysningstype.SPRAAK),
            // soeknadMottattDato(barnepensjonssoknad, Opplysningstype.SOEKNAD_MOTTATT_DATO),
            // soeknadsType(barnepensjonssoknad, Opplysningstype.SOEKNADSTYPE_V1),
            // personGalleri(barnepensjonssoknad),
            // gjenlevendeForelder(barnepensjonssoknad, Opplysningstype.GJENLEVENDE_FORELDER_SOEKNAD_V1),
        )
    }

    private fun kilde(soknad: Omstillingsstoenad): Grunnlagsopplysning.Kilde {
        return Grunnlagsopplysning.Privatperson(
            soknad.innsender.foedselsnummer.svar.value,
            soknad.mottattDato.toInstant(ZoneOffset.UTC)
        )
    }

    private fun avdoedOpplysning(soknad: Omstillingsstoenad): Grunnlagsopplysning<out Any?> {
        val opplysning = avdoedOpplysning(soknad.avdoed)
        return lagOpplysning(AVDOED_SOEKNAD_V1, kilde(soknad), opplysning, null)
    }
}

private fun avdoedOpplysning(avdoed: Avdoed): AvdoedSoeknad {
    return AvdoedSoeknad(
        type = PersonType.AVDOED,
        fornavn = avdoed.fornavn.svar,
        etternavn = avdoed.etternavn.svar,
        foedselsnummer = avdoed.foedselsnummer.svar,
        doedsdato = avdoed.datoForDoedsfallet.svar.innhold,
        statsborgerskap = avdoed.statsborgerskap.svar.innhold,
        utenlandsopphold = UtenlandsoppholdOpplysningstype(
            avdoed.utenlandsopphold.svar.verdi,
            avdoed.utenlandsopphold.opplysning?.map { opphold ->
                UtenlandsoppholdOpplysningerOld(
                    opphold.land.svar.innhold,
                    opphold.fraDato?.svar?.innhold,
                    opphold.tilDato?.svar?.innhold,
                    opphold.oppholdsType.svar.map { it.verdi },
                    opphold.medlemFolketrygd.svar.verdi,
                    opphold.pensjonsutbetaling?.svar?.innhold
                )
            }
        ),
        doedsaarsakSkyldesYrkesskadeEllerYrkessykdom = avdoed
            .doedsaarsakSkyldesYrkesskadeEllerYrkessykdom.svar.verdi
    )
}

private fun <T : Any> lagOpplysning(
    opplysningsType: Opplysningstype,
    kilde: Grunnlagsopplysning.Kilde,
    opplysning: T,
    periode: Periode?
): Grunnlagsopplysning<T> {
    return Grunnlagsopplysning(
        id = UUID.randomUUID(),
        kilde = kilde,
        opplysningType = opplysningsType,
        meta = objectMapper.createObjectNode(),
        opplysning = opplysning,
        periode = periode
    )
}