import { useKlage } from '~components/klage/useKlage'
import { useNavigate } from 'react-router-dom'
import { BodyShort, Box, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import Spinner from '~shared/Spinner'
import { erKlageRedigerbar, Klage } from '~shared/types/Klage'
import { useApiCall } from '~shared/hooks/useApiCall'
import { BrevProsessType, BrevStatus } from '~shared/types/Brev'
import ForhaandsvisningBrev from '~components/behandling/brev/ForhaandsvisningBrev'
import RedigerbartBrev from '~components/behandling/brev/RedigerbartBrev'
import { hentBrev } from '~shared/api/brev'
import styled from 'styled-components'
import { ApiErrorAlert } from '~ErrorBoundary'
import { JaNei } from '~shared/types/ISvar'
import { mapApiResult, mapSuccess } from '~shared/api/apiUtils'
import BrevTittel from '~components/person/brev/tittel/BrevTittel'
import { forrigeSteg } from '~components/klage/stegmeny/KlageStegmeny'
import { BrevMottakerWrapper } from '~components/person/brev/mottaker/BrevMottakerWrapper'

function hentBrevId(klage: Klage | null): number | null {
  switch (klage?.utfall?.utfall) {
    case 'DELVIS_OMGJOERING':
    case 'STADFESTE_VEDTAK':
      return klage.utfall.innstilling.brev.brevId
    case 'AVVIST':
      return klage.utfall.brev.brevId
    default:
      return null
  }
}

export function KlageBrev() {
  const navigate = useNavigate()
  const klage = useKlage()

  const brevId = hentBrevId(klage)
  const sakId = klage?.sak?.id
  const [hentetBrev, apiHentBrev] = useApiCall(hentBrev)
  const [tilbakestilt, setTilbakestilt] = useState(false)

  useEffect(() => {
    if (!sakId || !brevId) {
      return
    }
    void apiHentBrev({ brevId, sakId })
  }, [brevId, sakId, tilbakestilt])

  if (!klage) {
    return <Spinner label="Henter klage" />
  }

  const redigerbar = erKlageRedigerbar(klage)

  return (
    <>
      <BrevContent>
        <Sidebar>
          <Box paddingInline="space-16" paddingBlock="space-16 space-4">
            <Heading spacing level="1" size="large">
              Brev
            </Heading>
            <BodyShort spacing>
              {klage.formkrav?.formkrav.erKlagenFramsattInnenFrist === JaNei.JA
                ? 'Oversendelsesbrev til klager'
                : 'Avvisningsbrev til klager'}
            </BodyShort>
            {mapSuccess(hentetBrev, (brev) => (
              <VStack gap="space-4">
                <BrevTittel brevId={brev.id} sakId={brev.sakId} tittel={brev.tittel} kanRedigeres={redigerbar} />

                <BrevMottakerWrapper brev={brev} kanRedigeres={redigerbar} />
              </VStack>
            ))}
          </Box>
        </Sidebar>

        {mapApiResult(
          hentetBrev,
          <SpinnerContainer>
            <Spinner label="Henter brevet" />
          </SpinnerContainer>,
          () => (
            <ApiErrorAlert>Kunne ikke hente brevet. Prøv å laste siden på nytt</ApiErrorAlert>
          ),
          (brev) => {
            if (brev.status === BrevStatus.DISTRIBUERT || brev.prosessType !== BrevProsessType.REDIGERBAR) {
              return <ForhaandsvisningBrev brev={brev} />
            } else {
              return (
                <RedigerbartBrev
                  brev={brev}
                  kanRedigeres={redigerbar}
                  tilbakestillingsaction={() => setTilbakestilt(true)}
                />
              )
            }
          }
        )}
      </BrevContent>

      <Box paddingBlock="space-4 space-0" borderWidth="1 0 0 0">
        <HStack gap="space-4" justify="center">
          <Button className="button" variant="secondary" onClick={() => navigate(forrigeSteg(klage, 'brev'))}>
            Gå tilbake
          </Button>
          <Button className="button" variant="primary" onClick={() => navigate(`/klage/${klage.id}/oppsummering`)}>
            Se oppsummering
          </Button>
        </HStack>
      </Box>
    </>
  )
}

const BrevContent = styled.div`
  display: flex;
`

const SpinnerContainer = styled.div`
  height: 100%;
  width: 100%;
  text-align: center;
`

const Sidebar = styled.div`
  max-height: fit-content;
  min-width: 40%;
  width: 40%;
  border-right: 1px solid #c6c2bf;
`
