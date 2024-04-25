import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { useApiCall } from '~shared/hooks/useApiCall'
import React, { useState } from 'react'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { useForm } from 'react-hook-form'
import { formatISO } from 'date-fns'
import { isFailure, isPending } from '~shared/api/apiUtils'
import { Alert, Button, Heading, HStack, Select, Textarea, VStack } from '@navikt/ds-react'
import { PencilIcon } from '@navikt/aksel-icons'
import styled from 'styled-components'
import { AktivitetspliktType, IAktivitet, IOpprettAktivitet } from '~shared/types/Aktivitetsplikt'
import { opprettAktivitet } from '~shared/api/aktivitetsplikt'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { mapAktivitetstypeProps } from '~components/behandling/aktivitetsplikt/AktivitetspliktTidslinje'

interface AktivitetDefaultValue {
  type: AktivitetspliktType | ''
  datoFom?: Date
  datoTom?: Date | null
  beskrivelse: string
}

const aktivitetDefaultValue: AktivitetDefaultValue = {
  type: '',
  datoFom: undefined,
  datoTom: undefined,
  beskrivelse: '',
}

export const NyAktivitet = ({
  behandling,
  oppdaterAktiviteter,
}: {
  behandling: IBehandlingReducer
  oppdaterAktiviteter: (aktiviteter: IAktivitet[]) => void
}) => {
  const [opprettAktivitetResponse, opprettAktivitetRequest] = useApiCall(opprettAktivitet)
  const [visForm, setVisForm] = useState(false)
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors },
  } = useForm<AktivitetDefaultValue>({
    defaultValues: aktivitetDefaultValue,
  })

  const submitAktivitet = (data: AktivitetDefaultValue) => {
    const { type, datoFom, datoTom, beskrivelse } = data

    const opprettAktivitet: IOpprettAktivitet = {
      sakId: behandling.sakId,
      type: type as AktivitetspliktType,
      fom: formatISO(datoFom!, { representation: 'date' }),
      tom: datoTom ? formatISO(datoTom!, { representation: 'date' }) : undefined,
      beskrivelse: beskrivelse,
    }

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
  }

  return (
    <AktivitetspliktWrapper>
      {visForm && (
        <form onSubmit={handleSubmit(submitAktivitet)}>
          <Heading size="small" level="3" spacing>
            Ny aktivitet
          </Heading>
          <VStack gap="4">
            <HStack gap="4">
              <ControlledDatoVelger name="datoFom" label="Fra dato" control={control} errorVedTomInput="Obligatorisk" />
              <ControlledDatoVelger label="Dato til og med (valgfri)" name="datoTom" control={control} />
              <Select
                {...register('type', {
                  required: { value: true, message: 'Du må velge aktivitetstype' },
                })}
                label="Aktivitetstype"
                error={errors.beskrivelse?.message}
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
              <Button size="small" variant="primary" type="submit" loading={isPending(opprettAktivitetResponse)}>
                Lagre
              </Button>
            </HStack>
            {isFailure(opprettAktivitetResponse) && (
              <Alert variant="error">
                {opprettAktivitetResponse.error.detail || 'Det skjedde en feil ved lagring av aktivitet'}
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
            icon={<PencilIcon aria-hidden fontSize="1.5rem" />}
            loading={isPending(opprettAktivitetResponse)}
            onClick={(e) => {
              e.preventDefault()
              setVisForm(true)
            }}
          >
            Legg til aktivitet
          </Button>
        </HStack>
      )}
    </AktivitetspliktWrapper>
  )
}

const AktivitetspliktWrapper = styled.div`
  margin: 2em 0 1em 0;
`
