package no.nav.etterlatte.grunnlag.tmpjobb

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.klienter.GrunnlagBackupKlient
import org.slf4j.LoggerFactory

class GrunnlagPersongalleriService(
    private val grunnlagJobbDao: GrunnlagJobbDao,
    private val grunnlagBackupKlient: GrunnlagBackupKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun kjoer() {
        var count = 0
        logger.info("Kjører jobb for å hente tomme persongallerier")
        while (true) {
            if (count == 100) {
                logger.info("avsluttet etter 100 for å sjekke performance")
                break
            }
            val opplysningsid = grunnlagJobbDao.hentTommePersongalleriV1()
            if (opplysningsid.isEmpty()) {
                logger.info("Fant ingen flere persongallerier")
                break
            }
            runBlocking {
                val asyncs =
                    opplysningsid.map { it ->
                        async {
                            logger.info("Henter data for opplysningsid $opplysningsid")
                            val hentetDataForOpplysningId = grunnlagBackupKlient.hentPersonGallerierFraBackup(it)
                            grunnlagJobbDao.oppdaterTomtPersongalleri(hentetDataForOpplysningId)
                        }
                    }
                asyncs.awaitAll()
                logger.info("Oppdaterte ${opplysningsid.size} opplysninsgider")
            }
            count += 1
        }

        logger.info("Avsluttet jobb, fant ingen flere tomme persongallerier, kjørte $count antall ganger")
    }
}
