import { IAktivitetspliktUnntak, IAktivitetspliktVurderingNyDto } from '~shared/types/Aktivitetsplikt'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { slettAktivitetspliktUnntak } from '~shared/api/aktivitetsplikt'
import { useDispatch } from 'react-redux'
import { erOppgaveRedigerbar } from '~shared/types/oppgave'
import { setAktivitetspliktVurdering } from '~store/reducers/Aktivitetsplikt12mnd'
import { UnntakAktivitetspliktOppgaveMedForm } from '~components/aktivitetsplikt/vurdering/unntak/UnntakAktivitetspliktOppgave'
import { BodyShort, Button, HStack, Label, VStack } from '@navikt/ds-react'
import { isFailure, isPending } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { PencilIcon, TrashIcon } from '@navikt/aksel-icons'

export function VisUnntak(props: { unntak: IAktivitetspliktUnntak }) {
  const { unntak } = props
  const { oppgave } = useAktivitetspliktOppgaveVurdering()
  const [redigerer, setRedigerer] = useState(false)
  const [slettUnntakStatus, slettSpesifiktUnntak, resetSlettStatus] = useApiCall(slettAktivitetspliktUnntak)
  const dispatch = useDispatch()
  const oppgaveErRedigerbar = erOppgaveRedigerbar(oppgave.status)

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
      <UnntakAktivitetspliktOppgaveMedForm
        onSuccess={oppdaterStateEtterRedigertUnntak}
        onAvbryt={() => setRedigerer(false)}
        unntak={unntak}
      />
    )
  }

  return (
    <VStack gap="6">
      <VStack gap="2">
        <Label>Beskrivelse</Label>
        <BodyShort>{unntak.beskrivelse}</BodyShort>
      </VStack>

      {isFailure(slettUnntakStatus) && (
        <ApiErrorAlert>Kunne ikke slette unntaket, på grunn av feil: {slettUnntakStatus.error.detail}</ApiErrorAlert>
      )}

      {oppgaveErRedigerbar && (
        <HStack gap="4">
          <Button size="xsmall" variant="secondary" icon={<PencilIcon />} onClick={() => setRedigerer(true)}>
            Rediger
          </Button>
          <Button
            size="xsmall"
            variant="secondary"
            icon={<TrashIcon />}
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
