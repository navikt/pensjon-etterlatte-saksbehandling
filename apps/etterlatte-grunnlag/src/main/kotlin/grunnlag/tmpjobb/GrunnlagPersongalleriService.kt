package no.nav.etterlatte.grunnlag.tmpjobb

import no.nav.etterlatte.klienter.GrunnlagBackupKlient
import org.slf4j.LoggerFactory

class GrunnlagPersongalleriService(
    private val grunnlagJobbDao: GrunnlagJobbDao,
    private val grunnlagBackupKlient: GrunnlagBackupKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun kjoer() {
        logger.info("Kjører jobb for å hente tomme persongallerier")
        while (true) {
            val opplysningsid = grunnlagJobbDao.hentTommePersongalleriV1()
            if (opplysningsid == null) {
                logger.info("Fant ingen flere PERSONGALLERIER")
                break
            }
            logger.info("Henter data for opplysningsid $opplysningsid")
            val hentetDataForOpplysningId = grunnlagBackupKlient.hentPersonGallerierFraBackup(opplysningsid)
            grunnlagJobbDao.oppdaterTomtPersongalleri(hentetDataForOpplysningId)
        }

        logger.info("Avsluttet jobb, fant ingen flere tomme persongallerier")
    }
}
