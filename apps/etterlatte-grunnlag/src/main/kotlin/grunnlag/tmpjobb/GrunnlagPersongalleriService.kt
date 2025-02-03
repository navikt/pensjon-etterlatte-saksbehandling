package no.nav.etterlatte.grunnlag.tmpjobb

import no.nav.etterlatte.klienter.GrunnlagBackupKlient
import org.slf4j.LoggerFactory

class GrunnlagPersongalleriService(
    private val grunnlagJobbDao: GrunnlagJobbDao,
    private val grunnlagBackupKlient: GrunnlagBackupKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun kjoer() {
        var harFlere = true
        logger.info("Kjører jobb for å hente tomme persongallerier")
        while (harFlere) {
            val opplysningsider = grunnlagJobbDao.hentTommePersongalleriV1()
            logger.info("Fant antall tomme PERSONGALLERIER: ${opplysningsider.size}")
            val opplysningsid = opplysningsider.first()
            logger.info("Henter data for opplysningsid $opplysningsid")
            val hentetDataForOpplysningId = grunnlagBackupKlient.hentPersonGallerierFraBackup(opplysningsid)
            grunnlagJobbDao.oppdaterTomtPersongalleri(hentetDataForOpplysningId)
            if (opplysningsider.isEmpty()) {
                logger.info("Fant ingen flere PERSONGALLERIER")
                harFlere = false
            }
            logger.info("Kjorer igjen")
        }

        logger.info("Avsluttet jobb, fant ingen flere tomme persongallerier")
    }
}
