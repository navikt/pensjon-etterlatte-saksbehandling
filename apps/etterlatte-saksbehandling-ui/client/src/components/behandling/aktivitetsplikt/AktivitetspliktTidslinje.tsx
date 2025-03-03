import { HStack, VStack } from '@navikt/ds-react'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import React, { useState } from 'react'
import { IAktivitetHendelse, IAktivitetPeriode } from '~shared/types/Aktivitetsplikt'
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
  const [aktivitetspliktRedigeringModus, setAktivitetspliktRedigeringModus] = useState<AktivitetspliktRedigeringModus>(
    defaultAktivitetspliktRedigeringModus
  )

  return (
    <VStack gap="8" minWidth="50rem">
      <AktivitetspliktTimeline
        behandling={behandling}
        sakId={sakId}
        doedsdato={doedsdato}
        setAktivitetspliktRedigeringModus={setAktivitetspliktRedigeringModus}
      />

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
