import { IAktivitetPeriode } from '~shared/types/Aktivitetsplikt'
import { BodyShort, Button, HStack, Timeline, VStack } from '@navikt/ds-react'
import { formaterDato, formaterDatoMedTidspunkt } from '~utils/formatering/dato'
import React, { Dispatch, ReactNode, SetStateAction } from 'react'
import { PencilIcon, TrashIcon } from '@navikt/aksel-icons'
import {
  AktivitetspliktRedigeringModus,
  AktivitetspliktSkjemaAaVise,
} from '~components/behandling/aktivitetsplikt/AktivitetspliktTidslinje'
import { useApiCall } from '~shared/hooks/useApiCall'
import { slettAktivitetPeriodeForBehandling, slettAktivitetPeriodeForSak } from '~shared/api/aktivitetsplikt'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

interface Props {
  start: Date
  end: Date
  status: 'success' | 'warning' | 'danger' | 'info' | 'neutral'
  statusLabel: string
  icon: ReactNode
  behandling: IDetaljertBehandling | undefined
  sakId: number
  aktivitetPeriode: IAktivitetPeriode
  setAktivitetPerioder: Dispatch<SetStateAction<IAktivitetPeriode[]>>
  setAktivitetspliktRedigeringModus: Dispatch<SetStateAction<AktivitetspliktRedigeringModus>>
}

export const AktivitetspliktPeriodeTimelinePeriod = ({
  start,
  end,
  status,
  statusLabel,
  icon,
  behandling,
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
    if (!!behandling) {
      slettAktivitetPeriodeForBehandlingRequest(
        { behandlingId: behandling.id, aktivitetPeriodeId },
        setAktivitetPerioder
      )
    } else {
      slettAktivitetPeriodeForSakRequest({ sakId, aktivitetPeriodeId }, setAktivitetPerioder)
    }
  }

  return (
    <Timeline.Period start={start} end={end} status={status} statusLabel={statusLabel} icon={icon}>
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
    </Timeline.Period>
  )
}
