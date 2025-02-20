import { StatusBar } from '~shared/statusbar/Statusbar'
import React, { useEffect, useState } from 'react'
import { Box, Heading } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { OpplysningerForEtteroppgjoer } from '~components/etteroppgjoer/OpplysningerForEtteroppgjoer'
import { Etteroppgjoer } from '~shared/types/Etteroppgjoer'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentEtteroppgjoer } from '~shared/api/etteroppgjoer'
import { useParams } from 'react-router-dom'
import Spinner from '~shared/Spinner'
import { mapResult } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

export function Etteroppgjoersbehandling() {
  const { etteroppgjoerId } = useParams()

  const [etteroppgjoerResult, hentEtteroppgjoerRequest] = useApiCall(hentEtteroppgjoer)
  const [etteroppgjoer, setEtteroppgjoer] = useState<Etteroppgjoer>()

  useEffect(() => {
    if (!etteroppgjoerId) return
    hentEtteroppgjoerRequest(etteroppgjoerId, (etteroppgjoer) => {
      setEtteroppgjoer(etteroppgjoer)
    })
  }, [etteroppgjoerId])

  return (
    <>
      <StatusBar ident={etteroppgjoer?.behandling.sak.ident} />

      {mapResult(etteroppgjoerResult, {
        pending: <Spinner label="Henter etteroppgjørbehandling" />,
        success: (etteroppgjoer) => (
          <>
            <Box paddingInline="16" paddingBlock="16 4">
              <Heading level="1" size="large">
                Etteroppgjør {etteroppgjoer.behandling.aar}
              </Heading>
            </Box>
            <Box paddingInline="16" paddingBlock="4 2">
              Skatteoppgjør mottatt: {formaterDato(etteroppgjoer.behandling.opprettet)}
            </Box>
            <OpplysningerForEtteroppgjoer opplysninger={etteroppgjoer.opplysninger} />
          </>
        ),
      })}

      {isFailureHandler({
        apiResult: etteroppgjoerResult,
        errorMessage: 'Kunne ikke hente forbehandling etteroppgjør',
      })}
    </>
  )
}
