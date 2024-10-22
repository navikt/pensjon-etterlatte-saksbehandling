import styled from 'styled-components'
import { IBehandlingsType, Vedtaksloesning } from '~shared/types/IDetaljertBehandling'
import { NavLenke } from '~components/behandling/StegMeny/NavLenke'
import { IBehandlingReducer, updateVilkaarsvurdering } from '~store/reducers/BehandlingReducer'
import {
  BehandlingRouteTypes,
  revurderingRoutes,
  foerstegangsbehandlingRoutes,
} from '~components/behandling/BehandlingRoutes'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentVilkaarsvurdering } from '~shared/api/vilkaarsvurdering'
import React, { useEffect } from 'react'
import { useAppDispatch } from '~store/Store'
import { mapApiResult } from '~shared/api/apiUtils'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Box, HStack, Label } from '@navikt/ds-react'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'

export const StegMeny = (props: { behandling: IBehandlingReducer }) => {
  const dispatch = useAppDispatch()
  const behandling = props.behandling
  const { id, behandlingType } = behandling
  const personopplysninger = usePersonopplysninger()

  const [fetchVilkaarsvurderingStatus, fetchVilkaarsvurdering] = useApiCall(hentVilkaarsvurdering)

  const lagVarselbrev =
    behandling.kilde === Vedtaksloesning.GJENOPPRETTA ||
    behandling.revurderingsaarsak == Revurderingaarsak.AKTIVITETSPLIKT
  const routes =
    behandlingType === IBehandlingsType.FØRSTEGANGSBEHANDLING
      ? foerstegangsbehandlingRoutes(behandling, personopplysninger, lagVarselbrev)
      : revurderingRoutes(behandling, lagVarselbrev)

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
        <Spinner label="Laster stegmeny" />,
        () => (
          <ApiErrorAlert>Kunne ikke laste stegmeny</ApiErrorAlert>
        ),
        () => (
          <StegMenyBox>
            <HStack gap="6" align="center">
              {routes.map((pathInfo, index) => (
                <NavLenke
                  key={pathInfo.path}
                  pathInfo={pathInfo}
                  behandling={props.behandling}
                  separator={erSisteRoute(index, routes)}
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
  background: #f8f8f8;
`

export const DisabledLabel = styled(Label)`
  color: var(--a-gray-400);
  cursor: not-allowed;
`
