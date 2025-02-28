import { IAktivitetHendelse } from '~shared/types/Aktivitetsplikt'
import { BodyShort, Button, HStack, Timeline, TimelinePinProps, VStack } from '@navikt/ds-react'
import { formaterDatoMedTidspunkt } from '~utils/formatering/dato'
import { Dispatch, SetStateAction } from 'react'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useApiCall } from '~shared/hooks/useApiCall'
import { slettAktivitetHendelseForBehandling, slettAktivitetHendelseForSak } from '~shared/api/aktivitetsplikt'
import { PencilIcon, TrashIcon } from '@navikt/aksel-icons'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import {
  AktivitetspliktRedigeringModus,
  AktivitetspliktSkjemaAaVise,
} from '~components/behandling/aktivitetsplikt/AktivitetspliktTidslinje'

interface Props extends TimelinePinProps {
  behandling?: IDetaljertBehandling
  sakId: number
  aktivitetHendelse: IAktivitetHendelse
  setAktivitetHendelser: Dispatch<SetStateAction<IAktivitetHendelse[]>>
  setAktivitetspliktRedigeringModus: Dispatch<SetStateAction<AktivitetspliktRedigeringModus>>
}

export const AktivitetHendelseTimelinePin = ({
  date,
  aktivitetHendelse,
  behandling,
  sakId,
  setAktivitetHendelser,
  setAktivitetspliktRedigeringModus,
}: Props) => {
  const [slettAktivitetsHendelseForBehandlingResult, slettAktivitetsHendelseForBehandlingRequest] = useApiCall(
    slettAktivitetHendelseForBehandling
  )
  const [slettAktivitetsHendelseForSakResult, slettAktivitetsHendelseForSakRequest] =
    useApiCall(slettAktivitetHendelseForSak)

  const slettAktivitetHendelse = (aktivitetHendelseId: string) => {
    if (!!behandling) {
      slettAktivitetsHendelseForBehandlingRequest(
        { behandlingId: behandling.id, aktivitetHendelseId },
        setAktivitetHendelser
      )
    } else {
      slettAktivitetsHendelseForSakRequest({ sakId, aktivitetHendelseId }, setAktivitetHendelser)
    }
  }

  return (
    <Timeline.Pin date={date}>
      <VStack gap="2">
        <BodyShort>{aktivitetHendelse.beskrivelse}</BodyShort>
        <VStack>
          <BodyShort>
            <i>
              Lagt til {formaterDatoMedTidspunkt(new Date(aktivitetHendelse.opprettet.tidspunkt))} av{' '}
              {aktivitetHendelse.opprettet.ident}
            </i>
          </BodyShort>
          <BodyShort>
            <i>
              Sist endret {formaterDatoMedTidspunkt(new Date(aktivitetHendelse.opprettet.tidspunkt))} av{' '}
              {aktivitetHendelse.opprettet.ident}
            </i>
          </BodyShort>
        </VStack>
        {isFailureHandler({
          apiResult: slettAktivitetsHendelseForBehandlingResult || slettAktivitetsHendelseForSakResult,
          errorMessage: 'Kunne ikke slette aktivitet hendelse',
        })}
        <HStack gap="2">
          <Button
            variant="secondary"
            size="xsmall"
            icon={<PencilIcon aria-hidden />}
            iconPosition="right"
            loading={isPending(slettAktivitetsHendelseForBehandlingResult || slettAktivitetsHendelseForSakResult)}
            onClick={() =>
              setAktivitetspliktRedigeringModus({
                aktivitetspliktSkjemaAaVise: AktivitetspliktSkjemaAaVise.AKTIVITET_HENDELSE,
                aktivitetHendelse: aktivitetHendelse,
                aktivitetPeriode: undefined,
              })
            }
          >
            Rediger
          </Button>
          <Button
            variant="secondary"
            size="xsmall"
            icon={<TrashIcon aria-hidden />}
            iconPosition="right"
            loading={isPending(slettAktivitetsHendelseForBehandlingResult || slettAktivitetsHendelseForSakResult)}
            onClick={() => slettAktivitetHendelse(aktivitetHendelse.id)}
          >
            Slett
          </Button>
        </HStack>
      </VStack>
    </Timeline.Pin>
  )
}
