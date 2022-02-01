import styled from "styled-components";

export enum IBehandlingsStatus {
    FORSTEGANG = "førstegangsbehandling",
    REVURDERING = "revurdering",
}

const colors = {
  [IBehandlingsStatus.FORSTEGANG]: '#A18DBB',
  [IBehandlingsStatus.REVURDERING]: 'red',
}


export const BehandlingsStatus: React.FC<{ status: IBehandlingsStatus }> = ({ status }) => {
    return <BehandlingsStatusWrap status={status}>{status}</BehandlingsStatusWrap>;
};

const BehandlingsStatusWrap = styled.div<{status: IBehandlingsStatus}>`
    background-color: ${props => colors[props.status]};
    padding: 0.2em 2em;
    color: #fff;
    text-align: center;
    border-radius: 15px;
    text-transform: uppercase;
    font-size: 0.8em;
`;

export const BehandlingsStatusSmall: React.FC<{ status: IBehandlingsStatus }> = ({ status }) => {
    return <BehandlingsStatusWrapSmall status={status}>{status}</BehandlingsStatusWrapSmall>;
};

export const BehandlingsStatusWrapSmall = styled.div<{status: IBehandlingsStatus}>`
  background-color: ${props => colors[props.status]};
  padding: 0.1em 0.5em;
  text-align: center;
  border-radius: 15px;
  font-size: 14px;
  text-transform: capitalize;

  float: right;
`;
