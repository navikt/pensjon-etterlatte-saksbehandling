package no.nav.etterlatte.fordeler

import no.nav.etterlatte.fordeler.FordelerKriterie.AVDOED_ER_IKKE_FORELDER_TIL_BARN
import no.nav.etterlatte.fordeler.FordelerKriterie.AVDOED_ER_IKKE_REGISTRERT_SOM_DOED
import no.nav.etterlatte.fordeler.FordelerKriterie.AVDOED_HAR_HATT_UTLANDSOPPHOLD
import no.nav.etterlatte.fordeler.FordelerKriterie.AVDOED_HAR_UTVANDRING
import no.nav.etterlatte.fordeler.FordelerKriterie.AVDOED_HAR_YRKESSKADE
import no.nav.etterlatte.fordeler.FordelerKriterie.AVDOED_VAR_IKKE_BOSATT_I_NORGE
import no.nav.etterlatte.fordeler.FordelerKriterie.BARN_ER_FOR_GAMMELT
import no.nav.etterlatte.fordeler.FordelerKriterie.BARN_ER_IKKE_ALENEBARN
import no.nav.etterlatte.fordeler.FordelerKriterie.BARN_ER_IKKE_BOSATT_I_NORGE
import no.nav.etterlatte.fordeler.FordelerKriterie.BARN_ER_IKKE_FOEDT_I_NORGE
import no.nav.etterlatte.fordeler.FordelerKriterie.BARN_ER_IKKE_NORSK_STATSBORGER
import no.nav.etterlatte.fordeler.FordelerKriterie.BARN_HAR_ADRESSEBESKYTTELSE
import no.nav.etterlatte.fordeler.FordelerKriterie.BARN_HAR_HUKET_AV_UTLANDSADRESSE
import no.nav.etterlatte.fordeler.FordelerKriterie.BARN_HAR_UTVANDRING
import no.nav.etterlatte.fordeler.FordelerKriterie.BARN_HAR_VERGE
import no.nav.etterlatte.fordeler.FordelerKriterie.GJENLEVENDE_ER_IKKE_BOSATT_I_NORGE
import no.nav.etterlatte.fordeler.FordelerKriterie.GJENLEVENDE_HAR_IKKE_FORELDREANSVAR
import no.nav.etterlatte.fordeler.FordelerKriterie.GJENLEVENDE_OG_BARN_HAR_IKKE_SAMME_ADRESSE
import no.nav.etterlatte.fordeler.FordelerKriterie.INNSENDER_ER_IKKE_FORELDER
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.person.Adressebeskyttelse
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.aktiv
import no.nav.etterlatte.libs.common.person.alder
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Avdoed
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType

private const val NORGE = "NOR"

private typealias Sjekk = (Barnepensjon) -> Boolean

enum class FordelerKriterie(val forklaring: String) {
    BARN_ER_IKKE_NORSK_STATSBORGER("Barn er ikke norsk statsborger"),
    BARN_ER_FOR_GAMMELT("Barn er for gammelt"),
    BARN_HAR_ADRESSEBESKYTTELSE("Barn har adressebeskyttelse"),
    BARN_ER_IKKE_FOEDT_I_NORGE("Barn ikke fodt i Norge"),
    BARN_HAR_UTVANDRING("Barn har utvandring"),
    BARN_HAR_HUKET_AV_UTLANDSADRESSE("Det er huket av for utenlandsopphold for avdøde i søknaden"),
    BARN_HAR_VERGE("Barn er market med verge i søknaden"),
    BARN_ER_IKKE_BOSATT_I_NORGE("Barn er ikke bosatt i Norge"),
    AVDOED_HAR_UTVANDRING("Avdoed har utvandring"),
    AVDOED_HAR_YRKESSKADE("Avdød er market med yrkesskade i søknaden"),
    AVDOED_ER_IKKE_REGISTRERT_SOM_DOED("Avdød er ikke død"),
    AVDOED_HAR_HATT_UTLANDSOPPHOLD("Det er huket av for utenlandsopphold for avdøde i søknaden"),
    AVDOED_VAR_IKKE_BOSATT_I_NORGE("Avdød er ikke bosatt i Norge"),
    AVDOED_ER_IKKE_FORELDER_TIL_BARN("Avdød er forelder til søker"),
    GJENLEVENDE_ER_IKKE_BOSATT_I_NORGE("Gjenlevende er ikke bosatt i Norge"),
    GJENLEVENDE_OG_BARN_HAR_IKKE_SAMME_ADRESSE("Gjenlevende har samme adresse"),
    GJENLEVENDE_HAR_IKKE_FORELDREANSVAR("Gjenlevende søker har ikke foreldreanvar"),
    INNSENDER_ER_IKKE_FORELDER("Søker er ikke markert som forelder i søknaden"),
    BARN_ER_IKKE_ALENEBARN("Søker er ikke alenebarn"),
}

class FordelerKriterierService {

    /**
     * Grunnlag for regler er også dokumenter på Confluence:
     * https://confluence.adeo.no/display/TE/Fordelingsapp
     */
    private fun fordelerKriterier(
        barn: Person,
        avdoed: Person,
        gjenlevende: Person,
        soeknad : Barnepensjon
    ) = listOf(
        // Barn / søker
        Kriterie(BARN_ER_FOR_GAMMELT) { forGammel(barn) },
        Kriterie(BARN_ER_IKKE_NORSK_STATSBORGER) { ikkeNorskStatsborger(barn) },
        Kriterie(BARN_ER_IKKE_FOEDT_I_NORGE) { foedtUtland(barn) },
        Kriterie(BARN_ER_IKKE_BOSATT_I_NORGE) { ikkeGyldigBostedsAdresseINorge(barn) },
        Kriterie(BARN_HAR_HUKET_AV_UTLANDSADRESSE) { harHuketAvForUtenlandsadresse(soeknad) },
        Kriterie(BARN_HAR_UTVANDRING) { harUtvandring(barn) },
        Kriterie(BARN_HAR_ADRESSEBESKYTTELSE) { harAdressebeskyttelse(barn) },
        Kriterie(BARN_HAR_VERGE) { harHuketAvForVerge(soeknad) },
        Kriterie(BARN_ER_IKKE_ALENEBARN) { barnErIkkeAlenebarn(avdoed, barn) },

        // Avdød
        Kriterie(AVDOED_ER_IKKE_REGISTRERT_SOM_DOED) { personErIkkeRegistrertDoed(avdoed) },
        Kriterie(AVDOED_ER_IKKE_FORELDER_TIL_BARN) { ikkeForelderTilBarn(avdoed, barn) },
        Kriterie(AVDOED_VAR_IKKE_BOSATT_I_NORGE) { ikkeGyldigBostedsAdresseINorge(avdoed) },
        Kriterie(AVDOED_HAR_UTVANDRING) { harUtvandring(avdoed) },
        Kriterie(AVDOED_HAR_HATT_UTLANDSOPPHOLD) { harHuketAvForUtenlandsopphold(soeknad) },
        Kriterie(AVDOED_HAR_YRKESSKADE) { harHuketAvForYrkesskade(soeknad) },

        // Gjenlevende
        Kriterie(GJENLEVENDE_ER_IKKE_BOSATT_I_NORGE) { ikkeGyldigBostedsAdresseINorge(gjenlevende) },
        Kriterie(GJENLEVENDE_OG_BARN_HAR_IKKE_SAMME_ADRESSE) { gjenlevendeOgBarnHarIkkeSammeAdresse(gjenlevende, barn) },
        Kriterie(GJENLEVENDE_HAR_IKKE_FORELDREANSVAR) { gjenlevendeHarIkkeForeldreansvar(barn, gjenlevende) },

        // Innsender
        Kriterie(INNSENDER_ER_IKKE_FORELDER) { innsenderIkkeForelder(soeknad) },
    )

    private fun ikkeForelderTilBarn(avdoed: Person, barn: Person): Boolean {
        return barn.familieRelasjon?.foreldre?.let { avdoed.foedselsnummer !in it } ?: true
    }

    private fun harAdressebeskyttelse(person: Person): Boolean {
        return person.adressebeskyttelse != Adressebeskyttelse.UGRADERT
    }

    private fun ikkeNorskStatsborger(person: Person): Boolean {
        return person.statsborgerskap != NORGE
    }

    private fun foedtUtland(person: Person): Boolean {
        return person.foedeland != NORGE
    }

    private fun harUtvandring(person: Person): Boolean {
        return (person.utland?.innflyttingTilNorge?.isNotEmpty() == true
                || person.utland?.utflyttingFraNorge?.isNotEmpty() == true)
    }

    private fun personErIkkeRegistrertDoed(person: Person): Boolean {
        return person.doedsdato == null
    }

    private fun gjenlevendeHarIkkeForeldreansvar(barn: Person, gjenlevende: Person): Boolean {
        return barn.familieRelasjon?.ansvarligeForeldre
            ?.none { it == gjenlevende.foedselsnummer } == true
    }

    // TODO sjekke at gyldig-til dato ikke er satt
    // TODO sjekke hva som skjer med adresse på en død person - gyldig vil kanskje ikke lenger være aktuell?
    // TODO sjekke på dødsfallstidpunkt for gjenlevende?
    private fun ikkeGyldigBostedsAdresseINorge(person: Person): Boolean {
        return person.bostedsadresse?.aktiv()?.type !in listOf(AdresseType.VEGADRESSE, AdresseType.MATRIKKELADRESSE)
    }

    private fun gjenlevendeOgBarnHarIkkeSammeAdresse(gjenlevende: Person, barn: Person): Boolean {
        val gjenlevendeAdresse = gjenlevende.bostedsadresse?.aktiv()
        val barnAdresse = barn.bostedsadresse?.aktiv()
        return !(gjenlevendeAdresse?.adresseLinje1 == barnAdresse?.adresseLinje1
                && gjenlevendeAdresse?.postnr == barnAdresse?.postnr)
    }

    private fun forGammel(person: Person, alder: Int = 14): Boolean {
        return person.alder()?.let { it > alder } ?: true
    }

    private fun barnErIkkeAlenebarn(avdoed: Person, barn: Person): Boolean {
        return avdoed.familieRelasjon?.barn?.minus(barn.foedselsnummer)?.isNotEmpty() ?: false
    }

    private fun harHuketAvForYrkesskade(barnepensjon: Barnepensjon): Boolean {
        return barnepensjon.foreldre
            .filter { it.type == PersonType.AVDOED }
            .any { it is Avdoed && it.doedsaarsakSkyldesYrkesskadeEllerYrkessykdom.svar.verdi == JaNeiVetIkke.JA }
    }

    private fun innsenderIkkeForelder(barnepensjon: Barnepensjon): Boolean {
        return barnepensjon.innsender.foedselsnummer !in barnepensjon.foreldre
            .filter { it.type == PersonType.GJENLEVENDE_FORELDER }
            .map { it.foedselsnummer }
    }

    private fun harHuketAvForVerge(barnepensjon: Barnepensjon): Boolean {
        return barnepensjon.soeker.verge?.svar?.verdi == JaNeiVetIkke.JA
    }

    private fun harHuketAvForUtenlandsopphold(barnepensjon: Barnepensjon): Boolean {
        return barnepensjon.foreldre
            .filter { it.type == PersonType.AVDOED }
            .any { it is Avdoed && it.utenlandsopphold.svar.verdi == JaNeiVetIkke.JA }
    }

    private fun harHuketAvForUtenlandsadresse(barnepensjon: Barnepensjon): Boolean {
        return barnepensjon.soeker.utenlandsAdresse?.svar?.verdi == JaNeiVetIkke.JA
    }

    data class FordelerResultat(
        val kandidat: Boolean,
        val forklaring: List<FordelerKriterie>
    )

    private class Kriterie(val fordelerKriterie: FordelerKriterie, private val sjekk: Sjekk) {
        fun blirOppfyltAv(soeknadBarnepensjon: Barnepensjon): Boolean = sjekk(soeknadBarnepensjon)
    }

    fun sjekkMotKriterier(barn: Person, avdoed: Person, gjenlevende: Person, soeknad: Barnepensjon): FordelerResultat {
        return fordelerKriterier(barn, avdoed, gjenlevende, soeknad)
            .filter { it.blirOppfyltAv(soeknad) }
            .map { it.fordelerKriterie }
            .let { FordelerResultat(it.isEmpty(), it) }
    }
}