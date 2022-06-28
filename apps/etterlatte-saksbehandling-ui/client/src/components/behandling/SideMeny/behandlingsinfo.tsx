import styled from 'styled-components'
import { IBehandlingStatus } from '../../../store/reducers/BehandlingReducer'
import { formatterStringDato } from '../../../utils'
import { Innvilget } from '../attestering/resultat/innvilget'
import { Underkjent } from '../attestering/resultat/underkjent'
import { IBehandlingInfo } from './types'

export const Behandlingsinfo = ({ behandlingsInfo }: { behandlingsInfo: IBehandlingInfo }) => {
  const hentStatus = () => {
    switch (behandlingsInfo.status) {
      case IBehandlingStatus.UNDER_BEHANDLING:
        return 'Under behandling'

      case IBehandlingStatus.FATTET_VEDTAK:
        return 'To-trinnskontroll'
    }
  }

  return (
    <>
      {behandlingsInfo.status === IBehandlingStatus.ATTESTERT && <Innvilget behandlingsInfo={behandlingsInfo} />}
      {behandlingsInfo.status === IBehandlingStatus.underkjent && <Underkjent behandlingsInfo={behandlingsInfo} />}

      {(behandlingsInfo.status === IBehandlingStatus.GYLDIG_SOEKNAD ||
        behandlingsInfo.status === IBehandlingStatus.UNDER_BEHANDLING ||
        behandlingsInfo.status === IBehandlingStatus.FATTET_VEDTAK) && (
        <BehandlingsinfoContainer>
          <Overskrift>Førstegangsbehanling</Overskrift>
          <UnderOverskrift>{hentStatus()}</UnderOverskrift>

          <div className="info">
            <Info>Saksbehandler</Info>
            <Tekst>{behandlingsInfo.saksbehandler}</Tekst>
          </div>
          <div className="flex">
            <div>
              <Info>Virkningsdato</Info>
              <Tekst>
                {behandlingsInfo.virkningsdato ? formatterStringDato(behandlingsInfo.virkningsdato) : 'Ikke satt'}
              </Tekst>
            </div>
            <div>
              <Info>Vedtaksdato</Info>
              <Tekst>
                {behandlingsInfo.datoFattet ? formatterStringDato(behandlingsInfo.datoFattet) : 'Ikke satt'}
              </Tekst>
            </div>
          </div>
        </BehandlingsinfoContainer>
      )}
    </>
  )
}

const BehandlingsinfoContainer = styled.div`
  margin: 20px 8px 0px 8px;
  padding: 1em;
  border: 1px solid #c7c0c0;
  border-radius: 3px;

  .flex {
    display: flex;
    justify-content: space-between;
  }
  .info {
    margin-top: 1em;
    margin-bottom: 1em;
  }
`

const UnderOverskrift = styled.div`
  font-size: 16px;
  font-weight: 600;
  color: #005b82;
`

const Overskrift = styled.div`
  font-size: 20px;
  font-weight: 600;
  color: #3e3832;
`

const Info = styled.div`
  font-size: 14px;
  font-weight: 600;
`

const Tekst = styled.div`
  font-size: 14px;
  font-weight: 600;
  color: #595959;
`
