import { IBehandlingInfo } from '../../SideMeny/types'
import { Info, Overskrift, Tekst, UnderOverskrift, Wrapper } from '../styled'
import { useContext } from 'react'
import { AppContext } from '../../../../store/AppContext'
import { formaterDato, formaterEnumTilLesbarString, formaterStringDato } from '../../../../utils/formattering'

export const Innvilget = ({ behandlingsInfo }: { behandlingsInfo: IBehandlingInfo }) => {
  const innloggetId = useContext(AppContext).state.saksbehandlerReducer.ident
  const virkningsdato = behandlingsInfo.virkningsdato ? formaterStringDato(behandlingsInfo.virkningsdato) : '-'
  const attestertDato = behandlingsInfo.datoAttestert
    ? formaterStringDato(behandlingsInfo.datoAttestert)
    : formaterDato(new Date())

  return (
    <>
      <Wrapper innvilget={true}>
        <Overskrift>{formaterEnumTilLesbarString(behandlingsInfo.type)}</Overskrift>
        <UnderOverskrift innvilget={true}>Innvilget</UnderOverskrift>
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
      </Wrapper>
    </>
  )
}
