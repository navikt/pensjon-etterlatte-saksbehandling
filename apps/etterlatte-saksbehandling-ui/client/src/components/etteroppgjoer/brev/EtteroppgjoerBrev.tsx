import { useApiCall } from '~shared/hooks/useApiCall'
import { ferdigstillBrev, hentBrevTilBehandling, opprettBrevTilBehandling } from '~shared/api/brev'
import React, { useEffect } from 'react'
import { Box, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import { isPending, mapResult, mapSuccess } from '~shared/api/apiUtils'
import RedigerbartBrev from '~components/behandling/brev/RedigerbartBrev'
import { Link } from 'react-router-dom'
import { addEtteroppgjoerBrev, useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import Spinner from '~shared/Spinner'
import { EtteroppgjoerFramTilbakeKnapperad } from '~components/etteroppgjoer/stegmeny/EtteroppgjoerFramTilbakeKnapperad'
import { EtteroppjoerSteg } from '~components/etteroppgjoer/stegmeny/EtteroppjoerForbehandlingSteg'
import { useAppDispatch } from '~store/Store'
import { ApiErrorAlert } from '~ErrorBoundary'
import { BrevMottakerWrapper } from '~components/person/brev/mottaker/BrevMottakerWrapper'
import { Brevtype } from '~shared/types/Brev'
import {
  ferdigstillEtteroppgjoerForbehandlingVarselbrev,
  oppdaterStatusPaaEtteroppgjoerForbehandling,
} from '~shared/api/etteroppgjoer'
import { EtteroppgjoerForbehandlingStatus } from '~shared/types/Etteroppgjoer'

export function EtteroppgjoerBrev() {
  const etteroppgjoer = useEtteroppgjoer()
  const dispatch = useAppDispatch()

  const [hentBrevTilBehandlingResult, hentBrevTilBehandlingFetch] = useApiCall(hentBrevTilBehandling)
  const [opprettBrevTilBehandlingResult, opprettBrevTilBehandlingRequest] = useApiCall(opprettBrevTilBehandling)
  const [
    ferdigstillEtteroppgjoerForbehandlingVarselbrevResult,
    ferdigstillEtteroppgjoerForbehandlingVarselbrevRequest,
  ] = useApiCall(ferdigstillEtteroppgjoerForbehandlingVarselbrev)

  const [oppdaterStatusPaaEtteroppgjoerForbehandlingResult, oppdaterStatusPaaEtteroppgjoerForbehandlingRequest] =
    useApiCall(oppdaterStatusPaaEtteroppgjoerForbehandling)

  const ferdigstillForbehandling = () => {
    ferdigstillEtteroppgjoerForbehandlingVarselbrevRequest(
      {
        forbehandlingId: etteroppgjoer.behandling.id,
      },
      () => {
        oppdaterStatusPaaEtteroppgjoerForbehandlingRequest(
          {
            forbehandlingId: etteroppgjoer.behandling.id,
            nyStatus: EtteroppgjoerForbehandlingStatus.VARSELBREV_SENDT,
          },
          () => {
            // TODO, sende bruker til person oversikten
          }
        )
      }
    )
  }

  useEffect(() => {
    if (etteroppgjoer.behandling.brevId) {
      hentBrevTilBehandlingFetch(etteroppgjoer.behandling.id)
    } else {
      opprettBrevTilBehandlingRequest(
        {
          behandlingId: etteroppgjoer.behandling.id,
          sakId: etteroppgjoer.behandling.sak.id,
        },
        (brev) => dispatch(addEtteroppgjoerBrev(brev))
      )
    }
  }, [etteroppgjoer.behandling.brevId])

  return (
    <HStack height="100%" minHeight="100%" wrap={false}>
      <Box minWidth="30rem" maxWidth="40rem" borderColor="border-subtle" borderWidth="0 1 0 0">
        <VStack gap="4" margin="16">
          <Heading level="1" size="large">
            Brev
          </Heading>
          {mapSuccess(hentBrevTilBehandlingResult, (brev) => (
            <BrevMottakerWrapper brev={brev} kanRedigeres />
          ))}
        </VStack>
      </Box>
      <VStack width="100%">
        {mapResult(opprettBrevTilBehandlingResult, {
          pending: <Spinner label="Oppretter brev for etteroppgjør forbehandling" />,
          error: (error) => (
            <ApiErrorAlert>
              Kunne ikke opprette brevet for forbehandlingen for etteroppgjøret, på grunn av feil: {error.detail}
            </ApiErrorAlert>
          ),
        })}
        {mapResult(hentBrevTilBehandlingResult, {
          success: (brev) => (
            <RedigerbartBrev
              brev={brev}
              kanRedigeres={true}
              skalGaaViaBehandling
              tilbakestillingsaction={() => alert('Not supported')}
            />
          ),
          error: (error) => (
            <ApiErrorAlert>
              Kunne ikke hente ut brevet for etteroppgjør-forbehandlingen, på grunn av feil: {error.detail}
            </ApiErrorAlert>
          ),
          pending: <Spinner label="Laster brev" />,
        })}
        <EtteroppgjoerFramTilbakeKnapperad>
          <div>
            <Button
              as={Link}
              to={`/etteroppgjoer/${etteroppgjoer.behandling.id}/${EtteroppjoerSteg.OVERSIKT}`}
              variant="secondary"
            >
              Gå tilbake
            </Button>
          </div>
          <div>
            <Button
              variant="primary"
              onClick={ferdigstillForbehandling}
              loading={
                isPending(ferdigstillEtteroppgjoerForbehandlingVarselbrevResult) ||
                isPending(oppdaterStatusPaaEtteroppgjoerForbehandlingResult)
              }
            >
              Ferdigstill
            </Button>
          </div>
        </EtteroppgjoerFramTilbakeKnapperad>
      </VStack>
    </HStack>
  )
}
