import styled from 'styled-components'
import { NavLenke } from '~components/behandling/stegmeny/NavLenke'
import { IBehandlingReducer, updateVilkaarsvurdering } from '~store/reducers/BehandlingReducer'
import { BehandlingRouteContext, BehandlingRouteType } from '~components/behandling/BehandlingRoutes'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentVilkaarsvurdering } from '~shared/api/vilkaarsvurdering'
import React, { useContext, useEffect } from 'react'
import { useAppDispatch } from '~store/Store'
import { mapApiResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Box, HStack, Label } from '@navikt/ds-react'

export const StegMeny = (props: { behandling: IBehandlingReducer }) => {
  const dispatch = useAppDispatch()
  const behandling = props.behandling
  const { id } = behandling
  const { behandlingRoutes } = useContext(BehandlingRouteContext)

  const [fetchVilkaarsvurderingStatus, fetchVilkaarsvurdering] = useApiCall(hentVilkaarsvurdering)

  const erSisteRoute = (index: number, list: BehandlingRouteType[]) => index != list.length - 1

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
        <Spinner label="Laster stegmeny" />,
        () => (
          <ApiErrorAlert>Kunne ikke laste stegmeny</ApiErrorAlert>
        ),
        () => (
          <StegMenyBox>
            <HStack gap="space-6" align="center">
              {behandlingRoutes.map((pathInfo, index) => (
                <NavLenke
                  key={pathInfo.path}
                  pathInfo={pathInfo}
                  behandling={props.behandling}
                  separator={erSisteRoute(index, behandlingRoutes)}
                />
              ))}
            </HStack>
          </StegMenyBox>
        )
      )}
    </>
  )
}

export const StegMenyBox = styled(Box)`
  padding: var(--a-spacing-4) 0 var(--a-spacing-4) var(--a-spacing-4);
  border-left: 0.4rem solid var(--a-border-action);
  border-bottom: 1px solid var(--a-border-subtle);
  background: var(--a-surface-subtle);
`

export const DisabledLabel = styled(Label)`
  color: var(--a-gray-400);
  cursor: not-allowed;
`
