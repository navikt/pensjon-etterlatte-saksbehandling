import { IBehandlingInfo } from '../../SideMeny/types'
import { Info, Overskrift, Tekst, UnderOverskrift, Wrapper } from '../styled'

export const Underkjent = ({ behandlingsInfo }: { behandlingsInfo?: IBehandlingInfo }) => {
  return (
    <>
      <Wrapper innvilget={false}>
        <Overskrift>Førstegangsbehanling</Overskrift>
        <UnderOverskrift innvilget={false}>Underkjent</UnderOverskrift>
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
        <div className="info">
          <Info>Årsak til retur</Info>
          <Tekst>Inngangsvilkår er feilvurdert</Tekst>
        </div>

        <Tekst>
          Vedtaket underkjennes da vilkår om avdødes forutgående medlemskap ser ut til å være tolket feil fordi adresse
          .....
        </Tekst>
      </Wrapper>
    </>
  )
}
