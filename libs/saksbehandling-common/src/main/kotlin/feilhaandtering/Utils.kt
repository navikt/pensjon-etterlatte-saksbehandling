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

// Todo endre til if(value)
fun checkInternFeil(
    value: Boolean,
    message: () -> String,
) {
    if (!value) {
        throw InternfeilException(message())
    }
}

// Todo endre til if(value)
fun checkUgyldigForespoerselException(
    value: Boolean,
    code: String,
    message: () -> String,
) {
    if (!value) {
        throw UgyldigForespoerselException(code = code, detail = message())
    }
}

// TODO: må også bytte ut checknotnull og requirenotnull
