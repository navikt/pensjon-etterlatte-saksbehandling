package no.nav.etterlatte.behandlingfrasoknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.common.objectMapper
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.*
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Forelder
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Utenlandsopphold
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Verge
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.*
import java.time.ZoneOffset
import java.util.*

class Opplysningsuthenter {

    fun lagOpplysningsListe(jsonNode: JsonNode): List<Behandlingsopplysning<out Any>> {
        val barnepensjonssoknad = objectMapper.treeToValue<Barnepensjon>(jsonNode)!!

        return listOf<Behandlingsopplysning<out Any>?>(
            avdoed(barnepensjonssoknad, Opplysningstyper.AVDOED_SOEKNAD_V1),
            soeker(barnepensjonssoknad, Opplysningstyper.SOEKER_SOEKNAD_V1),
            gjenlevendeForelder(barnepensjonssoknad, Opplysningstyper.GJENLEVENDE_FORELDER_SOEKNAD_V1),
            innsender(barnepensjonssoknad, Opplysningstyper.INNSENDER_SOEKNAD_V1),
            utbetalingsinformasjon(barnepensjonssoknad, Opplysningstyper.UTBETALINGSINFORMASJON_V1),
            samtykke(barnepensjonssoknad, Opplysningstyper.SAMTYKKE),
            soeknadMottattDato(barnepensjonssoknad, Opplysningstyper.SOEKNAD_MOTTATT_DATO),
            soeknadsType(barnepensjonssoknad, Opplysningstyper.SOEKNADSTYPE_V1),
            //TODO: skal vi kun ha personGalleri i gyldig søknad? eg slettes her, eller fint med dobblet opp?
            personGalleri(barnepensjonssoknad)
        ).filterNotNull()
    }

    fun <T> setBehandlingsopplysninger(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstyper,
        data: T
    ): Behandlingsopplysning<T> {
        return Behandlingsopplysning(
            UUID.randomUUID(), Behandlingsopplysning.Privatperson(
                barnepensjon.innsender.foedselsnummer.svar.value,
                barnepensjon.mottattDato.toInstant(ZoneOffset.UTC)
            ), opplysningsType, objectMapper.createObjectNode(),
            data
        )
    }

    fun hentAvdoedForelder(barnepensjon: Barnepensjon): Avdoed? {
        return barnepensjon.foreldre.find { it.type === PersonType.AVDOED }?.let { it as Avdoed }
    }

    fun hentGjenlevendeForelder(barnepensjon: Barnepensjon): GjenlevendeForelder? {
        return barnepensjon.foreldre.find { it.type === PersonType.GJENLEVENDE_FORELDER }
            ?.let { it as GjenlevendeForelder }
    }

    fun avdoed(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstyper
    ): Behandlingsopplysning<out AvdoedSoeknad>? {
        return hentAvdoedForelder(barnepensjon)?.let { avdoed ->
            setBehandlingsopplysninger(barnepensjon, opplysningsType,
                AvdoedSoeknad(
                    type = PersonType.AVDOED,
                    fornavn = avdoed.fornavn.svar,
                    etternavn = avdoed.etternavn.svar,
                    foedselsnummer = avdoed.foedselsnummer.svar,
                    doedsdato = avdoed.datoForDoedsfallet.svar.innhold,
                    statsborgerskap = avdoed.statsborgerskap.svar.innhold,
                    utenlandsopphold = Utenlandsopphold(
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
                    doedsaarsakSkyldesYrkesskadeEllerYrkessykdom = avdoed.doedsaarsakSkyldesYrkesskadeEllerYrkessykdom.svar.verdi
                )
            )
        }
    }

    fun soeker(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstyper
    ): Behandlingsopplysning<out SoekerBarnSoeknad> {
        val adresse = barnepensjon.soeker.utenlandsAdresse

        return setBehandlingsopplysninger(
            barnepensjon, opplysningsType,
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
    ): Behandlingsopplysning<out GjenlevendeForelderSoeknad>? {
        return hentGjenlevendeForelder(barnepensjon)?.let { forelder ->
            setBehandlingsopplysninger(
                barnepensjon, opplysningsType,
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
    ): Behandlingsopplysning<out InnsenderSoeknad> {
        return setBehandlingsopplysninger(
            barnepensjon, opplysningsType,
            InnsenderSoeknad(
                PersonType.INNSENDER,
                barnepensjon.innsender.fornavn.svar,
                barnepensjon.innsender.etternavn.svar,
                barnepensjon.innsender.foedselsnummer.svar,
            )
        )
    }

    fun samtykke(barnepensjon: Barnepensjon, opplysningsType: Opplysningstyper): Behandlingsopplysning<out Samtykke> {
        return setBehandlingsopplysninger(barnepensjon, opplysningsType, Samtykke(barnepensjon.harSamtykket.svar))
    }

    fun utbetalingsinformasjon(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstyper
    ): Behandlingsopplysning<out Utbetalingsinformasjon>? {
        return barnepensjon.utbetalingsInformasjon?.let {
            setBehandlingsopplysninger(
                barnepensjon, opplysningsType,
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

    fun soeknadMottattDato(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstyper
    ): Behandlingsopplysning<out SoeknadMottattDato> {
        return setBehandlingsopplysninger(
            barnepensjon, opplysningsType,
            SoeknadMottattDato(barnepensjon.mottattDato)
        )
    }

    fun soeknadsType(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstyper
    ): Behandlingsopplysning<out SoeknadstypeOpplysning> {
        return setBehandlingsopplysninger(barnepensjon, opplysningsType, SoeknadstypeOpplysning(barnepensjon.type))
    }

    fun personGalleri(
        barnepensjon: Barnepensjon
    ): Behandlingsopplysning<out Persongalleri> {
        return setBehandlingsopplysninger(barnepensjon, Opplysningstyper.PERSONGALLERI_V1, Persongalleri(
            soker = barnepensjon.soeker.foedselsnummer.svar.value,
            innsender = barnepensjon.innsender.foedselsnummer.svar.value,
            soesken = barnepensjon.soesken.map { it.foedselsnummer.svar.value },
            avdoed = barnepensjon.foreldre.filter { it.type == PersonType.AVDOED }.map { it.foedselsnummer.svar.value },
            gjenlevende = barnepensjon.foreldre.filter { it.type == PersonType.GJENLEVENDE_FORELDER }.map { it.foedselsnummer.svar.value }
        ))
    }

}