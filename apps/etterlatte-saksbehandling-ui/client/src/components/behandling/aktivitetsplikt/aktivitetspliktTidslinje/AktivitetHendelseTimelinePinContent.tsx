import { IAktivitetHendelse } from '~shared/types/Aktivitetsplikt'
import { BodyShort, Button, HStack, VStack } from '@navikt/ds-react'
import { formaterDatoMedTidspunkt } from '~utils/formatering/dato'
import { ReactNode } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { slettAktivitetHendelseForSak } from '~shared/api/aktivitetsplikt'
import { PencilIcon, TrashIcon } from '@navikt/aksel-icons'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import {
  AktivitetspliktRedigeringModus,
  AktivitetspliktSkjemaAaVise,
} from '~components/behandling/aktivitetsplikt/aktivitetspliktTidslinje/AktivitetspliktTidslinje'

interface Props {
  behandlingId?: string
  sakId: number
  aktivitetHendelse: IAktivitetHendelse
  setAktivitetHendelser: (aktivitetHendelser: IAktivitetHendelse[]) => void
  setAktivitetspliktRedigeringModus: (aktivitetspliktRedigeringModus: AktivitetspliktRedigeringModus) => void
}

export const AktivitetHendelseTimelinePinContent = ({
  sakId,
  aktivitetHendelse,
  setAktivitetHendelser,
  setAktivitetspliktRedigeringModus,
}: Props): ReactNode => {
  const [slettAktivitetsHendelseForSakResult, slettAktivitetsHendelseForSakRequest] =
    useApiCall(slettAktivitetHendelseForSak)

  const slettAktivitetHendelse = (aktivitetHendelseId: string) => {
    slettAktivitetsHendelseForSakRequest({ sakId, aktivitetHendelseId }, setAktivitetHendelser)
  }

  return (
    <VStack gap="space-2">
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
        apiResult: slettAktivitetsHendelseForSakResult,
        errorMessage: 'Kunne ikke slette aktivitet hendelse',
      })}
      <HStack gap="space-2">
        <Button
          variant="secondary"
          size="xsmall"
          icon={<PencilIcon aria-hidden />}
          iconPosition="right"
          loading={isPending(slettAktivitetsHendelseForSakResult)}
          onClick={() =>
            setAktivitetspliktRedigeringModus({
              aktivitetspliktSkjemaAaVise: AktivitetspliktSkjemaAaVise.AKTIVITET_HENDELSE,
              aktivitetHendelse,
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
          loading={isPending(slettAktivitetsHendelseForSakResult)}
          onClick={() => slettAktivitetHendelse(aktivitetHendelse.id)}
        >
          Slett
        </Button>
      </HStack>
    </VStack>
  )
}
