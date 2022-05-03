package no.nav.etterlatte.grunnlag

class GrunnlagFactory(private val behandlinger: GrunnlagDao,
                      private val opplysninger: OpplysningDao
) {

    fun hent(id: Long): GrunnlagAggregat = GrunnlagAggregat(id, behandlinger, opplysninger)
    fun opprett(sakId: Long): GrunnlagAggregat = GrunnlagAggregat.opprett(sakId, behandlinger, opplysninger)
}