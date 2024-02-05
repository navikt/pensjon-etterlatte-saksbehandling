package no.nav.etterlatte.vent

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.slf4j.LoggerFactory
import java.util.UUID

class VentService(private val dao: VentDao) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun settPaaVent(
        oppgaveId: UUID,
        behandlingId: UUID,
        ventetype: Ventetype,
        paaVentTil: Tidspunkt,
    ) = dao.settPaaVent(
        Vent(
            id = UUID.randomUUID(),
            oppgaveId = oppgaveId,
            behandlingId = behandlingId,
            ventetype = ventetype,
            paaVentTil = paaVentTil,
        ),
    )

    fun haandterVentaFerdig(): List<Vent> {
        logger.info("Starter håndtering av de som har venta ferdig")

        try {
            val hentVentaFerdig = dao.hentVentaFerdig(Tidspunkt.now())
            hentVentaFerdig
                .also { logger.info("${it.size} som var satt på vent har nå venta ferdig og blir gjenopptatt") }
                .forEach { gjenoppta(it) }
            return hentVentaFerdig
        } catch (e: Exception) {
            logger.error("Kunne ikke håndtere venta ferdig", e)
            throw e
        }
    }

    private fun gjenoppta(vent: Vent) {
        when (vent.ventetype) {
            Ventetype.VARSLING -> gjenopptaEtterVarsling(vent)
        }
        try {
            logger.info("Setter ${vent.id} som håndtert")
            dao.lagreHaandtert(vent.id)
        } catch (e: Exception) {
            logger.error(
                "Hendelse ${vent.id} er håndtert, men vi klarte ikke å lagre det i databasen." +
                    "Det er dermed fare for at denne blir behandla to ganger",
            )
        }
    }

    private fun gjenopptaEtterVarsling(vent: Vent) {
        logger.info("Gjenopptar ${vent.id} etter varsling")
    }
}
