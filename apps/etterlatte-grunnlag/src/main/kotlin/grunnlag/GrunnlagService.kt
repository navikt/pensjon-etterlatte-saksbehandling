package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.runBlocking
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
    private val opplysninger: OpplysningDao,
    private val grunnlagFactory: GrunnlagFactory,
    //private val grunnlagHendelser: SendChannel<Pair<Long, GrunnlagHendelserType>>
) : GrunnlagService {
    private val logger = LoggerFactory.getLogger(RealGrunnlagService::class.java)

    override fun hentGrunnlag(saksid: Long): Grunnlag {
        return inTransaction { grunnlagFactory.hent(saksid).serialiserbarUtgave() }
    }

    //TODO Lage nytt grunnlag og skrive om til å returnere grunnlag
    override fun opprettGrunnlag(sak: Long, nyeOpplysninger: List<Grunnlagsopplysning<ObjectNode>>): Grunnlag {
        logger.info("Starter en behandling")
        return inTransaction {
            grunnlagFactory.opprett(sak)
                .also { behandling ->
                    behandling.leggTilGrunnlagListe(nyeOpplysninger)
                }

        }.also {
            runBlocking {
            //TODO returnere grunnlag istedenfor
            //grunnlagHendelser.send(it.lagretGrunnlag.saksId to GrunnlagHendelserType.OPPRETTET)
            }
        }.serialiserbarUtgave()
    }

    override fun leggTilGrunnlagFraRegister(
        saksid: Long,
        opplysninger: List<Grunnlagsopplysning<ObjectNode>>
    ) {
        inTransaction { grunnlagFactory.hent(saksid).leggTilGrunnlagListe(
            opplysninger
        )}.also {
            runBlocking {
                //TODO unødvendig å gjøre noe her?
                //grunnlagHendelser.send(saksid to GrunnlagHendelserType.GRUNNLAGENDRET)
            }
        }
    }
}
