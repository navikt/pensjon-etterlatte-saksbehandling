package no.nav.etterlatte.testdata.dolly

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.etterlatte.testdata.features.dolly.alderVoksenRandom
import no.nav.etterlatte.testdata.features.dolly.defaultDagerSidenDoedsfall

@JsonIgnoreProperties(ignoreUnknown = true)
data class Bruker(
    val brukerId: String?,
    val brukertype: String?,
    val navIdent: String?,
    val epost: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Gruppe(
    val id: Long,
    val navn: String,
    val hensikt: String,
)

data class HentGruppeResponse(
    val antallPages: Int,
    val pageNo: Int,
    val pageSize: Int,
    val antallElementer: Int,
    val contents: List<Gruppe>,
)

data class OpprettGruppeRequest(
    val navn: String,
    val hensikt: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DollyIBrukResponse(
    val ident: String,
    val ibruk: Boolean,
    val gruppeId: Long,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BestillingStatus(
    val id: Long,
    val ferdig: Boolean,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TestGruppeBestillinger(
    val identer: List<TestGruppeBestilling>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TestGruppeBestilling(
    val ident: String,
    val bestillingId: List<Long>,
    val ibruk: Boolean,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DollyPersonResponse(
    val ident: String,
    val person: DollyPerson,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DollyPerson(
    val doedsfall: List<Doedsfall>,
    val navn: List<Navn>,
    val forelderBarnRelasjon: List<ForelderBarnRelasjon>,
    val sivilstand: List<Sivilstand>,
)

data class ForelderBarnRelasjon(
    val relatertPersonsIdent: String,
    val relatertPersonsRolle: String,
)

data class Sivilstand(
    val type: String,
    val relatertVedSivilstand: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Doedsfall(
    val doedsdato: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Navn(
    val fornavn: String,
    val etternavn: String,
)

data class ForenkletFamilieModell(
    val ibruk: Boolean,
    val avdoed: String,
    val gjenlevende: String,
    val barn: List<String>,
)

data class BestillingRequest(
    val erOver18: Boolean = false,
    val helsoesken: Int,
    val halvsoeskenAvdoed: Int,
    val halvsoeskenGjenlevende: Int = 0,
    val gruppeId: Long,
    val antall: Int,
    val antallDagerSidenDoedsfall: Int = defaultDagerSidenDoedsfall(),
    val gjenlevendeAlder: Int = alderVoksenRandom(),
    val avdoedAlder: Int = alderVoksenRandom(),
)
