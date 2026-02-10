import { Alert, Box, Heading, HStack, VStack } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentBrev } from '~shared/api/brev'
import { BrevProsessType, BrevStatus, IBrev } from '~shared/types/Brev'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import BrevTittel from '~components/person/brev/tittel/BrevTittel'
import BrevSpraak from '~components/person/brev/spraak/BrevSpraak'
import { BrevMottakerWrapper } from '~components/person/brev/mottaker/BrevMottakerWrapper'
import ForhaandsvisningBrev from '~components/behandling/brev/ForhaandsvisningBrev'
import RedigerbartBrev from '~components/behandling/brev/RedigerbartBrev'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/AktivitetspliktOppgaveVurderingRoutes'
import { InfobrevKnapperad } from '~components/aktivitetsplikt/brev/VurderingInfoBrevOgOppsummering'
import { FerdigstillAktivitetspliktBrevModal } from '~components/aktivitetsplikt/brev/FerdigstillBrevModal'
import { LoependeUnntakInfo } from '~components/aktivitetsplikt/brev/LoependeUnntakInfo'
import { erOppgaveRedigerbar } from '~shared/types/oppgave'

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
        <HStack height="100%" minHeight="100vh" wrap={false}>
          <VStack gap="space-4" margin="space-4" minWidth="30rem">
            {endringerHarKommetEtterBrevOpprettelse(brev) && (
              <Alert variant="warning">
                Vurdering av aktivitet eller valgene for infobrevet er oppdatert etter at brevet ble opprettet. Se nøye
                over brevet for å se om innholdet stemmer med nåværende verdier.
              </Alert>
            )}
            <Box marginInline="space-0 space-8">
              <Heading size="large">Infobrev aktivitetsplikt</Heading>
            </Box>

            <LoependeUnntakInfo />
            <BrevTittel brevId={brev.id} sakId={brev.sakId} tittel={brev.tittel} kanRedigeres={kanRedigeres} />
            <BrevSpraak brev={brev} kanRedigeres={false} />
            {kanRedigeres && (
              <Alert size="small" variant="info">
                For å endre målform i brevet må det endres på forrige steg. Endring i brevvalg vil nullstille innholdet
                i brevet.
              </Alert>
            )}

            <BrevMottakerWrapper brev={brev} kanRedigeres={kanRedigeres} />
          </VStack>

          <Box borderWidth="0 1" borderColor="neutral-subtle">
            <VStack gap="space-4" width="50rem">
              {brevErFerdigstilt ? (
                <>
                  <Box maxHeight="955px" width="100%" height="100%" marginBlock="space-0 space-16">
                    <ForhaandsvisningBrev brev={brev} />
                  </Box>
                  <InfobrevKnapperad>
                    {erOppgaveRedigerbar(oppgave.status) ? <FerdigstillAktivitetspliktBrevModal /> : undefined}
                  </InfobrevKnapperad>
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
            </VStack>
          </Box>
        </HStack>
      )
    },
  })
}
