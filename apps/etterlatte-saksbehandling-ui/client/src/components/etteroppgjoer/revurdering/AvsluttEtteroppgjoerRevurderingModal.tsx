import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { erFerdigBehandlet } from '~components/behandling/felles/utils'
import { BodyShort, Button, Heading, HStack, Modal } from '@navikt/ds-react'
import { AarsakTilAvsluttingRevurdering } from '~shared/types/AnnullerBehandling'
import { useApiCall } from '~shared/hooks/useApiCall'
import { avbrytBehandling } from '~shared/api/behandling'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

export const AvsluttEtteroppgjoerRevurderingModal = ({
  behandling,
  beskrivelseAvUgunst,
}: {
  behandling: IDetaljertBehandling
  beskrivelseAvUgunst: string | undefined
}) => {
  const navigate = useNavigate()

  const [erAapen, setErAapen] = useState<boolean>(false)

  const [avbrytBehandlingResult, avbrytBehandlingRequest] = useApiCall(avbrytBehandling)

  const avsluttEtteroppgjoerRevurdering = () => {
    avbrytBehandlingRequest(
      {
        id: behandling.id,
        avbrytBehandlingRequest: {
          aarsakTilAvbrytelse: AarsakTilAvsluttingRevurdering.ETTEROPPGJOER_ENDRING_ER_TIL_UGUNST,
          kommentar: beskrivelseAvUgunst ?? '',
        },
      },
      () => {
        navigate('/')
      }
    )
  }

  return (
    !erFerdigBehandlet(behandling.status) && (
      <>
        <Button variant="danger" onClick={() => setErAapen(true)}>
          Avslutt revurdering
        </Button>

        <Modal
          open={erAapen}
          onClose={() => setErAapen(false)}
          aria-labelledby="Modal for avslutting av etteroppgjør revurdering"
        >
          <Modal.Header>
            <Heading level="1" size="medium" spacing>
              Er du sikker på at du vil avslutte revurderingen?
            </Heading>
          </Modal.Header>
          <Modal.Body>
            <BodyShort>Husk å opprette ny forbehandling når du har avsluttet revurderingen.</BodyShort>

            {isFailureHandler({
              apiResult: avbrytBehandlingResult,
              errorMessage: 'Kunne ikke avslutte revurdering',
            })}

            <HStack gap="4" justify="end">
              <Button
                variant="secondary"
                onClick={() => setErAapen(false)}
                disabled={isPending(avbrytBehandlingResult)}
              >
                Nei, fortsett revurderingen
              </Button>
              <Button
                variant="danger"
                onClick={avsluttEtteroppgjoerRevurdering}
                loading={isPending(avbrytBehandlingResult)}
              >
                Ja, avslutt
              </Button>
            </HStack>
          </Modal.Body>
        </Modal>
      </>
    )
  )
}
