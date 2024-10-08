package no.nav.etterlatte.hendelserpdl

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.hendelserpdl.pdl.PdlTjenesterKlient
import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.pdl.FantIkkePersonException
import no.nav.etterlatte.libs.common.pdlhendelse.DoedshendelsePdl
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype.OPPRETTET
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.PdlHendelserKeys
import no.nav.etterlatte.libs.common.pdlhendelse.SivilstandHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.VergeMaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.NavPersonIdent
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
import no.nav.etterlatte.libs.testdata.grunnlag.GJENLEVENDE_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.INNSENDER_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.VERGE_FOEDSELSNUMMER
import no.nav.etterlatte.pdl.hendelse.LeesahOpplysningstype
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
    private val pdlTjenesterKlient: PdlTjenesterKlient = mockk()
    private val kafkaProduser: KafkaProdusent<String, JsonMessage> = mockk()
    private lateinit var personHendelseFordeler: PersonHendelseFordeler

    @BeforeEach
    fun setup() {
        coEvery { pdlTjenesterKlient.hentPdlIdentifikator(any()) } returns
            PdlIdentifikator.FolkeregisterIdent(
                SOEKER_FOEDSELSNUMMER,
            )
        coEvery { kafkaProduser.publiser(any(), any()) } returns mockk(relaxed = true)

        personHendelseFordeler = PersonHendelseFordeler(kafkaProduser, pdlTjenesterKlient)
    }

    @Test
    fun `skal ignorere hendelse for ident vi ikke finner i PDL`() {
        clearMocks(pdlTjenesterKlient)
        coEvery { pdlTjenesterKlient.hentPdlIdentifikator(SOEKER_FOEDSELSNUMMER.value) } throws
            FantIkkePersonException()
        val personHendelse: Personhendelse =
            Personhendelse().apply {
                hendelseId = "1"
                endringstype = Endringstype.OPPRETTET
                personidenter = listOf(SOEKER_FOEDSELSNUMMER.value)
                opplysningstype = LeesahOpplysningstype.DOEDSFALL_V1.toString()
                doedsfall =
                    Doedsfall().apply {
                        doedsdato = LocalDate.now()
                    }
            }

        runBlocking { personHendelseFordeler.haandterHendelse(personHendelse) }

        coVerify(exactly = 1) { pdlTjenesterKlient.hentPdlIdentifikator(SOEKER_FOEDSELSNUMMER.value) }
        coVerify(exactly = 0) { kafkaProduser.publiser(any(), any()) }

        confirmVerified(pdlTjenesterKlient, kafkaProduser)
    }

    @Test
    fun `skal ignorere hendelse vi ikke fordeler`() {
        val personHendelse: Personhendelse =
            Personhendelse().apply {
                hendelseId = "1"
                endringstype = Endringstype.OPPRETTET
                personidenter = listOf(SOEKER_FOEDSELSNUMMER.value)
                opplysningstype = "NOE_ANNET_V1"
            }

        runBlocking { personHendelseFordeler.haandterHendelse(personHendelse) }

        coVerify(exactly = 0) { pdlTjenesterKlient.hentPdlIdentifikator(SOEKER_FOEDSELSNUMMER.value) }
        coVerify(exactly = 0) { kafkaProduser.publiser(any(), any()) }

        confirmVerified(pdlTjenesterKlient, kafkaProduser)
    }

    @Test
    fun `skal ignorere hendelse som har ident av type NPID`() {
        val npid = NavPersonIdent("09706511617")
        coEvery {
            pdlTjenesterKlient.hentPdlIdentifikator(any())
        } returns PdlIdentifikator.Npid(npid)

        val personHendelse: Personhendelse =
            Personhendelse().apply {
                hendelseId = "1"
                endringstype = Endringstype.OPPRETTET
                personidenter = listOf(npid.ident)
                opplysningstype = LeesahOpplysningstype.DOEDSFALL_V1.toString()
            }

        runBlocking { personHendelseFordeler.haandterHendelse(personHendelse) }

        coVerify(exactly = 1) { pdlTjenesterKlient.hentPdlIdentifikator(npid.ident) }
        coVerify(exactly = 0) { kafkaProduser.publiser(any(), any()) }

        confirmVerified(pdlTjenesterKlient, kafkaProduser)
    }

    @Test
    fun `skal ignorere hendelser om vergemaal som vi ikke har spesifisert som aktuelle`() {
        val personHendelse: Personhendelse =
            Personhendelse().apply {
                hendelseId = "1"
                endringstype = Endringstype.OPPRETTET
                personidenter = listOf(SOEKER_FOEDSELSNUMMER.value)
                opplysningstype = LeesahOpplysningstype.VERGEMAAL_ELLER_FREMTIDSFULLMAKT_V1.toString()
                vergemaalEllerFremtidsfullmakt =
                    VergemaalEllerFremtidsfullmakt().apply {
                        type = "NoeSomIkkeErStoettet"
                    }
            }

        runBlocking { personHendelseFordeler.haandterHendelse(personHendelse) }

        coVerify { pdlTjenesterKlient.hentPdlIdentifikator(SOEKER_FOEDSELSNUMMER.value) }
        coVerify(exactly = 0) { kafkaProduser.publiser(any(), any()) }

        confirmVerified(pdlTjenesterKlient, kafkaProduser)
    }

    @Test
    fun `skal ignorere hendelser om adressebeskyttelse dersom det er UGRADERT eller ingen`() {
        val personHendelse: Personhendelse =
            Personhendelse().apply {
                hendelseId = "1"
                endringstype = Endringstype.OPPRETTET
                personidenter = listOf(SOEKER_FOEDSELSNUMMER.value)
                opplysningstype = LeesahOpplysningstype.ADRESSEBESKYTTELSE_V1.toString()
                adressebeskyttelse =
                    Adressebeskyttelse().apply {
                        gradering = Gradering.UGRADERT
                    }
            }

        runBlocking { personHendelseFordeler.haandterHendelse(personHendelse) }

        coVerify { pdlTjenesterKlient.hentPdlIdentifikator(SOEKER_FOEDSELSNUMMER.value) }
        coVerify(exactly = 0) { kafkaProduser.publiser(any(), any()) }

        confirmVerified(pdlTjenesterKlient, kafkaProduser)
    }

    @Test
    fun `skal mappe om og publisere melding om doedsfall paa rapid`() {
        val personHendelse: Personhendelse =
            Personhendelse().apply {
                hendelseId = "1"
                endringstype = Endringstype.OPPRETTET
                personidenter = listOf(SOEKER_FOEDSELSNUMMER.value)
                opplysningstype = LeesahOpplysningstype.DOEDSFALL_V1.toString()
                doedsfall =
                    Doedsfall().apply {
                        doedsdato = LocalDate.of(2020, 1, 1)
                    }
            }

        val forventetMeldingPaaRapid =
            MeldingSendtPaaRapid(
                eventName = PdlHendelserKeys.PERSONHENDELSE.lagEventnameForType(),
                hendelse = LeesahOpplysningstype.DOEDSFALL_V1,
                hendelse_data =
                    DoedshendelsePdl(
                        hendelseId = personHendelse.hendelseId,
                        endringstype = OPPRETTET,
                        fnr = personHendelse.personidenter.first(),
                        doedsdato = personHendelse.doedsfall?.doedsdato,
                    ),
            )

        runBlocking { personHendelseFordeler.haandterHendelse(personHendelse) }

        coVerify { pdlTjenesterKlient.hentPdlIdentifikator(SOEKER_FOEDSELSNUMMER.value) }
        coVerify {
            kafkaProduser.publiser(
                any(),
                match {
                    val hendelse: MeldingSendtPaaRapid<DoedshendelsePdl> = objectMapper.readValue(it.toJson())
                    hendelse == forventetMeldingPaaRapid
                },
            )
        }

        confirmVerified(pdlTjenesterKlient, kafkaProduser)
    }

    @Test
    fun `skal mappe om og publisere melding om sivilstand paa rapid`() {
        val personHendelse: Personhendelse =
            Personhendelse().apply {
                hendelseId = "1"
                endringstype = Endringstype.OPPRETTET
                personidenter = listOf(SOEKER_FOEDSELSNUMMER.value)
                opplysningstype = LeesahOpplysningstype.SIVILSTAND_V1.toString()
                sivilstand =
                    Sivilstand().apply {
                        type = "GIFT"
                        relatertVedSivilstand = GJENLEVENDE_FOEDSELSNUMMER.value
                        gyldigFraOgMed = LocalDate.of(2020, 1, 1)
                        bekreftelsesdato = LocalDate.of(2020, 1, 1)
                    }
            }

        val forventetMeldingPaaRapid =
            MeldingSendtPaaRapid(
                eventName = PdlHendelserKeys.PERSONHENDELSE.lagEventnameForType(),
                hendelse = LeesahOpplysningstype.SIVILSTAND_V1,
                hendelse_data =
                    SivilstandHendelse(
                        hendelseId = personHendelse.hendelseId,
                        endringstype = OPPRETTET,
                        fnr = personHendelse.personidenter.first(),
                        type = personHendelse.sivilstand?.type,
                        relatertVedSivilstand = personHendelse.sivilstand?.relatertVedSivilstand,
                        gyldigFraOgMed = personHendelse.sivilstand?.gyldigFraOgMed,
                        bekreftelsesdato = personHendelse.sivilstand?.bekreftelsesdato,
                    ),
            )

        runBlocking { personHendelseFordeler.haandterHendelse(personHendelse) }

        coVerify { pdlTjenesterKlient.hentPdlIdentifikator(SOEKER_FOEDSELSNUMMER.value) }
        coVerify {
            kafkaProduser.publiser(
                any(),
                match {
                    val hendelse: MeldingSendtPaaRapid<SivilstandHendelse> = objectMapper.readValue(it.toJson())
                    hendelse == forventetMeldingPaaRapid
                },
            )
        }

        confirmVerified(pdlTjenesterKlient, kafkaProduser)
    }

    @Test
    fun `skal mappe om og publisere melding om utflytting paa rapid`() {
        val personHendelse: Personhendelse =
            Personhendelse().apply {
                hendelseId = "1"
                endringstype = Endringstype.OPPRETTET
                personidenter = listOf(SOEKER_FOEDSELSNUMMER.value)
                opplysningstype = LeesahOpplysningstype.UTFLYTTING_FRA_NORGE.toString()
                utflyttingFraNorge =
                    UtflyttingFraNorge().apply {
                        tilflyttingsland = "Tyskland"
                        tilflyttingsstedIUtlandet = "Berlin"
                        utflyttingsdato = LocalDate.of(2023, 1, 1)
                    }
            }

        val forventetMeldingPaaRapid =
            MeldingSendtPaaRapid(
                eventName = PdlHendelserKeys.PERSONHENDELSE.lagEventnameForType(),
                hendelse = LeesahOpplysningstype.UTFLYTTING_FRA_NORGE,
                hendelse_data =
                    UtflyttingsHendelse(
                        hendelseId = personHendelse.hendelseId,
                        endringstype = OPPRETTET,
                        fnr = personHendelse.personidenter.first(),
                        tilflyttingsLand = personHendelse.utflyttingFraNorge?.tilflyttingsland,
                        tilflyttingsstedIUtlandet = personHendelse.utflyttingFraNorge?.tilflyttingsstedIUtlandet,
                        utflyttingsdato = personHendelse.utflyttingFraNorge?.utflyttingsdato,
                    ),
            )

        runBlocking { personHendelseFordeler.haandterHendelse(personHendelse) }

        coVerify { pdlTjenesterKlient.hentPdlIdentifikator(SOEKER_FOEDSELSNUMMER.value) }
        coVerify {
            kafkaProduser.publiser(
                any(),
                match {
                    val hendelse: MeldingSendtPaaRapid<UtflyttingsHendelse> = objectMapper.readValue(it.toJson())
                    hendelse == forventetMeldingPaaRapid
                },
            )
        }

        confirmVerified(pdlTjenesterKlient, kafkaProduser)
    }

    @Test
    fun `skal mappe om og publisere melding om vergemaal paa rapid`() {
        val personHendelse: Personhendelse =
            Personhendelse().apply {
                hendelseId = "1"
                endringstype = Endringstype.OPPRETTET
                personidenter = listOf(SOEKER_FOEDSELSNUMMER.value)
                opplysningstype = LeesahOpplysningstype.VERGEMAAL_ELLER_FREMTIDSFULLMAKT_V1.toString()
                vergemaalEllerFremtidsfullmakt =
                    VergemaalEllerFremtidsfullmakt().apply {
                        vergeEllerFullmektig =
                            VergeEllerFullmektig().apply {
                                type = "mindreaarig"
                                motpartsPersonident = VERGE_FOEDSELSNUMMER.value
                            }
                    }
            }

        val forventetMeldingPaaRapid =
            MeldingSendtPaaRapid(
                eventName = PdlHendelserKeys.PERSONHENDELSE.lagEventnameForType(),
                hendelse = LeesahOpplysningstype.VERGEMAAL_ELLER_FREMTIDSFULLMAKT_V1,
                hendelse_data =
                    VergeMaalEllerFremtidsfullmakt(
                        hendelseId = personHendelse.hendelseId,
                        endringstype = OPPRETTET,
                        fnr = personHendelse.personidenter.first(),
                        vergeIdent = personHendelse.vergemaalEllerFremtidsfullmakt?.vergeEllerFullmektig?.motpartsPersonident,
                    ),
            )

        runBlocking { personHendelseFordeler.haandterHendelse(personHendelse) }

        coVerify { pdlTjenesterKlient.hentPdlIdentifikator(SOEKER_FOEDSELSNUMMER.value) }
        coVerify {
            kafkaProduser.publiser(
                any(),
                match {
                    val hendelse: MeldingSendtPaaRapid<VergeMaalEllerFremtidsfullmakt> =
                        objectMapper.readValue(it.toJson())
                    hendelse == forventetMeldingPaaRapid
                },
            )
        }

        confirmVerified(pdlTjenesterKlient, kafkaProduser)
    }

    @Test
    fun `skal mappe om og publisere melding om adressebeskyttelse paa rapid`() {
        val personHendelse: Personhendelse =
            Personhendelse().apply {
                hendelseId = "1"
                endringstype = Endringstype.OPPRETTET
                personidenter = listOf(SOEKER_FOEDSELSNUMMER.value)
                opplysningstype = LeesahOpplysningstype.ADRESSEBESKYTTELSE_V1.toString()
                adressebeskyttelse =
                    Adressebeskyttelse().apply {
                        gradering = Gradering.FORTROLIG
                    }
            }

        val forventetMeldingPaaRapid =
            MeldingSendtPaaRapid(
                eventName = PdlHendelserKeys.PERSONHENDELSE.lagEventnameForType(),
                hendelse = LeesahOpplysningstype.ADRESSEBESKYTTELSE_V1,
                hendelse_data =
                    no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse(
                        hendelseId = personHendelse.hendelseId,
                        endringstype = OPPRETTET,
                        fnr = personHendelse.personidenter.first(),
                        adressebeskyttelseGradering = AdressebeskyttelseGradering.FORTROLIG,
                    ),
            )

        runBlocking { personHendelseFordeler.haandterHendelse(personHendelse) }

        coVerify { pdlTjenesterKlient.hentPdlIdentifikator(SOEKER_FOEDSELSNUMMER.value) }
        coVerify {
            kafkaProduser.publiser(
                any(),
                match {
                    val hendelse: MeldingSendtPaaRapid<no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse> =
                        objectMapper.readValue(it.toJson())
                    hendelse == forventetMeldingPaaRapid
                },
            )
        }

        confirmVerified(pdlTjenesterKlient, kafkaProduser)
    }

    @Test
    fun `skal mappe om og publisere melding om foreldrebarnrelasjon paa rapid`() {
        val personHendelse: Personhendelse =
            Personhendelse().apply {
                hendelseId = "1"
                endringstype = Endringstype.OPPRETTET
                personidenter = listOf(SOEKER_FOEDSELSNUMMER.value)
                opplysningstype = LeesahOpplysningstype.FORELDERBARNRELASJON_V1.toString()
                forelderBarnRelasjon =
                    ForelderBarnRelasjon().apply {
                        relatertPersonsIdent = INNSENDER_FOEDSELSNUMMER.value
                        relatertPersonsRolle = "BARN"
                        minRolleForPerson = "FAR"
                        relatertPersonUtenFolkeregisteridentifikator =
                            RelatertBiPerson().apply {
                                navn =
                                    Personnavn().apply {
                                        fornavn = "Ola"
                                        etternavn = "Nordmann"
                                    }
                            }
                    }
            }

        val forventetMeldingPaaRapid =
            MeldingSendtPaaRapid(
                eventName = PdlHendelserKeys.PERSONHENDELSE.lagEventnameForType(),
                hendelse = LeesahOpplysningstype.FORELDERBARNRELASJON_V1,
                hendelse_data =
                    ForelderBarnRelasjonHendelse(
                        hendelseId = personHendelse.hendelseId,
                        endringstype = OPPRETTET,
                        fnr = personHendelse.personidenter.first(),
                        relatertPersonsIdent = personHendelse.forelderBarnRelasjon?.relatertPersonsIdent,
                        relatertPersonsRolle = personHendelse.forelderBarnRelasjon?.relatertPersonsRolle,
                        minRolleForPerson = personHendelse.forelderBarnRelasjon?.minRolleForPerson,
                        relatertPersonUtenFolkeregisteridentifikator =
                            personHendelse.forelderBarnRelasjon?.relatertPersonUtenFolkeregisteridentifikator.toString(),
                    ),
            )

        runBlocking { personHendelseFordeler.haandterHendelse(personHendelse) }

        coVerify { pdlTjenesterKlient.hentPdlIdentifikator(SOEKER_FOEDSELSNUMMER.value) }
        coVerify {
            kafkaProduser.publiser(
                any(),
                match {
                    val hendelse: MeldingSendtPaaRapid<ForelderBarnRelasjonHendelse> =
                        objectMapper.readValue(it.toJson())
                    hendelse == forventetMeldingPaaRapid
                },
            )
        }

        confirmVerified(pdlTjenesterKlient, kafkaProduser)
    }
}
