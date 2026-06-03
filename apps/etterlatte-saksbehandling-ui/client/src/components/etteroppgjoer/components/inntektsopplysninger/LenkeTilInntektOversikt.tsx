import { Alert, Box, HStack, Link, Loader, Tag } from '@navikt/ds-react'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import React, { useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentUrlForInntektOversikt } from '~shared/api/arbeidOgInntekt'
import { useEtteroppgjoerForbehandling } from '~store/reducers/EtteroppgjoerReducer'
import { mapResult } from '~shared/api/apiUtils'

export function LenkeTilInntektOversikt() {
  const { forbehandling } = useEtteroppgjoerForbehandling()

  const [urlForInntektOversiktResult, urlForInntektOversiktRequest] = useApiCall(hentUrlForInntektOversikt)

  useEffect(() => {
    urlForInntektOversiktRequest(forbehandling.sak.ident)
  }, [forbehandling.sak.ident])

  return (
    <Box
      borderWidth="2"
      paddingInline="space-16"
      paddingBlock="space-12"
      maxWidth="fit-content"
      background="default"
      borderRadius="4"
      borderColor="neutral-subtle"
    >
      {mapResult(urlForInntektOversiktResult, {
        pending: <Loader />,
        error: (
          <Alert variant="error" size="small">
            Kunne ikke hente lenke til inntekt oversikt
          </Alert>
        ),
        success: ({ url }) => (
          <HStack gap="space-8">
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
