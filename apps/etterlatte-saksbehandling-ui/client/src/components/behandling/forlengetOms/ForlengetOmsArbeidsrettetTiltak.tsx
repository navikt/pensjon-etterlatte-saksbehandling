import { Radio, RadioGroup, TextField, VStack } from '@navikt/ds-react'
import { ForlengetOmsPeriode } from './ForlengetOmsPeriode'
import { useForlengetOms } from './ForlengetOmsContext'
import { Tiltaksomfang } from '~shared/api/forlengetOms'
import { JaNei } from '~shared/types/ISvar'

export const ForlengetOmsArbeidsrettetTiltak = () => {
  const { kanRedigere, arbeidsrettetTiltak, oppdaterArbeidsrettetTiltak } = useForlengetOms()
  const { tiltak, omfang, aktivitetsplan } = arbeidsrettetTiltak

  return (
    <>
      <VStack paddingInline="space-64 space-32" paddingBlock="space-24 space-0" gap="space-24">
        <VStack gap="space-16" maxWidth="30rem">
          <TextField
            label="Hvilket tiltak deltar bruker i?"
            value={tiltak}
            onChange={(event) => oppdaterArbeidsrettetTiltak({ tiltak: event.target.value })}
            readOnly={!kanRedigere}
          />
        </VStack>
        <VStack gap="space-16">
          <RadioGroup
            legend="Deltar bruker i tiltaket på heltid eller deltid?"
            value={omfang}
            onChange={(verdi: Tiltaksomfang) => oppdaterArbeidsrettetTiltak({ omfang: verdi })}
            disabled={!kanRedigere}
          >
            <Radio value="HELTID">Heltid</Radio>
            <Radio value="DELTID">Deltid</Radio>
          </RadioGroup>
        </VStack>
        <VStack gap="space-16">
          <RadioGroup
            legend="Finnes det en aktivitetsplan?"
            value={aktivitetsplan}
            onChange={(verdi: JaNei) => oppdaterArbeidsrettetTiltak({ aktivitetsplan: verdi })}
            disabled={!kanRedigere}
          >
            <Radio value="JA">Ja, bruker har en aktivitetsplan</Radio>
            <Radio value="NEI">Nei, bruker har ikke en aktivitetsplan</Radio>
          </RadioGroup>
        </VStack>
      </VStack>
      <ForlengetOmsPeriode />
    </>
  )
}
