import { IBehandlingInfo } from '../../SideMeny/types'
import { Info, Overskrift, Tekst, UnderOverskrift, Wrapper } from '../styled'
import { useContext } from 'react'
import { AppContext } from '../../../../store/AppContext'
import { formatterDato, formatterTidspunkt } from '../../../../utils'

export const Underkjent = ({ behandlingsInfo }: { behandlingsInfo?: IBehandlingInfo }) => {
  const innloggetId = useContext(AppContext).state.saksbehandlerReducer.ident
  const dato = new Date()

  return (
    <>
      <Wrapper innvilget={false}>
        <Overskrift>Førstegangsbehandling</Overskrift>
        <UnderOverskrift innvilget={false}>Underkjent</UnderOverskrift>
        <Tekst>
          {formatterDato(dato)} kl: {formatterTidspunkt(dato)}
        </Tekst>

        <div className="flex">
          <div>
            <Info>Attestant</Info>
            <Tekst>{innloggetId}</Tekst>
          </div>
          <div>
            <Info>Saksbehandler</Info>
            <Tekst>{behandlingsInfo?.saksbehandler}</Tekst>
          </div>
        </div>
        <div className="info">
          <Info>Årsak til retur</Info>
          <Tekst>Hardkodet: Inngangsvilkår er feilvurdert</Tekst>
        </div>

        <Tekst>
          Harkodet: Vedtaket underkjennes da vilkår om avdødes forutgående medlemskap ser ut til å være tolket feil
          fordi adresse .....
        </Tekst>
      </Wrapper>
    </>
  )
}
