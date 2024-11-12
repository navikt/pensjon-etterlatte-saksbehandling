import { Alert, Box, Heading, HStack, VStack } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentBrev } from '~shared/api/brev'
import { BrevProsessType, BrevStatus, IBrev } from '~shared/types/Brev'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Column, GridContainer } from '~shared/styled'
import BrevTittel from '~components/person/brev/tittel/BrevTittel'
import BrevSpraak from '~components/person/brev/spraak/BrevSpraak'
import { BrevMottakerWrapper } from '~components/person/brev/mottaker/BrevMottakerWrapper'
import ForhaandsvisningBrev from '~components/behandling/brev/ForhaandsvisningBrev'
import RedigerbartBrev from '~components/behandling/brev/RedigerbartBrev'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'
import { InfobrevKnapperad } from '~components/aktivitetsplikt/brev/VurderingInfoBrevOgOppsummering'
import { FerdigstillAktivitetspliktBrevModal } from '~components/aktivitetsplikt/brev/FerdigstillModal'

export function Aktivitetspliktbrev({ brevId }: { brevId: number }) {
  const { oppgave, sistEndret } = useAktivitetspliktOppgaveVurdering()
  const [kanRedigeres, setKanRedigeres] = useState(false)

  const [brevStatus, apiHentBrev] = useApiCall(hentBrev)

  const hentBrevOgSetStatus = () => {
    apiHentBrev({ brevId: brevId, sakId: oppgave.sakId }, (brev) => {
      if ([BrevStatus.OPPRETTET, BrevStatus.OPPDATERT].includes(brev.status)) {
        setKanRedigeres(true)
      } else {
        setKanRedigeres(false)
      }
    })
  }

  const endringerHarKommetEtterBrevOpprettelse = (brev: IBrev) => {
    if (sistEndret) {
      return new Date(sistEndret).getTime() > new Date(brev.statusEndret).getTime()
    } else {
      return false
    }
  }

  useEffect(() => {
    hentBrevOgSetStatus()
  }, [brevId, oppgave.status])

  return mapResult(brevStatus, {
    pending: <Spinner label="Henter brev ..." />,
    error: (error) => <ApiErrorAlert>En feil oppsto ved henting av brev: {error.detail}</ApiErrorAlert>,
    success: (brev) => {
      const brevErFerdigstilt =
        brev.prosessType === BrevProsessType.OPPLASTET_PDF || brev.status === BrevStatus.DISTRIBUERT
      return (
        <GridContainer>
          <Column>
            {endringerHarKommetEtterBrevOpprettelse(brev) && (
              <Alert variant="warning">
                Vurdering av aktivitet eller valgene for infobrevet er oppdatert etter at brevet ble opprettet. Se nøye
                over brevet for å se om innholdet stemmer med nåværende verdier.
              </Alert>
            )}
            <VStack gap="4" margin="4">
              <Box marginInline="0 8">
                <Heading size="large">Infobrev aktivitetsplikt</Heading>
              </Box>
              <BrevTittel brevId={brev.id} sakId={brev.sakId} tittel={brev.tittel} kanRedigeres={kanRedigeres} />
              <BrevSpraak brev={brev} kanRedigeres={kanRedigeres} />

              <BrevMottakerWrapper brev={brev} kanRedigeres={kanRedigeres} />
            </VStack>
          </Column>
          <Column>
            {brevErFerdigstilt ? (
              <>
                <Box maxHeight="955px" width="100%" height="100%" marginBlock="0 16">
                  <ForhaandsvisningBrev brev={brev} />
                </Box>
                <InfobrevKnapperad />
              </>
            ) : (
              <>
                <HStack wrap={false}>
                  <RedigerbartBrev brev={brev} kanRedigeres={kanRedigeres} tilbakestillingsaction={() => undefined} />
                </HStack>
                <InfobrevKnapperad>
                  <FerdigstillAktivitetspliktBrevModal />
                </InfobrevKnapperad>
              </>
            )}
          </Column>
        </GridContainer>
      )
    },
  })
}
