import { IAktivitetPeriode } from '~shared/types/Aktivitetsplikt'
import { BodyShort, Button, HStack, VStack } from '@navikt/ds-react'
import { formaterDato, formaterDatoMedTidspunkt } from '~utils/formatering/dato'
import React from 'react'
import { PencilIcon, TrashIcon } from '@navikt/aksel-icons'
import {
  AktivitetspliktRedigeringModus,
  AktivitetspliktSkjemaAaVise,
} from '~components/behandling/aktivitetsplikt/aktivitetspliktTidslinje/AktivitetspliktTidslinje'
import { useApiCall } from '~shared/hooks/useApiCall'
import { slettAktivitetPeriodeForBehandling, slettAktivitetPeriodeForSak } from '~shared/api/aktivitetsplikt'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

interface Props {
  behandlingId?: string
  sakId: number
  aktivitetPeriode: IAktivitetPeriode
  setAktivitetPerioder: (aktivitetPerioder: IAktivitetPeriode[]) => void
  setAktivitetspliktRedigeringModus: (aktivitetspliktRedigeringModus: AktivitetspliktRedigeringModus) => void
}

export const AktivitetspliktPeriodeTimelinePeriodContent = ({
  behandlingId,
  sakId,
  aktivitetPeriode,
  setAktivitetPerioder,
  setAktivitetspliktRedigeringModus,
}: Props) => {
  const [slettAktivitetPeriodeForBehandlingResult, slettAktivitetPeriodeForBehandlingRequest] = useApiCall(
    slettAktivitetPeriodeForBehandling
  )
  const [slettAktivitetPeriodeForSakResult, slettAktivitetPeriodeForSakRequest] =
    useApiCall(slettAktivitetPeriodeForSak)

  const slettAktivitetPeriode = (aktivitetPeriodeId: string) => {
    if (!!behandlingId) {
      slettAktivitetPeriodeForBehandlingRequest({ behandlingId, aktivitetPeriodeId }, setAktivitetPerioder)
    } else {
      slettAktivitetPeriodeForSakRequest({ sakId, aktivitetPeriodeId }, setAktivitetPerioder)
    }
  }

  return (
    <VStack gap="space-2">
      <BodyShort weight="semibold">
        Fra {formaterDato(new Date(aktivitetPeriode.fom))}{' '}
        {aktivitetPeriode.tom && `til ${formaterDato(new Date(aktivitetPeriode.tom))}`}
      </BodyShort>
      <BodyShort>{aktivitetPeriode.beskrivelse}</BodyShort>
      <BodyShort>
        <em>
          Lagt til {formaterDatoMedTidspunkt(new Date(aktivitetPeriode.opprettet.tidspunkt))} av{' '}
          {aktivitetPeriode.opprettet.ident}
        </em>
      </BodyShort>
      <BodyShort>
        <em>
          Sist endret {formaterDatoMedTidspunkt(new Date(aktivitetPeriode.endret.tidspunkt))} av{' '}
          {aktivitetPeriode.endret.ident}
        </em>
      </BodyShort>
      {isFailureHandler({
        apiResult: slettAktivitetPeriodeForBehandlingResult,
        errorMessage: 'Kunne ikke slette aktivitet periode',
      })}
      {isFailureHandler({
        apiResult: slettAktivitetPeriodeForSakResult,
        errorMessage: 'Kunne ikke slette aktivitet periode',
      })}
      <HStack gap="space-2">
        <Button
          variant="secondary"
          size="xsmall"
          icon={<PencilIcon aria-hidden />}
          loading={isPending(slettAktivitetPeriodeForBehandlingResult) || isPending(slettAktivitetPeriodeForSakResult)}
          onClick={() =>
            setAktivitetspliktRedigeringModus({
              aktivitetspliktSkjemaAaVise: AktivitetspliktSkjemaAaVise.AKTIVITET_PERIODE,
              aktivitetHendelse: undefined,
              aktivitetPeriode,
            })
          }
        >
          Rediger
        </Button>
        <Button
          variant="secondary"
          size="xsmall"
          icon={<TrashIcon aria-hidden />}
          loading={isPending(slettAktivitetPeriodeForBehandlingResult) || isPending(slettAktivitetPeriodeForSakResult)}
          onClick={() => slettAktivitetPeriode(aktivitetPeriode.id)}
        >
          Slett
        </Button>
      </HStack>
    </VStack>
  )
}
