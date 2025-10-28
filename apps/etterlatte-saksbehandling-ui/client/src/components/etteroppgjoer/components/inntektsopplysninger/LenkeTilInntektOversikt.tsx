import { Alert, Box, HStack, Link, Loader, Tag } from '@navikt/ds-react'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import React, { useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentUrlForInntektOversikt } from '~shared/api/arbeidOgInntekt'
import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { mapResult } from '~shared/api/apiUtils'

export function LenkeTilInntektOversikt() {
  const etteroppgjoer = useEtteroppgjoer()

  const [urlForInntektOversiktResult, urlForInntektOversiktRequest] = useApiCall(hentUrlForInntektOversikt)

  useEffect(() => {
    urlForInntektOversiktRequest(etteroppgjoer.behandling.sak.ident)
  }, [etteroppgjoer.behandling.sak.ident])

  return (
    <Box
      borderWidth="2"
      paddingInline="4"
      paddingBlock="3"
      maxWidth="fit-content"
      background="bg-default"
      borderRadius="4"
      borderColor="border-on-inverted"
    >
      {mapResult(urlForInntektOversiktResult, {
        pending: <Loader />,
        error: (
          <Alert variant="error" size="small">
            Kunne ikke hente lenke til inntekt oversikt
          </Alert>
        ),
        success: ({ url }) => (
          <HStack gap="2">
            <Tag variant="alt3" size="small">
              Ai
            </Tag>
            <Link href={url} target="_blank">
              Oversikt i Arbeid og Inntekt <ExternalLinkIcon aria-hidden />
            </Link>
          </HStack>
        ),
      })}
    </Box>
  )
}
