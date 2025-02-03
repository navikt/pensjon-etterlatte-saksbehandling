package no.nav.etterlatte.grunnlag.tmpjobb

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
            val opplysningsid = grunnlagJobbDao.hentTommePersongalleriV1()
            if (opplysningsid.isEmpty()) {
                logger.info("Fant ingen flere persongallerier")
                break
            }

            logger.info("Henter data for opplysningsid $opplysningsid")
            val hentetDataForOpplysningIder = grunnlagBackupKlient.hentPersonGallerierFraBackup(opplysningsid).hendelser
            hentetDataForOpplysningIder.forEach {
                grunnlagJobbDao.oppdaterTomtPersongalleri(it)
            }
            logger.info("Oppdaterte antall ${opplysningsid.size} opplysninsider")
            count += 1
        }

        logger.info("Avsluttet jobb, fant ingen flere tomme persongallerier, kjørte $count antall ganger")
    }
}
