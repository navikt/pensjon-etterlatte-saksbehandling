package no.nav.etterlatte.behandlingfrasoknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.common.objectMapper
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Doedsaarsak
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Doedsdato
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.*
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Naeringsinntekt
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Utenlandsadresse
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Utenlandsopphold
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Verge
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import no.nav.etterlatte.libs.common.soeknad.dataklasser.GjenlevendeForelder
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.*
import java.time.ZoneOffset
import java.util.*

class Opplysningsuthenter {

    fun lagOpplysningsListe(jsonNode: JsonNode): List<Behandlingsopplysning<out Any>> {
        val barnepensjonssoknad = objectMapper.treeToValue<Barnepensjon>(jsonNode)!!

        return listOf<Behandlingsopplysning<out Any>?>(
            innsender(barnepensjonssoknad, Opplysningstyper.INNSENDER_PERSONINFO_V1),
            samtykke(barnepensjonssoknad, Opplysningstyper.SAMTYKKE),
            utbetalingsinformasjon(barnepensjonssoknad, Opplysningstyper.UTBETALINGSINFORMASJON_V1),
            soekerPersoninfo(barnepensjonssoknad, Opplysningstyper.SOEKER_PERSONINFO_V1),
            soekerStatsborgerskap(barnepensjonssoknad, Opplysningstyper.SOEKER_STATSBORGERSKAP_V1),
            soekerUtenlandsadresse(barnepensjonssoknad, Opplysningstyper.SOEKER_UTENLANDSADRESSE_V1),
            soekerVerge(barnepensjonssoknad, Opplysningstyper.SOEKER_VERGE_V1),
            soekerDagligOmsorg(barnepensjonssoknad, Opplysningstyper.SOEKER_DAGLIG_OMSORG_V1),
            gjenlevendeForelderPersoninfo(barnepensjonssoknad, Opplysningstyper.GJENLEVENDE_FORELDER_PERSONINFO_V1),
            avdoedForelderPersoninfo(barnepensjonssoknad, Opplysningstyper.AVDOED_PERSONINFO_V1),
            avdoedDoedsdato(barnepensjonssoknad, Opplysningstyper.AVDOED_DOEDSFALL_V1),
            avdoedDoedsaarsak(barnepensjonssoknad, Opplysningstyper.AVDOED_DOEDSAARSAK_V1),
            avdoedUtenlandsopphold(barnepensjonssoknad, Opplysningstyper.AVDOED_UTENLANDSOPPHOLD_V1),
            avdoedNaeringsinntekt(barnepensjonssoknad, Opplysningstyper.AVDOED_NAERINGSINNTEKT_V1),
            avdoedMilitaertjeneste(barnepensjonssoknad, Opplysningstyper.AVDOED_MILITAERTJENESTE_V1),
            soesken(barnepensjonssoknad, Opplysningstyper.SOEKER_RELASJON_SOESKEN_V1),
            soeknadMottattDato(barnepensjonssoknad, Opplysningstyper.SOEKNAD_MOTTATT_DATO),
            //soeknadsType(barnepensjonssoknad, Opplysningstyper.SOEKNADSTYPE_V1)
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

    fun innsender(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstyper
    ): Behandlingsopplysning<out PersonInfo> {
        return setBehandlingsopplysninger(
            barnepensjon, opplysningsType,
            PersonInfo(
                barnepensjon.innsender.fornavn.svar,
                barnepensjon.innsender.etternavn.svar,
                barnepensjon.innsender.foedselsnummer.svar,
                null,
                PersonType.INNSENDER
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
                    it.opplysning?.skattetrekk?.svar,
                    it.opplysning?.skattetrekk?.opplysning?.svar?.innhold
                )
            )
        }
    }

    fun soekerPersoninfo(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstyper
    ): Behandlingsopplysning<out PersonInfo> {
        return setBehandlingsopplysninger(
            barnepensjon, opplysningsType,
            PersonInfo(
                barnepensjon.soeker.fornavn.svar,
                barnepensjon.soeker.etternavn.svar,
                barnepensjon.soeker.foedselsnummer.svar,
                null,
                PersonType.BARN
            )
        )
    }

    fun soekerStatsborgerskap(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstyper
    ): Behandlingsopplysning<out Statsborgerskap> {
        return setBehandlingsopplysninger(
            barnepensjon, opplysningsType,
            Statsborgerskap(barnepensjon.soeker.statsborgerskap.svar, barnepensjon.soeker.foedselsnummer.svar.value)
        )
    }

    fun soekerUtenlandsadresse(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstyper
    ): Behandlingsopplysning<out Utenlandsadresse> {
        val adresse = barnepensjon.soeker.utenlandsAdresse
        return setBehandlingsopplysninger(
            barnepensjon, opplysningsType,
            Utenlandsadresse(
                adresse?.svar?.innhold,
                adresse?.opplysning?.land?.svar?.innhold,
                adresse?.opplysning?.adresse?.svar?.innhold,
                barnepensjon.soeker.foedselsnummer.svar.value
            )
        )
    }

    fun soekerVerge(barnepensjon: Barnepensjon, opplysningsType: Opplysningstyper): Behandlingsopplysning<out Verge>? {
        val verge = barnepensjon.soeker.verge
        return setBehandlingsopplysninger(
            barnepensjon, opplysningsType,
            Verge(
                verge?.svar,
                verge?.opplysning?.fornavn?.svar,
                verge?.opplysning?.etternavn?.svar,
                verge?.opplysning?.foedselsnummer?.svar?.value
            )
        )
    }

    fun soekerDagligOmsorg(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstyper
    ): Behandlingsopplysning<out DagligOmsorg>? {
        return setBehandlingsopplysninger(
            barnepensjon, opplysningsType,
            DagligOmsorg(barnepensjon.soeker.dagligOmsorg?.svar?.verdi)
        )
    }

    fun gjenlevendeForelderPersoninfo(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstyper
    ): Behandlingsopplysning<out PersonInfo>? {
        return hentGjenlevendeForelder(barnepensjon)?.let { forelder ->
            setBehandlingsopplysninger(
                barnepensjon, opplysningsType,
                PersonInfo(
                    forelder.fornavn.svar,
                    forelder.etternavn.svar,
                    forelder.foedselsnummer.svar,
                    forelder.adresse.svar,
                    PersonType.GJENLEVENDE_FORELDER
                )
            )
        }
    }

    fun avdoedForelderPersoninfo(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstyper
    ): Behandlingsopplysning<out PersonInfo>? {
        return hentAvdoedForelder(barnepensjon)?.let { avdoed ->
            setBehandlingsopplysninger(
                barnepensjon, opplysningsType,
                PersonInfo(
                    avdoed.fornavn.svar,
                    avdoed.etternavn.svar,
                    avdoed.foedselsnummer.svar,
                    null,
                    PersonType.AVDOED
                )
            )
        }
    }

    fun avdoedDoedsdato(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstyper
    ): Behandlingsopplysning<out Doedsdato>? {
        return hentAvdoedForelder(barnepensjon)?.let { avdoed ->
            setBehandlingsopplysninger(
                barnepensjon, opplysningsType,
                Doedsdato(
                    avdoed.datoForDoedsfallet.svar.innhold,
                    avdoed.foedselsnummer.svar.value
                )
            )
        }
    }

    fun avdoedDoedsaarsak(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstyper
    ): Behandlingsopplysning<out Doedsaarsak>? {
        return hentAvdoedForelder(barnepensjon)?.let { avdoed ->
            setBehandlingsopplysninger(
                barnepensjon, opplysningsType,
                Doedsaarsak(
                    avdoed.doedsaarsakSkyldesYrkesskadeEllerYrkessykdom.svar,
                    avdoed.foedselsnummer.svar.value
                )
            )
        }
    }


    fun avdoedUtenlandsopphold(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstyper
    ): Behandlingsopplysning<out Utenlandsopphold>? {
        return hentAvdoedForelder(barnepensjon)?.let { avdoed ->
            setBehandlingsopplysninger(
                barnepensjon, opplysningsType,
                Utenlandsopphold(
                    avdoed.utenlandsopphold.svar.innhold,
                    avdoed.utenlandsopphold.opplysning?.map { opphold ->
                        UtenlandsoppholdOpplysninger(
                            opphold.land.svar.innhold,
                            opphold.fraDato?.svar?.innhold,
                            opphold.tilDato?.svar?.innhold,
                            opphold.oppholdsType.svar.map { it.verdi },
                            opphold.medlemFolketrygd.svar.innhold,
                            opphold.pensjonsutbetaling?.svar?.innhold
                        )
                    },
                    avdoed.foedselsnummer.svar.value
                )
            )
        }
    }

    fun avdoedNaeringsinntekt(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstyper
    ): Behandlingsopplysning<out Naeringsinntekt>? {
        return hentAvdoedForelder(barnepensjon)?.let { avdoed ->
            setBehandlingsopplysninger(
                barnepensjon, opplysningsType,
                Naeringsinntekt(
                    avdoed.naeringsInntekt?.svar,
                    avdoed.naeringsInntekt?.opplysning?.naeringsinntektVedDoedsfall?.svar,
                    avdoed.naeringsInntekt?.opplysning?.naeringsinntektPrAarFoerDoedsfall?.svar?.innhold,
                    avdoed.foedselsnummer.svar.value
                )
            )
        }
    }

    fun avdoedMilitaertjeneste(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstyper
    ): Behandlingsopplysning<out Militaertjeneste>? {
        return hentAvdoedForelder(barnepensjon)?.let { avdoed ->
            setBehandlingsopplysninger(
                barnepensjon, opplysningsType,
                Militaertjeneste(
                    avdoed.militaertjeneste?.svar,
                    avdoed.militaertjeneste?.opplysning?.svar?.innhold,
                    avdoed.foedselsnummer.svar.value
                )
            )
        }
    }

    fun soesken(barnepensjon: Barnepensjon, opplysningsType: Opplysningstyper): Behandlingsopplysning<out Soesken> {
        val soeskenPersonInfo = barnepensjon.soesken.map { barn ->
            PersonInfo(
                barn.fornavn.svar,
                barn.etternavn.svar,
                barn.foedselsnummer.svar,
                null,
                PersonType.BARN
            )
        }
        return setBehandlingsopplysninger(
            barnepensjon, opplysningsType,
            Soesken(soeskenPersonInfo)
        )
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

/*    fun soeknadsType(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstyper
    ): Behandlingsopplysning<out SoeknadType> {
        return setBehandlingsopplysninger(barnepensjon, opplysningsType, barnepensjon.type)
    }*/

}