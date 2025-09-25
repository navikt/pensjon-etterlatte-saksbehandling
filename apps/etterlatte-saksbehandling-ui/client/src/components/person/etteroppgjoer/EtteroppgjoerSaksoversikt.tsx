import { Box, List, VStack } from '@navikt/ds-react'
import { isSuccess, Result } from '~shared/api/apiUtils'
import { SakMedBehandlinger } from '~components/person/typer'
import React, { ReactNode, useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentEtteroppgjoer } from '~shared/api/etteroppgjoer'
import { ArrowRightIcon, CheckmarkCircleIcon, CircleIcon } from '@navikt/aksel-icons'

const steps = [
  {
    index: 1,
    status: 'VENTER_PAA_SKATTEOPPGJOER',
    text: () => 'Venter på skatteoppgjøret',
  },
  {
    index: 2,
    status: 'MOTTATT_SKATTEOPPGJOER',
    text: () => 'Mottatt skatteoppgjør',
  },
  {
    index: 3,
    status: 'UNDER_FORBEHANDLING',
    text: () => 'Etteroppgjøret er under forbehandling',
  },
  {
    index: 4,
    status: 'VENTER_PAA_SVAR',
    text: () => 'Varselbrev sendt, venter på svar fra bruker',
  },
  {
    index: 5,
    status: 'UNDER_REVURDERING',
    text: () => 'Behandler mottatt svar',
  },
  {
    index: 6,
    status: 'FERDIGSTILT',
    text: () => 'Etteroppgjøret er ferdigstilt',
  },
]

const getIcon = (current: number, target: number) => {
  if (current > target) return <CheckmarkCircleIcon />
  if (current === target) return <ArrowRightIcon />
  return <CircleIcon />
}

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
        {isSuccess(hentEtteroppgjoerResponse) &&
          hentEtteroppgjoerResponse.data.map((etteroppgjoer, index) => {
            const currentLevel = steps.find((s) => s.status === etteroppgjoer.status)?.index ?? 0

            return (
              <div key={index}>
                <h1>Etteroppgjør for {etteroppgjoer.inntektsaar}</h1>
                <List as="ul">
                  {steps.map(({ status, text, index: stepIndex }) => (
                    <List.Item
                      key={status}
                      icon={getIcon(currentLevel, stepIndex)}
                      style={{ color: currentLevel >= stepIndex ? 'black' : 'gray' }}
                    >
                      {text()}
                    </List.Item>
                  ))}
                </List>
              </div>
            )
          })}
      </Box>
    </VStack>
  )
}

export default EtteroppgjoerSaksoversikt
