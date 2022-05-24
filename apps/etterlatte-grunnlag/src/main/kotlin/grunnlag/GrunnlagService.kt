package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import org.slf4j.LoggerFactory

interface GrunnlagService {
    fun hentGrunnlag(saksid: Long): Grunnlag?
    fun opprettGrunnlag(sak: Long, nyeOpplysninger: List<Grunnlagsopplysning<ObjectNode>>): Grunnlag
    fun leggTilGrunnlagFraRegister(saksid: Long, opplysninger: List<Grunnlagsopplysning<ObjectNode>>)
}

class RealGrunnlagService(
    //TODO stemmer det at jeg ikke trenger dette nå?
    private val grunnlagFactory: GrunnlagFactory,
) : GrunnlagService {
    private val logger = LoggerFactory.getLogger(RealGrunnlagService::class.java)

    override fun hentGrunnlag(saksid: Long): Grunnlag {
        return inTransaction { grunnlagFactory.hent(saksid).serialiserbarUtgave() }
    }

    //TODO Lage nytt grunnlag og skrive om til å returnere grunnlag
    override fun opprettGrunnlag(sak: Long, nyeOpplysninger: List<Grunnlagsopplysning<ObjectNode>>): Grunnlag {
        logger.info("Oppretter et grunnlag")
        return inTransaction {
            grunnlagFactory.opprett(sak)
                .also { grunnlag ->
                    grunnlag.leggTilGrunnlagListe(nyeOpplysninger)
                }
        }.serialiserbarUtgave()
    }

    override fun leggTilGrunnlagFraRegister(
        saksid: Long,
        opplysninger: List<Grunnlagsopplysning<ObjectNode>>
    ) {
        inTransaction {
            grunnlagFactory.hent(saksid).leggTilGrunnlagListe(
                opplysninger
            )
        }
    }
}
