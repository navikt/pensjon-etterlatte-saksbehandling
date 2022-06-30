import { IBehandlingInfo } from '../../SideMeny/types'
import { Info, Overskrift, Tekst, UnderOverskrift, Wrapper } from '../styled'
import { useContext } from 'react'
import { AppContext } from '../../../../store/AppContext'

export const Underkjent = ({ behandlingsInfo }: { behandlingsInfo?: IBehandlingInfo }) => {
  const innloggetId = useContext(AppContext).state.saksbehandlerReducer.ident

  return (
    <>
      <Wrapper innvilget={false}>
        <Overskrift>Førstegangsbehandling</Overskrift>
        <UnderOverskrift innvilget={false}>Underkjent</UnderOverskrift>
        <Tekst>
          {
            //formatterDato(dato)} kl: {formatterTidspunkt(dato) todo: legg inn tidspunk for returnering her
          }
        </Tekst>

        <div className="flex">
          <div>
            <Info>Attestant</Info>
            <Tekst>{innloggetId}</Tekst>
          </div>
          <div>
            <Info>Saksbehandler</Info>
            {
              // todo: få med opprinnelige saksbehandler fra loggene her?
            }
            <Tekst>{behandlingsInfo?.saksbehandler}</Tekst>
          </div>
        </div>

        {/*          todo: hentes fra logger og vises kun om data finnes
       <div className="info">
          <Info>Årsak til retur</Info>
          <Tekst>Hardkodet: Inngangsvilkår er feilvurdert</Tekst>
        </div>

        <Tekst>
          Harkodet: Vedtaket underkjennes da vilkår om avdødes forutgående medlemskap ser ut til å være tolket feil
          fordi adresse .....
        </Tekst>*/}
      </Wrapper>
    </>
  )
}
