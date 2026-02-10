import { Button, HStack, VStack } from '@navikt/ds-react'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import React, { useEffect, useState } from 'react'
import { IAktivitetHendelse, IAktivitetPeriode } from '~shared/types/Aktivitetsplikt'
import { AktivitetspliktTimeline } from '~components/behandling/aktivitetsplikt/aktivitetspliktTidslinje/AktivitetspliktTimeline'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAktiviteterOgHendelser } from '~shared/api/aktivitetsplikt'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { AktivitetHendelse } from '~components/behandling/aktivitetsplikt/aktivitetspliktTidslinje/AktivitetHendelse'
import { PlusIcon } from '@navikt/aksel-icons'
import { AktivitetPeriode } from '~components/behandling/aktivitetsplikt/aktivitetspliktTidslinje/AktivitetPeriode'

export enum AktivitetspliktSkjemaAaVise {
  AKTIVITET_HENDELSE,
  AKTIVITET_PERIODE,
  INGEN,
}

export interface AktivitetspliktRedigeringModus {
  aktivitetspliktSkjemaAaVise: AktivitetspliktSkjemaAaVise
  aktivitetHendelse: IAktivitetHendelse | undefined
  aktivitetPeriode: IAktivitetPeriode | undefined
}

const defaultAktivitetspliktRedigeringModus: AktivitetspliktRedigeringModus = {
  aktivitetspliktSkjemaAaVise: AktivitetspliktSkjemaAaVise.INGEN,
  aktivitetHendelse: undefined,
  aktivitetPeriode: undefined,
}
interface Props {
  behandling?: IDetaljertBehandling
  doedsdato: Date
  sakId: number
}

export const AktivitetspliktTidslinje = ({ behandling, doedsdato, sakId }: Props) => {
  const [aktivitetHendelser, setAktivitetHendelser] = useState<IAktivitetHendelse[]>([])
  const [aktivitetPerioder, setAktivitetPerioder] = useState<IAktivitetPeriode[]>([])

  const [aktivitetspliktRedigeringModus, setAktivitetspliktRedigeringModus] = useState<AktivitetspliktRedigeringModus>(
    defaultAktivitetspliktRedigeringModus
  )

  const [aktivitetOgHendelserResult, aktiviteterOgHendelserFetch] = useApiCall(hentAktiviteterOgHendelser)

  useEffect(() => {
    aktiviteterOgHendelserFetch({ sakId, behandlingId: behandling?.id }, (data) => {
      setAktivitetHendelser(data.hendelser)
      setAktivitetPerioder(data.perioder)
    })
  }, [])

  return (
    <VStack gap="space-8" minWidth="50rem">
      <AktivitetspliktTimeline
        doedsdato={doedsdato}
        aktivitetHendelser={aktivitetHendelser}
        setAktivitetHendelser={setAktivitetHendelser}
        aktivitetPerioder={aktivitetPerioder}
        setAktivitetPerioder={setAktivitetPerioder}
        setAktivitetspliktRedigeringModus={setAktivitetspliktRedigeringModus}
      />

      {isFailureHandler({
        apiResult: aktivitetOgHendelserResult,
        errorMessage: 'Kunne ikke hente aktivitet hendelser og perioder',
      })}

      {aktivitetspliktRedigeringModus.aktivitetspliktSkjemaAaVise === AktivitetspliktSkjemaAaVise.AKTIVITET_PERIODE && (
        <AktivitetPeriode
          behandling={behandling}
          sakId={sakId}
          aktivitetPeriode={aktivitetspliktRedigeringModus.aktivitetPeriode}
          setAktivitetPerioder={setAktivitetPerioder}
          setAktivitetspliktRedigeringModus={setAktivitetspliktRedigeringModus}
        />
      )}
      {aktivitetspliktRedigeringModus.aktivitetspliktSkjemaAaVise ===
        AktivitetspliktSkjemaAaVise.AKTIVITET_HENDELSE && (
        <AktivitetHendelse
          behandling={behandling}
          sakId={sakId}
          aktivitetHendelse={aktivitetspliktRedigeringModus.aktivitetHendelse}
          setAktivitetHendelser={setAktivitetHendelser}
          setAktivitetspliktRedigeringModus={setAktivitetspliktRedigeringModus}
        />
      )}

      {aktivitetspliktRedigeringModus.aktivitetspliktSkjemaAaVise === AktivitetspliktSkjemaAaVise.INGEN && (
        <HStack gap="space-4">
          <Button
            size="small"
            variant="secondary"
            icon={<PlusIcon aria-hidden />}
            iconPosition="right"
            onClick={() => {
              setAktivitetspliktRedigeringModus({
                aktivitetspliktSkjemaAaVise: AktivitetspliktSkjemaAaVise.AKTIVITET_PERIODE,
                aktivitetHendelse: undefined,
                aktivitetPeriode: undefined,
              })
            }}
          >
            Ny aktivitet
          </Button>
          <Button
            size="small"
            variant="secondary"
            icon={<PlusIcon aria-hidden />}
            iconPosition="right"
            onClick={() => {
              setAktivitetspliktRedigeringModus({
                aktivitetspliktSkjemaAaVise: AktivitetspliktSkjemaAaVise.AKTIVITET_HENDELSE,
                aktivitetHendelse: undefined,
                aktivitetPeriode: undefined,
              })
            }}
          >
            Ny hendelse
          </Button>
        </HStack>
      )}
    </VStack>
  )
}
