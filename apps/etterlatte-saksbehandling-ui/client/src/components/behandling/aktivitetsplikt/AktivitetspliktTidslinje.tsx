import { HStack, VStack } from '@navikt/ds-react'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import React, { useEffect, useState } from 'react'
import { IAktivitetHendelse, IAktivitetPeriode } from '~shared/types/Aktivitetsplikt'
import { AktivitetspliktTimeline } from '~components/behandling/aktivitetsplikt/aktivitetspliktTimeline/AktivitetspliktTimeline'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAktiviteterOgHendelser } from '~shared/api/aktivitetsplikt'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

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
    <VStack gap="8" minWidth="50rem">
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

      <HStack align="center" justify="space-between">
        {/*<AktivitetOgHendelse*/}
        {/*  key={redigerAktivitet?.id}*/}
        {/*  behandling={behandling}*/}
        {/*  oppdaterAktiviteter={setAktivitetPerioder}*/}
        {/*  redigerAktivitet={redigerAktivitet}*/}
        {/*  sakId={sakId}*/}
        {/*  setHendelser={setAktivitetHendelser}*/}
        {/*  redigerHendelse={redigerHendelse}*/}
        {/*  avbrytRedigering={avbrytRedigering}*/}
        {/*/>*/}
      </HStack>
    </VStack>
  )
}
