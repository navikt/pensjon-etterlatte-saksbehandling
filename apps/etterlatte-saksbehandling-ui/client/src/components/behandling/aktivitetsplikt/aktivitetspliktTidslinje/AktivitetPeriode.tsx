import {
  AktivitetspliktType,
  aktivitetspliktTypeTilLesbarStreng,
  IAktivitetPeriode,
  OpprettAktivitetPeriode,
} from '~shared/types/Aktivitetsplikt'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import {
  AktivitetspliktRedigeringModus,
  AktivitetspliktSkjemaAaVise,
} from '~components/behandling/aktivitetsplikt/aktivitetspliktTidslinje/AktivitetspliktTidslinje'
import { useForm } from 'react-hook-form'
import { Button, HStack, Select, Textarea, VStack } from '@navikt/ds-react'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import React from 'react'
import { FloppydiskIcon, XMarkIcon } from '@navikt/aksel-icons'
import { isPending } from '~shared/api/apiUtils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettAktivitetPeriodeForBehandling, opprettAktivitetPeriodeForSak } from '~shared/api/aktivitetsplikt'
import { formaterTilISOString } from '~utils/formatering/dato'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

interface AktivitetPeriodeSkjema {
  fom: string
  tom?: string
  type: AktivitetspliktType
  beskrivelse: string
}

interface Props {
  behandling?: IDetaljertBehandling
  sakId: number
  aktivitetPeriode?: IAktivitetPeriode
  setAktivitetPerioder: (aktivitetPerioder: IAktivitetPeriode[]) => void
  setAktivitetspliktRedigeringModus: (aktivitetspliktRedigeringModus: AktivitetspliktRedigeringModus) => void
}

export const AktivitetPeriode = ({
  behandling,
  sakId,
  aktivitetPeriode,
  setAktivitetPerioder,
  setAktivitetspliktRedigeringModus,
}: Props) => {
  const [opprettAktivitetPeriodeForBehandlingResult, opprettAktivitetPeriodeForBehandlingRequest] = useApiCall(
    opprettAktivitetPeriodeForBehandling
  )
  const [opprettAktivitetPeriodeForSakResult, opprettAktivitetPeriodeForSakRequest] =
    useApiCall(opprettAktivitetPeriodeForSak)

  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<AktivitetPeriodeSkjema>({
    defaultValues: !!aktivitetPeriode
      ? {
          ...aktivitetPeriode,
        }
      : {
          fom: '',
          tom: '',
          type: undefined,
          beskrivelse: '',
        },
  })

  const lagreAktivitetPeriode = (data: AktivitetPeriodeSkjema) => {
    const aktivitetPeriodeRequestBody: OpprettAktivitetPeriode = !!aktivitetPeriode
      ? {
          fom: formaterTilISOString(data.fom),
          tom: data.tom ? formaterTilISOString(data.tom) : '',
          type: data.type,
          beskrivelse: data.beskrivelse,
          id: aktivitetPeriode.id,
          sakId,
        }
      : {
          fom: formaterTilISOString(data.fom),
          tom: data.tom ? formaterTilISOString(data.tom) : '',
          type: data.type,
          beskrivelse: data.beskrivelse,
          id: '',
          sakId,
        }

    if (!!behandling) {
      opprettAktivitetPeriodeForBehandlingRequest(
        { behandlingId: behandling.id, request: aktivitetPeriodeRequestBody },
        setAktivitetPerioder
      )
    } else {
      opprettAktivitetPeriodeForSakRequest({ sakId, request: aktivitetPeriodeRequestBody }, setAktivitetPerioder)
    }

    setAktivitetspliktRedigeringModus({
      aktivitetspliktSkjemaAaVise: AktivitetspliktSkjemaAaVise.INGEN,
      aktivitetPeriode: undefined,
      aktivitetHendelse: undefined,
    })
  }

  return (
    <form onSubmit={handleSubmit(lagreAktivitetPeriode)}>
      <VStack gap="4">
        <HStack gap="4">
          <ControlledDatoVelger
            name="fom"
            label="Fra og med"
            control={control}
            errorVedTomInput="Du må sette en fra og med dato"
          />
          <ControlledDatoVelger name="tom" label="Du må sette en til og med dato" control={control} required={false} />
        </HStack>
        <VStack gap="4" maxWidth="20rem">
          <Select
            {...register('type', {
              required: { value: true, message: 'Du må velge en aktivitetstype' },
            })}
            label="Aktivitetstype"
            error={errors.type?.message}
          >
            <option value="">Velg aktivitet</option>
            {Object.keys(AktivitetspliktType).map((type) => (
              <option key={type} value={type}>
                {aktivitetspliktTypeTilLesbarStreng(type)}
              </option>
            ))}
          </Select>

          <Textarea
            {...register('beskrivelse', {
              required: { value: true, message: 'Beskrivelse må gis' },
            })}
            label="Beskrivelse"
            error={errors.beskrivelse?.message}
          />
        </VStack>
        {isFailureHandler({
          apiResult: opprettAktivitetPeriodeForBehandlingResult,
          errorMessage: 'Kunne ikke opprette/oppdatere aktivitet periode',
        })}
        {isFailureHandler({
          apiResult: opprettAktivitetPeriodeForSakResult,
          errorMessage: 'Kunne ikke opprette/oppdatere aktivitet periode',
        })}
        <HStack gap="4">
          <Button
            size="small"
            variant="secondary"
            type="button"
            icon={<XMarkIcon aria-hidden />}
            iconPosition="right"
            loading={
              isPending(opprettAktivitetPeriodeForBehandlingResult) || isPending(opprettAktivitetPeriodeForSakResult)
            }
            onClick={() =>
              setAktivitetspliktRedigeringModus({
                aktivitetspliktSkjemaAaVise: AktivitetspliktSkjemaAaVise.INGEN,
                aktivitetHendelse: undefined,
                aktivitetPeriode: undefined,
              })
            }
          >
            Avbryt
          </Button>
          <Button
            size="small"
            icon={<FloppydiskIcon aria-hidden />}
            loading={
              isPending(opprettAktivitetPeriodeForBehandlingResult) || isPending(opprettAktivitetPeriodeForSakResult)
            }
          >
            Lagre
          </Button>
        </HStack>
      </VStack>
    </form>
  )
}
