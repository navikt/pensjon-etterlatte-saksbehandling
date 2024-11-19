import { Button } from '@navikt/ds-react'
import { AktivitetspliktSteg } from '~components/aktivitetsplikt/stegmeny/AktivitetspliktStegmeny'
import React from 'react'
import { OppgaveDTO, Oppgavestatus } from '~shared/types/oppgave'
import { AktivitetspliktInfoModal } from './AktivitetspliktInfoModal'
import { FeatureToggle, useFeaturetoggle } from '~useUnleash'

/**
 * TODO: Burde fjernes når feature-toggle ikke lenger er aktuelt
 **/
export const AktivitetspliktOppgaveHandling = ({
  oppgave,
  oppdaterStatus,
}: {
  oppgave: OppgaveDTO
  oppdaterStatus: (oppgaveId: string, status: Oppgavestatus) => void
}) => {
  console.log('AktivitetspliktOppgaveHandling renders')
  const brukNyVurderingssideAktivitetsplikt = useFeaturetoggle(FeatureToggle.aktivitetsplikt_ny_vurdering)

  return brukNyVurderingssideAktivitetsplikt ? (
    <Button size="small" as="a" href={`/aktivitet-vurdering/${oppgave.id}/${AktivitetspliktSteg.VURDERING}`}>
      Gå til vurdering
    </Button>
  ) : (
    <AktivitetspliktInfoModal oppgave={oppgave} oppdaterStatus={oppdaterStatus} />
  )
}
