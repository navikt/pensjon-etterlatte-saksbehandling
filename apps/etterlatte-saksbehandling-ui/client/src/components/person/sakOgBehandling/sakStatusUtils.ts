import { VedtakSammendrag, VedtakType } from '~components/vedtak/typer'

export const hentLoependeVedtak = (vedtak: VedtakSammendrag[]): VedtakSammendrag | undefined => {
  return vedtak
    .filter(
      (vedtak) =>
        ![VedtakType.AVVIST_KLAGE, VedtakType.TILBAKEKREVING, VedtakType.INGEN_ENDRING].includes(vedtak.vedtakType!)
    )
    .filter((vedtak) => !!vedtak.datoAttestert)
    .sort((a, b) => new Date(a.datoAttestert!).valueOf() - new Date(b.datoAttestert!).valueOf())
    .pop()
}

export const hentInnvilgelseVedtak = (vedtak: VedtakSammendrag[]): VedtakSammendrag | undefined => {
  return vedtak
    .filter((vedtak) => vedtak.vedtakType == VedtakType.INNVILGELSE)
    .filter((vedtak) => !!vedtak.datoAttestert)
    .pop()
}

export const ytelseErOpphoert = (loependeVedtak: VedtakSammendrag) => {
  const opphoerFraOgMed = vedtakDatoStrengTilDate(loependeVedtak.opphoerFraOgMed)
  return opphoerFraOgMed && opphoerFraOgMed < new Date()
}

export const ytelseErLoependeMedOpphoerFremITid = (loependeVedtak: VedtakSammendrag) => {
  const opphoerFraOgMed = vedtakDatoStrengTilDate(loependeVedtak.opphoerFraOgMed)
  return opphoerFraOgMed && opphoerFraOgMed >= new Date()
}

const vedtakDatoStrengTilDate = (dato: string | undefined) => (dato ? new Date(dato) : undefined)

// TODO opphoerFraOgMed er nytt felt slik at opphørsvedtak som fantes før feltet vil ikke dette feltet populert
// Det skal populeres og når det er gjort kan vi anta her at et opphør alltid vil ha opphoerFraOgMed og vi vil slippe
// å sjekke den for å erstatte med virkningstidspunkt
export const ytelseOpphoersdato = (loependeVedtak: VedtakSammendrag) =>
  loependeVedtak.opphoerFraOgMed ? loependeVedtak.opphoerFraOgMed : loependeVedtak.virkningstidspunkt
