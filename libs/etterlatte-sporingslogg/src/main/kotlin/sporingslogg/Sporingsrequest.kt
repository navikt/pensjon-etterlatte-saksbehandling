package no.nav.etterlatte.libs.sporingslogg

enum class HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH,
    HEAD,
    OPTIONS,
}

data class Sporingsrequest(
    val kallendeApplikasjon: String,
    val oppdateringstype: HttpMethod,
    val brukerId: String,
    val hvemBlirSlaattOpp: String,
    val endepunkt: String,
    val resultat: Decision?,
    val melding: String,
) {
    fun tilCEFEntry(): CEFEntry =
        CEFEntry(
            deviceProduct = kallendeApplikasjon,
            deviceEventClassId = fraHttpMethod(oppdateringstype),
            severity =
                when (oppdateringstype) {
                    HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS, HttpMethod.POST -> Severity.INFO
                    HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE -> Severity.WARN
                },
            name = Name.OnBehalfOfAccess,
            extension =
                Extension(
                    sourceUserId = brukerId,
                    destinationUserId = hvemBlirSlaattOpp,
                    request = endepunkt,
                    flexString1 = resultat,
                    message = melding,
                ),
        )
}

fun fraHttpMethod(oppdateringstype: HttpMethod): DeviceEventClassId =
    when (oppdateringstype) {
        HttpMethod.GET -> DeviceEventClassId.Access
        HttpMethod.POST -> DeviceEventClassId.Create
        HttpMethod.PUT, HttpMethod.PATCH -> DeviceEventClassId.Update
        HttpMethod.DELETE -> DeviceEventClassId.Delete
        HttpMethod.HEAD, HttpMethod.OPTIONS -> DeviceEventClassId.Meta
    }
