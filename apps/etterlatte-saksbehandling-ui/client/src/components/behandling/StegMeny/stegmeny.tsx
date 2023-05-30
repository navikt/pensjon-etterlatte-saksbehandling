import styled from 'styled-components'
import { IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import { NavLenke } from '~components/behandling/StegMeny/NavLenke'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { manueltOpphoerRoutes, revurderingRoutes, soeknadRoutes } from '~components/behandling/BehandlingRoutes'

export const StegMeny = (props: { behandling: IBehandlingReducer }) => {
  const { behandlingType } = props.behandling

  return (
    <StegMenyWrapper role="navigation">
      {behandlingType === IBehandlingsType.MANUELT_OPPHOER &&
        manueltOpphoerRoutes.map((pathInfo) => (
          <NavLenke key={pathInfo.path} pathInfo={pathInfo} behandling={props.behandling} />
        ))}
      {behandlingType === IBehandlingsType.FØRSTEGANGSBEHANDLING &&
        soeknadRoutes.map((pathInfo) => (
          <NavLenke key={pathInfo.path} pathInfo={pathInfo} behandling={props.behandling} />
        ))}
      {behandlingType === IBehandlingsType.REVURDERING &&
        revurderingRoutes(props.behandling).map((pathInfo) => (
          <NavLenke key={pathInfo.path} pathInfo={pathInfo} behandling={props.behandling} />
        ))}
    </StegMenyWrapper>
  )
}

const StegMenyWrapper = styled.ul`
  display: block;
  list-style: none;
  padding: 1em 0;
  background: #f8f8f8;
  border-bottom: 1px solid #c6c2bf;
  box-shadow: 0 5px 10px 0 #ddd;

  li {
    display: inline-block;

    a {
      padding: 1em 1em 1em;
      margin-bottom: 0.4em;
      font-weight: 600;
      color: #0067c5;
      text-decoration: none;
      border-left: 8px solid transparent;

      &:hover {
        text-decoration: underline;
      }

      &.active {
        color: #262626;
      }
    }

    &:first-child a {
      border-left: 8px solid #0067c5;
    }
  }

  .disabled {
    cursor: not-allowed;

    a {
      color: #b0b0b0;
      text-decoration: none;
      pointer-events: none;
    }
  }
`
