import { Box, List, VStack } from '@navikt/ds-react'
import { isFailure, isSuccess, Result } from '~shared/api/apiUtils'
import { SakMedBehandlinger } from '~components/person/typer'
import React, { ReactNode, useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentEtteroppgjoer } from '~shared/api/etteroppgjoer'
import { ArrowRightIcon, CheckmarkCircleIcon, CircleIcon } from '@navikt/aksel-icons'
import { ApiErrorAlert } from '~ErrorBoundary'
import { FeatureToggle, useFeaturetoggle } from '~useUnleash'
import { TilbakestillOgOpprettNyForbehandling } from '~components/person/sakOgBehandling/TilbakestillOgOpprettNyForbehandling'

const steg = [
  { status: ['VENTER_PAA_SKATTEOPPGJOER'], text: () => 'Venter på skatteoppgjøret' },
  { status: ['UNDER_FORBEHANDLING'], text: () => 'Etteroppgjøret er under forbehandling' },
  { status: ['VENTER_PAA_SVAR'], text: () => 'Varselbrev sendt, venter på svar fra bruker' },
  { status: ['UNDER_REVURDERING'], text: () => 'Behandler mottatt svar fra bruker' },
  { status: ['FERDIGSTILT'], text: () => 'Etteroppgjøret er ferdigstilt' },
]

const getIcon = (current: number, idx: number) => {
  if (current > idx) return <CheckmarkCircleIcon />
  if (current === idx) return <ArrowRightIcon />
  return <CircleIcon />
}

const EtteroppgjoerSaksoversikt = ({ sakResult }: { sakResult: Result<SakMedBehandlinger> }): ReactNode => {
  const [hentEtteroppgjoerResponse, hentEtteroppgjoerFetch] = useApiCall(hentEtteroppgjoer)
  const tilbakestillEtteroppgjoerEnabled = useFeaturetoggle(FeatureToggle.vis_tilbakestill_etteroppgjoer)

  useEffect(() => {
    if (isSuccess(sakResult)) {
      void hentEtteroppgjoerFetch(sakResult.data.sak.id.toString())
    }
  }, [sakResult])

  if (isFailure(hentEtteroppgjoerResponse)) {
    return <ApiErrorAlert>{hentEtteroppgjoerResponse.error.detail}</ApiErrorAlert>
  }

  if (!isSuccess(hentEtteroppgjoerResponse) || !hentEtteroppgjoerResponse.data) {
    return null
  }

  const etteroppgjoer = hentEtteroppgjoerResponse.data
  const currentIndex = steg.findIndex((s) => s.status.includes(etteroppgjoer.status))

  return (
    <VStack gap="space-4">
      <Box padding="space-8" maxWidth="70rem">
        <h1>Etteroppgjør for {etteroppgjoer.inntektsaar}</h1>
        <Box marginBlock="space-16" asChild>
          <List data-aksel-migrated-v8 as="ul">
            {steg.map((step, idx) => (
              <List.Item
                key={step.status.join(',')}
                icon={getIcon(currentIndex, idx)}
                style={{ color: currentIndex >= idx ? 'black' : 'gray' }}
              >
                {step.text()}
              </List.Item>
            ))}
          </List>
        </Box>
      </Box>
      {tilbakestillEtteroppgjoerEnabled && (
        <Box padding="space-8" maxWidth="70rem">
          <TilbakestillOgOpprettNyForbehandling sakId={etteroppgjoer.sakId} />
        </Box>
      )}
    </VStack>
  )
}

export default EtteroppgjoerSaksoversikt
