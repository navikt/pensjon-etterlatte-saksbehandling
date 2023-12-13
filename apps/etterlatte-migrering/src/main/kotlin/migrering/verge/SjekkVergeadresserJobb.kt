package no.nav.etterlatte.migrering.verge

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.migrering.PesysRepository
import no.nav.etterlatte.migrering.Pesyssak
import no.nav.etterlatte.migrering.grunnlag.GrunnlagKlient
import no.nav.etterlatte.migrering.verifisering.PDLKlient
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import org.slf4j.LoggerFactory
import kotlin.concurrent.thread

internal class SjekkVergeadresserJobb(
    private val pdlKlient: PDLKlient,
    private val repository: PesysRepository,
    private val vergeRepository: VergeRepository,
    private val grunnlagKlient: GrunnlagKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun sjekkMuligeProblemsaker() {
        thread {
            Thread.sleep(60_000)
            val saker = muligeProblemsaker()
            saker.forEach {
                runBlocking {
                    sjekkVergemaal(it)
                }
            }
        }
    }

    private suspend fun sjekkVergemaal(sak: Pesyssak) {
        val vergesAdressePensjonFullmakt = grunnlagKlient.hentVergesAdresse(sak.soeker.value)
        val pdl =
            try {
                sjekkPDL(sak)
            } catch (e: Exception) {
                logger.error("Feil i adressesjekken mot PDL", e)
                "{}"
            }
        vergeRepository.lagreVergeadresse(
            pensjonFullmakt = vergesAdressePensjonFullmakt?.toJson() ?: "{}",
            sak = sak.toJson(),
            pesysId = PesysId(sak.id),
            pdl = pdl,
        )
    }

    private fun sjekkPDL(sak: Pesyssak): String {
        val resultatFraPDL = pdlKlient.hentPerson(PersonRolle.BARN, sak.soeker)
        val vergemaalFraPDL = resultatFraPDL.vergemaalEllerFremtidsfullmakt ?: listOf()

        if (vergemaalFraPDL.size != 1) {
            repository.lagreFeilkjoering(
                request = sak.toJson(),
                feilendeSteg = "EkstraVergesjekkAdresse",
                feil = "Personen har ikke akkurat éitt vergemål i PDL, men ${vergemaalFraPDL.size}",
                pesysId = PesysId(sak.id),
            )
            return "{}"
        }

        val vergeFraPDL = vergemaalFraPDL.first().verdi.vergeEllerFullmektig.motpartsPersonident
        if (vergeFraPDL == null) {
            repository.lagreFeilkjoering(
                request = sak.toJson(),
                feilendeSteg = "EkstraVergesjekkAdresse",
                feil = "Vergen fins ikke i PDL. Rart scenario",
                pesysId = PesysId(sak.id),
            )
            return "{}"
        }

        val svarFraPDL = pdlKlient.hentPerson(PersonRolle.TILKNYTTET_BARN, vergeFraPDL)
        return svarFraPDL.toJson()
    }

    private fun muligeProblemsaker() = repository.hentSakerMedVerge()
}
