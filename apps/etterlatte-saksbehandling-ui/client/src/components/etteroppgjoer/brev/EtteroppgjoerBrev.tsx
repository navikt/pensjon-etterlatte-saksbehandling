import { useApiCall } from '~shared/hooks/useApiCall'
import { hentBrev } from '~shared/api/brev'
import React, { useEffect } from 'react'
import { BodyShort, Box, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import { mapResult } from '~shared/api/apiUtils'
import RedigerbartBrev from '~components/behandling/brev/RedigerbartBrev'
import { Link } from 'react-router-dom'
import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import Spinner from '~shared/Spinner'
import { EtteroppgjoerFramTilbakeKnapperad } from '~components/etteroppgjoer/stegmeny/EtteroppgjoerFramTilbakeKnapperad'
import { EtteroppjoerSteg } from '~components/etteroppgjoer/stegmeny/EtteroppjoerForbehandlingSteg'

export function EtteroppgjoerBrev() {
  const etteroppgjoer = useEtteroppgjoer()
  const [brevResult] = useApiCall(hentBrev)
  useEffect(() => {
    // TODO: avklar hvordan vi henter brevId for eventuelt etteroppgjørsbrev
    // fetchBrev({ sakId: etteroppgjoer.behandling.sak.id, brevId: 0 })
  }, [])

  return (
    <HStack height="100%" minHeight="100%" wrap={false}>
      <Box minWidth="30rem" maxWidth="40rem" borderColor="border-subtle" borderWidth="0 1 0 0">
        <VStack gap="4" margin="16" width="100%">
          <Heading level="1" size="large">
            Brev
          </Heading>
          <BodyShort>Her kan du endre på brevet for etteroppgjøret</BodyShort>
        </VStack>
      </Box>
      <VStack width="100%">
        {mapResult(brevResult, {
          success: (brev) => (
            <RedigerbartBrev brev={brev} kanRedigeres={true} tilbakestillingsaction={() => alert('Not supported')} />
          ),
          pending: <Spinner label="Laster brev" />,
        })}
        <EtteroppgjoerFramTilbakeKnapperad>
          <div>
            <Button
              as={Link}
              to={`/etteroppgjoer/${etteroppgjoer.behandling.id}/${EtteroppjoerSteg.OVERSIKT}`}
              variant="secondary"
            >
              Gå tilbake
            </Button>
          </div>
          <div>
            <Button variant="primary">Ferdigstill</Button>
          </div>
        </EtteroppgjoerFramTilbakeKnapperad>
      </VStack>
    </HStack>
  )
}
