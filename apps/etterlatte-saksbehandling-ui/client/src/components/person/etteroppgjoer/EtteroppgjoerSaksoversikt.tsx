import { Box, VStack } from '@navikt/ds-react'
import { isSuccess, Result } from '~shared/api/apiUtils'
import { SakMedBehandlinger } from '~components/person/typer'
import React, { ReactNode, useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentEtteroppgjoer } from '~shared/api/etteroppgjoer'

const EtteroppgjoerSaksoversikt = ({ sakResult }: { sakResult: Result<SakMedBehandlinger> }): ReactNode => {
  const [hentEtteroppgjoerResponse, hentEtteroppgjoerFetch] = useApiCall(hentEtteroppgjoer)

  useEffect(() => {
    if (isSuccess(sakResult)) {
      void hentEtteroppgjoerFetch(sakResult.data.sak.id.toString())
    }
  }, [sakResult])

  return (
    <VStack gap="4">
      <Box padding="8" maxWidth="70rem">
        {isSuccess(hentEtteroppgjoerResponse) && (
          <div>
            {hentEtteroppgjoerResponse.data.map((etteroppgjoer, index) => (
              <div key={index}>
                <h1>Etteroppgjør {etteroppgjoer.inntektsaar}</h1>
                <p>Status: {etteroppgjoer.status}</p>
              </div>
            ))}
          </div>
        )}
      </Box>
    </VStack>
  )
}

export default EtteroppgjoerSaksoversikt
