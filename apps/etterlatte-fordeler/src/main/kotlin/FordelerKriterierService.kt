package no.nav.etterlatte

import no.nav.etterlatte.FordelerKriterie.AVDOED_ER_IKKE_REGISTRERT_SOM_DOED
import no.nav.etterlatte.FordelerKriterie.AVDOED_HAR_HATT_UTLANDSOPPHOLD
import no.nav.etterlatte.FordelerKriterie.AVDOED_HAR_UTVANDRING
import no.nav.etterlatte.FordelerKriterie.AVDOED_HAR_YRKESSKADE
import no.nav.etterlatte.FordelerKriterie.AVDOED_VAR_IKKE_BOSATT_I_NORGE
import no.nav.etterlatte.FordelerKriterie.BARN_ER_FOR_GAMMELT
import no.nav.etterlatte.FordelerKriterie.BARN_ER_IKKE_BOSATT_I_NORGE
import no.nav.etterlatte.FordelerKriterie.BARN_ER_IKKE_FOEDT_I_NORGE
import no.nav.etterlatte.FordelerKriterie.BARN_ER_IKKE_NORSK_STATSBORGER
import no.nav.etterlatte.FordelerKriterie.BARN_HAR_ADRESSEBESKYTTELSE
import no.nav.etterlatte.FordelerKriterie.BARN_HAR_UTVANDRING
import no.nav.etterlatte.FordelerKriterie.BARN_HAR_VERGE
import no.nav.etterlatte.FordelerKriterie.GJENLEVENDE_ER_IKKE_BOSATT_I_NORGE
import no.nav.etterlatte.FordelerKriterie.GJENLEVENDE_HAR_IKKE_FORELDREANSVAR
import no.nav.etterlatte.FordelerKriterie.INNSENDER_ER_IKKE_FORELDER
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.alder
import no.nav.helse.rapids_rivers.JsonMessage

typealias Sjekk = (JsonMessage) -> Boolean

enum class FordelerKriterie(val forklaring: String) {
    BARN_ER_IKKE_NORSK_STATSBORGER("Barn er ikke norsk statsborger"),
    BARN_ER_FOR_GAMMELT("Barn er for gammelt"),
    BARN_HAR_ADRESSEBESKYTTELSE("Barn har adressebeskyttelse"),
    BARN_ER_IKKE_FOEDT_I_NORGE("Barn ikke fodt i Norge"),
    BARN_HAR_UTVANDRING("Barn har utvandring"),
    BARN_HAR_VERGE("Barn er market med verge i søknaden"),
    BARN_ER_IKKE_BOSATT_I_NORGE("Barn er ikke bosatt i Norge"),
    AVDOED_HAR_UTVANDRING("Avdoed har utvandring"),
    AVDOED_HAR_YRKESSKADE("Avdød er market med yrkesskade i søknaden"),
    AVDOED_ER_IKKE_REGISTRERT_SOM_DOED("Avdød er ikke død"),
    AVDOED_HAR_HATT_UTLANDSOPPHOLD("Det er huket av for utenlandsopphold for avdøde i søknaden"),
    AVDOED_VAR_IKKE_BOSATT_I_NORGE("Avdød er ikke bosatt i Norge"),
    GJENLEVENDE_ER_IKKE_BOSATT_I_NORGE("Gjenlevende er ikke bosatt i Norge"),
    GJENLEVENDE_HAR_IKKE_FORELDREANSVAR("Gjenlevende søker har ikke foreldreanvar"),
    INNSENDER_ER_IKKE_FORELDER("Søker er ikke markert som forelder i søknaden"),
    SOEKER_ER_IKKE_ALENEBARN("Søker er ikke alenebarn"),
}

class FordelerKriterierService {

    /**
     * Dødsfallet er registrert
     * Alder - barn under 15 år
     * Enebarn
     * Verge og foreldreansvar  -trenger vi eksplisitt sjekk på verge?
     * Ingen barn på vei
     * Yrkesskade
     * Avdød- ingen ut- og innvandringsdatoer
     * Avdød - Ikke oppgitt utenlandsopphold
     * Gjenlevende ektefelle/samboer - bosatt i Norge
     * Barnet - bosatt i Norge + ingen ut- og innvandringsdatoe
     * Avdød er biologisk forelder til søker
     */
    private fun fordelerKriterier(
        barn: Person,
        avdoed: Person,
        gjenlevende: Person,
        soeknad: JsonMessage,
    ) = listOf(
        Kriterie(BARN_ER_IKKE_NORSK_STATSBORGER) { sjekkStatsborgerskap(barn) },
        Kriterie(BARN_ER_FOR_GAMMELT) { forGammel(barn, 15) },
        Kriterie(BARN_ER_IKKE_FOEDT_I_NORGE) { foedtUtland(barn) },
        Kriterie(BARN_ER_IKKE_BOSATT_I_NORGE) { ikkeGyldigBostedsAdresseINorge(barn) },
        Kriterie(BARN_HAR_ADRESSEBESKYTTELSE) { harAdressebeskyttelse(barn) },
        Kriterie(BARN_HAR_UTVANDRING) { harUtvandring(barn) },
        Kriterie(BARN_HAR_VERGE) { harVerge(soeknad) },
        Kriterie(AVDOED_ER_IKKE_REGISTRERT_SOM_DOED) { personErIkkeDoed(avdoed) },
        Kriterie(AVDOED_VAR_IKKE_BOSATT_I_NORGE) { ikkeGyldigBostedsAdresseINorge(avdoed) },
        Kriterie(AVDOED_HAR_UTVANDRING) { harUtvandring(avdoed) },
        Kriterie(AVDOED_HAR_YRKESSKADE) { harYrkesskade(soeknad) },
        Kriterie(AVDOED_HAR_HATT_UTLANDSOPPHOLD) { harHuketAvForUtenlandsopphold(soeknad) },
        Kriterie(GJENLEVENDE_ER_IKKE_BOSATT_I_NORGE) { ikkeGyldigBostedsAdresseINorge(gjenlevende) },
        Kriterie(GJENLEVENDE_HAR_IKKE_FORELDREANSVAR) { gjenlevendeHarIkkeForeldreansvar(barn, gjenlevende) },
        Kriterie(INNSENDER_ER_IKKE_FORELDER) { soekerIkkeForelder(soeknad) },
        //Kriterie(SOEKER_ER_IKKE_ALENEBARN) {soekerErIkkeAlenebarn(avdoed,gjenlevende, barn)}
    )

    private fun harAdressebeskyttelse(person: Person): Boolean {
        return person.adressebeskyttelse
    }

    private fun forGammel(person: Person, alder: Int): Boolean {
        return person.alder() > alder
    }

    private fun sjekkStatsborgerskap(person: Person): Boolean {
        return person.statsborgerskap != "NOR"
    }

    private fun foedtUtland(person: Person): Boolean {
        return person.foedeland != "NOR"
    }

    private fun harUtvandring(person: Person): Boolean {
        return (person.utland?.innflyttingTilNorge?.isNotEmpty() == true
                || person.utland?.utflyttingFraNorge?.isNotEmpty() == true)
    }

    private fun personErIkkeDoed(person: Person): Boolean {
        return person.doedsdato.isNullOrEmpty()
    }

    private fun gjenlevendeHarIkkeForeldreansvar(barn: Person, gjenlevende: Person): Boolean {
        return barn.familieRelasjon?.ansvarligeForeldre
            ?.none { it.foedselsnummer == gjenlevende.foedselsnummer } == true
    }

    //TODO tenke litt mer på dette kriteriet
    private fun ikkeGyldigBostedsAdresseINorge(person: Person): Boolean {
        return person.adresse?.bostedsadresse?.vegadresse?.adressenavn == null
    }

    private fun soekerErIkkeAlenebarn(avdoed: Person, gjenlevende: Person, barn: Person): Boolean {
        val barnFnr = barn.foedselsnummer
        return false
    }

    private fun harYrkesskade(sok: JsonMessage): Boolean {
        return sok["@skjema_info"]["foreldre"]
            .filter { it["type"].asText() == "AVDOED" }
            .filter { it["doedsaarsakSkyldesYrkesskadeEllerYrkessykdom"]["svar"]["verdi"].asText() == "JA" }
            .isNotEmpty()
    }

    private fun soekerIkkeForelder(sok: JsonMessage): Boolean {
        return sok["@skjema_info"]["innsender"]["foedselsnummer"]["svar"].asText() !in sok["@skjema_info"]["foreldre"]
            .filter { it["type"].asText() == "GJENLEVENDE_FORELDER" }
            .map { it["foedselsnummer"]["svar"].asText() }
    }

    private fun harVerge(sok: JsonMessage): Boolean {
        return sok["@skjema_info"]["soeker"]["verge"]["svar"]["verdi"].asText() == "JA"
    }

    private fun harHuketAvForUtenlandsopphold(sok: JsonMessage): Boolean {
        return sok["@skjema_info"]["foreldre"]
            .filter { it["type"].asText() == "AVDOED" }
            .filter { it["utenlandsopphold"]["svar"]["verdi"].asText() == "JA" }
            .isNotEmpty()
    }

    data class FordelerResultat(
        val kandidat: Boolean,
        val forklaring: List<FordelerKriterie>
    )

    private class Kriterie(val fordelerKriterie: FordelerKriterie, private val sjekk: Sjekk) {
        fun blirOppfyltAv(message: JsonMessage): Boolean = sjekk(message)
    }

    fun sjekkMotKriterier(barn: Person, avdoed: Person, gjenlevende: Person, packet: JsonMessage): FordelerResultat {
        return fordelerKriterier(barn, avdoed, gjenlevende, packet)
            .filter { it.blirOppfyltAv(packet) }
            .map { it.fordelerKriterie }
            .let { FordelerResultat(it.isEmpty(), it) }
    }
}