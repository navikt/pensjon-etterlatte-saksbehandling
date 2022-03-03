package no.nav.etterlatte.fordeler

import no.nav.etterlatte.fordeler.FordelerKriterie.AVDOED_ER_IKKE_REGISTRERT_SOM_DOED
import no.nav.etterlatte.fordeler.FordelerKriterie.AVDOED_HAR_HATT_UTLANDSOPPHOLD
import no.nav.etterlatte.fordeler.FordelerKriterie.AVDOED_HAR_UTVANDRING
import no.nav.etterlatte.fordeler.FordelerKriterie.AVDOED_HAR_YRKESSKADE
import no.nav.etterlatte.fordeler.FordelerKriterie.AVDOED_VAR_IKKE_BOSATT_I_NORGE
import no.nav.etterlatte.fordeler.FordelerKriterie.BARN_ER_FOR_GAMMELT
import no.nav.etterlatte.fordeler.FordelerKriterie.BARN_ER_IKKE_BOSATT_I_NORGE
import no.nav.etterlatte.fordeler.FordelerKriterie.BARN_ER_IKKE_FOEDT_I_NORGE
import no.nav.etterlatte.fordeler.FordelerKriterie.BARN_ER_IKKE_NORSK_STATSBORGER
import no.nav.etterlatte.fordeler.FordelerKriterie.BARN_HAR_ADRESSEBESKYTTELSE
import no.nav.etterlatte.fordeler.FordelerKriterie.BARN_HAR_UTVANDRING
import no.nav.etterlatte.fordeler.FordelerKriterie.BARN_HAR_VERGE
import no.nav.etterlatte.fordeler.FordelerKriterie.GJENLEVENDE_ER_IKKE_BOSATT_I_NORGE
import no.nav.etterlatte.fordeler.FordelerKriterie.GJENLEVENDE_HAR_IKKE_FORELDREANSVAR
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

typealias Sjekk = (Barnepensjon) -> Boolean

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
        soeknad : Barnepensjon
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
        Kriterie(SOEKER_ER_IKKE_ALENEBARN) { soekerErIkkeAlenebarn(avdoed,gjenlevende, barn) }
    )

    private fun harAdressebeskyttelse(person: Person): Boolean {
        return person.adressebeskyttelse != Adressebeskyttelse.UGRADERT
    }

    // TODO denne må vi se på - kanskje ha en feil dersom alder ikke finnes?
    private fun forGammel(person: Person, alder: Int): Boolean {
        return person.alder()?.let { it > alder } ?: true
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
        return person.doedsdato == null
    }

    private fun gjenlevendeHarIkkeForeldreansvar(barn: Person, gjenlevende: Person): Boolean {
        return barn.familieRelasjon?.ansvarligeForeldre
            ?.none { it == gjenlevende.foedselsnummer } == true
    }

    //TODO tenke litt mer på dette kriteriet
    private fun ikkeGyldigBostedsAdresseINorge(person: Person): Boolean {
        return person.bostedsadresse?.aktiv()?.type != AdresseType.VEGADRESSE
    }

    private fun soekerErIkkeAlenebarn(avdoed: Person, gjenlevende: Person, barn: Person): Boolean {

        val barnFnr = barn.foedselsnummer
        return false
    }

    private fun harYrkesskade(barnepensjon: Barnepensjon): Boolean {
        return barnepensjon.foreldre
            .filter { it.type == PersonType.AVDOED }
            .any { it is Avdoed && it.doedsaarsakSkyldesYrkesskadeEllerYrkessykdom.svar.verdi == JaNeiVetIkke.JA }
    }

    private fun soekerIkkeForelder(barnepensjon: Barnepensjon): Boolean {
        return barnepensjon.innsender.foedselsnummer !in barnepensjon.foreldre
            .filter { it.type == PersonType.GJENLEVENDE_FORELDER }
            .map { it.foedselsnummer }
    }

    private fun harVerge(barnepensjon: Barnepensjon): Boolean {
        return barnepensjon.soeker.verge?.svar?.verdi == JaNeiVetIkke.JA
    }

    private fun harHuketAvForUtenlandsopphold(barnepensjon: Barnepensjon): Boolean {
        return barnepensjon.foreldre
            .filter { it.type == PersonType.AVDOED }
            .any { it is Avdoed && it.utenlandsopphold.svar.verdi == JaNeiVetIkke.JA }
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