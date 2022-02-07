package no.nav.etterlatte.behandlingfrasoknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.common.objectMapper
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Doedsaarsak
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Doedsdato
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.*
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Naeringsinntekt
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Utenlandsopphold
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import no.nav.etterlatte.libs.common.soeknad.dataklasser.GjenlevendeForelder
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.*
import java.time.ZoneOffset
import java.util.*

class Opplysningsuthenter {

    fun lagOpplysningsListe(jsonNode: JsonNode): List<Behandlingsopplysning<out Any>> {

        val barnepensjonssoknad = objectMapper.treeToValue<Barnepensjon>(jsonNode)!!
        val tomNode = objectMapper.createObjectNode()
        val kilde = Behandlingsopplysning.Privatperson(
            barnepensjonssoknad.innsender.foedselsnummer.value,
            barnepensjonssoknad.mottattDato.toInstant(ZoneOffset.UTC)
        )

        return listOf(
            innsender(barnepensjonssoknad),
            samtykke(barnepensjonssoknad),
            utbetalingsinformasjon(barnepensjonssoknad),
            soeker_personinfo(barnepensjonssoknad),
            soeker_statsborgerskap(barnepensjonssoknad),
            soeker_utenlandsadresse(barnepensjonssoknad),
            soeker_verge(barnepensjonssoknad),
            soeker_daglig_omsorg(barnepensjonssoknad),
            soesken(barnepensjonssoknad),
            soeknad_mottatt_dato(barnepensjonssoknad)
        ).filter { it.second != null }
            .map { Behandlingsopplysning(UUID.randomUUID(), kilde, it.first, tomNode, it.second!!) } +
                listOf<Behandlingsopplysning<out Any>?>(
                    avdoedDoedsdato(barnepensjonssoknad),
                    avdoedDoedsaarsak(barnepensjonssoknad),
                    avdoedUtenlandsopphold(barnepensjonssoknad),
                    avdoedNaeringsinntekt(barnepensjonssoknad),
                    avdoedMilitaertjeneste(barnepensjonssoknad),
                    gjenlevendeForelderPersoninfo(barnepensjonssoknad),
                    avdoedForelderPersoninfo(barnepensjonssoknad)
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
        return barnepensjon.foreldre.find { it.type === PersonType.GJENLEVENDE_FORELDER}?.let {it as GjenlevendeForelder}
    }

    fun innsender(barnepensjon: Barnepensjon) =
        "innsender" to barnepensjon.innsender

    fun samtykke(barnepensjon: Barnepensjon) =
        "samtykke" to barnepensjon.harSamtykket

    fun utbetalingsinformasjon(barnepensjon: Barnepensjon) =
        "utbetalingsinformasjon" to barnepensjon.utbetalingsInformasjon

    fun soeker_personinfo(barnepensjon: Barnepensjon) =
        "soeker_personinfo" to
                PersonInfo(
                    barnepensjon.soeker.fornavn,
                    barnepensjon.soeker.etternavn,
                    barnepensjon.soeker.foedselsnummer,
                    PersonType.BARN
                )

    fun soeker_statsborgerskap(barnepensjon: Barnepensjon) =
        "soeker_statsborgerskap" to barnepensjon.soeker.statsborgerskap

    fun soeker_utenlandsadresse(barnepensjon: Barnepensjon) =
        "soeker_utenlandsadresse" to barnepensjon.soeker.utenlandsAdresse

    fun soeker_verge(barnepensjon: Barnepensjon) =
        "soeker_verge" to barnepensjon.soeker.verge

    fun soeker_daglig_omsorg(barnepensjon: Barnepensjon) =
        "soeker_daglig_omsorg" to barnepensjon.soeker.dagligOmsorg


    fun gjenlevendeForelderPersoninfo(barnepensjon: Barnepensjon): Behandlingsopplysning<PersonInfo>? {
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

    fun avdoedForelderPersoninfo(barnepensjon: Barnepensjon): Behandlingsopplysning<PersonInfo>? {
        return hentAvdoedForelder(barnepensjon)?.let { avdoed ->
            setBehandlingsopplysninger(
                barnepensjon, "forelder_avdoed_personinfo:v1",
                PersonInfo(
                    avdoed.fornavn,
                    avdoed.etternavn,
                    avdoed.foedselsnummer,
                    PersonType.AVDOED
                )
            )
        }
    }

    fun avdoedDoedsdato(barnepensjon: Barnepensjon): Behandlingsopplysning<Doedsdato>? {
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

    fun avdoedDoedsaarsak(barnepensjon: Barnepensjon): Behandlingsopplysning<Doedsaarsak>? {
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


    fun avdoedUtenlandsopphold(barnepensjon: Barnepensjon): Behandlingsopplysning<Utenlandsopphold>? {
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

    fun avdoedNaeringsinntekt(barnepensjon: Barnepensjon): Behandlingsopplysning<Naeringsinntekt>? {
        return hentAvdoedForelder(barnepensjon)?.let { avdoed ->
            setBehandlingsopplysninger(
                barnepensjon, "avdoed_naeringsinntekt:v1",
                Naeringsinntekt(
                    avdoed.naeringsInntekt?.opplysning?.naeringsinntektVedDoedsfall?.svar,
                    avdoed.naeringsInntekt?.opplysning?.naeringsinntektPrAarFoerDoedsfall?.svar,
                    avdoed.foedselsnummer.value
                )
            )
        }
    }

    fun avdoedMilitaertjeneste(barnepensjon: Barnepensjon): Behandlingsopplysning<Militaertjeneste>? {
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

    fun soesken(barnepensjon: Barnepensjon) =
        "soesken" to objectMapper.createObjectNode()
            .set<ObjectNode>("soesken", objectMapper.valueToTree<ArrayNode>(barnepensjon.soesken))

    fun soeknad_mottatt_dato(barnepensjon: Barnepensjon) =
        "soeknad_mottatt_dato" to objectMapper.valueToTree<ObjectNode>(SoeknadMottattDato(barnepensjon.mottattDato))
}