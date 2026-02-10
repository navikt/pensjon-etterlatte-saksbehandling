import {
  IAktivitetspliktAktivitetsgrad,
  IAktivitetspliktVurderingNyDto,
  teksterAktivitetspliktSkjoennsmessigVurdering,
} from '~shared/types/Aktivitetsplikt'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/AktivitetspliktOppgaveVurderingRoutes'
import { useApiCall } from '~shared/hooks/useApiCall'
import { slettAktivitetsgradForOppgave } from '~shared/api/aktivitetsplikt'
import React, { useState } from 'react'
import { useDispatch } from 'react-redux'
import { erOppgaveRedigerbar } from '~shared/types/oppgave'
import { setAktivitetspliktVurdering } from '~store/reducers/AktivitetsplikReducer'
import { VurderingAktivitetsgradWrapperOppgave } from '~components/aktivitetsplikt/vurdering/aktivitetsgrad/VurderingAktivitetsgradWrapperOppgave'
import { BodyShort, Button, Heading, HStack, Label, VStack } from '@navikt/ds-react'
import { PencilIcon, TrashIcon } from '@navikt/aksel-icons'
import { isFailure, isPending, Result } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'

export function RedigerbarAktivitetsgradOppgave(props: { aktivitet: IAktivitetspliktAktivitetsgrad }) {
  const { oppgave } = useAktivitetspliktOppgaveVurdering()
  const [slettStatus, slettAktivitetsgrad] = useApiCall(slettAktivitetsgradForOppgave)
  const erRedigerbar = erOppgaveRedigerbar(oppgave.status)

  const { aktivitet } = props
  const [redigerer, setRedigerer] = useState<boolean>(false)
  const dispatch = useDispatch()

  function oppdaterTilstandLagretVurdering(data: IAktivitetspliktVurderingNyDto) {
    setRedigerer(false)
    dispatch(setAktivitetspliktVurdering(data))
  }

  function slettAktivitetsgradIOppgave(aktivitet: IAktivitetspliktAktivitetsgrad) {
    slettAktivitetsgrad(
      {
        sakId: aktivitet.sakId,
        oppgaveId: oppgave.id,
        aktivitetsgradId: aktivitet.id,
      },
      (data) => {
        dispatch(setAktivitetspliktVurdering(data))
        setRedigerer(false)
      }
    )
  }

  if (redigerer) {
    return (
      <VurderingAktivitetsgradWrapperOppgave
        onSuccess={oppdaterTilstandLagretVurdering}
        onAvbryt={() => setRedigerer(false)}
        aktivitet={aktivitet}
      />
    )
  }

  return (
    <RedigerbarAktivitsgradKnapper
      erRedigerbar={erRedigerbar}
      aktivitet={aktivitet}
      setRedigerer={setRedigerer}
      slettStatus={slettStatus}
      slettAktivitetsgrad={slettAktivitetsgradIOppgave}
      erBehandling={false}
    />
  )
}

export const RedigerbarAktivitsgradKnapper = ({
  aktivitet,
  erRedigerbar,
  setRedigerer,
  slettStatus,
  slettAktivitetsgrad,
  erBehandling,
}: {
  aktivitet: IAktivitetspliktAktivitetsgrad
  erRedigerbar: boolean
  setRedigerer: (redigerer: boolean) => void
  slettStatus: Result<IAktivitetspliktVurderingNyDto>
  slettAktivitetsgrad: (aktivitet: IAktivitetspliktAktivitetsgrad) => void
  erBehandling: boolean
}) => {
  return (
    <VStack gap="space-6" maxWidth="50rem">
      <Heading size="small">
        {erBehandling
          ? 'Vurdering av aktivitetsplikten'
          : `Vurdering gjort for aktiviteten fra ${aktivitet.vurdertFra12Mnd ? '12 måneder' : '6 måneder'}`}
      </Heading>
      <VStack gap="space-2">
        <Label>Beskrivelse</Label>
        <BodyShort>{aktivitet.beskrivelse}</BodyShort>
      </VStack>

      {aktivitet.skjoennsmessigVurdering && (
        <VStack gap="space-4">
          <Label>Vil bruker være selvforsørget etter stønaden utløper?</Label>
          <BodyShort>{teksterAktivitetspliktSkjoennsmessigVurdering[aktivitet.skjoennsmessigVurdering]}</BodyShort>
        </VStack>
      )}

      {erRedigerbar && (
        <HStack gap="space-4">
          <Button
            size="xsmall"
            variant="secondary"
            onClick={() => setRedigerer(true)}
            icon={<PencilIcon aria-hidden />}
          >
            Rediger
          </Button>
          <Button
            size="xsmall"
            variant="secondary"
            icon={<TrashIcon aria-hidden />}
            loading={isPending(slettStatus)}
            onClick={() => slettAktivitetsgrad(aktivitet)}
          >
            Slett
          </Button>
        </HStack>
      )}

      {isFailure(slettStatus) && (
        <ApiErrorAlert>
          Kunne ikke slette aktivitetsvurderingen, på grunn av feil: {slettStatus.error.detail}
        </ApiErrorAlert>
      )}
    </VStack>
  )
}
