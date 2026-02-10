import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useEffect, useState } from 'react'
import { erFerdigBehandlet } from '~components/behandling/felles/utils'
import { BodyShort, Button, Heading, HStack, Modal, VStack } from '@navikt/ds-react'
import { AarsakTilAvsluttingRevurdering } from '~shared/types/AnnullerBehandling'
import { useApiCall } from '~shared/hooks/useApiCall'
import { avbrytBehandling, hentSak } from '~shared/api/behandling'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { navigerTilPersonOversikt } from '~components/person/lenker/navigerTilPersonOversikt'
import { PersonOversiktFane } from '~components/person/Person'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'

export const AvsluttEtteroppgjoerRevurderingModal = ({
  behandling,
  beskrivelseAvUgunst,
}: {
  behandling: IDetaljertBehandling
  beskrivelseAvUgunst: string | undefined
}) => {
  const [erAapen, setErAapen] = useState<boolean>(false)

  const [sakResult, sakRequest] = useApiCall(hentSak)

  const [avbrytBehandlingResult, avbrytBehandlingRequest] = useApiCall(avbrytBehandling)

  const avsluttEtteroppgjoerRevurdering = (ident: string) => {
    avbrytBehandlingRequest(
      {
        id: behandling.id,
        avbrytBehandlingRequest: {
          aarsakTilAvbrytelse: AarsakTilAvsluttingRevurdering.ETTEROPPGJOER_ENDRING_ER_TIL_UGUNST,
          kommentar: beskrivelseAvUgunst ?? '',
        },
      },
      () => {
        navigerTilPersonOversikt(ident, PersonOversiktFane.SAKER)
      }
    )
  }

  useEffect(() => {
    if (erAapen) sakRequest(behandling.sakId.toString())
  }, [erAapen])

  return (
    !erFerdigBehandlet(behandling.status) && (
      <>
        <Button variant="danger" onClick={() => setErAapen(true)}>
          Avslutt revurdering
        </Button>

        {mapResult(sakResult, {
          pending: <Spinner label="Henter sak..." />,
          error: (error) => <ApiErrorAlert>{error.detail ?? 'Kunne ikke hente sak'}</ApiErrorAlert>,
          success: ({ ident }) => (
            <Modal
              open={erAapen}
              onClose={() => setErAapen(false)}
              aria-labelledby="Modal for avslutting av etteroppgjør revurdering"
              width={600}
            >
              <Modal.Header>
                <Heading level="1" size="medium" spacing>
                  Er du sikker på at du vil avslutte revurderingen?
                </Heading>
              </Modal.Header>
              <Modal.Body>
                <VStack gap="space-4">
                  <BodyShort>Du får oppgave om ny forbehandling når du avslutter revurderingen.</BodyShort>

                  {isFailureHandler({
                    apiResult: avbrytBehandlingResult,
                    errorMessage: 'Kunne ikke avslutte revurdering',
                  })}
                </VStack>
              </Modal.Body>
              <Modal.Footer>
                <HStack gap="space-4" justify="end">
                  <Button
                    variant="secondary"
                    onClick={() => setErAapen(false)}
                    disabled={isPending(avbrytBehandlingResult)}
                  >
                    Nei, fortsett revurderingen
                  </Button>
                  <Button
                    variant="danger"
                    onClick={() => avsluttEtteroppgjoerRevurdering(ident)}
                    loading={isPending(avbrytBehandlingResult)}
                  >
                    Ja, avslutt
                  </Button>
                </HStack>
              </Modal.Footer>
            </Modal>
          ),
        })}
      </>
    )
  )
}
