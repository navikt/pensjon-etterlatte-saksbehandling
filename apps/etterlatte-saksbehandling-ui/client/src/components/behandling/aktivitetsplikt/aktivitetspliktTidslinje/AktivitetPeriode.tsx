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
import { Box, Button, Heading, HStack, Select, Textarea, VStack } from '@navikt/ds-react'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import React from 'react'
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
      <VStack gap="space-4">
        <Heading size="small">Legg til aktivitet</Heading>
        <HStack gap="space-4">
          <ControlledDatoVelger
            name="fom"
            label="Fra dato"
            description="Fra dato oppgitt"
            control={control}
            errorVedTomInput="Du må sette en fra og med dato"
          />
          <ControlledDatoVelger
            name="tom"
            label="Dato til og med"
            description="Hvis det er oppgitt sluttdato"
            control={control}
            required={false}
          />
        </HStack>
        <VStack gap="space-4">
          <Box maxWidth="fit-content">
            <Select
              {...register('type', {
                required: { value: true, message: 'Du må velge en aktivitetstype' },
              })}
              label="Aktivitetstype"
              description="Velg aktivitet"
              error={errors.type?.message}
            >
              <option value="">Velg aktivitet</option>
              {Object.keys(AktivitetspliktType).map((type) => (
                <option key={type} value={type}>
                  {aktivitetspliktTypeTilLesbarStreng(type)}
                </option>
              ))}
            </Select>
          </Box>
          <Box maxWidth="fit-content">
            <Textarea
              {...register('beskrivelse', {
                required: { value: true, message: 'Beskrivelse må gis' },
              })}
              label="Beskrivelse"
              description="Gjerne oppgi brukers aktivitetsgrad og annen relevant informasjon"
              error={errors.beskrivelse?.message}
            />
          </Box>
        </VStack>
        {isFailureHandler({
          apiResult: opprettAktivitetPeriodeForBehandlingResult,
          errorMessage: 'Kunne ikke opprette/oppdatere aktivitet periode',
        })}
        {isFailureHandler({
          apiResult: opprettAktivitetPeriodeForSakResult,
          errorMessage: 'Kunne ikke opprette/oppdatere aktivitet periode',
        })}
        <HStack gap="space-4">
          <Button
            size="small"
            variant="secondary"
            type="button"
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
            loading={
              isPending(opprettAktivitetPeriodeForBehandlingResult) || isPending(opprettAktivitetPeriodeForSakResult)
            }
          >
            Legg til aktivitet
          </Button>
        </HStack>
      </VStack>
    </form>
  )
}
