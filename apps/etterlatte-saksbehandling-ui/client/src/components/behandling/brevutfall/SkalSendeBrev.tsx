import { Alert, BodyShort, Button, Heading, HStack, Radio, RadioGroup } from '@navikt/ds-react'
import { PencilIcon } from '@navikt/aksel-icons'
import React, { useState } from 'react'
import { IBehandlingReducer, oppdaterSendeBrev } from '~store/reducers/BehandlingReducer'
import styled from 'styled-components'
import { Controller, useForm } from 'react-hook-form'
import { isFailure, isPending } from '~shared/api/apiUtils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { redigerSendeBrev } from '~shared/api/behandling'
import { useAppDispatch } from '~store/Store'

export interface ISendBrev {
  sendBrev: boolean
}

export const SkalSendeBrev = (props: { behandling: IBehandlingReducer }) => {
  // TODO redigerbart
  const { behandling } = props

  const dispatch = useAppDispatch()
  const [requestStatus, redigereRequest] = useApiCall(redigerSendeBrev)

  const [redigere, setRedigere] = useState<boolean>(false)

  const { handleSubmit, control } = useForm<ISendBrev>({
    defaultValues: {
      sendBrev: behandling.sendeBrev,
    },
  })
  const submit = (sendeBrev: ISendBrev) => {
    redigereRequest(
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
    <SkalSendeBrevContent>
      <Heading size="small">Skal sende vedtaksbrev</Heading>

      {redigere ? (
        <form onSubmit={handleSubmit(submit)}>
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
          {isFailure(requestStatus) && <Alert variant="error">{requestStatus.error.detail}</Alert>}
          <HStack gap="4">
            <Button type="submit" loading={isPending(requestStatus)} variant="primary" size="small">
              Lagre
            </Button>
            <Button variant="secondary" size="small" onClick={() => setRedigere(false)}>
              Avbryt
            </Button>
          </HStack>
        </form>
      ) : (
        <>
          <InnholdWrapper>
            <BodyShort>{behandling.sendeBrev ? 'Ja' : 'Nei'}</BodyShort>
          </InnholdWrapper>
          <Button
            variant="secondary"
            icon={<PencilIcon title="a11y-title" fontSize="1.5rem" />}
            size="small"
            onClick={() => setRedigere(true)}
          >
            Rediger
          </Button>
        </>
      )}
    </SkalSendeBrevContent>
  )
}

const SkalSendeBrevContent = styled.div`
  margin-top: 4em;
  margin-bottom: 2em;
  max-width: 500px;
`
const InnholdWrapper = styled.div`
  margin-top: 0.5em;
  margin-bottom: 0.5em;
`
