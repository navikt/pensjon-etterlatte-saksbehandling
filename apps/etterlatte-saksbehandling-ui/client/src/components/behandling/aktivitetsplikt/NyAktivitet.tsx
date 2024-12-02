import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { useApiCall } from '~shared/hooks/useApiCall'
import React, { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { formatISO } from 'date-fns'
import { isFailure, isPending } from '~shared/api/apiUtils'
import { Alert, Button, Heading, HStack, Select, Textarea, VStack } from '@navikt/ds-react'
import { AktivitetspliktType, IAktivitetPeriode, IOpprettAktivitet } from '~shared/types/Aktivitetsplikt'
import { opprettAktivitet, opprettAktivitetForSak } from '~shared/api/aktivitetsplikt'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { mapAktivitetstypeProps } from '~components/behandling/aktivitetsplikt/AktivitetspliktTidslinje'

function dtoTilSkjema(periode: IAktivitetPeriode): AktivitetSkjemaValue {
  return {
    id: periode.id,
    type: periode.type,
    datoFom: periode.fom ? new Date(periode.fom) : undefined,
    datoTom: periode.tom ? new Date(periode.tom) : null,
    behandlingId: periode.behandlingId,
    sakId: periode.sakId,
    beskrivelse: periode.beskrivelse,
  }
}

interface AktivitetSkjemaValue {
  id?: string
  type?: AktivitetspliktType
  datoFom?: Date
  datoTom?: Date | null
  sakId: number
  behandlingId?: string
  beskrivelse?: string
}

export const NyAktivitet = ({
  oppdaterAktiviteter,
  sakId,
  avbryt,
  redigerAktivitet,
  behandling = undefined,
}: {
  oppdaterAktiviteter: (aktiviteter: IAktivitetPeriode[]) => void
  sakId: number
  avbryt: () => void
  redigerAktivitet: IAktivitetPeriode | undefined
  behandling?: IBehandlingReducer
}) => {
  const [opprettAktivitetResponse, opprettAktivitetRequest] = useApiCall(opprettAktivitet)
  const [opprettAktivitetForSakResponse, opprettAktivitetForSakRequest] = useApiCall(opprettAktivitetForSak)
  const defaultValue: AktivitetSkjemaValue = { sakId, behandlingId: behandling?.id }
  const {
    getValues,
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors },
  } = useForm<AktivitetSkjemaValue>({
    defaultValues: redigerAktivitet ? dtoTilSkjema(redigerAktivitet) : defaultValue,
  })

  useEffect(() => {
    if (redigerAktivitet) {
      reset(dtoTilSkjema(redigerAktivitet))
    }
  }, [redigerAktivitet])

  const submitAktivitet = (data: AktivitetSkjemaValue) => {
    const { id, type, datoFom, datoTom, beskrivelse } = data

    const opprettAktivitet: IOpprettAktivitet = {
      id: id,
      sakId: behandling ? behandling.sakId : sakId!!,
      type: type as AktivitetspliktType,
      fom: formatISO(datoFom!, { representation: 'date' }),
      tom: datoTom ? formatISO(datoTom, { representation: 'date' }) : undefined,
      beskrivelse: beskrivelse!,
    }

    if (behandling) {
      opprettAktivitetRequest(
        {
          behandlingId: behandling.id,
          request: opprettAktivitet,
        },
        (aktiviteter) => {
          reset({})
          oppdaterAktiviteter(aktiviteter)
        }
      )
    } else if (sakId) {
      opprettAktivitetForSakRequest(
        {
          sakId: sakId,
          request: opprettAktivitet,
        },
        (aktiviteter) => {
          reset({})
          oppdaterAktiviteter(aktiviteter)
        }
      )
    }
  }

  return (
    <>
      <form onSubmit={handleSubmit(submitAktivitet)}>
        <Heading size="small" level="3" spacing>
          {getValues('id') ? 'Endre' : 'Ny'} aktivitet
        </Heading>
        <VStack gap="4">
          <HStack gap="4">
            <ControlledDatoVelger name="datoFom" label="Fra dato" control={control} errorVedTomInput="Obligatorisk" />
            <ControlledDatoVelger name="datoTom" label="Dato til og med" control={control} required={false} />
            <Select
              {...register('type', {
                required: { value: true, message: 'Du må velge aktivitetstype' },
              })}
              label="Aktivitetstype"
              error={errors.type?.message}
            >
              <option value="">Velg aktivitet</option>
              {Object.keys(AktivitetspliktType).map((type, index) => (
                <option key={index} value={type}>
                  {mapAktivitetstypeProps(type as AktivitetspliktType).beskrivelse}
                </option>
              ))}
            </Select>
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
                reset({})
                avbryt()
              }}
            >
              Avbryt
            </Button>
            <Button
              size="small"
              variant="primary"
              type="submit"
              loading={isPending(opprettAktivitetResponse) || isPending(opprettAktivitetForSakResponse)}
            >
              Lagre
            </Button>
          </HStack>
        </VStack>
      </form>
      {isFailure(opprettAktivitetResponse) && (
        <Alert variant="error">
          {opprettAktivitetResponse.error.detail || 'Det skjedde en feil ved lagring av aktivitet'}
        </Alert>
      )}
      {isFailure(opprettAktivitetForSakResponse) && (
        <Alert variant="error">
          {opprettAktivitetForSakResponse.error.detail || 'Det skjedde en feil ved lagring av aktivitet'}
        </Alert>
      )}
    </>
  )
}
