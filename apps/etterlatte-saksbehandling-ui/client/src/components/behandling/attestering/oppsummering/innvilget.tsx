import { Info, Overskrift, Tekst, UnderOverskrift, Wrapper } from '../styled'
import { formaterBehandlingstype, formaterDato, formaterStringDato } from '~utils/formattering'
import { useAppSelector } from '~store/Store'
import { IBehandlingInfo } from '~components/behandling/SideMeny/types'
import { useVedtaksResultat, VedtakResultat } from '~components/behandling/useVedtaksResultat'

function innvilgelsestekst(vedtaksresultat: VedtakResultat): string {
  switch (vedtaksresultat) {
    case 'innvilget':
      return 'Innvilget'
    case 'opphoer':
      return 'Opphørt'
    case 'avslag':
      return 'Avslått'
    case 'endring':
      return 'Revurdert'
    case 'uavklart':
      return 'Uavklart'
  }
}

function Resultat({ vedtaksresultat }: { vedtaksresultat: VedtakResultat }) {
  const tekst = innvilgelsestekst(vedtaksresultat)
  const erInnvilget = vedtaksresultat !== 'uavklart' && vedtaksresultat !== 'avslag'
  return <UnderOverskrift innvilget={erInnvilget}>{tekst}</UnderOverskrift>
}

export const Innvilget = ({
  behandlingsInfo,
  children,
}: {
  behandlingsInfo: IBehandlingInfo
  children: JSX.Element
}) => {
  const innloggetId = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler?.ident)
  const virkningsdato = behandlingsInfo.virkningsdato ? formaterStringDato(behandlingsInfo.virkningsdato) : '-'
  const vedtaksResultat = useVedtaksResultat()
  const attestertDato = behandlingsInfo.datoAttestert
    ? formaterStringDato(behandlingsInfo.datoAttestert)
    : formaterDato(new Date())

  return (
    <Wrapper innvilget={true}>
      <Overskrift>{formaterBehandlingstype(behandlingsInfo.type)}</Overskrift>
      <Resultat vedtaksresultat={vedtaksResultat} />
      <div className="flex">
        <div>
          <Info>Attestant</Info>
          <Tekst>{innloggetId}</Tekst>
        </div>
        <div>
          <Info>Saksbehandler</Info>
          <Tekst>{behandlingsInfo.saksbehandler}</Tekst>
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
      {children}
    </Wrapper>
  )
}
