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
            innsender(barnepensjonssoknad),
            samtykke(barnepensjonssoknad),
            utbetalingsinformasjon(barnepensjonssoknad),
            soekerPersoninfo(barnepensjonssoknad, Opplysningstyper.SOEKER_PERSONINFO_V1.value),
            soekerStatsborgerskap(barnepensjonssoknad),
            soekerUtenlandsadresse(barnepensjonssoknad),
            soekerVerge(barnepensjonssoknad),
            soekerDagligOmsorg(barnepensjonssoknad),
            gjenlevendeForelderPersoninfo(barnepensjonssoknad, Opplysningstyper.GJENLEVENDE_FORELDER_PERSONINFO_V1.value),
            avdoedForelderPersoninfo(barnepensjonssoknad, Opplysningstyper.AVDOED_PERSONINFO_V1.value),
            avdoedDoedsdato(barnepensjonssoknad, Opplysningstyper.AVDOED_DOEDSFALL_V1.value),
            avdoedDoedsaarsak(barnepensjonssoknad, Opplysningstyper.AVDOED_DOEDSAARSAK_V1.value),
            avdoedUtenlandsopphold(barnepensjonssoknad),
            avdoedNaeringsinntekt(barnepensjonssoknad),
            avdoedMilitaertjeneste(barnepensjonssoknad),
            soesken(barnepensjonssoknad, Opplysningstyper.SOEKER_RELASJON_SOESKEN_V1.value),
            soeknadMottattDato(barnepensjonssoknad, Opplysningstyper.SOEKNAD_MOTTATT_DATO.value)
        ).filterNotNull()
    }

    fun <T> setBehandlingsopplysninger(
        barnepensjon: Barnepensjon,
        opplysningsType: String,
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

    fun innsender(barnepensjon: Barnepensjon): Behandlingsopplysning<out PersonInfo> {
        return setBehandlingsopplysninger(
            barnepensjon, "innsender_personinfo:v1",
            PersonInfo(
                barnepensjon.innsender.fornavn.svar,
                barnepensjon.innsender.etternavn.svar,
                barnepensjon.innsender.foedselsnummer.svar,
                PersonType.INNSENDER
            )
        )
    }

    fun samtykke(barnepensjon: Barnepensjon): Behandlingsopplysning<out Samtykke> {
        return setBehandlingsopplysninger(barnepensjon, "samtykke", Samtykke(barnepensjon.harSamtykket.svar))
    }

    fun utbetalingsinformasjon(barnepensjon: Barnepensjon): Behandlingsopplysning<out Utbetalingsinformasjon>? {
        return barnepensjon.utbetalingsInformasjon?.let {
            setBehandlingsopplysninger(
                barnepensjon, "utbetalingsinformasjon:v1",
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

    fun soekerPersoninfo(barnepensjon: Barnepensjon, opplysningsType: String): Behandlingsopplysning<out PersonInfo> {
        return setBehandlingsopplysninger(
            barnepensjon, opplysningsType,
            PersonInfo(
                barnepensjon.soeker.fornavn.svar,
                barnepensjon.soeker.etternavn.svar,
                barnepensjon.soeker.foedselsnummer.svar,
                PersonType.BARN
            )
        )
    }

    fun soekerStatsborgerskap(barnepensjon: Barnepensjon): Behandlingsopplysning<out Statsborgerskap> {
        return setBehandlingsopplysninger(
            barnepensjon, "soeker_statsborgerskap:v1",
            Statsborgerskap(barnepensjon.soeker.statsborgerskap.svar, barnepensjon.soeker.foedselsnummer.svar.value)
        )
    }

    fun soekerUtenlandsadresse(barnepensjon: Barnepensjon): Behandlingsopplysning<out Utenlandsadresse> {
        val adresse = barnepensjon.soeker.utenlandsAdresse
        return setBehandlingsopplysninger(
            barnepensjon, "soeker_utenlandsadresse:v1",
            Utenlandsadresse(
                adresse?.svar,
                adresse?.opplysning?.land?.svar?.innhold,
                adresse?.opplysning?.adresse?.svar?.innhold,
                barnepensjon.soeker.foedselsnummer.svar.value
            )
        )
    }

    fun soekerVerge(barnepensjon: Barnepensjon): Behandlingsopplysning<out Verge>? {
        val verge = barnepensjon.soeker.verge
        return setBehandlingsopplysninger(
            barnepensjon, "soeker_verge:v1",
            Verge(
                verge?.svar,
                verge?.opplysning?.fornavn?.svar,
                verge?.opplysning?.etternavn?.svar,
                verge?.opplysning?.foedselsnummer?.svar?.value
            )
        )
    }

    fun soekerDagligOmsorg(barnepensjon: Barnepensjon): Behandlingsopplysning<out DagligOmsorg>? {
        return setBehandlingsopplysninger(
            barnepensjon, "soker_daglig_omsorg:v1",
            DagligOmsorg(barnepensjon.soeker.dagligOmsorg?.svar?.verdi)
        )
    }

    fun gjenlevendeForelderPersoninfo(barnepensjon: Barnepensjon, opplysningsType: String): Behandlingsopplysning<out PersonInfo>? {
        return hentGjenlevendeForelder(barnepensjon)?.let { forelder ->
            setBehandlingsopplysninger(
                barnepensjon, opplysningsType,
                PersonInfo(
                    forelder.fornavn.svar,
                    forelder.etternavn.svar,
                    forelder.foedselsnummer.svar,
                    PersonType.GJENLEVENDE_FORELDER
                )
            )
        }
    }

    fun avdoedForelderPersoninfo(barnepensjon: Barnepensjon, opplysningsType: String): Behandlingsopplysning<out PersonInfo>? {
        return hentAvdoedForelder(barnepensjon)?.let { avdoed ->
            setBehandlingsopplysninger(
                barnepensjon, opplysningsType,
                PersonInfo(
                    avdoed.fornavn.svar,
                    avdoed.etternavn.svar,
                    avdoed.foedselsnummer.svar,
                    PersonType.AVDOED
                )
            )
        }
    }

    fun avdoedDoedsdato(barnepensjon: Barnepensjon, opplysningsType: String): Behandlingsopplysning<out Doedsdato>? {
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

    fun avdoedDoedsaarsak(barnepensjon: Barnepensjon, opplysningsType: String): Behandlingsopplysning<out Doedsaarsak>? {
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


    fun avdoedUtenlandsopphold(barnepensjon: Barnepensjon): Behandlingsopplysning<out Utenlandsopphold>? {
        return hentAvdoedForelder(barnepensjon)?.let { avdoed ->
            setBehandlingsopplysninger(
                barnepensjon, "avdoed_utenlandsopphold:v1",
                Utenlandsopphold(
                    avdoed.utenlandsopphold.svar,
                    avdoed.utenlandsopphold.opplysning?.map { opphold ->
                        UtenlandsoppholdOpplysninger(
                            opphold.land.svar.innhold,
                            opphold.fraDato?.svar?.innhold,
                            opphold.tilDato?.svar?.innhold,
                            opphold.oppholdsType.svar.map {it.verdi},
                            opphold.medlemFolketrygd.svar,
                            opphold.pensjonsutbetaling?.svar?.innhold
                        )
                    },
                    avdoed.foedselsnummer.svar.value
                )
            )
        }
    }

    fun avdoedNaeringsinntekt(barnepensjon: Barnepensjon): Behandlingsopplysning<out Naeringsinntekt>? {
        return hentAvdoedForelder(barnepensjon)?.let { avdoed ->
            setBehandlingsopplysninger(
                barnepensjon, "avdoed_naeringsinntekt:v1",
                Naeringsinntekt(
                    avdoed.naeringsInntekt?.svar,
                    avdoed.naeringsInntekt?.opplysning?.naeringsinntektVedDoedsfall?.svar,
                    avdoed.naeringsInntekt?.opplysning?.naeringsinntektPrAarFoerDoedsfall?.svar?.innhold,
                    avdoed.foedselsnummer.svar.value
                )
            )
        }
    }

    fun avdoedMilitaertjeneste(barnepensjon: Barnepensjon): Behandlingsopplysning<out Militaertjeneste>? {
        return hentAvdoedForelder(barnepensjon)?.let { avdoed ->
            setBehandlingsopplysninger(
                barnepensjon, "avdoed_militaertjeneste:v1",
                Militaertjeneste(
                    avdoed.militaertjeneste?.svar,
                    avdoed.militaertjeneste?.opplysning?.svar?.innhold,
                    avdoed.foedselsnummer.svar.value
                )
            )
        }
    }

    fun soesken(barnepensjon: Barnepensjon, opplysningsType: String): Behandlingsopplysning<out Soesken> {
        val soeskenPersonInfo = barnepensjon.soesken.map { barn ->
            PersonInfo(
                barn.fornavn.svar,
                barn.etternavn.svar,
                barn.foedselsnummer.svar,
                PersonType.BARN
            )
        }
        return setBehandlingsopplysninger(
            barnepensjon, opplysningsType,
            Soesken(soeskenPersonInfo)
        )
    }

    fun soeknadMottattDato(barnepensjon: Barnepensjon, opplysningsType: String): Behandlingsopplysning<out SoeknadMottattDato> {
        return setBehandlingsopplysninger(
            barnepensjon, opplysningsType,
            SoeknadMottattDato(barnepensjon.mottattDato)
        )
    }

}