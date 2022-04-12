package no.nav.etterlatte.vilkaar

/*
fun Route.vilkaarRoute(service: VilkaarService) {

    route("vurdertvilkaar") {
        val logger = application.log

        get("/{behandlingId}") {
            val accessToken = getAccessToken(call)
            val behandlingId = call.parameters["behandlingId"]
            logger.info("Henter vurdert vilkaar for behandoingId $behandlingId")

            behandlingId?.let {
                service.hentVurdertVilkaar(behandlingId, accessToken).let {
                    call.respond(it)
                }
            } ?: call.respond(HttpStatusCode.BadRequest)
        }
    }
}

 */