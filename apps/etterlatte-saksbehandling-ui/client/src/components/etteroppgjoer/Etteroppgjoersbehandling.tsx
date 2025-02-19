import { StatusBar } from '~shared/statusbar/Statusbar'
import React, { useEffect, useState } from 'react'
import { Box, Heading } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { EtteroppgjoerOpplysninger } from '~components/etteroppgjoer/EtteroppgjoerOpplysninger'
import { IEtteroppgjoer } from '~shared/types/Etteroppgjoer'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentEtteroppgjoer } from '~shared/api/etteroppgjoer'
import { useParams } from 'react-router-dom'
import Spinner from '~shared/Spinner'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

export function Etteroppgjoersbehandling() {
  const { etteroppgjoerId } = useParams()

  const [etteroppgjoerStatus, hentEtteroppgjoerRequest] = useApiCall(hentEtteroppgjoer)
  const [etteroppgjoer, setEtteroppgjoer] = useState<IEtteroppgjoer>()

  useEffect(() => {
    if (!etteroppgjoerId) return
    hentEtteroppgjoerRequest(etteroppgjoerId, (etteroppgjoer) => {
      setEtteroppgjoer(etteroppgjoer)
    })
  }, [etteroppgjoerId])

  return (
    <>
      <StatusBar ident={etteroppgjoer?.behandling.sak.ident} />
      <Spinner visible={isPending(etteroppgjoerStatus)} label="Henter etteroppgjørbehandling" />

      {etteroppgjoer && (
        <>
          <Box paddingInline="16" paddingBlock="16 4">
            <Heading level="1" size="large">
              Etteroppgjør {etteroppgjoer.behandling.aar}
            </Heading>
          </Box>
          <Box paddingInline="16" paddingBlock="4 2">
            Skatteoppgjør mottatt: {formaterDato(etteroppgjoer.behandling.opprettet)}
          </Box>
          <EtteroppgjoerOpplysninger opplysninger={etteroppgjoer.opplysninger} />
        </>
      )}

      {isFailureHandler({
        apiResult: etteroppgjoerStatus,
        errorMessage: 'Kunne ikke hente forbehandling etteroppgjør',
      })}
    </>
  )
}
