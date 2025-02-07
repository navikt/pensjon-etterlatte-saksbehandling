import { IAktivitetspliktVurderingNyDto } from '~shared/types/Aktivitetsplikt'
import { FeatureToggle, useFeaturetoggle } from '~useUnleash'
import { Alert, Box } from '@navikt/ds-react'
import React from 'react'
import { formaterDato } from '~utils/formatering/dato'

export function InformasjonUnntakOppfoelging(props: { vurdering: IAktivitetspliktVurderingNyDto }) {
  const oppgaveFristUtloeper = useFeaturetoggle(FeatureToggle.aktivitetsplikt_oppgave_unntak_med_frist)
  const oppgaveIngenFrist = useFeaturetoggle(FeatureToggle.aktivitetsplikt_oppgave_unntak_uten_frist)

  const {
    vurdering: { unntak },
  } = props

  const unntakIngenFrist = unntak.find((unntak) => !unntak.tom)
  const unntakMedFrist = unntak.find((unntak) => !!unntak.tom && new Date(unntak.tom) >= new Date())

  return (
    <>
      {oppgaveFristUtloeper && !!unntakMedFrist && (
        <Box width="fit-content">
          <Alert variant="info">
            Du har lagt til et unntak med utløp {formaterDato(unntakMedFrist.tom)}. To måneder før fristen blir det
            opprettet en oppfølgingsoppgave.
          </Alert>
        </Box>
      )}
      {oppgaveIngenFrist && !!unntakIngenFrist && (
        <Box width="fit-content">
          <Alert variant="info">
            Du har lagt til et unntak uten frist. Når denne behandlingen om aktivtetplikt er ferdig blir det opprettet
            en oppfølgingsoppgave med frist to måneder fram i tid.
          </Alert>
        </Box>
      )}
    </>
  )
}
