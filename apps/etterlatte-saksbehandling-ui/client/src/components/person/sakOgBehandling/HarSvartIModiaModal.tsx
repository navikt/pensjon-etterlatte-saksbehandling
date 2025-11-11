import { BodyShort, Button, Heading, HStack, Modal, Textarea, VStack } from '@navikt/ds-react'
import { PersonChatIcon } from '@navikt/aksel-icons'
import React, { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettRevurdering as opprettRevurderingApi } from '~shared/api/revurdering'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'
import { useNavigate } from 'react-router-dom'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

export const HarSvartIModiaModal = ({ sakId }: { sakId: number }) => {
  const [aapen, setAapen] = useState<boolean>(false)

  const [opprettRevurderingResult, opprettRevurderingRequest, resetApiCall] = useApiCall(opprettRevurderingApi)

  const navigate = useNavigate()

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<{ begrunnelse: string }>({ defaultValues: { begrunnelse: '' } })

  const lukkModal = () => {
    resetApiCall()
    reset()
    setAapen(false)
  }

  const opprettRevurdering = (begrunnelse: string) => {
    opprettRevurderingRequest(
      {
        sakId,
        aarsak: Revurderingaarsak.ETTEROPPGJOER,
        begrunnelse,
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

            <Textarea
              {...register('begrunnelse', { required: { value: true, message: 'Du må gi en begrunnelse' } })}
              label="Begrunnelse"
              error={errors.begrunnelse?.message}
            />

            {isFailureHandler({
              apiResult: opprettRevurderingResult,
              errorMessage: 'Feil under opprettelse av revurdering',
            })}

            <HStack gap="4" justify="end">
              <Button
                loading={isPending(opprettRevurderingResult)}
                onClick={handleSubmit((data) => opprettRevurdering(data.begrunnelse))}
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
