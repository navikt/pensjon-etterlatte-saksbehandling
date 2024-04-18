import { VedtaketKlagenGjelder } from '~shared/types/Klage'
import { VedtakType } from '~components/vedtak/typer'

const filtrerBortUrelevanteVedtak = (vedtak: VedtaketKlagenGjelder[]): VedtaketKlagenGjelder[] => {
  return vedtak
    .filter((vedtak) => ![VedtakType.AVVIST_KLAGE, VedtakType.TILBAKEKREVING].includes(vedtak.vedtakType!))
    .filter((vedtak) => !!vedtak.datoAttestert)
    .sort((a, b) => new Date(a.datoAttestert!).valueOf() - new Date(b.datoAttestert!).valueOf())
}

const filtrerVedtakPaaInnvilgelse = (vedtak: VedtaketKlagenGjelder[]): VedtaketKlagenGjelder[] => {
  return vedtak
    .filter((vedtak) => vedtak.vedtakType === VedtakType.INNVILGELSE)
    .filter((vedtak) => !!vedtak.datoAttestert)
    .sort((a, b) => new Date(a.datoAttestert!).valueOf() - new Date(b.datoAttestert!).valueOf())
}

export const hentStatusPaaSak = (vedtak: VedtaketKlagenGjelder[]): VedtaketKlagenGjelder | undefined => {
  let sisteVedtak = filtrerBortUrelevanteVedtak(vedtak).pop()

  if (sisteVedtak?.vedtakType === VedtakType.ENDRING) {
    sisteVedtak = filtrerVedtakPaaInnvilgelse(vedtak).pop()
  }

  return sisteVedtak
}
