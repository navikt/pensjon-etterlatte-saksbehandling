import styled from 'styled-components'

export enum IBehandlingsType {
  BARNEPENSJON = 'barnepensjon',
  GJENLEVENDEPENSJON = 'gjenlevendeperson',
}

const colors = {
  [IBehandlingsType.BARNEPENSJON]: '#66A3C4',
  [IBehandlingsType.GJENLEVENDEPENSJON]: 'red',
}

export const BehandlingsTypeSmall: React.FC<{ type: IBehandlingsType }> = ({ type }) => {
  return <BehandlingsTypeWrapSmall type={type}>{type}</BehandlingsTypeWrapSmall>
}

export const BehandlingsTypeWrapSmall = styled.div<{ type: IBehandlingsType }>`
  background-color: ${(props) => colors[props.type]};
  padding: 0.1em 0.5em;
  text-align: center;
  border-radius: 15px;
  font-size: 14px;
  text-transform: capitalize;
  float: right;
  margin-left: 0.7em;
  margin-right: 0.5em;  
`
