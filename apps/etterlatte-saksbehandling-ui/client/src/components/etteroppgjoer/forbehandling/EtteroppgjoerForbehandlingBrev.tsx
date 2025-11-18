import { useApiCall } from '~shared/hooks/useApiCall'
import { hentBrevTilBehandling, opprettBrevTilBehandling } from '~shared/api/brev'
import React, { useEffect, useState } from 'react'
import { Alert, BodyShort, Box, Button, ConfirmationPanel, Heading, HStack, Modal, VStack } from '@navikt/ds-react'
import { isPending, mapResult, mapSuccess } from '~shared/api/apiUtils'
import RedigerbartBrev from '~components/behandling/brev/RedigerbartBrev'
import { Link } from 'react-router-dom'
import { addEtteroppgjoerBrev, useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import Spinner from '~shared/Spinner'
import { EtteroppjoerForbehandlingSteg } from '~components/etteroppgjoer/forbehandling/stegmeny/EtteroppjoerForbehandlingStegmeny'
import { useAppDispatch } from '~store/Store'
import { ApiErrorAlert } from '~ErrorBoundary'
import { BrevMottakerWrapper } from '~components/person/brev/mottaker/BrevMottakerWrapper'
import { ferdigstillEtteroppgjoerForbehandlingMedBrev } from '~shared/api/etteroppgjoer'
import {
  DetaljertEtteroppgjoerForbehandling,
  kanRedigereEtteroppgjoerBehandling,
} from '~shared/types/EtteroppgjoerForbehandling'
import { navigerTilPersonOversikt } from '~components/person/lenker/navigerTilPersonOversikt'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { IBrev } from '~shared/types/Brev'
import BrevSpraak from '~components/person/brev/spraak/BrevSpraak'

export function EtteroppgjoerForbehandlingBrev() {
  const etteroppgjoer = useEtteroppgjoer()
  const dispatch = useAppDispatch()
  const [brevResult, fetchBrev] = useApiCall(hentBrevTilBehandling)
  const [opprettBrevResult, opprettBrevApi] = useApiCall(opprettBrevTilBehandling)
  const [modalOpen, setModalOpen] = useState(false)

  const [ferdigstillForbehandlingResult, ferdigstillForbehandlingRequest, resetFerdigstillForbehandlingStatus] =
    useApiCall(ferdigstillEtteroppgjoerForbehandlingMedBrev)

  const kanRedigeres = kanRedigereEtteroppgjoerBehandling(etteroppgjoer.behandling.status)
  const [tilbakestilt, setTilbakestilt] = useState(false)
  const [visAdvarselBehandlingEndret, setVisAdvarselBehandlingEndret] = useState(false)
  const [bekreftetSettOverBrev, setBekreftetSettOverBrev] = useState<boolean>(false)

  const lukkAdvarselBehandlingEndret = () => {
    setVisAdvarselBehandlingEndret(false)
  }

  const etteroppgjoerBeregnetEtterOpprettetBrev = (brev: IBrev, etteroppgjoer: DetaljertEtteroppgjoerForbehandling) => {
    if (etteroppgjoer.beregnetEtteroppgjoerResultat != undefined) {
      return (
        new Date(etteroppgjoer.beregnetEtteroppgjoerResultat?.tidspunkt).getTime() >
        new Date(brev.statusEndret).getTime()
      )
    }
    return false
  }

  const ferdigstillForbehandling = async () => {
    const forbehandlingId = etteroppgjoer.behandling.id
    ferdigstillForbehandlingRequest({ forbehandlingId }, () => {
      navigerTilPersonOversikt(etteroppgjoer.behandling.sak.ident)
    })
  }

  function lukkModal() {
    setBekreftetSettOverBrev(false)
    resetFerdigstillForbehandlingStatus()
    setModalOpen(false)
  }

  useEffect(() => {
    if (etteroppgjoer.behandling.brevId) {
      fetchBrev(etteroppgjoer.behandling.id, (brev) => {
        setVisAdvarselBehandlingEndret(etteroppgjoerBeregnetEtterOpprettetBrev(brev, etteroppgjoer))
      })
    } else {
      opprettBrevApi(
        {
          behandlingId: etteroppgjoer.behandling.id,
          sakId: etteroppgjoer.behandling.sak.id,
        },
        (brev) => dispatch(addEtteroppgjoerBrev(brev))
      )
    }
  }, [etteroppgjoer.behandling.brevId, tilbakestilt])

  return (
    <HStack height="100%" minHeight="100%" wrap={false}>
      <Modal
        open={modalOpen}
        onClose={lukkModal}
        header={{
          heading: 'Ferdigstill forbehandling etteroppgjør',
        }}
      >
        <Modal.Body>
          <VStack gap="4">
            <ConfirmationPanel
              name="BekreftSettOverBrev"
              checked={bekreftetSettOverBrev}
              label="Jeg har sett over redigerbart innhold inkludert vedlegg, og tilpasset det til brukers situasjon og inntekt"
              onChange={(e) => {
                setBekreftetSettOverBrev(e.currentTarget.checked)
              }}
            />
            <BodyShort spacing>
              Når forbehandlingen for etteroppgjøret ferdigstilles vil brevet sendes ut og forbehandlingen låses for
              endringer.
            </BodyShort>
          </VStack>
          {isFailureHandler({
            apiResult: ferdigstillForbehandlingResult,
            errorMessage: 'Kunne ikke ferdigstille forbehandling',
          })}
        </Modal.Body>
        <Modal.Footer>
          <Button
            variant="primary"
            onClick={ferdigstillForbehandling}
            loading={isPending(ferdigstillForbehandlingResult)}
            disabled={!bekreftetSettOverBrev}
          >
            Ferdigstill
          </Button>
          <Button
            variant="secondary"
            disabled={isPending(ferdigstillForbehandlingResult)}
            onClick={() => setModalOpen(false)}
          >
            Avbryt
          </Button>
        </Modal.Footer>
      </Modal>
      <Box minWidth="30rem" maxWidth="40rem" borderColor="border-subtle" borderWidth="0 1 0 0">
        <VStack gap="4" margin="16">
          <Heading level="1" size="large">
            Forhåndsvarsel
          </Heading>

          {kanRedigeres && (
            <Box marginBlock="0 2">
              <Alert variant="info" size="small">
                Husk å se over redigerbart innhold i brevet slik at det er tilpasset brukers situasjon og inntekt.
              </Alert>
            </Box>
          )}
          {visAdvarselBehandlingEndret && (
            <Alert variant="warning">
              Behandling er redigert etter brevet ble opprettet. Gå gjennom brevet og vurder om det bør tilbakestilles
              for å få oppdaterte verdier fra behandlingen.
            </Alert>
          )}

          {mapSuccess(brevResult, (brev) => (
            <>
              <BrevSpraak brev={brev} kanRedigeres={kanRedigeres} />
              <BrevMottakerWrapper brev={brev} kanRedigeres={kanRedigeres} />
            </>
          ))}
        </VStack>
      </Box>
      <VStack width="100%">
        {mapResult(opprettBrevResult, {
          pending: <Spinner label="Oppretter brev for etteroppgjør forbehandling" />,
          error: (error) => (
            <ApiErrorAlert>
              Kunne ikke opprette brevet for forbehandlingen for etteroppgjøret, på grunn av feil: {error.detail}
            </ApiErrorAlert>
          ),
        })}
        {mapResult(brevResult, {
          success: (brev) => (
            <RedigerbartBrev
              brev={brev}
              kanRedigeres={kanRedigeres}
              skalGaaViaBehandling
              lukkAdvarselBehandlingEndret={lukkAdvarselBehandlingEndret}
              tilbakestillingsaction={() => {
                setTilbakestilt(true)
              }}
            />
          ),
          error: (error) => (
            <ApiErrorAlert>
              Kunne ikke hente ut brevet for etteroppgjør-forbehandlingen, på grunn av feil: {error.detail}
            </ApiErrorAlert>
          ),
          pending: <Spinner label="Laster brev" />,
        })}

        <Box borderWidth="1 0 0 0" borderColor="border-subtle" paddingBlock="8 16">
          <HStack width="100%" justify="center" gap="6">
            <div>
              <Button
                as={Link}
                to={`/etteroppgjoer/${etteroppgjoer.behandling.id}/${EtteroppjoerForbehandlingSteg.OVERSIKT}`}
                variant="secondary"
              >
                Gå tilbake
              </Button>
            </div>

            {kanRedigeres && (
              <div>
                <Button variant="primary" onClick={() => setModalOpen(true)}>
                  Ferdigstill
                </Button>
              </div>
            )}
          </HStack>
        </Box>
      </VStack>
    </HStack>
  )
}
