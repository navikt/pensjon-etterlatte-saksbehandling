import React from 'react'
import { Box, ReadMore } from '@navikt/ds-react'
import { SakType } from '~shared/types/sak'

export const EksportvurderingReadMore = ({
  sakType,
  virkningstidspunktEtterNyRegelDato,
}: {
  sakType: SakType
  virkningstidspunktEtterNyRegelDato?: boolean
}) => {
  const eksportVurderingTekst = () => {
    switch (sakType) {
      case SakType.BARNEPENSJON:
        return `Poengår skal registreres kun ved eksportvurdering når pensjonen er innvilget etter unntaksregelen om
              at avdød har mindre enn 20 års botid, ${virkningstidspunktEtterNyRegelDato ? 'men har minimum tre poengår' : 'men har opptjent rett til tilleggspensjon'}. 
              Når poengår registreres, blir trygdetiden beregnet til antall poengår.`
      case SakType.OMSTILLINGSSTOENAD:
        return `Poengår skal registreres kun ved eksportvurdering når stønaden er innvilget etter unntaksregelen om
        at avdød har mindre enn 20 års botid, men har minimum tre poengår. Når poengår registreres, blir trygdetiden
        beregnet til antall poengår.`
    }
  }

  return (
    <Box maxWidth="42.5rem">
      <ReadMore header="Mer om eksportvurdering">{eksportVurderingTekst()}</ReadMore>
    </Box>
  )
}
