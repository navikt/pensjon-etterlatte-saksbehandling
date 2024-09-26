import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { useApiCall } from '~shared/hooks/useApiCall'
import React, { useEffect, useState } from 'react'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { useForm } from 'react-hook-form'
import { formatISO } from 'date-fns'
import { isFailure, isPending } from '~shared/api/apiUtils'
import { Alert, Button, Heading, HStack, Select, Textarea, VStack } from '@navikt/ds-react'
import { PlusIcon } from '@navikt/aksel-icons'
import { AktivitetspliktType, IAktivitet, IOpprettAktivitet } from '~shared/types/Aktivitetsplikt'
import { opprettAktivitet, opprettAktivitetForSak } from '~shared/api/aktivitetsplikt'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { mapAktivitetstypeProps } from '~components/behandling/aktivitetsplikt/AktivitetspliktTidslinje'

interface AktivitetDefaultValue {
  id: string | undefined
  type: AktivitetspliktType | ''
  datoFom?: Date
  datoTom?: Date | null
  beskrivelse: string
}

const aktivitetDefaultValue: AktivitetDefaultValue = {
  id: undefined,
  type: '',
  datoFom: undefined,
  datoTom: undefined,
  beskrivelse: '',
}

export const NyAktivitet = ({
  oppdaterAktiviteter,
  redigerAktivitet,
  behandling = undefined,
  sakId = undefined,
}: {
  oppdaterAktiviteter: (aktiviteter: IAktivitet[]) => void
  redigerAktivitet: IAktivitet | undefined
  behandling?: IBehandlingReducer
  sakId?: number
}) => {
  const [opprettAktivitetResponse, opprettAktivitetRequest] = useApiCall(opprettAktivitet)
  const [opprettAktivitetForSakResponse, opprettAktivitetForSakRequest] = useApiCall(opprettAktivitetForSak)
  const [visForm, setVisForm] = useState(false)
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const redigerbar = behandling
    ? behandlingErRedigerbar(behandling.status, behandling.sakEnhetId, innloggetSaksbehandler.skriveEnheter)
    : true

  const {
    getValues,
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors },
  } = useForm<AktivitetDefaultValue>({
    defaultValues: aktivitetDefaultValue,
  })

  useEffect(() => {
    if (redigerAktivitet) {
      reset({
        id: redigerAktivitet.id,
        type: redigerAktivitet.type,
        datoFom: new Date(redigerAktivitet.fom),
        datoTom: redigerAktivitet.tom ? new Date(redigerAktivitet.tom) : null,
        beskrivelse: redigerAktivitet.beskrivelse,
      })
      setVisForm(true)
    }
  }, [redigerAktivitet])

  const submitAktivitet = (data: AktivitetDefaultValue) => {
    const { id, type, datoFom, datoTom, beskrivelse } = data

    const opprettAktivitet: IOpprettAktivitet = {
      id: id,
      sakId: behandling ? behandling.sakId : sakId!!,
      type: type as AktivitetspliktType,
      fom: formatISO(datoFom!, { representation: 'date' }),
      tom: datoTom ? formatISO(datoTom!, { representation: 'date' }) : undefined,
      beskrivelse: beskrivelse,
    }

    if (behandling) {
      opprettAktivitetRequest(
        {
          behandlingId: behandling.id,
          request: opprettAktivitet,
        },
        (aktiviteter) => {
          reset(aktivitetDefaultValue)
          setVisForm(false)
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
          reset(aktivitetDefaultValue)
          setVisForm(false)
          oppdaterAktiviteter(aktiviteter)
        }
      )
    }
  }

  return (
    <>
      {visForm && (
        <form onSubmit={handleSubmit(submitAktivitet)}>
          <Heading size="small" level="3" spacing>
            {getValues('id') ? 'Endre' : 'Ny'} aktivitet
          </Heading>
          <VStack gap="4">
            <HStack gap="4">
              <ControlledDatoVelger name="datoFom" label="Fra dato" control={control} errorVedTomInput="Obligatorisk" />
              <ControlledDatoVelger label="Dato til og med" name="datoTom" control={control} required={false} />
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
                  reset(aktivitetDefaultValue)
                  setVisForm(false)
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
          </VStack>
        </form>
      )}

      {!visForm && redigerbar && (
        <HStack>
          <Button
            size="small"
            variant="secondary"
            icon={<PlusIcon aria-hidden fontSize="1.5rem" />}
            loading={isPending(opprettAktivitetResponse) || isPending(opprettAktivitetForSakResponse)}
            onClick={(e) => {
              e.preventDefault()
              setVisForm(true)
            }}
          >
            Legg til aktivitet
          </Button>
        </HStack>
      )}
    </>
  )
}
