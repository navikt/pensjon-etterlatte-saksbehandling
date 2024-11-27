import { useApiCall } from '~shared/hooks/useApiCall'
import React, { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { isFailure, isPending } from '~shared/api/apiUtils'
import { Alert, Button, Heading, HStack, Select, Textarea, VStack } from '@navikt/ds-react'
import { IAktivitetHendelse, IAktivitetPeriode, IOpprettHendelse } from '~shared/types/Aktivitetsplikt'
import { opprettHendelse, opprettHendelseForSak } from '~shared/api/aktivitetsplikt'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'

interface HendelseDefaultValue {
  id: string | undefined
  dato?: string
  beskrivelse: string
}

const hendelseDefaultValue: HendelseDefaultValue = {
  id: undefined,
  dato: undefined,
  beskrivelse: '',
}

export const NyHendelse = ({
  redigerHendelse,
  oppdaterHendelser,
  behandling,
  sakId,
  avbryt,
}: {
  redigerHendelse: IAktivitetHendelse | undefined
  oppdaterHendelser: (hendelser: IAktivitetHendelse[]) => void
  behandling?: IBehandlingReducer
  sakId: number | undefined
  avbryt: () => void
}) => {
  const [opprettHendelseResponse, opprettHendelseRequest] = useApiCall(opprettHendelse)
  const [opprettHendelseForSakResponse, opprettHendelseForSakRequest] = useApiCall(opprettHendelseForSak)
  const {
    getValues,
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors },
  } = useForm<IOpprettHendelse>({
    defaultValues: { ...hendelseDefaultValue, sakId },
  })

  useEffect(() => {
    if (redigerHendelse) {
      reset({
        id: redigerHendelse.id,
        dato: redigerHendelse.dato,
        beskrivelse: redigerHendelse.beskrivelse,
      })
    }
  }, [redigerHendelse])

  const submitHendelse = (opprettHendelse: IOpprettHendelse) => {
    if (behandling) {
      opprettHendelseRequest(
        {
          behandlingId: behandling.id,
          request: opprettHendelse,
        },
        (hendelser) => {
          oppdaterHendelser(hendelser)
        }
      )
    } else if (sakId) {
      opprettHendelseForSakRequest(
        {
          sakId: sakId,
          request: opprettHendelse,
        },
        (hendelser) => {
          oppdaterHendelser(hendelser)
        }
      )
    }
  }

  return (
    <>
      <form onSubmit={handleSubmit(submitHendelse)}>
        <Heading size="small" level="3" spacing>
          {getValues('id') ? 'Endre' : 'Ny'} aktivitet
        </Heading>
        <VStack gap="4">
          <HStack gap="4">
            <ControlledDatoVelger name="dato" label="Dato" control={control} errorVedTomInput="Obligatorisk" />
          </HStack>
          <HStack>
            <Textarea
              style={{ width: '630px' }}
              {...register('beskrivelse', {
                required: { value: true, message: 'Må fylles ut' },
              })}
              label="Beskrivelse"
              error={errors.beskrivelse?.message}
            />
          </HStack>
          <HStack gap="4">
            <Button
              size="small"
              variant="secondary"
              type="button"
              onClick={(e) => {
                e.preventDefault()
                reset(hendelseDefaultValue)
                avbryt()
              }}
            >
              Avbryt
            </Button>
            <Button
              size="small"
              variant="primary"
              type="submit"
              loading={isPending(opprettHendelseResponse) || isPending(opprettHendelseForSakResponse)}
            >
              Lagre
            </Button>
          </HStack>
        </VStack>
      </form>
      {isFailure(opprettHendelseResponse) && (
        <Alert variant="error">
          {opprettHendelseResponse.error.detail || 'Det skjedde en feil ved lagring av hendelse'}
        </Alert>
      )}

      {isFailure(opprettHendelseForSakResponse) && (
        <Alert variant="error">
          {opprettHendelseForSakResponse.error.detail || 'Det skjedde en feil ved lagring av hendelse'}
        </Alert>
      )}
    </>
  )
}
