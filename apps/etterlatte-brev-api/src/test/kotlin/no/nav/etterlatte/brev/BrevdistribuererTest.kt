package no.nav.etterlatte.brev

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.distribusjon.Brevdistribuerer
import no.nav.etterlatte.brev.distribusjon.DistribusjonServiceImpl
import no.nav.etterlatte.brev.distribusjon.DistribusjonsTidspunktType
import no.nav.etterlatte.brev.distribusjon.DistribusjonsType
import no.nav.etterlatte.brev.distribusjon.FeilStatusForDistribusjon
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.common.person.MottakerFoedselsnummer
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.random.Random

class BrevdistribuererTest {
    private val db = mockk<BrevRepository>(relaxed = true)
    private val distribusjonService = mockk<DistribusjonServiceImpl>()

    private val brevdistribuerer = Brevdistribuerer(db, distribusjonService)

    @Test
    fun `Distribusjon fungerer som forventet`() {
        val brev = opprettBrev(Status.JOURNALFOERT, BrevProsessType.REDIGERBAR)
        val journalpostId = "1"

        every { db.hentBrev(any()) } returns brev
        every { db.hentJournalpostId(any()) } returns journalpostId
        every { distribusjonService.distribuerJournalpost(any(), any(), any(), any(), any(), any()) } returns "123"

        val bestillingsID = brevdistribuerer.distribuer(brev.id)
        bestillingsID shouldBe "123"

        verify {
            db.hentBrev(brev.id)
            db.hentJournalpostId(brev.id)
            distribusjonService.distribuerJournalpost(
                brev.id,
                journalpostId,
                DistribusjonsType.ANNET,
                DistribusjonsTidspunktType.KJERNETID,
                brev.mottaker.adresse,
                false,
            )
        }
    }

    @Test
    fun `Distribusjon avbrytes hvis journalpostId mangler`() {
        val brev = opprettBrev(Status.JOURNALFOERT, BrevProsessType.REDIGERBAR)

        every { db.hentBrev(any()) } returns brev
        every { db.hentJournalpostId(any()) } returns null

        assertThrows<IllegalArgumentException> {
            brevdistribuerer.distribuer(brev.id)
        }

        verify {
            db.hentBrev(brev.id)
            db.hentJournalpostId(brev.id)
        }
    }

    @ParameterizedTest
    @EnumSource(
        Status::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["JOURNALFOERT"],
    )
    fun `Distribusjon avbrytes ved feil status`(status: Status) {
        val brev = opprettBrev(status, BrevProsessType.REDIGERBAR)

        every { db.hentBrev(any()) } returns brev

        assertThrows<FeilStatusForDistribusjon> {
            brevdistribuerer.distribuer(brev.id)
        }

        verify {
            db.hentBrev(brev.id)
        }
    }

    private fun opprettBrev(
        status: Status,
        prosessType: BrevProsessType,
    ) = Brev(
        id = Random.nextLong(10000),
        sakId = randomSakId(),
        behandlingId = null,
        tittel = null,
        spraak = Spraak.NB,
        prosessType = prosessType,
        soekerFnr = "fnr",
        status = status,
        statusEndret = Tidspunkt.now(),
        opprettet = Tidspunkt.now(),
        mottaker = opprettMottaker(),
        brevtype = Brevtype.MANUELT,
        brevkoder = Brevkoder.TOMT_INFORMASJONSBREV,
    )

    private fun opprettMottaker() =
        Mottaker(
            "Stor Snerk",
            foedselsnummer = MottakerFoedselsnummer(SOEKER_FOEDSELSNUMMER.value),
            orgnummer = null,
            adresse =
                Adresse(
                    adresseType = "NORSKPOSTADRESSE",
                    adresselinje1 = "Testgaten 13",
                    postnummer = "1234",
                    poststed = "OSLO",
                    land = "Norge",
                    landkode = "NOR",
                ),
        )
}
