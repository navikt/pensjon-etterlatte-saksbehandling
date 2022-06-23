import { IBehandlingInfo } from '../../SideMeny/types'
import { Info, Overskrift, Tekst, UnderOverskrift, Wrapper } from '../styled'

export const Innvilget = ({ behandlingsInfo }: { behandlingsInfo?: IBehandlingInfo }) => {
  return (
    <>
      <Wrapper innvilget={true}>
        <Overskrift>Førstegangsbehandling</Overskrift>
        <UnderOverskrift innvilget={true}>Innvilget</UnderOverskrift>
        <Tekst>22.12.21 kl: 14:07</Tekst>
        <div className="flex">
          <div>
            <Info>Attestant</Info>
            <Tekst>{behandlingsInfo?.attestant}</Tekst>
          </div>
          <div>
            <Info>Saksbehandler</Info>
            <Tekst>{behandlingsInfo?.saksbehandler}</Tekst>
          </div>
        </div>
        <div className="flex">
          <div>
            <Info>Virkningsdato</Info>
            <Tekst>{behandlingsInfo?.virkningsdato}</Tekst>
          </div>
          <div>
            <Info>Vedtaksdato</Info>
            <Tekst>{behandlingsInfo?.vedtaksdato}</Tekst>
          </div>
        </div>
      </Wrapper>
    </>
  )
}
