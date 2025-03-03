import { HStack, VStack } from '@navikt/ds-react'
import { hentAktiviteterOgHendelser } from '~shared/api/aktivitetsplikt'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useApiCall } from '~shared/hooks/useApiCall'
import React, { useEffect, useState } from 'react'
import { IAktivitetHendelse, IAktivitetPeriode } from '~shared/types/Aktivitetsplikt'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { AktivitetOgHendelse } from '~components/behandling/aktivitetsplikt/AktivitetOgHendelse'
import { AktivitetspliktTimeline } from '~components/behandling/aktivitetsplikt/aktivitetspliktTimeline/AktivitetspliktTimeline'

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
  const [hentAktivitetOgHendelserResult, hentAktiviteterOgHendelserRequest] = useApiCall(hentAktiviteterOgHendelser)
  const [aktivitetPerioder, setAktivitetPerioder] = useState<IAktivitetPeriode[]>([])
  const [aktivitetHendelser, setAktivitetHendelser] = useState<IAktivitetHendelse[]>([])
  const [redigerAktivitet, setRedigerAktivitet] = useState<IAktivitetPeriode | undefined>(undefined)
  const [redigerHendelse, setRedigerHendelse] = useState<IAktivitetHendelse | undefined>(undefined)

  const [aktivitetspliktRedigeringModus, setAktivitetspliktRedigeringModus] = useState<AktivitetspliktRedigeringModus>(
    defaultAktivitetspliktRedigeringModus
  )

  useEffect(() => {
    hentAktiviteterOgHendelserRequest({ sakId: sakId, behandlingId: behandling?.id }, (aktiviteter) => {
      setAktivitetPerioder(aktiviteter.perioder)
      setAktivitetHendelser(aktiviteter.hendelser)
    })
  }, [behandling, sakId])

  function avbrytRedigering() {
    setRedigerAktivitet(undefined)
    setRedigerHendelse(undefined)
  }

  return (
    <VStack gap="8" minWidth="50rem">
      <AktivitetspliktTimeline
        behandling={behandling}
        sakId={sakId}
        doedsdato={doedsdato}
        aktivitetHendelser={aktivitetHendelser}
        setAktivitetHendelser={setAktivitetHendelser}
        aktivitetPerioder={aktivitetPerioder}
        setAktivitetPerioder={setAktivitetPerioder}
        setAktivitetspliktRedigeringModus={setAktivitetspliktRedigeringModus}
      />

      <HStack align="center" justify="space-between">
        <AktivitetOgHendelse
          key={redigerAktivitet?.id}
          behandling={behandling}
          oppdaterAktiviteter={setAktivitetPerioder}
          redigerAktivitet={redigerAktivitet}
          sakId={sakId}
          setHendelser={setAktivitetHendelser}
          redigerHendelse={redigerHendelse}
          avbrytRedigering={avbrytRedigering}
        />
      </HStack>
      {isFailureHandler({
        errorMessage: 'En feil oppsto ved henting av tidslinje',
        apiResult: hentAktivitetOgHendelserResult,
      })}
    </VStack>
  )
}
