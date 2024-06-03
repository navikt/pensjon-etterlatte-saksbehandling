import styled from 'styled-components'
import { IBehandlingsType, Vedtaksloesning } from '~shared/types/IDetaljertBehandling'
import { NavLenke } from '~components/behandling/StegMeny/NavLenke'
import { IBehandlingReducer, updateVilkaarsvurdering } from '~store/reducers/BehandlingReducer'
import { BehandlingRouteTypes, revurderingRoutes, soeknadRoutes } from '~components/behandling/BehandlingRoutes'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentVilkaarsvurderingUtendelVilkaar } from '~shared/api/vilkaarsvurdering'
import React, { useEffect } from 'react'
import { useAppDispatch } from '~store/Store'

import { mapApiResult } from '~shared/api/apiUtils'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'

export const StegMeny = (props: { behandling: IBehandlingReducer }) => {
  const dispatch = useAppDispatch()
  const behandling = props.behandling
  const { id, behandlingType } = behandling
  const personopplysninger = usePersonopplysninger()

  const [fetchVilkaarsvurderingStatus, fetchVilkaarsvurdering] = useApiCall(hentVilkaarsvurderingUtendelVilkaar)

  const lagVarselbrev = behandling.kilde === Vedtaksloesning.GJENOPPRETTA
  const soeknadRoutes_ = soeknadRoutes(behandling, personopplysninger, lagVarselbrev)
  const revurderingRoutes_ = revurderingRoutes(behandling, lagVarselbrev)

  const erSisteRoute = (index: number, list: BehandlingRouteTypes[]) => index != list.length - 1

  // Trenger vilkårsvurdering for å kunne vise riktig routes etter at vv i en behandling er utført
  useEffect(() => {
    fetchVilkaarsvurdering(id, (vilkaarsvurdering) => {
      dispatch(updateVilkaarsvurdering(vilkaarsvurdering))
    })
  }, [behandling.status])

  return (
    <>
      {mapApiResult(
        fetchVilkaarsvurderingStatus,
        <Spinner label="Laster stegmeny" visible />,
        () => (
          <ApiErrorAlert>Kunne ikke laste stegmeny</ApiErrorAlert>
        ),
        () => (
          <StegMenyWrapper role="navigation">
            {behandlingType === IBehandlingsType.FØRSTEGANGSBEHANDLING &&
              soeknadRoutes_.map((pathInfo, index) => (
                <NavLenke
                  key={pathInfo.path}
                  pathInfo={pathInfo}
                  behandling={props.behandling}
                  separator={erSisteRoute(index, soeknadRoutes_)}
                />
              ))}
            {behandlingType === IBehandlingsType.REVURDERING &&
              revurderingRoutes_.map((pathInfo, index) => (
                <NavLenke
                  key={pathInfo.path}
                  pathInfo={pathInfo}
                  behandling={props.behandling}
                  separator={erSisteRoute(index, revurderingRoutes_)}
                />
              ))}
          </StegMenyWrapper>
        )
      )}
    </>
  )
}

export const StegMenyWrapper = styled.ul`
  display: block;
  list-style: none;
  padding: 1em 0;
  background: #f8f8f8;
  border-bottom: 1px solid #c6c2bf;
  box-shadow: var(--a-shadow-xsmall);

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
