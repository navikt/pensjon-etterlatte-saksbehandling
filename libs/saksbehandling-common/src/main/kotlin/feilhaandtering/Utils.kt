package no.nav.etterlatte.libs.common.feilhaandtering

fun <T : Any> checkNotNullOrThrowException(
    value: T?,
    throwable: () -> Throwable,
): T {
    if (value == null) {
        throw throwable()
    } else {
        return value
    }
}

fun checkInternFeil(
    value: Boolean,
    message: () -> String,
) {
    if (!value) {
        throw InternfeilException(message())
    }
}

fun checkUgyldigForespoerselException(
    value: Boolean,
    code: String,
    message: () -> String,
) {
    if (!value) {
        throw UgyldigForespoerselException(code = code, detail = message())
    }
}
