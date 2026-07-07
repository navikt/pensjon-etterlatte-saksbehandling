import { Alert, BodyShort, Button, Heading, HStack, Radio, RadioGroup, VStack } from '@navikt/ds-react'
import { PencilIcon } from '@navikt/aksel-icons'
import React, { useState } from 'react'
import { IBehandlingReducer, oppdaterSendeBrev } from '~store/reducers/BehandlingReducer'
import { Controller, useForm } from 'react-hook-form'
import { isFailure, isPending } from '~shared/api/apiUtils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { redigerSendeBrev } from '~shared/api/behandling'
import { useAppDispatch } from '~store/Store'

export interface ISendBrev {
  sendBrev: boolean
}

export const SkalSendeBrev = (props: { behandling: IBehandlingReducer; behandlingRedigerbart: boolean }) => {
  const { behandling, behandlingRedigerbart } = props

  const dispatch = useAppDispatch()
  const [skalSendeBrevStatus, skalSendeBrevRequest] = useApiCall(redigerSendeBrev)

  const [redigere, setRedigere] = useState<boolean>(false)

  const { handleSubmit, control } = useForm<ISendBrev>({
    defaultValues: {
      sendBrev: behandling.sendeBrev,
    },
  })

  const lagreSkalSendeBrev = (sendeBrev: ISendBrev) => {
    skalSendeBrevRequest(
      {
        behandlingId: behandling.id,
        sendBrev: sendeBrev,
      },
      () => {
        dispatch(oppdaterSendeBrev(sendeBrev))
        setRedigere(false)
      }
    )
  }

  return (
    <VStack gap="space-16">
      <div>
        <Heading size="small" level="2" spacing>
          Skal sende vedtaksbrev
        </Heading>
        {redigere ? (
          <form onSubmit={handleSubmit(lagreSkalSendeBrev)}>
            <Controller
              control={control}
              name="sendBrev"
              render={({ field }) => (
                <RadioGroup legend="" {...field}>
                  <Radio value={true}>Ja</Radio>
                  <Radio value={false}>Nei</Radio>
                </RadioGroup>
              )}
            />
            {isFailure(skalSendeBrevStatus) && <Alert variant="error">{skalSendeBrevStatus.error.detail}</Alert>}
            <HStack gap="space-16" marginBlock="space-16 space-0">
              <Button type="submit" loading={isPending(skalSendeBrevStatus)} variant="primary" size="small">
                Lagre
              </Button>
              <Button variant="secondary" size="small" onClick={() => setRedigere(false)}>
                Avbryt
              </Button>
            </HStack>
          </form>
        ) : (
          <BodyShort>{behandling.sendeBrev ? 'Ja' : 'Nei'}</BodyShort>
        )}
      </div>
      {!redigere && behandlingRedigerbart && (
        <HStack>
          <Button
            variant="secondary"
            icon={<PencilIcon title="a11y-title" fontSize="1.5rem" />}
            size="small"
            onClick={() => setRedigere(true)}
          >
            Rediger
          </Button>
        </HStack>
      )}
    </VStack>
  )
}
