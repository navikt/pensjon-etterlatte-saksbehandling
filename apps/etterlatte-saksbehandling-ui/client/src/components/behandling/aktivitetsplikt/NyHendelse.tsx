import { useApiCall } from '~shared/hooks/useApiCall'
import React, { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { isFailure, isPending } from '~shared/api/apiUtils'
import { Alert, Button, Heading, HStack, Textarea, VStack } from '@navikt/ds-react'
import { IAktivitetHendelse, IOpprettHendelse } from '~shared/types/Aktivitetsplikt'
import { opprettHendelse, opprettHendelseForSak } from '~shared/api/aktivitetsplikt'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { formatISO } from 'date-fns'

interface HendelseSkjemaValue {
  id?: string
  dato?: Date
  beskrivelse?: string
  sakId: number
  behandlingId?: string
}

function dtoTilSkjema(rediger: IAktivitetHendelse): HendelseSkjemaValue {
  return {
    id: rediger.id,
    dato: new Date(rediger.dato),
    beskrivelse: rediger.beskrivelse,
    sakId: rediger.sakId,
    behandlingId: rediger.behandlingId,
  }
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
  sakId: number
  avbryt: () => void
}) => {
  const [opprettHendelseResponse, opprettHendelseRequest] = useApiCall(opprettHendelse)
  const [opprettHendelseForSakResponse, opprettHendelseForSakRequest] = useApiCall(opprettHendelseForSak)
  const defaultValue: HendelseSkjemaValue = { sakId, behandlingId: behandling?.id }

  const {
    getValues,
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors },
  } = useForm<HendelseSkjemaValue>({
    defaultValues: redigerHendelse ? dtoTilSkjema(redigerHendelse) : defaultValue,
  })

  useEffect(() => {
    if (redigerHendelse) {
      reset(dtoTilSkjema(redigerHendelse))
    }
  }, [redigerHendelse])

  const submitHendelse = (opprettHendelse: HendelseSkjemaValue) => {
    const request: IOpprettHendelse = {
      beskrivelse: opprettHendelse.beskrivelse!,
      dato: formatISO(opprettHendelse.dato!, { representation: 'date' }),
      id: opprettHendelse.id,
      sakId: opprettHendelse.sakId,
    }

    if (behandling) {
      opprettHendelseRequest(
        {
          behandlingId: behandling.id,
          request: request,
        },
        (hendelser) => {
          oppdaterHendelser(hendelser)
        }
      )
    } else if (sakId) {
      opprettHendelseForSakRequest(
        {
          sakId: sakId,
          request: request,
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
                reset(defaultValue)
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