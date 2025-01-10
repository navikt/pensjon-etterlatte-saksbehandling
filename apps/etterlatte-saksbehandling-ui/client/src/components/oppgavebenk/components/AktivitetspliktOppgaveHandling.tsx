import { Button } from '@navikt/ds-react'
import { AktivitetspliktSteg } from '~components/aktivitetsplikt/stegmeny/AktivitetspliktStegmeny'
import React from 'react'
import { OppgaveDTO, Oppgavestatus } from '~shared/types/oppgave'
import { FeatureToggle, useFeaturetoggle } from '~useUnleash'
import { AktivitetspliktInfoModal } from '~components/oppgavebenk/oppgaveModal/aktivitetsplikt/AktivitetspliktInfoModal'

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
  const brukNyVurderingssideAktivitetsplikt = useFeaturetoggle(FeatureToggle.aktivitetsplikt_ny_vurdering)

  return brukNyVurderingssideAktivitetsplikt ? (
    <Button size="small" as="a" href={`/aktivitet-vurdering/${oppgave.id}/${AktivitetspliktSteg.VURDERING}`}>
      Gå til vurdering
    </Button>
  ) : (
    <AktivitetspliktInfoModal oppgave={oppgave} oppdaterStatus={oppdaterStatus} />
  )
}
