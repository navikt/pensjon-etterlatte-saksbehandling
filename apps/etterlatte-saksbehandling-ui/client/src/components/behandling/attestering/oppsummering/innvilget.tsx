import { IBehandlingInfo } from '../../SideMeny/types'
import { Info, Overskrift, Tekst, UnderOverskrift, Wrapper } from '../styled'
import { useContext } from 'react'
import { AppContext } from '../../../../store/AppContext'
import { formatterDato, formatterStringDato } from '../../../../utils/formattering'

export const Innvilget = ({behandlingsInfo}: {behandlingsInfo?: IBehandlingInfo}) => {
  const innloggetId = useContext(AppContext).state.saksbehandlerReducer.ident
  const virkningsdato = behandlingsInfo?.virkningsdato ? formatterStringDato(behandlingsInfo?.virkningsdato) : '-'
  const attestertDato = behandlingsInfo?.datoAttestert ? formatterStringDato(behandlingsInfo?.datoAttestert) :
    formatterDato(new Date())

  return (
    <>
      <Wrapper innvilget={true}>
        <Overskrift>Førstegangsbehandling</Overskrift>
        <UnderOverskrift innvilget={true}>Innvilget</UnderOverskrift>
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
      </Wrapper>
    </>
  )
}
