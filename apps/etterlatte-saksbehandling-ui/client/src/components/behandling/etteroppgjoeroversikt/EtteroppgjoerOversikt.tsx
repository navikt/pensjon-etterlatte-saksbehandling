import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { Box, Heading } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { Start } from '~components/behandling/handlinger/Start'
import { NesteOgTilbake } from '~components/behandling/handlinger/NesteOgTilbake'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'

export const EtteroppgjoerOversikt = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )

  return (
    <>
      <Box paddingInline="16" paddingBlock="12 4">
        <Heading spacing size="large" level="1">
          Etteroppgjør
        </Heading>
      </Box>
      <Box paddingInline="16" paddingBlock="12 4">
        TODO {behandling.id}
      </Box>
      <Box paddingBlock="4 0" borderWidth="1 0 0 0" borderColor="border-subtle">
        {redigerbar ? (
          <BehandlingHandlingKnapper>
            <Start disabled={behandling.virkningstidspunkt === null} />
          </BehandlingHandlingKnapper>
        ) : (
          <NesteOgTilbake />
        )}
      </Box>
    </>
  )
}
