import {
  Buildings2Icon,
  HatSchoolIcon,
  PencilIcon,
  PersonIcon,
  ReceptionIcon,
  RulerIcon,
  WaitingRoomIcon,
} from '@navikt/aksel-icons'
import { HStack, VStack } from '@navikt/ds-react'
import { hentAktiviteterOgHendelser } from '~shared/api/aktivitetsplikt'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useApiCall } from '~shared/hooks/useApiCall'
import React, { ReactNode, useEffect, useState } from 'react'
import { AktivitetspliktType, IAktivitetHendelse, IAktivitetPeriode } from '~shared/types/Aktivitetsplikt'
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
  const [aktivitetsTypeProps, setAktivitetsTypeProps] = useState<AktivitetstypeProps[]>([])

  const [aktivitetspliktRedigeringModus, setAktivitetspliktRedigeringModus] = useState<AktivitetspliktRedigeringModus>(
    defaultAktivitetspliktRedigeringModus
  )

  useEffect(() => {
    hentAktiviteterOgHendelserRequest({ sakId: sakId, behandlingId: behandling?.id }, (aktiviteter) => {
      oppdaterAktiviteter(aktiviteter.perioder)
      setAktivitetHendelser(aktiviteter.hendelser)
    })
  }, [behandling, sakId])

  const oppdaterAktiviteter = (aktiviteter: IAktivitetPeriode[]) => {
    setAktivitetsTypeProps([...new Set(aktiviteter.map((a) => a.type))].map(mapAktivitetstypeProps))
    setAktivitetPerioder(aktiviteter)
  }

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
          oppdaterAktiviteter={oppdaterAktiviteter}
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

interface AktivitetstypeProps {
  type: AktivitetspliktType
  beskrivelse: string
  ikon: ReactNode
  status: 'success' | 'warning' | 'danger' | 'info' | 'neutral'
}

export const mapAktivitetstypeProps = (type: AktivitetspliktType): AktivitetstypeProps => {
  switch (type) {
    case AktivitetspliktType.ARBEIDSTAKER:
      return {
        type: AktivitetspliktType.ARBEIDSTAKER,
        beskrivelse: 'Arbeidstaker',
        ikon: <PersonIcon aria-hidden />,
        status: 'success',
      }
    case AktivitetspliktType.SELVSTENDIG_NAERINGSDRIVENDE:
      return {
        type: AktivitetspliktType.SELVSTENDIG_NAERINGSDRIVENDE,
        beskrivelse: 'Selvstendig næringsdrivende',
        ikon: <RulerIcon aria-hidden />,
        status: 'info',
      }
    case AktivitetspliktType.ETABLERER_VIRKSOMHET:
      return {
        type: AktivitetspliktType.ETABLERER_VIRKSOMHET,
        beskrivelse: 'Etablerer virksomhet',
        ikon: <Buildings2Icon aria-hidden />,
        status: 'danger',
      }
    case AktivitetspliktType.ARBEIDSSOEKER:
      return {
        type: AktivitetspliktType.ARBEIDSSOEKER,
        beskrivelse: 'Arbeidssøker',
        ikon: <PencilIcon aria-hidden />,
        status: 'warning',
      }
    case AktivitetspliktType.UTDANNING:
      return {
        type: AktivitetspliktType.UTDANNING,
        beskrivelse: 'Utdanning',
        ikon: <HatSchoolIcon aria-hidden />,
        status: 'neutral',
      }
    case AktivitetspliktType.INGEN_AKTIVITET:
      return {
        type: AktivitetspliktType.INGEN_AKTIVITET,
        beskrivelse: 'Ingen Aktivitet',
        ikon: <WaitingRoomIcon aria-hidden />,
        status: 'neutral',
      }
    case AktivitetspliktType.OPPFOELGING_LOKALKONTOR:
      return {
        type: AktivitetspliktType.OPPFOELGING_LOKALKONTOR,
        beskrivelse: 'Oppfølging lokalkontor',
        ikon: <ReceptionIcon aria-hidden />,
        status: 'warning',
      }
  }
}
