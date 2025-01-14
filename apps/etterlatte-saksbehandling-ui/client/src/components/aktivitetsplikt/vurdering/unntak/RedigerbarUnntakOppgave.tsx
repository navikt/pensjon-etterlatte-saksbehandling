import { IAktivitetspliktUnntak, IAktivitetspliktVurderingNyDto } from '~shared/types/Aktivitetsplikt'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/AktivitetspliktOppgaveVurderingRoutes'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { slettAktivitetspliktUnntakForOppgave } from '~shared/api/aktivitetsplikt'
import { useDispatch } from 'react-redux'
import { erOppgaveRedigerbar } from '~shared/types/oppgave'
import { setAktivitetspliktVurdering } from '~store/reducers/AktivitetsplikReducer'
import { VelgOgLagreUnntakAktivitetspliktOppgave } from '~components/aktivitetsplikt/vurdering/unntak/UnntakAktivitetspliktOppgave'
import { BodyShort, Button, HStack, Label, VStack } from '@navikt/ds-react'
import { isFailure, isPending, Result } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { PencilIcon, TrashIcon } from '@navikt/aksel-icons'

export function RedigerbarUnntakOppgave(props: { unntak: IAktivitetspliktUnntak }) {
  const dispatch = useDispatch()
  const { unntak } = props
  const [redigerer, setRedigerer] = useState(false)

  const { oppgave } = useAktivitetspliktOppgaveVurdering()
  const [slettUnntakStatus, slettSpesifiktUnntak, resetSlettStatus] = useApiCall(slettAktivitetspliktUnntakForOppgave)
  const redigerbar = erOppgaveRedigerbar(oppgave.status)

  function slettUnntak(unntak: IAktivitetspliktUnntak) {
    slettSpesifiktUnntak(
      {
        oppgaveId: oppgave.id,
        sakId: unntak.sakId,
        unntakId: unntak.id,
      },
      (data) => {
        dispatch(setAktivitetspliktVurdering(data))
        setRedigerer(false)
      }
    )
  }

  function oppdaterStateEtterRedigertUnntak(data: IAktivitetspliktVurderingNyDto) {
    dispatch(setAktivitetspliktVurdering(data))
    setRedigerer(false)
    resetSlettStatus()
  }

  if (redigerer) {
    return (
      <VelgOgLagreUnntakAktivitetspliktOppgave
        oppdaterStateEtterRedigertUnntak={oppdaterStateEtterRedigertUnntak}
        onAvbryt={() => setRedigerer(false)}
        unntak={unntak}
      />
    )
  }

  return (
    <UnntakRedigeringsKnapper
      redigerbar={redigerbar}
      unntak={unntak}
      slettUnntakStatus={slettUnntakStatus}
      setRedigerer={setRedigerer}
      slettUnntak={slettUnntak}
    />
  )
}

//TODO flytte ut?
export const UnntakRedigeringsKnapper = ({
  unntak,
  slettUnntakStatus,
  redigerbar,
  setRedigerer,
  slettUnntak,
}: {
  unntak: IAktivitetspliktUnntak
  slettUnntakStatus: Result<IAktivitetspliktVurderingNyDto>
  redigerbar: boolean
  setRedigerer: (arg: boolean) => void
  slettUnntak: (unntak: IAktivitetspliktUnntak) => void
}) => {
  return (
    <VStack gap="6">
      <VStack gap="2">
        <Label>Beskrivelse</Label>
        <BodyShort>{unntak.beskrivelse}</BodyShort>
      </VStack>

      {isFailure(slettUnntakStatus) && (
        <ApiErrorAlert>Kunne ikke slette unntaket, på grunn av feil: {slettUnntakStatus.error.detail}</ApiErrorAlert>
      )}

      {redigerbar && (
        <HStack gap="4">
          <Button
            size="xsmall"
            variant="secondary"
            icon={<PencilIcon aria-hidden />}
            onClick={() => setRedigerer(true)}
          >
            Rediger
          </Button>
          <Button
            size="xsmall"
            variant="secondary"
            icon={<TrashIcon aria-hidden />}
            onClick={() => slettUnntak(unntak)}
            loading={isPending(slettUnntakStatus)}
          >
            Slett
          </Button>
        </HStack>
      )}
    </VStack>
  )
}
