package no.nav.etterlatte.vilkaarsvurdering

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarTypeOgUtfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarVurderingData
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VurdertVilkaar
import no.nav.etterlatte.vilkaarsvurdering.behandling.BehandlingKlient
import no.nav.etterlatte.vilkaarsvurdering.grunnlag.GrunnlagKlient
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import java.time.LocalDateTime
import java.util.*

fun Route.vilkaarsvurdering(
    vilkaarsvurderingService: VilkaarsvurderingService,
    behandlingKlient: BehandlingKlient,
    grunnlagKlient: GrunnlagKlient,
    vilkaarsvurderingRepository: VilkaarsvurderingRepositoryImpl
) {
    route("/api/vilkaarsvurdering") {
        val logger = application.log

        get("/{behandlingId}") {
            withBehandlingId { behandlingId ->
                logger.info("Henter vilkårsvurdering for $behandlingId")
                when (val vilkaarsvurdering = vilkaarsvurderingService.hentVilkaarsvurdering(behandlingId)) {
                    null -> {
                        val accessToken = getAccessToken(call)
                        val behandling = behandlingKlient.hentBehandling(behandlingId, accessToken)

                        if (behandling.kanStarteVilkaarsvurdering()) {
                            val nyVilkaarsvurdering = vilkaarsvurderingService.opprettVilkaarsvurdering(
                                behandlingId = behandlingId,
                                sakType = SakType.BARNEPENSJON, // todo: Støtte for omstillingsstønad
                                behandlingType = behandling.behandlingType!!,
                                virkningstidspunkt = behandling.virkningstidspunkt!!,
                                grunnlag = grunnlagKlient.hentGrunnlag(behandling.sak, accessToken),
                                revurderingAarsak = behandling.revurderingsaarsak
                            )
                            call.respond(nyVilkaarsvurdering)
                        } else {
                            logger.info(
                                "Kan ikke opprette vilkårsvurdering for $behandlingId før virkningstidspunkt er satt"
                            )
                            call.respond(HttpStatusCode.PreconditionFailed)
                        }
                    }
                    else -> call.respond(vilkaarsvurdering.toDto())
                }
            }

            val behandlingsIder = " 91d84faa-860a-489f-8a20-eea7a643a21b\n" +
                " 551b44f9-c32a-4557-9eab-09aa82cb2492\n" +
                " 03947f04-f907-4941-bcf2-595d2f519de5\n" +
                " a35f91de-9c3d-4f74-bc81-210a74005c35\n" +
                " 185d91ea-e025-490d-b48a-d1c6c85063e0\n" +
                " 6e4403fe-4983-48af-b3ec-b974f8fe603d\n" +
                " bbf6083b-363a-4917-9a2a-eda5250445cb\n" +
                " 57590b91-a145-4aa2-a448-b7c8884d5146\n" +
                " 6b84e024-7f34-416e-a201-b84b4b3920fd\n" +
                " 8f8a7545-a0ec-46cd-82f3-1942d2f2389c\n" +
                " 114f890d-6d61-4224-961b-32ba6ecb33ac\n" +
                " 47cd1322-e760-46f6-b4a0-62c2de2a8875\n" +
                " 77d7fc71-8f86-4bb7-8410-2fad65ce519a\n" +
                " 231c6d69-0fb5-4b30-88da-37ef553c33f5\n" +
                " 805f05ae-43ca-47e5-8d29-6dff41c13f6c\n" +
                " faa5d1ce-a988-413b-8d28-819c2afb2ce5\n" +
                " 3b50ef07-9a08-498e-97fb-863e2f4fc46c\n" +
                " 68ddb3fe-2e06-480a-947b-1c1d259264bd\n" +
                " 5b069fcb-de83-4e1c-b866-9b3253a6ce92\n" +
                " 7b555144-0ea8-481a-8075-a0faa7a87876\n" +
                " 9118f183-99bf-4b09-8743-1869860813f5\n" +
                " 0e10d9bd-2fb4-41a2-a367-7063a332810c\n" +
                " a475fdfd-e834-4397-8eaa-3e06210624ed\n" +
                " e8c603cd-942b-4dea-be1f-e62b1a10959e\n" +
                " 24dc5847-1343-4e90-87e5-2c0395642e48\n" +
                " bea701b1-649c-42d7-afc7-a3a584783e8b\n" +
                " 5c5b7c40-f217-4272-b8f1-7b5ebd750956\n" +
                " c71305e7-4434-4f3c-9e27-de9692e20584\n" +
                " 739c1c1d-044c-45b6-90b5-47d88893c718\n" +
                " 4910771d-c46a-4d9c-ae6b-39e92bc5adfc\n" +
                " c1570642-61ab-4911-8ef3-aaf0dba82df2\n" +
                " d6d7e8e4-bbeb-4616-a097-775ae2b35f15\n" +
                " 578b8e1f-e401-49fc-b292-077f7b7fc18a\n" +
                " a27fb986-c8d7-41e0-bed4-9c7a55cd66f4\n" +
                " dee58ddb-3ba4-4263-89e7-29e1447a5dc3\n" +
                " cb49623c-3dce-4055-b27e-910fd7fc10d0\n" +
                " 1cf02c60-bfc2-4f95-8778-9f2f1bd740ff\n" +
                " a63c1e39-f97a-4b05-9a0b-fcf167fdb9b4\n" +
                " 87adb120-f8b2-4afe-a569-9568ac54e99c\n" +
                " 855c35c9-aec5-4d2d-b28b-aec3c3b3bffa\n" +
                " 4c4a7ed8-ac3c-428b-9a15-49455bd44de4\n" +
                " 42f6096c-eb21-4cf7-8c5b-bbf07ca72d9a\n" +
                " 17bdf92f-a773-4411-9487-3e5263a05188\n" +
                " 3f85733e-ff67-43c2-b179-02f96794bda5\n" +
                " c4fe8598-b1d0-4148-b974-a8a3f0e37ea6\n" +
                " 2ced155e-0f2f-4291-b90f-e20aaf96062f"

            val accessToken = getAccessToken(call)
            behandlingsIder.split("\n").forEach {
                val behandling = behandlingKlient.hentBehandling(UUID.fromString(it), accessToken)
                val grunnlag = grunnlagKlient.hentGrunnlag(behandling.sak, accessToken)
                vilkaarsvurderingRepository.leggtilMetadata(UUID.fromString(it), grunnlag.metadata)
            }
        }

        post("/{behandlingId}") {
            withBehandlingId { behandlingId ->
                val vurdertVilkaarDto = call.receive<VurdertVilkaarDto>()

                logger.info("Oppdaterer vilkårsvurdering for $behandlingId")
                val oppdatertVilkaarsvurdering =
                    vilkaarsvurderingService.oppdaterVurderingPaaVilkaar(
                        behandlingId,
                        toVurdertVilkaar(vurdertVilkaarDto, saksbehandler)
                    )

                call.respond(oppdatertVilkaarsvurdering)
            }
        }

        delete("/{behandlingId}/{vilkaarType}") {
            withBehandlingId { behandlingId ->
                val vilkaarType = VilkaarType.valueOf(requireNotNull(call.parameters["vilkaarType"]))

                logger.info("Sletter vurdering på vilkår $vilkaarType for $behandlingId")
                val oppdatertVilkaarsvurdering =
                    vilkaarsvurderingService.slettVurderingPaaVilkaar(behandlingId, vilkaarType)

                call.respond(oppdatertVilkaarsvurdering)
            }
        }

        route("/resultat") {
            post("/{behandlingId}") {
                withBehandlingId { behandlingId ->
                    val vurdertResultatDto = call.receive<VurdertVilkaarsvurderingResultatDto>()

                    logger.info("Oppdaterer vilkårsvurderingsresultat for $behandlingId")

                    val accessToken = getAccessToken(call)
                    val behandling = behandlingKlient.hentBehandling(behandlingId, accessToken)
                    val oppdatertVilkaarsvurdering = vilkaarsvurderingService.oppdaterTotalVurdering(
                        behandlingId,
                        toVilkaarsvurderingResultat(vurdertResultatDto, saksbehandler)
                    )
                    vilkaarsvurderingService.publiserVilkaarsvurdering(
                        vilkaarsvurdering = oppdatertVilkaarsvurdering,
                        grunnlag = grunnlagKlient
                            .hentGrunnlagMedVersjon(
                                behandling.sak,
                                oppdatertVilkaarsvurdering.grunnlagsmetadata.versjon,
                                accessToken
                            ),
                        behandling = behandling
                    )

                    call.respond(oppdatertVilkaarsvurdering)
                }
            }

            delete("/{behandlingId}") {
                withBehandlingId { behandlingId ->
                    logger.info("Sletter vilkårsvurderingsresultat for $behandlingId")
                    val oppdatertVilkaarsvurdering = vilkaarsvurderingService.slettTotalVurdering(behandlingId)

                    call.respond(oppdatertVilkaarsvurdering)
                }
            }
        }
    }
}

private suspend inline fun PipelineContext<*, ApplicationCall>.withBehandlingId(onSuccess: (id: UUID) -> Unit) {
    val id = call.parameters["behandlingId"]
    if (id == null) {
        call.respond(HttpStatusCode.BadRequest, "Fant ikke behandlingId")
    }

    try {
        onSuccess(UUID.fromString(id))
    } catch (e: IllegalArgumentException) {
        call.respond(HttpStatusCode.BadRequest, "behandlingId må være en UUID")
    }
}

private inline val PipelineContext<*, ApplicationCall>.saksbehandler: String
    get() = requireNotNull(
        call.principal<TokenValidationContextPrincipal>()
            ?.context?.getJwtToken("azure")
            ?.jwtTokenClaims?.getStringClaim("NAVident")
    )

private fun toVurdertVilkaar(vurdertVilkaarDto: VurdertVilkaarDto, saksbehandler: String) =
    VurdertVilkaar(
        hovedvilkaar = vurdertVilkaarDto.hovedvilkaar,
        unntaksvilkaar = vurdertVilkaarDto.unntaksvilkaar,
        vurdering = VilkaarVurderingData(
            kommentar = vurdertVilkaarDto.kommentar,
            tidspunkt = LocalDateTime.now(),
            saksbehandler = saksbehandler
        )
    )

private fun DetaljertBehandling.kanStarteVilkaarsvurdering() =
    this.virkningstidspunkt != null && this.behandlingType != null

private fun toVilkaarsvurderingResultat(
    vurdertResultatDto: VurdertVilkaarsvurderingResultatDto,
    saksbehandler: String
) = VilkaarsvurderingResultat(
    vurdertResultatDto.resultat,
    vurdertResultatDto.kommentar,
    LocalDateTime.now(),
    saksbehandler
)

data class VurdertVilkaarDto(
    val hovedvilkaar: VilkaarTypeOgUtfall,
    val unntaksvilkaar: VilkaarTypeOgUtfall? = null,
    val kommentar: String?
)

data class VurdertVilkaarsvurderingResultatDto(
    val resultat: VilkaarsvurderingUtfall,
    val kommentar: String?
)