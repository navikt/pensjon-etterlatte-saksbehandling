import { Info, Overskrift, Tekst, UnderOverskrift, Wrapper } from '../styled'
import { formaterBehandlingstype } from '~utils/formatering/formatering'
import { formaterDato } from '~utils/formatering/dato'
import { IBehandlingInfo } from '~components/behandling/sidemeny/IBehandlingInfo'
import { useVedtaksResultat, VedtakResultat } from '~components/behandling/useVedtaksResultat'
import { KopierbarVerdi } from '~shared/statusbar/kopierbarVerdi'

function innvilgelsestekst(vedtaksresultat: VedtakResultat | null): string {
  switch (vedtaksresultat) {
    case 'innvilget':
      return 'Innvilget'
    case 'opphoer':
      return 'Opphørt'
    case 'avslag':
      return 'Avslått'
    case 'endring':
      return 'Revurdert'
    case null:
      return ''
  }
}

function Resultat({ vedtaksresultat }: { vedtaksresultat: VedtakResultat | null }) {
  const tekst = innvilgelsestekst(vedtaksresultat)
  const erInnvilget = vedtaksresultat == 'innvilget' || vedtaksresultat == 'endring'
  return <UnderOverskrift $innvilget={erInnvilget}>{tekst}</UnderOverskrift>
}

export const Innvilget = ({ behandlingsInfo }: { behandlingsInfo: IBehandlingInfo }) => {
  const virkningsdato = behandlingsInfo.virkningsdato ? formaterDato(behandlingsInfo.virkningsdato) : '-'
  const vedtaksResultat = useVedtaksResultat()
  const attestertDato = behandlingsInfo.datoAttestert
    ? formaterDato(behandlingsInfo.datoAttestert)
    : formaterDato(new Date())

  return (
    <Wrapper $innvilget={true}>
      <Overskrift>{formaterBehandlingstype(behandlingsInfo.type)}</Overskrift>
      <Resultat vedtaksresultat={vedtaksResultat} />
      <div className="flex">
        <div>
          <Info>Attestant</Info>
          <Tekst>{behandlingsInfo.attesterendeSaksbehandler}</Tekst>
        </div>
        <div>
          <Info>Saksbehandler</Info>
          <Tekst>{behandlingsInfo.behandlendeSaksbehandler}</Tekst>
        </div>
      </div>
      <div className="flex">
        <div>
          <Info>Virkningsdato</Info>
          <Tekst>{virkningsdato}</Tekst>
        </div>
        <div>
          <Info>Vedtaksdato</Info>
          <Tekst>{attestertDato}</Tekst>
        </div>
      </div>
      <KopierbarVerdi value={behandlingsInfo.sakId.toString()} />
    </Wrapper>
  )
}
