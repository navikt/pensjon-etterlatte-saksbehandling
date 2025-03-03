import { IAktivitetPeriode } from '~shared/types/Aktivitetsplikt'
import { BodyShort, Button, HStack, VStack } from '@navikt/ds-react'
import { formaterDato, formaterDatoMedTidspunkt } from '~utils/formatering/dato'
import React from 'react'
import { PencilIcon, TrashIcon } from '@navikt/aksel-icons'
import {
  AktivitetspliktRedigeringModus,
  AktivitetspliktSkjemaAaVise,
} from '~components/behandling/aktivitetsplikt/AktivitetspliktTidslinje'
import { useApiCall } from '~shared/hooks/useApiCall'
import { slettAktivitetPeriodeForBehandling, slettAktivitetPeriodeForSak } from '~shared/api/aktivitetsplikt'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

interface Props {
  behandlingId?: string
  sakId: number
  aktivitetPeriode: IAktivitetPeriode
  setAktivitetspliktRedigeringModus: (aktivitetspliktRedigeringModus: AktivitetspliktRedigeringModus) => void
}

export const AktivitetspliktPeriodeTimelinePeriodContent = ({
  behandlingId,
  sakId,
  aktivitetPeriode,
  setAktivitetspliktRedigeringModus,
}: Props) => {
  const [slettAktivitetPeriodeForBehandlingResult, slettAktivitetPeriodeForBehandlingRequest] = useApiCall(
    slettAktivitetPeriodeForBehandling
  )
  const [slettAktivitetPeriodeForSakResult, slettAktivitetPeriodeForSakRequest] =
    useApiCall(slettAktivitetPeriodeForSak)

  const slettAktivitetPeriode = (aktivitetPeriodeId: string) => {
    if (!!behandlingId) {
      slettAktivitetPeriodeForBehandlingRequest({ behandlingId, aktivitetPeriodeId })
    } else {
      slettAktivitetPeriodeForSakRequest({ sakId, aktivitetPeriodeId })
    }
  }

  return (
    <VStack gap="2">
      <BodyShort weight="semibold">
        Fra {formaterDato(new Date(aktivitetPeriode.fom))}{' '}
        {aktivitetPeriode.tom && `til ${formaterDato(new Date(aktivitetPeriode.tom))}`}
      </BodyShort>
      <BodyShort>{aktivitetPeriode.beskrivelse}</BodyShort>
      <BodyShort>
        <i>
          Lagt til {formaterDatoMedTidspunkt(new Date(aktivitetPeriode.opprettet.tidspunkt))} av{' '}
          {aktivitetPeriode.opprettet.ident}
        </i>
      </BodyShort>
      <BodyShort>
        <i>
          Sist endret {formaterDatoMedTidspunkt(new Date(aktivitetPeriode.endret.tidspunkt))} av{' '}
          {aktivitetPeriode.endret.ident}
        </i>
      </BodyShort>
      {isFailureHandler({
        apiResult: slettAktivitetPeriodeForBehandlingResult || slettAktivitetPeriodeForSakResult,
        errorMessage: 'Kunne ikke slette aktivitet periode',
      })}
      <HStack gap="2">
        <Button
          variant="secondary"
          size="xsmall"
          icon={<PencilIcon aria-hidden />}
          loading={isPending(slettAktivitetPeriodeForBehandlingResult || slettAktivitetPeriodeForSakResult)}
          onClick={() =>
            setAktivitetspliktRedigeringModus({
              aktivitetspliktSkjemaAaVise: AktivitetspliktSkjemaAaVise.AKTIVITET_PERIODE,
              aktivitetHendelse: undefined,
              aktivitetPeriode: aktivitetPeriode,
            })
          }
        >
          Rediger
        </Button>
        <Button
          variant="secondary"
          size="xsmall"
          icon={<TrashIcon aria-hidden />}
          loading={isPending(slettAktivitetPeriodeForBehandlingResult || slettAktivitetPeriodeForSakResult)}
          onClick={() => slettAktivitetPeriode(aktivitetPeriode.id)}
        >
          Slett
        </Button>
      </HStack>
    </VStack>
  )
}
