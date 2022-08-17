import { IBehandlingInfo } from '../../SideMeny/types'
import { Info, Overskrift, Tekst, UnderOverskrift, Wrapper } from '../styled'
import { useContext } from 'react'
import { AppContext } from '../../../../store/AppContext'
import { formatterStringDato, formatterStringTidspunkt } from "../../../../utils/formattering";
import { IBehandlingStatus } from "../../../../store/reducers/BehandlingReducer";

export const Underkjent = ({behandlingsInfo}: {behandlingsInfo?: IBehandlingInfo}) => {
  const innloggetId = useContext(AppContext).state.saksbehandlerReducer.ident

  const underkjentSiste = behandlingsInfo?.underkjentLogg?.slice(-1)[0]
  const fattetSiste = behandlingsInfo?.fattetLogg?.slice(-1)[0]

  const erReturnert = behandlingsInfo?.status === IBehandlingStatus.RETURNERT
  const saksbehandler = fattetSiste?.ident
  const attestant = erReturnert ? underkjentSiste?.ident : innloggetId

  return (
    <>
      <Wrapper innvilget={false}>
        <Overskrift>Førstegangsbehandling</Overskrift>
        <UnderOverskrift innvilget={false}>Underkjent</UnderOverskrift>
        {underkjentSiste && <Tekst>
          {formatterStringDato(underkjentSiste.opprettet)} kl: {formatterStringTidspunkt(underkjentSiste.opprettet)}
        </Tekst>}
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

        {erReturnert && underkjentSiste && <>
          <div className="info">
            <Info>Årsak til retur</Info>
            <Tekst>{underkjentSiste.valgtBegrunnelse}</Tekst>
          </div>
          <Tekst>{underkjentSiste.kommentar}</Tekst>
        </>}
      </Wrapper>
    </>
  )
}
