/*
TODO: Aksel Box migration:
Could not migrate the following:
  - borderColor=border-neutral-subtle
*/

import { Box, Button, Heading } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { useVedtaksResultat } from '~components/behandling/useVedtaksResultat'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { NesteOgTilbake } from '~components/behandling/handlinger/NesteOgTilbake'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { BehandlingRouteContext } from '~components/behandling/BehandlingRoutes'
import { useApiCall } from '~shared/hooks/useApiCall'
import React, { useContext } from 'react'
import { Trygdetid } from '~components/behandling/trygdetid/Trygdetid'
import { oppdaterStatus } from '~shared/api/trygdetid'
import { IBehandlingReducer, oppdaterBehandlingsstatus } from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'
import { handlinger } from '~components/behandling/handlinger/typer'
import { Vedtaksresultat } from '~components/behandling/felles/Vedtaksresultat'

import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'

const TrygdetidVisning = (props: { behandling: IBehandlingReducer }) => {
  const { behandling } = props
  const dispatch = useAppDispatch()
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )
  const { next } = useContext(BehandlingRouteContext)

  const vedtaksresultat = useVedtaksResultat()

  const virkningstidspunkt = behandling.virkningstidspunkt?.dato
    ? formaterDato(behandling.virkningstidspunkt.dato)
    : undefined
  const [oppdaterStatusResult, oppdaterStatusRequest] = useApiCall(oppdaterStatus)
  const virkningstidspunktEtterNyRegelDato = () => {
    return (
      behandling.virkningstidspunkt == null || new Date(behandling.virkningstidspunkt.dato) >= new Date('2024-01-01')
    )
  }

  const sjekkGyldighetOgOppdaterStatus = () => {
    if (vedtaksresultat === 'avslag') {
      next()
    } else {
      oppdaterStatusRequest(behandling.id, (result) => {
        if (result.statusOppdatert) {
          dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.TRYGDETID_OPPDATERT))
        }
        next()
      })
    }
  }

  return (
    <>
      <Box paddingInline="space-16" paddingBlock="space-16 space-4">
        <Heading spacing size="large" level="1">
          Trygdetid
        </Heading>
        <Vedtaksresultat vedtaksresultat={vedtaksresultat} virkningstidspunktFormatert={virkningstidspunkt} />
      </Box>

      <Trygdetid
        redigerbar={redigerbar}
        behandling={behandling}
        vedtaksresultat={vedtaksresultat}
        virkningstidspunktEtterNyRegelDato={virkningstidspunktEtterNyRegelDato()}
      />

      <Box paddingBlock="space-4 space-0" borderWidth="1 0 0 0" borderColor="border-neutral-subtle">
        {isFailureHandler({
          apiResult: oppdaterStatusResult,
          errorMessage: 'Kunne ikke oppdatere trygdetid status',
        })}

        {redigerbar ? (
          <BehandlingHandlingKnapper>
            <Button
              variant="primary"
              loading={isPending(oppdaterStatusResult)}
              onClick={sjekkGyldighetOgOppdaterStatus}
            >
              {handlinger.NESTE.navn}
            </Button>
          </BehandlingHandlingKnapper>
        ) : (
          <NesteOgTilbake />
        )}
      </Box>
    </>
  )
}

export default TrygdetidVisning
