package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import org.slf4j.LoggerFactory

class GrunnlagAggregat(
    private val saksid: Long,
    private val opplysninger: OpplysningDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GrunnlagAggregat::class.java)

        fun opprett(
            sak: Long,
            opplysninger: OpplysningDao
        ): GrunnlagAggregat {
            logger.info("Oppretter grunnlag for $sak")
            return Grunnlag(sak, emptyList())
                .also {
                    logger.info("Opprettet grunnlag for sak ${it.saksId}")
                }
                .let { GrunnlagAggregat(it.saksId, opplysninger) }
        }
    }

    private var lagredeOpplysninger = opplysninger.finnOpplysningerIGrunnlag(saksid)

    fun leggTilGrunnlagListe(nyeOpplysninger: List<Grunnlagsopplysning<ObjectNode>>) {
        if (nyeOpplysninger.isEmpty()) return

        for (opplysning in nyeOpplysninger) {

            opplysninger.slettSpesifikkOpplysningISak(saksid,opplysning.opplysningType)
            lagredeOpplysninger = lagredeOpplysninger + opplysning
            opplysninger.leggOpplysningTilGrunnlag(saksid, opplysning)
        }
    }

    fun serialiserbarUtgave() = Grunnlag(saksId = saksid, grunnlag = lagredeOpplysninger)
}

