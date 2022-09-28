package no.nav.etterlatte.vilkaarsvurdering

class VilkaarsvurderingService(val vilkaarsvurderingRepository: VilkaarsvurderingRepository) {

    fun hentVilkaarsvurdering(behandlingId: String): Vilkaarsvurdering {
        return vilkaarsvurderingRepository.hent(behandlingId) ?: opprettVilkaarsvurdering(behandlingId)
    }

    private fun opprettVilkaarsvurdering(behandlingId: String): Vilkaarsvurdering {
        val nyVilkaarsvurdering = Vilkaarsvurdering(behandlingId, vilkaarBarnepensjon())
        vilkaarsvurderingRepository.lagre(nyVilkaarsvurdering)
        return nyVilkaarsvurdering
    }

    fun oppdaterVurderingPaaVilkaar(behandlingId: String, vurdertVilkaar: VurdertVilkaar): Vilkaarsvurdering {
        return vilkaarsvurderingRepository.oppdaterVurderingPaaVilkaar(behandlingId, vurdertVilkaar)
    }

    fun slettVurderingPaaVilkaar(behandlingId: String, vilkaarType: VilkaarType): Vilkaarsvurdering {
        return vilkaarsvurderingRepository.slettVurderingPaaVilkaar(behandlingId, vilkaarType)
    }
}

fun vilkaarBarnepensjon() = listOf(
    Vilkaar(
        type = VilkaarType.FORMAAL,
        paragraf = Paragraf(
            paragraf = "§ 18-1",
            tittel = "Formål",
            lenke = "https://lovdata.no/lov/1997-02-28-19/%C2%A718-1",
            lovtekst = "Formålet med barnepensjon er å sikre inntekt for barn når en av foreldrene eller begge er døde."
        )
    ),
    Vilkaar(
        type = VilkaarType.FORUTGAAENDE_MEDLEMSKAP,
        paragraf = Paragraf(
            paragraf = "§ 18-2",
            tittel = "Avdødes forutgående medlemskap",
            lenke = "https://lovdata.no/lov/1997-02-28-19/%C2%A718-2",
            lovtekst = "Det er et vilkår for rett til barnepensjon at a) den avdøde faren eller moren var medlem i " +
                "trygden de siste fem årene fram til dødsfallet, eller b) at den avdøde faren eller moren mottok " +
                "pensjon eller uføretrygd fra folketrygden de siste fem årene fram til dødsfallet."
        )
    ),
    Vilkaar(
        type = VilkaarType.FORTSATT_MEDLEMSKAP,
        paragraf = Paragraf(
            paragraf = "§ 18-3",
            tittel = "Fortsatt medlemskap",
            lenke = "https://lovdata.no/lov/1997-02-28-19/%C2%A718-3",
            lovtekst = "Det er et vilkår for at et barn skal ha rett til pensjon, at det fortsatt er medlem i trygden."
        )
    ),
    Vilkaar(
        type = VilkaarType.ALDER_BARN,
        paragraf = Paragraf(
            paragraf = "§ 18-4 ledd 1",
            tittel = "Stønadssituasjonen – barnets alder",
            lenke = "https://lovdata.no/lov/1997-02-28-19/%C2%A718-4",
            lovtekst = "Pensjon ytes inntil barnet fyller 18 år."
        )
    ),
    Vilkaar(
        type = VilkaarType.DOEDSFALL_FORELDER,
        paragraf = Paragraf(
            paragraf = "§ 18-4 ledd 2",
            tittel = "Dødsfall forelder",
            lenke = "https://lovdata.no/lov/1997-02-28-19/%C2%A718-4",
            lovtekst = "En eller begge foreldrene døde."
        )
    )
)