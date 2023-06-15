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
import no.nav.etterlatte.libs.common.person.NavPersonIdent
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Adressebeskyttelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import no.nav.person.pdl.leesah.doedsfall.Doedsfall
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
}