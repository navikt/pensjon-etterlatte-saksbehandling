import styled from 'styled-components'

export enum IBehandlingsType {
  FØRSTEGANGSBEHANDLING = 'FØRSTEGANGSBEHANDLING', REVURDERING = 'REVURDERING'
}

const colors = {
  [IBehandlingsType.FØRSTEGANGSBEHANDLING]: '#99DEAD', [IBehandlingsType.REVURDERING]: '#A18DBB',
}

export const BehandlingsType: React.FC<{status: IBehandlingsType}> = ({status}) => {
  return <BehandlingsTypeWrap status={status}>{status}</BehandlingsTypeWrap>
}

const BehandlingsTypeWrap = styled.div<{status: IBehandlingsType}>`
  background-color: ${(props) => colors[props.status]};
  padding: 0.2em 2em;
  color: #fff;
  text-align: center;
  border-radius: 15px;
  text-transform: uppercase;
  font-size: 0.8em;
`

export const BehandlingsTypeSmall: React.FC<{status: IBehandlingsType}> = ({status}) => {
  return <BehandlingsTypeWrapSmall status={status}>{status}</BehandlingsTypeWrapSmall>
}

export const BehandlingsTypeWrapSmall = styled.div<{status: IBehandlingsType}>`
  background-color: ${(props) => colors[props.status]};
  padding: 0.1em 0.5em;
  text-align: center;
  border-radius: 15px;
  font-size: 14px;
  text-transform: capitalize;

  float: right;
`
