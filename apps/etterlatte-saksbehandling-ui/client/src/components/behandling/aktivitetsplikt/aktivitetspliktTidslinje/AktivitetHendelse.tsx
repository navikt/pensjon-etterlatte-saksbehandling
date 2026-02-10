import { IAktivitetHendelse, OpprettAktivitetHendelse } from '~shared/types/Aktivitetsplikt'
import { useForm } from 'react-hook-form'
import { Button, Heading, HStack, Textarea, VStack } from '@navikt/ds-react'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import {
  AktivitetspliktRedigeringModus,
  AktivitetspliktSkjemaAaVise,
} from '~components/behandling/aktivitetsplikt/aktivitetspliktTidslinje/AktivitetspliktTidslinje'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettAktivitetHendelseForBehandling, opprettAktivitetHendelseForSak } from '~shared/api/aktivitetsplikt'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { formaterTilISOString } from '~utils/formatering/dato'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import React from 'react'

interface AktivitetHendelseSkjema {
  dato: string
  beskrivelse: string
}

interface Props {
  behandling?: IDetaljertBehandling
  sakId: number
  aktivitetHendelse?: IAktivitetHendelse
  setAktivitetHendelser: (aktivitetHendelser: IAktivitetHendelse[]) => void
  setAktivitetspliktRedigeringModus: (aktivitetspliktRedigeringModus: AktivitetspliktRedigeringModus) => void
}

export const AktivitetHendelse = ({
  behandling,
  sakId,
  aktivitetHendelse,
  setAktivitetHendelser,
  setAktivitetspliktRedigeringModus,
}: Props) => {
  const [opprettAktivitetHendelseForBehandlingResult, opprettAktivitetHendelseForBehandlingRequest] = useApiCall(
    opprettAktivitetHendelseForBehandling
  )
  const [opprettAktivitetHendelseForSakResult, opprettAktivitetHendelseForSakRequest] =
    useApiCall(opprettAktivitetHendelseForSak)

  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<AktivitetHendelseSkjema>({
    defaultValues: !!aktivitetHendelse
      ? {
          dato: aktivitetHendelse.dato,
          beskrivelse: aktivitetHendelse.beskrivelse,
        }
      : {
          dato: '',
          beskrivelse: '',
        },
  })

  const lagreAktivitetHendelse = (data: AktivitetHendelseSkjema) => {
    const aktivitetHendelseRequestBody: OpprettAktivitetHendelse = !!aktivitetHendelse
      ? {
          beskrivelse: data.beskrivelse,
          dato: formaterTilISOString(data.dato),
          id: aktivitetHendelse.id,
          sakId,
        }
      : {
          beskrivelse: data.beskrivelse,
          dato: formaterTilISOString(data.dato),
          id: '',
          sakId,
        }

    if (!!behandling) {
      opprettAktivitetHendelseForBehandlingRequest(
        {
          behandlingId: behandling.id,
          request: aktivitetHendelseRequestBody,
        },
        setAktivitetHendelser
      )
    } else {
      opprettAktivitetHendelseForSakRequest({
        sakId,
        request: aktivitetHendelseRequestBody,
      })
    }

    setAktivitetspliktRedigeringModus({
      aktivitetspliktSkjemaAaVise: AktivitetspliktSkjemaAaVise.INGEN,
      aktivitetPeriode: undefined,
      aktivitetHendelse: undefined,
    })
  }

  return (
    <form onSubmit={handleSubmit(lagreAktivitetHendelse)}>
      <VStack gap="space-4" maxWidth="20rem">
        <Heading size="small">Legg til hendelse</Heading>
        <ControlledDatoVelger
          name="dato"
          label="Dato for hendelse"
          control={control}
          errorVedTomInput="Dato må settes"
        />
        <Textarea
          {...register('beskrivelse', {
            required: { value: true, message: 'Beskrivelse må gis' },
          })}
          label="Hva gjelder hendelsen"
          description="Gi en kort beskrivelse"
          error={errors.beskrivelse?.message}
        />
        {isFailureHandler({
          apiResult: opprettAktivitetHendelseForBehandlingResult,
          errorMessage: 'Kunne ikke opprette/oppdatere aktivitet hendelse',
        })}
        {isFailureHandler({
          apiResult: opprettAktivitetHendelseForSakResult,
          errorMessage: 'Kunne ikke opprette/oppdatere aktivitet hendelse',
        })}
        <HStack gap="space-4">
          <Button
            size="small"
            variant="secondary"
            type="button"
            loading={
              isPending(opprettAktivitetHendelseForBehandlingResult) || isPending(opprettAktivitetHendelseForSakResult)
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
              isPending(opprettAktivitetHendelseForBehandlingResult) || isPending(opprettAktivitetHendelseForSakResult)
            }
          >
            Legg til hendelse
          </Button>
        </HStack>
      </VStack>
    </form>
  )
}
