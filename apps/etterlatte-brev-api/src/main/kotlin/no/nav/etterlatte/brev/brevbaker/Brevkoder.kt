package no.nav.etterlatte.brev.brevbaker

import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.AVVIST_KLAGE_FERDIG
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.AVVIST_KLAGE_INNHOLD
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_AVSLAG
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_AVSLAG_UTFALL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE_UTFALL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_OPPHOER
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_OPPHOER_UTFALL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_REVURDERING
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_REVURDERING_UTFALL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_VARSEL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_VARSEL_UTFALL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_VEDTAK_OMREGNING
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.BARNEPENSJON_VEDTAK_OMREGNING_FERDIG
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_AVSLAG
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_AVSLAG_UTFALL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_INNVILGELSE
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_INNVILGELSE_UTFALL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_OPPHOER
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_OPPHOER_UTFALL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_REVURDERING
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_VARSEL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.OMSTILLINGSSTOENAD_VARSEL_UTFALL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TILBAKEKREVING_FERDIG
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TILBAKEKREVING_INNHOLD
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TOM_DELMAL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TOM_MAL
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode.UTSATT_KLAGEFRIST

enum class Brevkoder(val redigering: EtterlatteBrevKode, val ferdigstilling: EtterlatteBrevKode = redigering) {
    OMREGNING(
        BARNEPENSJON_VEDTAK_OMREGNING,
        BARNEPENSJON_VEDTAK_OMREGNING_FERDIG,
    ),
    TILBAKEKREVING(
        TILBAKEKREVING_INNHOLD,
        TILBAKEKREVING_FERDIG,
    ),
    TOMT_INFORMASJONSBREV(
        TOM_DELMAL,
        TOM_MAL_INFORMASJONSBREV,
    ),
    BP_AVSLAG(
        BARNEPENSJON_AVSLAG_UTFALL,
        BARNEPENSJON_AVSLAG,
    ),
    BP_INNVILGELSE(
        BARNEPENSJON_INNVILGELSE_UTFALL,
        BARNEPENSJON_INNVILGELSE,
    ),
    BP_OPPHOER(BARNEPENSJON_OPPHOER_UTFALL, BARNEPENSJON_OPPHOER),
    BP_REVURDERING(BARNEPENSJON_REVURDERING_UTFALL, BARNEPENSJON_REVURDERING),
    BP_VARSEL(BARNEPENSJON_VARSEL_UTFALL, BARNEPENSJON_VARSEL),
    OMS_AVSLAG(
        OMSTILLINGSSTOENAD_AVSLAG_UTFALL,
        OMSTILLINGSSTOENAD_AVSLAG,
    ),
    OMS_INNVILGELSE(
        OMSTILLINGSSTOENAD_INNVILGELSE_UTFALL,
        OMSTILLINGSSTOENAD_INNVILGELSE,
    ),
    OMS_OPPHOER(
        OMSTILLINGSSTOENAD_OPPHOER_UTFALL,
        OMSTILLINGSSTOENAD_OPPHOER,
    ),
    OMS_REVURDERING(TOM_MAL, OMSTILLINGSSTOENAD_REVURDERING),
    OMS_VARSEL(OMSTILLINGSSTOENAD_VARSEL_UTFALL, OMSTILLINGSSTOENAD_VARSEL),

    UTSATT_KLAGEFRIST_INFORMASJONSBREV(UTSATT_KLAGEFRIST, TOM_MAL_INFORMASJONSBREV),
    AVVIST_KLAGE(AVVIST_KLAGE_INNHOLD, AVVIST_KLAGE_FERDIG)
}
