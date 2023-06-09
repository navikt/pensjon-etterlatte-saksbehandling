import { Info, Overskrift, Tekst, UnderOverskrift, Wrapper } from '../styled'
import { useAppSelector } from '~store/Store'
import { formaterBehandlingstype, formaterStringDato, formaterStringTidspunkt } from '~utils/formattering'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { IBehandlingInfo } from '~components/behandling/SideMeny/types'

export const Underkjent = ({
  behandlingsInfo,
  children,
}: {
  behandlingsInfo: IBehandlingInfo
  children: JSX.Element
}) => {
  const innloggetId = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler.ident)
  const underkjentSiste = behandlingsInfo.underkjentLogg?.slice(-1)[0]
  const fattetSiste = behandlingsInfo.fattetLogg?.slice(-1)[0]

  const erReturnert = behandlingsInfo.status === IBehandlingStatus.RETURNERT
  const saksbehandler = fattetSiste?.ident
  const attestant = erReturnert ? underkjentSiste?.ident : innloggetId

  return (
    <Wrapper innvilget={false}>
      <Overskrift>{formaterBehandlingstype(behandlingsInfo.type)}</Overskrift>
      <UnderOverskrift innvilget={false}>Underkjent</UnderOverskrift>
      {underkjentSiste && (
        <Tekst>
          {formaterStringDato(underkjentSiste.opprettet)} kl: {formaterStringTidspunkt(underkjentSiste.opprettet)}
        </Tekst>
      )}
      <div className="flex">
        <div>
          <Info>Attestant</Info>
          <Tekst>{attestant}</Tekst>
        </div>
        <div>
          <Info>Saksbehandler</Info>
          <Tekst>{saksbehandler}</Tekst>
        </div>
      </div>

      {erReturnert && underkjentSiste && (
        <>
          <div className="info">
            <Info>Årsak til retur</Info>
            <Tekst>{underkjentSiste.valgtBegrunnelse}</Tekst>
          </div>
          <Tekst>{underkjentSiste.kommentar}</Tekst>
        </>
      )}
      {children}
    </Wrapper>
  )
}
