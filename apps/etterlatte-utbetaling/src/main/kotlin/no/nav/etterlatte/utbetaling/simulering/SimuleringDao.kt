package no.nav.etterlatte.utbetaling.simulering

import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.database.firstOrNull
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.SimulerBeregningRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.SimulerBeregningResponse
import java.util.UUID
import javax.sql.DataSource

class SimuleringDao(
    private val dataSource: DataSource,
) {
    fun lagre(
        behandlingId: UUID,
        saksbehandler: String,
        vedtak: VedtakDto,
        simuleringRequest: SimulerBeregningRequest,
        simuleringResponse: SimulerBeregningResponse,
    ) {
        dataSource.connection.use {
            val statement =
                it.prepareStatement(
                    """
                    INSERT INTO simulering (behandlingid, saksbehandlerid, vedtak, request, response)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT (behandlingid) 
                    DO UPDATE SET tidspunkt = now() 
                    , saksbehandlerid = excluded.saksbehandlerid 
                    , vedtak = excluded.vedtak
                    , request = excluded.request
                    , response = excluded.response
                    """,
                )
            statement.setObject(1, behandlingId)
            statement.setString(2, saksbehandler)
            statement.setJsonb(3, vedtak)
            statement.setJsonb(4, simuleringRequest)
            statement.setJsonb(5, simuleringResponse)

            statement.executeUpdate()
        }
    }

    fun hent(behandlingId: UUID): SimulerBeregningResponse? =
        dataSource.connection.use {
            it
                .prepareStatement("SELECT response::jsonb FROM simulering WHERE behandlingid = ?")
                .apply {
                    setObject(1, behandlingId)
                }.executeQuery()
                .firstOrNull {
                    objectMapper.readValue(getString("response"), SimulerBeregningResponse::class.java)
                }
        }
}
