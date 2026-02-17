import { Accordion, Box, Heading, List, VStack } from '@navikt/ds-react'
import { isFailure, isSuccess, Result } from '~shared/api/apiUtils'
import { SakMedBehandlinger } from '~components/person/typer'
import React, { ReactNode, useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentEtteroppgjoerListe } from '~shared/api/etteroppgjoer'
import { ArrowRightIcon, CheckmarkCircleIcon, CircleIcon } from '@navikt/aksel-icons'
import { ApiErrorAlert } from '~ErrorBoundary'
import { FeatureToggle, useFeaturetoggle } from '~useUnleash'
import { TilbakestillOgOpprettNyForbehandling } from '~components/person/sakOgBehandling/TilbakestillOgOpprettNyForbehandling'
import { OpprettEtteroppgjoerIDev } from '~components/etteroppgjoer/components/utils/OpprettEtteroppgjoerIDev'

const steg = [
  { status: ['VENTER_PAA_SKATTEOPPGJOER'], text: () => 'Venter på skatteoppgjøret' },
  { status: ['MOTTATT_SKATTEOPPGJOER'], text: () => 'Mottatt skatteoppgjør' },
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
  const [fetchEtteroppgjoerListeResult, fetchEtteroppgjoerListe] = useApiCall(hentEtteroppgjoerListe)
  const etteroppgjoerDevKnappEnabled = useFeaturetoggle(FeatureToggle.etteroppgjoer_dev_opprett_forbehandling)
  const tilbakestillEtteroppgjoerEnabled = useFeaturetoggle(FeatureToggle.vis_tilbakestill_etteroppgjoer)

  useEffect(() => {
    if (isSuccess(sakResult)) {
      void fetchEtteroppgjoerListe(sakResult.data.sak.id.toString())
    }
  }, [sakResult])

  if (isFailure(fetchEtteroppgjoerListeResult)) {
    return <ApiErrorAlert>{fetchEtteroppgjoerListeResult.error.detail}</ApiErrorAlert>
  }

  if (
    !isSuccess(sakResult) ||
    !isSuccess(fetchEtteroppgjoerListeResult) ||
    !fetchEtteroppgjoerListeResult.data?.length
  ) {
    return null
  }

  const sakId = sakResult.data.sak.id
  const etteroppgjoerListe = fetchEtteroppgjoerListeResult.data

  return (
    <VStack gap="2" maxWidth="70rem">
      <Box padding="8">
        <Box marginBlock="0 8">
          <Heading size="medium">Etteroppgjoer Saksoveriskt</Heading>
        </Box>
        <Accordion>
          {etteroppgjoerListe
            .sort((a, b) => b.inntektsaar - a.inntektsaar)
            .map((etteroppgjoer) => {
              const currentIndex = steg.findIndex((s) => s.status.includes(etteroppgjoer.status))

              return (
                <Accordion.Item key={`${etteroppgjoer.inntektsaar}-${etteroppgjoer.status}`}>
                  <Accordion.Header>Etteroppgjør for {etteroppgjoer.inntektsaar}</Accordion.Header>
                  <Accordion.Content>
                    <Box padding="4">
                      <List as="ul">
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
                  </Accordion.Content>
                </Accordion.Item>
              )
            })}
        </Accordion>
      </Box>

      {etteroppgjoerDevKnappEnabled && (
        <Box padding="8">
          <OpprettEtteroppgjoerIDev sakId={sakId} />
        </Box>
      )}

      {tilbakestillEtteroppgjoerEnabled && (
        <Box padding="8">
          <TilbakestillOgOpprettNyForbehandling sakId={sakId} />
        </Box>
      )}
    </VStack>
  )
}

export default EtteroppgjoerSaksoversikt
