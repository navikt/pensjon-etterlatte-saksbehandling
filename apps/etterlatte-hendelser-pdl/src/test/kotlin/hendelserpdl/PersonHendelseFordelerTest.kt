package no.nav.etterlatte.hendelserpdl

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.hendelserpdl.pdl.PdlKlient
import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype.OPPRETTET
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.SivilstandHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.VergeMaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.NavPersonIdent
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
import no.nav.person.identhendelse.v1.common.Personnavn
import no.nav.person.identhendelse.v1.common.RelatertBiPerson
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Adressebeskyttelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import no.nav.person.pdl.leesah.doedsfall.Doedsfall
import no.nav.person.pdl.leesah.forelderbarnrelasjon.ForelderBarnRelasjon
import no.nav.person.pdl.leesah.sivilstand.Sivilstand
import no.nav.person.pdl.leesah.utflytting.UtflyttingFraNorge
import no.nav.person.pdl.leesah.verge.VergeEllerFullmektig
import no.nav.person.pdl.leesah.verge.VergemaalEllerFremtidsfullmakt
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class PersonHendelseFordelerTest {

    private val pdlKlient: PdlKlient = mockk()
    private val kafkaProduser: KafkaProdusent<String, JsonMessage> = mockk()
    private lateinit var personHendelseFordeler: PersonHendelseFordeler

    @BeforeEach
    fun setup() {
        coEvery { pdlKlient.hentPdlIdentifikator(any()) } returns PdlIdentifikator.FolkeregisterIdent(FNR)
        coEvery { kafkaProduser.publiser(any(), any()) } returns mockk(relaxed = true)

        personHendelseFordeler = PersonHendelseFordeler(kafkaProduser, pdlKlient)
    }

    @Test
    fun `skal ignorere hendelse vi ikke fordeler`() {
        val personHendelse: Personhendelse = Personhendelse().apply {
            hendelseId = "1"
            endringstype = Endringstype.OPPRETTET
            personidenter = listOf(FNR.value)
            opplysningstype = "NOE_ANNET_V1"
        }

        runBlocking { personHendelseFordeler.haandterHendelse(personHendelse) }

        coVerify(exactly = 0) { pdlKlient.hentPdlIdentifikator(FNR.value) }
        coVerify(exactly = 0) { kafkaProduser.publiser(any(), any()) }

        confirmVerified(pdlKlient, kafkaProduser)
    }

    @Test
    fun `skal ignorere hendelse som har ident av type NPID`() {
        val npid = NavPersonIdent("09706511617")
        coEvery {
            pdlKlient.hentPdlIdentifikator(any())
        } returns PdlIdentifikator.Npid(npid)

        val personHendelse: Personhendelse = Personhendelse().apply {
            hendelseId = "1"
            endringstype = Endringstype.OPPRETTET
            personidenter = listOf(npid.ident)
            opplysningstype = LeesahOpplysningstype.DOEDSFALL_V1.toString()
        }

        runBlocking { personHendelseFordeler.haandterHendelse(personHendelse) }

        coVerify(exactly = 1) { pdlKlient.hentPdlIdentifikator(npid.ident) }
        coVerify(exactly = 0) { kafkaProduser.publiser(any(), any()) }

        confirmVerified(pdlKlient, kafkaProduser)
    }

    @Test
    fun `skal ignorere hendelser om vergemaal som vi ikke har spesifisert som aktuelle`() {
        val personHendelse: Personhendelse = Personhendelse().apply {
            hendelseId = "1"
            endringstype = Endringstype.OPPRETTET
            personidenter = listOf(FNR.value)
            opplysningstype = LeesahOpplysningstype.VERGEMAAL_ELLER_FREMTIDSFULLMAKT_V1.toString()
            vergemaalEllerFremtidsfullmakt = VergemaalEllerFremtidsfullmakt().apply {
                type = "NoeSomIkkeErStoettet"
            }
        }

        runBlocking { personHendelseFordeler.haandterHendelse(personHendelse) }

        coVerify { pdlKlient.hentPdlIdentifikator(FNR.value) }
        coVerify(exactly = 0) { kafkaProduser.publiser(any(), any()) }

        confirmVerified(pdlKlient, kafkaProduser)
    }

    @Test
    fun `skal ignorere hendelser om adressebeskyttelse dersom det er UGRADERT eller ingen`() {
        val personHendelse: Personhendelse = Personhendelse().apply {
            hendelseId = "1"
            endringstype = Endringstype.OPPRETTET
            personidenter = listOf(FNR.value)
            opplysningstype = LeesahOpplysningstype.ADRESSEBESKYTTELSE_V1.toString()
            adressebeskyttelse = Adressebeskyttelse().apply {
                gradering = Gradering.UGRADERT
            }
        }

        runBlocking { personHendelseFordeler.haandterHendelse(personHendelse) }

        coVerify { pdlKlient.hentPdlIdentifikator(FNR.value) }
        coVerify(exactly = 0) { kafkaProduser.publiser(any(), any()) }

        confirmVerified(pdlKlient, kafkaProduser)
    }

    @Test
    fun `skal mappe om og publisere melding om doedsfall paa rapid`() {
        val personHendelse: Personhendelse = Personhendelse().apply {
            hendelseId = "1"
            endringstype = Endringstype.OPPRETTET
            personidenter = listOf(FNR.value)
            opplysningstype = LeesahOpplysningstype.DOEDSFALL_V1.toString()
            doedsfall = Doedsfall().apply {
                doedsdato = LocalDate.of(2020, 1, 1)
            }
        }

        val forventetMeldingPaaRapid = MeldingSendtPaaRapid(
            eventName = "PDL:PERSONHENDELSE",
            hendelse = LeesahOpplysningstype.DOEDSFALL_V1,
            hendelse_data = Doedshendelse(
                hendelseId = personHendelse.hendelseId,
                endringstype = OPPRETTET,
                fnr = personHendelse.personidenter.first(),
                doedsdato = personHendelse.doedsfall?.doedsdato
            )
        )

        runBlocking { personHendelseFordeler.haandterHendelse(personHendelse) }

        coVerify { pdlKlient.hentPdlIdentifikator(FNR.value) }
        coVerify {
            kafkaProduser.publiser(
                any(),
                match {
                    val hendelse: MeldingSendtPaaRapid<Doedshendelse> = objectMapper.readValue(it.toJson())
                    hendelse == forventetMeldingPaaRapid
                }
            )
        }

        confirmVerified(pdlKlient, kafkaProduser)
    }

    @Test
    fun `skal mappe om og publisere melding om sivilstand paa rapid`() {
        val personHendelse: Personhendelse = Personhendelse().apply {
            hendelseId = "1"
            endringstype = Endringstype.OPPRETTET
            personidenter = listOf(FNR.value)
            opplysningstype = LeesahOpplysningstype.SIVILSTAND_V1.toString()
            sivilstand = Sivilstand().apply {
                type = "GIFT"
                relatertVedSivilstand = FNR_2.value
                gyldigFraOgMed = LocalDate.of(2020, 1, 1)
                bekreftelsesdato = LocalDate.of(2020, 1, 1)
            }
        }

        val forventetMeldingPaaRapid = MeldingSendtPaaRapid(
            eventName = "PDL:PERSONHENDELSE",
            hendelse = LeesahOpplysningstype.SIVILSTAND_V1,
            hendelse_data = SivilstandHendelse(
                hendelseId = personHendelse.hendelseId,
                endringstype = OPPRETTET,
                fnr = personHendelse.personidenter.first(),
                type = personHendelse.sivilstand?.type,
                relatertVedSivilstand = personHendelse.sivilstand?.relatertVedSivilstand,
                gyldigFraOgMed = personHendelse.sivilstand?.gyldigFraOgMed,
                bekreftelsesdato = personHendelse.sivilstand?.bekreftelsesdato
            )
        )

        runBlocking { personHendelseFordeler.haandterHendelse(personHendelse) }

        coVerify { pdlKlient.hentPdlIdentifikator(FNR.value) }
        coVerify {
            kafkaProduser.publiser(
                any(),
                match {
                    val hendelse: MeldingSendtPaaRapid<SivilstandHendelse> = objectMapper.readValue(it.toJson())
                    hendelse == forventetMeldingPaaRapid
                }
            )
        }

        confirmVerified(pdlKlient, kafkaProduser)
    }

    @Test
    fun `skal mappe om og publisere melding om utflytting paa rapid`() {
        val personHendelse: Personhendelse = Personhendelse().apply {
            hendelseId = "1"
            endringstype = Endringstype.OPPRETTET
            personidenter = listOf(FNR.value)
            opplysningstype = LeesahOpplysningstype.UTFLYTTING_FRA_NORGE.toString()
            utflyttingFraNorge = UtflyttingFraNorge().apply {
                tilflyttingsland = "Tyskland"
                tilflyttingsstedIUtlandet = "Berlin"
                utflyttingsdato = LocalDate.of(2023, 1, 1)
            }
        }

        val forventetMeldingPaaRapid = MeldingSendtPaaRapid(
            eventName = "PDL:PERSONHENDELSE",
            hendelse = LeesahOpplysningstype.UTFLYTTING_FRA_NORGE,
            hendelse_data = UtflyttingsHendelse(
                hendelseId = personHendelse.hendelseId,
                endringstype = OPPRETTET,
                fnr = personHendelse.personidenter.first(),
                tilflyttingsLand = personHendelse.utflyttingFraNorge?.tilflyttingsland,
                tilflyttingsstedIUtlandet = personHendelse.utflyttingFraNorge?.tilflyttingsstedIUtlandet,
                utflyttingsdato = personHendelse.utflyttingFraNorge?.utflyttingsdato
            )
        )

        runBlocking { personHendelseFordeler.haandterHendelse(personHendelse) }

        coVerify { pdlKlient.hentPdlIdentifikator(FNR.value) }
        coVerify {
            kafkaProduser.publiser(
                any(),
                match {
                    val hendelse: MeldingSendtPaaRapid<UtflyttingsHendelse> = objectMapper.readValue(it.toJson())
                    hendelse == forventetMeldingPaaRapid
                }
            )
        }

        confirmVerified(pdlKlient, kafkaProduser)
    }

    @Test
    fun `skal mappe om og publisere melding om vergemaal paa rapid`() {
        val personHendelse: Personhendelse = Personhendelse().apply {
            hendelseId = "1"
            endringstype = Endringstype.OPPRETTET
            personidenter = listOf(FNR.value)
            opplysningstype = LeesahOpplysningstype.VERGEMAAL_ELLER_FREMTIDSFULLMAKT_V1.toString()
            vergemaalEllerFremtidsfullmakt = VergemaalEllerFremtidsfullmakt().apply {
                vergeEllerFullmektig = VergeEllerFullmektig().apply {
                    type = "mindreaarig"
                    motpartsPersonident = FNR_2.value
                }
            }
        }

        val forventetMeldingPaaRapid = MeldingSendtPaaRapid(
            eventName = "PDL:PERSONHENDELSE",
            hendelse = LeesahOpplysningstype.VERGEMAAL_ELLER_FREMTIDSFULLMAKT_V1,
            hendelse_data = VergeMaalEllerFremtidsfullmakt(
                hendelseId = personHendelse.hendelseId,
                endringstype = OPPRETTET,
                fnr = personHendelse.personidenter.first(),
                vergeIdent = personHendelse.vergemaalEllerFremtidsfullmakt?.vergeEllerFullmektig?.motpartsPersonident
            )
        )

        runBlocking { personHendelseFordeler.haandterHendelse(personHendelse) }

        coVerify { pdlKlient.hentPdlIdentifikator(FNR.value) }
        coVerify {
            kafkaProduser.publiser(
                any(),
                match {
                    val hendelse: MeldingSendtPaaRapid<VergeMaalEllerFremtidsfullmakt> =
                        objectMapper.readValue(it.toJson())
                    hendelse == forventetMeldingPaaRapid
                }
            )
        }

        confirmVerified(pdlKlient, kafkaProduser)
    }

    @Test
    fun `skal mappe om og publisere melding om adressebeskyttelse paa rapid`() {
        val personHendelse: Personhendelse = Personhendelse().apply {
            hendelseId = "1"
            endringstype = Endringstype.OPPRETTET
            personidenter = listOf(FNR.value)
            opplysningstype = LeesahOpplysningstype.ADRESSEBESKYTTELSE_V1.toString()
            adressebeskyttelse = Adressebeskyttelse().apply {
                gradering = Gradering.FORTROLIG
            }
        }

        val forventetMeldingPaaRapid = MeldingSendtPaaRapid(
            eventName = "PDL:PERSONHENDELSE",
            hendelse = LeesahOpplysningstype.ADRESSEBESKYTTELSE_V1,
            hendelse_data = no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse(
                hendelseId = personHendelse.hendelseId,
                endringstype = OPPRETTET,
                fnr = personHendelse.personidenter.first(),
                adressebeskyttelseGradering = AdressebeskyttelseGradering.FORTROLIG
            )
        )

        runBlocking { personHendelseFordeler.haandterHendelse(personHendelse) }

        coVerify { pdlKlient.hentPdlIdentifikator(FNR.value) }
        coVerify {
            kafkaProduser.publiser(
                any(),
                match {
                    val hendelse: MeldingSendtPaaRapid<no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse> =
                        objectMapper.readValue(it.toJson())
                    hendelse == forventetMeldingPaaRapid
                }
            )
        }

        confirmVerified(pdlKlient, kafkaProduser)
    }

    @Test
    fun `skal mappe om og publisere melding om foreldrebarnrelasjon paa rapid`() {
        val personHendelse: Personhendelse = Personhendelse().apply {
            hendelseId = "1"
            endringstype = Endringstype.OPPRETTET
            personidenter = listOf(FNR.value)
            opplysningstype = LeesahOpplysningstype.FORELDERBARNRELASJON_V1.toString()
            forelderBarnRelasjon = ForelderBarnRelasjon().apply {
                relatertPersonsIdent = FNR_2.value
                relatertPersonsRolle = "BARN"
                minRolleForPerson = "FAR"
                relatertPersonUtenFolkeregisteridentifikator = RelatertBiPerson().apply {
                    navn = Personnavn().apply {
                        fornavn = "Ola"
                        etternavn = "Nordmann"
                    }
                }
            }
        }

        val forventetMeldingPaaRapid = MeldingSendtPaaRapid(
            eventName = "PDL:PERSONHENDELSE",
            hendelse = LeesahOpplysningstype.FORELDERBARNRELASJON_V1,
            hendelse_data = ForelderBarnRelasjonHendelse(
                hendelseId = personHendelse.hendelseId,
                endringstype = OPPRETTET,
                fnr = personHendelse.personidenter.first(),
                relatertPersonsIdent = personHendelse.forelderBarnRelasjon?.relatertPersonsIdent,
                relatertPersonsRolle = personHendelse.forelderBarnRelasjon?.relatertPersonsRolle,
                minRolleForPerson = personHendelse.forelderBarnRelasjon?.minRolleForPerson,
                relatertPersonUtenFolkeregisteridentifikator =
                personHendelse.forelderBarnRelasjon?.relatertPersonUtenFolkeregisteridentifikator.toString()
            )
        )

        runBlocking { personHendelseFordeler.haandterHendelse(personHendelse) }

        coVerify { pdlKlient.hentPdlIdentifikator(FNR.value) }
        coVerify {
            kafkaProduser.publiser(
                any(),
                match {
                    val hendelse: MeldingSendtPaaRapid<ForelderBarnRelasjonHendelse> =
                        objectMapper.readValue(it.toJson())
                    hendelse == forventetMeldingPaaRapid
                }
            )
        }

        confirmVerified(pdlKlient, kafkaProduser)
    }
}