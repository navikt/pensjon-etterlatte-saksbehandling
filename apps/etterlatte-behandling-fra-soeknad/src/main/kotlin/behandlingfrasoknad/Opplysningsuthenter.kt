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
            soekerPersoninfo(barnepensjonssoknad),
            soekerStatsborgerskap(barnepensjonssoknad),
            soekerUtenlandsadresse(barnepensjonssoknad),
            soekerVerge(barnepensjonssoknad),
            soekerDagligOmsorg(barnepensjonssoknad),
            gjenlevendeForelderPersoninfo(barnepensjonssoknad),
            avdoedForelderPersoninfo(barnepensjonssoknad),
            avdoedDoedsdato(barnepensjonssoknad),
            avdoedDoedsaarsak(barnepensjonssoknad),
            avdoedUtenlandsopphold(barnepensjonssoknad),
            avdoedNaeringsinntekt(barnepensjonssoknad),
            avdoedMilitaertjeneste(barnepensjonssoknad),
            soesken(barnepensjonssoknad),
            soeknadMottattDato(barnepensjonssoknad)
        ).filterNotNull()
    }

    fun <T> setBehandlingsopplysninger(
        barnepensjon: Barnepensjon,
        opplysningsType: String,
        data: T
    ): Behandlingsopplysning<T> {
        return Behandlingsopplysning(
            UUID.randomUUID(), Behandlingsopplysning.Privatperson(
                barnepensjon.innsender.foedselsnummer.value,
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
                barnepensjon.innsender.fornavn,
                barnepensjon.innsender.etternavn,
                barnepensjon.innsender.foedselsnummer,
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
                    it.svar,
                    it.opplysning?.kontonummer?.svar,
                    it.opplysning?.utenlandskBankNavn?.svar,
                    it.opplysning?.utenlandskBankAdresse?.svar,
                    it.opplysning?.iban?.svar,
                    it.opplysning?.swift?.svar,
                    it.opplysning?.skattetrekk?.svar,
                    it.opplysning?.skattetrekk?.opplysning?.svar
                )
            )
        }
    }

    fun soekerPersoninfo(barnepensjon: Barnepensjon): Behandlingsopplysning<out PersonInfo> {
        return setBehandlingsopplysninger(
            barnepensjon, "soeker_personinfo:v1",
            PersonInfo(
                barnepensjon.soeker.fornavn,
                barnepensjon.soeker.etternavn,
                barnepensjon.soeker.foedselsnummer,
                PersonType.BARN
            )
        )
    }

    fun soekerStatsborgerskap(barnepensjon: Barnepensjon): Behandlingsopplysning<out Statsborgerskap> {
        return setBehandlingsopplysninger(
            barnepensjon, "soeker_statsborgerskap:v1",
            Statsborgerskap(barnepensjon.soeker.statsborgerskap.svar, barnepensjon.soeker.foedselsnummer.value)
        )
    }

    fun soekerUtenlandsadresse(barnepensjon: Barnepensjon): Behandlingsopplysning<out Utenlandsadresse> {
        val adresse = barnepensjon.soeker.utenlandsAdresse
        return setBehandlingsopplysninger(
            barnepensjon, "soeker_utenlandsadresse:v1",
            Utenlandsadresse(
                adresse?.svar,
                adresse?.opplysning?.land?.svar,
                adresse?.opplysning?.adresse?.svar,
                barnepensjon.soeker.foedselsnummer.value
            )
        )
    }

    fun soekerVerge(barnepensjon: Barnepensjon): Behandlingsopplysning<out Verge>? {
        val verge = barnepensjon.soeker.verge
        return setBehandlingsopplysninger(
            barnepensjon, "soeker_verge:v1",
            Verge(
                verge?.svar,
                verge?.opplysning?.fornavn,
                verge?.opplysning?.etternavn,
                verge?.opplysning?.foedselsnummer?.value
            )
        )
    }

    fun soekerDagligOmsorg(barnepensjon: Barnepensjon): Behandlingsopplysning<out DagligOmsorg>? {
        return setBehandlingsopplysninger(
            barnepensjon, "soker_daglig_omsorg:v1",
            DagligOmsorg(barnepensjon.soeker.dagligOmsorg?.svar)
        )
    }

    fun gjenlevendeForelderPersoninfo(barnepensjon: Barnepensjon): Behandlingsopplysning<out PersonInfo>? {
        return hentGjenlevendeForelder(barnepensjon)?.let { forelder ->
            setBehandlingsopplysninger(
                barnepensjon, "forelder_gjenlevende_personinfo:v1",
                PersonInfo(
                    forelder.fornavn,
                    forelder.etternavn,
                    forelder.foedselsnummer,
                    PersonType.GJENLEVENDE_FORELDER
                )
            )
        }
    }

    fun avdoedForelderPersoninfo(barnepensjon: Barnepensjon): Behandlingsopplysning<out PersonInfo>? {
        return hentAvdoedForelder(barnepensjon)?.let { avdoed ->
            setBehandlingsopplysninger(
                barnepensjon, "avdoed_personinfo:v1",
                PersonInfo(
                    avdoed.fornavn,
                    avdoed.etternavn,
                    avdoed.foedselsnummer,
                    PersonType.AVDOED
                )
            )
        }
    }

    fun avdoedDoedsdato(barnepensjon: Barnepensjon): Behandlingsopplysning<out Doedsdato>? {
        return hentAvdoedForelder(barnepensjon)?.let { avdoed ->
            setBehandlingsopplysninger(
                barnepensjon, "avdoed_doedsfall:v1",
                Doedsdato(
                    avdoed.datoForDoedsfallet.svar,
                    avdoed.foedselsnummer.value
                )
            )
        }
    }

    fun avdoedDoedsaarsak(barnepensjon: Barnepensjon): Behandlingsopplysning<out Doedsaarsak>? {
        return hentAvdoedForelder(barnepensjon)?.let { avdoed ->
            setBehandlingsopplysninger(
                barnepensjon, "avdoed_doedsaarsak:v1",
                Doedsaarsak(
                    avdoed.doedsaarsakSkyldesYrkesskadeEllerYrkessykdom.svar,
                    avdoed.foedselsnummer.value
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
                            opphold.land.svar,
                            opphold.fraDato?.svar,
                            opphold.tilDato?.svar,
                            opphold.oppholdsType.svar,
                            opphold.medlemFolketrygd.svar,
                            opphold.pensjonsutbetaling?.svar
                        )
                    },
                    avdoed.foedselsnummer.value
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
                    avdoed.naeringsInntekt?.opplysning?.naeringsinntektPrAarFoerDoedsfall?.svar,
                    avdoed.foedselsnummer.value
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
                    avdoed.militaertjeneste?.opplysning?.svar,
                    avdoed.foedselsnummer.value
                )
            )
        }
    }

    fun soesken(barnepensjon: Barnepensjon): Behandlingsopplysning<out Soesken> {
        val soeskenPersonInfo = barnepensjon.soesken.map { barn ->
            PersonInfo(
                barn.fornavn,
                barn.etternavn,
                barn.foedselsnummer,
                PersonType.BARN
            )
        }
        return setBehandlingsopplysninger(
            barnepensjon, "soeker_soesken:v1",
            Soesken(soeskenPersonInfo)
        )
    }

    fun soeknadMottattDato(barnepensjon: Barnepensjon): Behandlingsopplysning<out SoeknadMottattDato> {
        return setBehandlingsopplysninger(
            barnepensjon, "soeknad_mottatt_dato",
            SoeknadMottattDato(barnepensjon.mottattDato)
        )
    }

}