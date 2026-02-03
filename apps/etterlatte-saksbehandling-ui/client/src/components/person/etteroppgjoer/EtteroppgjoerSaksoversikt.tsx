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
  { index: 1, status: 'VENTER_PAA_SKATTEOPPGJOER', text: () => 'Venter på skatteoppgjøret' },
  { index: 2, status: 'MOTTATT_SKATTEOPPGJOER', text: () => 'Mottatt skatteoppgjør' },
  { index: 3, status: 'MANGLER_SKATTEOPPGJOER', text: () => 'Mangler skatteoppgjør' },
  { index: 4, status: 'UNDER_FORBEHANDLING', text: () => 'Etteroppgjøret er under forbehandling' },
  { index: 5, status: 'VENTER_PAA_SVAR', text: () => 'Varselbrev sendt, venter på svar fra bruker' },
  { index: 6, status: 'UNDER_REVURDERING', text: () => 'Behandler mottatt svar fra bruker' },
  { index: 7, status: 'FERDIGSTILT', text: () => 'Etteroppgjøret er ferdigstilt' },
]

const getIcon = (currentStatus: string, stepStatus: string, stepIndex: number) => {
  if (stepStatus === 'MANGLER_SKATTEOPPGJOER') {
    return currentStatus === 'MANGLER_SKATTEOPPGJOER' ? <ArrowRightIcon /> : <CircleIcon />
  }
  const currentStep = steg.find((s) => s.status === currentStatus)?.index ?? 0
  if (currentStep > stepIndex) return <CheckmarkCircleIcon />
  if (currentStep === stepIndex) return <ArrowRightIcon />
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
  const currentStatus = etteroppgjoer.status
  const currentIndex = steg.find((s) => s.status === currentStatus)?.index ?? 0

  return (
    <VStack gap="4">
      <Box padding="8" maxWidth="70rem">
        <h1>Etteroppgjør for {etteroppgjoer.inntektsaar}</h1>

        <List as="ul">
          {steg.map(({ status, text, index: stepIndex }) => (
            <List.Item
              key={status}
              icon={getIcon(currentStatus, status, stepIndex)}
              style={{ color: currentIndex >= stepIndex ? 'black' : 'gray' }}
            >
              {text()}
            </List.Item>
          ))}
        </List>
      </Box>

      {tilbakestillEtteroppgjoerEnabled && (
        <Box padding="8" maxWidth="70rem">
          <TilbakestillOgOpprettNyForbehandling sakId={etteroppgjoer.sakId} />
        </Box>
      )}
    </VStack>
  )
}

export default EtteroppgjoerSaksoversikt
