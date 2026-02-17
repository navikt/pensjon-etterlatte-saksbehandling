import { BodyShort, Button, Heading, HStack, Modal, VStack } from '@navikt/ds-react'
import { PersonChatIcon } from '@navikt/aksel-icons'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettRevurderingEtteroppgjoer } from '~shared/api/revurdering'
import { useNavigate } from 'react-router-dom'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { Opprinnelse } from '~shared/types/IDetaljertBehandling'
import { VelgEtteroppgjoersAar } from '~components/etteroppgjoer/components/utils/VelgEtteroppgjoersAar'

export const HarSvartIModiaModal = ({ sakId }: { sakId: number }) => {
  const [aapen, setAapen] = useState<boolean>(false)
  const [valgtEtteroppgjoer, setValgtEtteroppgjoer] = useState<string>('')

  const [opprettRevurderingResult, opprettRevurderingRequest, resetApiCall] = useApiCall(
    opprettRevurderingEtteroppgjoer
  )

  const navigate = useNavigate()

  const lukkModal = () => {
    resetApiCall()
    setValgtEtteroppgjoer('')
    setAapen(false)
  }

  const opprettRevurdering = () => {
    if (!valgtEtteroppgjoer) return

    opprettRevurderingRequest(
      {
        sakId,
        opprinnelse: Opprinnelse.SVAR_I_MODIA,
        inntektsaar: valgtEtteroppgjoer,
      },
      (revurderingId) => navigate(`/behandling/${revurderingId}/`)
    )
  }

  return (
    <>
      <Button variant="secondary" icon={<PersonChatIcon />} iconPosition="right" onClick={() => setAapen(true)}>
        Har svart i Modia
      </Button>

      <Modal open={aapen} onClose={lukkModal} aria-labelledby="Bruker har svart på etteroppgjør i Modia modal">
        <Modal.Header closeButton>
          <Heading size="medium" level="2">
            Bruker har svart på etteroppgjør i Modia
          </Heading>
        </Modal.Header>

        <Modal.Body>
          <VStack gap="4">
            <BodyShort>
              Hvis bruker har svart på etteroppgjøret i Modia kan du opprette revurdering for etteroppgjøret her.
            </BodyShort>

            <VelgEtteroppgjoersAar
              sakId={sakId.toString()}
              value={valgtEtteroppgjoer}
              onChange={setValgtEtteroppgjoer}
            />

            {isFailureHandler({
              apiResult: opprettRevurderingResult,
              errorMessage: 'Feil under opprettelse av revurdering',
            })}

            <HStack gap="4" justify="end">
              <Button
                loading={isPending(opprettRevurderingResult)}
                disabled={!valgtEtteroppgjoer}
                onClick={opprettRevurdering}
              >
                Opprett revurdering
              </Button>

              <Button variant="secondary" onClick={lukkModal} disabled={isPending(opprettRevurderingResult)}>
                Avbryt
              </Button>
            </HStack>
          </VStack>
        </Modal.Body>
      </Modal>
    </>
  )
}
