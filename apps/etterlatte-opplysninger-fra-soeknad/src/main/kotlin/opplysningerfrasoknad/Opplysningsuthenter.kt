package no.nav.etterlatte.opplysningerfrasoknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.common.objectMapper
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Forelder
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.GjenlevendeForelderSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.InnsenderSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.UTENLANDSOPPHOLD
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Samtykke
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoekerBarnSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeknadMottattDato
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeknadstypeOpplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Utbetalingsinformasjon
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.UtenlandsadresseBarn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.UtenlandsoppholdOpplysninger
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Verge
import no.nav.etterlatte.libs.common.person.Utenlandsopphold
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Avdoed
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.GjenlevendeForelder
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType
import java.time.ZoneOffset
import java.util.UUID
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Utenlandsopphold as UtenlandsoppholdOpplysningstype

class Opplysningsuthenter {

    fun lagOpplysningsListe(jsonNode: JsonNode): List<Grunnlagsopplysning<out Any>> {
        val barnepensjonssoknad = objectMapper.treeToValue<Barnepensjon>(jsonNode)
        val kilde = Grunnlagsopplysning.Privatperson(
            barnepensjonssoknad.innsender.foedselsnummer.svar.value,
            barnepensjonssoknad.mottattDato.toInstant(ZoneOffset.UTC)
        )

        val utenlandsoppholdSøker = barnepensjonssoknad.soeker.utenlandsAdresse

        val søkerUtenlandsopphold = utenlandsoppholdSøker?.svar?.verdi?.let {
            lagOpplysning(
                UTENLANDSOPPHOLD,
                kilde,
                Utenlandsopphold(
                    it,
                    utenlandsoppholdSøker.opplysning?.land?.svar?.innhold,
                    utenlandsoppholdSøker.opplysning?.adresse?.svar?.innhold
                )
            )
        }

        val avdød = hentAvdoedForelder(barnepensjonssoknad)

        val avdødUtenlandsopphold = avdød?.let {
            lagOpplysning(
                UTENLANDSOPPHOLD,
                kilde,
                Utenlandsopphold(it.utenlandsopphold.svar.verdi, null, null)
            )
        }

        return listOfNotNull(
            søkerUtenlandsopphold,
            avdødUtenlandsopphold,
            avdoed(barnepensjonssoknad, Opplysningstyper.AVDOED_SOEKNAD_V1),
            soeker(barnepensjonssoknad, Opplysningstyper.SOEKER_SOEKNAD_V1),
            gjenlevendeForelder(barnepensjonssoknad, Opplysningstyper.GJENLEVENDE_FORELDER_SOEKNAD_V1),
            innsender(barnepensjonssoknad, Opplysningstyper.INNSENDER_SOEKNAD_V1),
            utbetalingsinformasjon(barnepensjonssoknad, Opplysningstyper.UTBETALINGSINFORMASJON_V1),
            samtykke(barnepensjonssoknad, Opplysningstyper.SAMTYKKE),
            soeknadMottattDato(barnepensjonssoknad, Opplysningstyper.SOEKNAD_MOTTATT_DATO),
            soeknadsType(barnepensjonssoknad, Opplysningstyper.SOEKNADSTYPE_V1),
            personGalleri(barnepensjonssoknad)
        )
    }

    private fun <T> setBehandlingsopplysninger(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstyper,
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
        opplysningsType: Opplysningstyper
    ): Grunnlagsopplysning<out AvdoedSoeknad>? {
        return hentAvdoedForelder(barnepensjon)?.let { avdoed ->
            setBehandlingsopplysninger(
                barnepensjon,
                opplysningsType,
                AvdoedSoeknad(
                    type = PersonType.AVDOED,
                    fornavn = avdoed.fornavn.svar,
                    etternavn = avdoed.etternavn.svar,
                    foedselsnummer = avdoed.foedselsnummer.svar,
                    doedsdato = avdoed.datoForDoedsfallet.svar.innhold,
                    statsborgerskap = avdoed.statsborgerskap.svar.innhold,
                    utenlandsopphold = UtenlandsoppholdOpplysningstype(
                        avdoed.utenlandsopphold.svar.verdi,
                        avdoed.utenlandsopphold.opplysning?.map { opphold ->
                            UtenlandsoppholdOpplysninger(
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
            )
        }
    }

    fun soeker(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstyper
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
        opplysningsType: Opplysningstyper
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
        opplysningsType: Opplysningstyper
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

    fun samtykke(barnepensjon: Barnepensjon, opplysningsType: Opplysningstyper): Grunnlagsopplysning<out Samtykke> {
        return setBehandlingsopplysninger(barnepensjon, opplysningsType, Samtykke(barnepensjon.harSamtykket.svar))
    }

    fun utbetalingsinformasjon(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstyper
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
        opplysningsType: Opplysningstyper
    ): Grunnlagsopplysning<out SoeknadMottattDato> {
        return setBehandlingsopplysninger(
            barnepensjon,
            opplysningsType,
            SoeknadMottattDato(barnepensjon.mottattDato)
        )
    }

    private fun soeknadsType(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstyper
    ): Grunnlagsopplysning<out SoeknadstypeOpplysning> {
        return setBehandlingsopplysninger(barnepensjon, opplysningsType, SoeknadstypeOpplysning(barnepensjon.type))
    }

    private fun personGalleri(
        barnepensjon: Barnepensjon
    ): Grunnlagsopplysning<out Persongalleri> {
        return setBehandlingsopplysninger(
            barnepensjon,
            Opplysningstyper.PERSONGALLERI_V1,
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

private fun <T> lagOpplysning(
    opplysningsType: Opplysningstyper,
    kilde: Grunnlagsopplysning.Kilde,
    opplysning: T
): Grunnlagsopplysning<T> {
    return Grunnlagsopplysning(
        UUID.randomUUID(),
        kilde,
        opplysningsType,
        objectMapper.createObjectNode(),
        opplysning
    )
}