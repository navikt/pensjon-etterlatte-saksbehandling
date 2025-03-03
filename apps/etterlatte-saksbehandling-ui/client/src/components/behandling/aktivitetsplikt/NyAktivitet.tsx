import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { useApiCall } from '~shared/hooks/useApiCall'
import React, { ReactNode, useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { formatISO } from 'date-fns'
import { isFailure, isPending } from '~shared/api/apiUtils'
import { Alert, Box, Button, Heading, HStack, Select, Textarea, VStack } from '@navikt/ds-react'
import { AktivitetspliktType, IAktivitetPeriode, OpprettAktivitetPeriode } from '~shared/types/Aktivitetsplikt'
import { opprettAktivitetPeriodeForBehandling, opprettAktivitetPeriodeForSak } from '~shared/api/aktivitetsplikt'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import {
  Buildings2Icon,
  HatSchoolIcon,
  PencilIcon,
  PersonIcon,
  ReceptionIcon,
  RulerIcon,
  WaitingRoomIcon,
} from '@navikt/aksel-icons'

function dtoTilSkjema(periode: IAktivitetPeriode): NyAktivitetPeriode {
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

interface NyAktivitetPeriode {
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
  aktivitetTilRedigering,
  behandling = undefined,
}: {
  oppdaterAktiviteter: (aktiviteter: IAktivitetPeriode[]) => void
  sakId: number
  avbryt: () => void
  aktivitetTilRedigering: IAktivitetPeriode | undefined
  behandling?: IBehandlingReducer
}) => {
  const [opprettAktivitetResponse, opprettAktivitetRequest] = useApiCall(opprettAktivitetPeriodeForBehandling)
  const [opprettAktivitetForSakResponse, opprettAktivitetForSakRequest] = useApiCall(opprettAktivitetPeriodeForSak)
  const defaultValue: NyAktivitetPeriode = { sakId, behandlingId: behandling?.id }
  const {
    getValues,
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors },
  } = useForm<NyAktivitetPeriode>({
    defaultValues: aktivitetTilRedigering ? dtoTilSkjema(aktivitetTilRedigering) : defaultValue,
  })

  useEffect(() => {
    if (aktivitetTilRedigering) {
      reset(dtoTilSkjema(aktivitetTilRedigering))
    }
  }, [aktivitetTilRedigering])

  const submitAktivitet = (data: NyAktivitetPeriode) => {
    const { id, type, datoFom, datoTom, beskrivelse } = data

    const opprettAktivitet: OpprettAktivitetPeriode = {
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
          <Box width="630px">
            <Textarea
              {...register('beskrivelse', {
                required: { value: true, message: 'Må fylles ut' },
              })}
              label="Beskrivelse"
              error={errors.beskrivelse?.message}
            />
          </Box>
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

interface AktivitetstypeProps {
  type: AktivitetspliktType
  beskrivelse: string
  ikon: ReactNode
  status: 'success' | 'warning' | 'danger' | 'info' | 'neutral'
}

export const mapAktivitetstypeProps = (type: AktivitetspliktType): AktivitetstypeProps => {
  switch (type) {
    case AktivitetspliktType.ARBEIDSTAKER:
      return {
        type: AktivitetspliktType.ARBEIDSTAKER,
        beskrivelse: 'Arbeidstaker',
        ikon: <PersonIcon aria-hidden />,
        status: 'success',
      }
    case AktivitetspliktType.SELVSTENDIG_NAERINGSDRIVENDE:
      return {
        type: AktivitetspliktType.SELVSTENDIG_NAERINGSDRIVENDE,
        beskrivelse: 'Selvstendig næringsdrivende',
        ikon: <RulerIcon aria-hidden />,
        status: 'info',
      }
    case AktivitetspliktType.ETABLERER_VIRKSOMHET:
      return {
        type: AktivitetspliktType.ETABLERER_VIRKSOMHET,
        beskrivelse: 'Etablerer virksomhet',
        ikon: <Buildings2Icon aria-hidden />,
        status: 'danger',
      }
    case AktivitetspliktType.ARBEIDSSOEKER:
      return {
        type: AktivitetspliktType.ARBEIDSSOEKER,
        beskrivelse: 'Arbeidssøker',
        ikon: <PencilIcon aria-hidden />,
        status: 'warning',
      }
    case AktivitetspliktType.UTDANNING:
      return {
        type: AktivitetspliktType.UTDANNING,
        beskrivelse: 'Utdanning',
        ikon: <HatSchoolIcon aria-hidden />,
        status: 'neutral',
      }
    case AktivitetspliktType.INGEN_AKTIVITET:
      return {
        type: AktivitetspliktType.INGEN_AKTIVITET,
        beskrivelse: 'Ingen Aktivitet',
        ikon: <WaitingRoomIcon aria-hidden />,
        status: 'neutral',
      }
    case AktivitetspliktType.OPPFOELGING_LOKALKONTOR:
      return {
        type: AktivitetspliktType.OPPFOELGING_LOKALKONTOR,
        beskrivelse: 'Oppfølging lokalkontor',
        ikon: <ReceptionIcon aria-hidden />,
        status: 'warning',
      }
  }
}
