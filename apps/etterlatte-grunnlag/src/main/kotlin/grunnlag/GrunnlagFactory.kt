package no.nav.etterlatte.grunnlag

class GrunnlagFactory(private val opplysninger: OpplysningDao
) {

    fun hent(id: Long): GrunnlagAggregat = GrunnlagAggregat(id, opplysninger)
    fun opprett(sakId: Long): GrunnlagAggregat = GrunnlagAggregat.opprett(sakId, opplysninger)
}