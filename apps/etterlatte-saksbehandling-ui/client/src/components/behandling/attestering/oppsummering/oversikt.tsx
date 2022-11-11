import styled from 'styled-components'
import { useAppSelector } from '~store/Store'
import { IBehandlingStatus } from '~store/reducers/BehandlingReducer'
import { formaterEnumTilLesbarString, formaterStringDato, formaterStringTidspunkt } from '~utils/formattering'
import { IBehandlingInfo } from '~components/behandling/SideMeny/types'

export const Oversikt = ({ behandlingsInfo }: { behandlingsInfo: IBehandlingInfo }) => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler.ident)

  const hentStatus = () => {
    switch (behandlingsInfo.status) {
      case IBehandlingStatus.UNDER_BEHANDLING:
        return 'Under behandling'
      case IBehandlingStatus.GYLDIG_SOEKNAD:
        return 'Under behandling'
      case IBehandlingStatus.FATTET_VEDTAK:
        return 'To-trinnskontroll'
    }
  }

  const fattetDato = behandlingsInfo.datoFattet
    ? formaterStringDato(behandlingsInfo.datoFattet) + ' kl. ' + formaterStringTidspunkt(behandlingsInfo.datoFattet)
    : null

  return (
    <BehandlingsinfoContainer>
      <Overskrift>{formaterEnumTilLesbarString(behandlingsInfo?.type ?? '')}</Overskrift>
      <UnderOverskrift>{hentStatus()}</UnderOverskrift>
      {fattetDato && <Tekst>{fattetDato}</Tekst>}
      <div className="info">
        <Info>Saksbehandler</Info>
        <Tekst>{behandlingsInfo.saksbehandler ? behandlingsInfo.saksbehandler : innloggetSaksbehandler}</Tekst>
      </div>
      <div className="flex">
        <div>
          <Info>Virkningstidspunkt</Info>
          <Tekst>
            {behandlingsInfo.virkningsdato ? formaterStringDato(behandlingsInfo.virkningsdato) : 'Ikke satt'}
          </Tekst>
        </div>
        <div>
          <Info>Vedtaksdato</Info>
          <Tekst>
            {behandlingsInfo.datoAttestert ? formaterStringDato(behandlingsInfo.datoAttestert) : 'Ikke satt'}
          </Tekst>
        </div>
      </div>
    </BehandlingsinfoContainer>
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
