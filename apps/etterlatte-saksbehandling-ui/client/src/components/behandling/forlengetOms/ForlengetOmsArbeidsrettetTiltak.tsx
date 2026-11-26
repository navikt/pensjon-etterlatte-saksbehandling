import { Radio, RadioGroup, Textarea, TextField, VStack } from '@navikt/ds-react'
import { ForlengetOmsPeriode } from './ForlengetOmsPeriode'
import { useForlengetOms } from './ForlengetOmsContext'
import { Tiltaksomfang } from '~shared/api/forlengetOms'
import { JaNei } from '~shared/types/ISvar'

export const ForlengetOmsArbeidsrettetTiltak = () => {
  const { sporsmaalRedigerbare, arbeidsrettetTiltak, oppdaterArbeidsrettetTiltak } = useForlengetOms()
  const { tiltak, omfang, aktivitetsplan, begrunnelse } = arbeidsrettetTiltak

  return (
    <>
      <VStack paddingInline="space-64 space-32" paddingBlock="space-24 space-0" gap="space-24">
        <VStack gap="space-16" maxWidth="30rem">
          <TextField
            label="Hvilket tiltak deltar bruker i?"
            value={tiltak}
            onChange={(event) => oppdaterArbeidsrettetTiltak({ tiltak: event.target.value })}
            readOnly={!sporsmaalRedigerbare}
          />
        </VStack>
        <VStack gap="space-16">
          <RadioGroup
            legend="Deltar bruker i tiltaket på heltid eller deltid?"
            value={omfang}
            onChange={(verdi: Tiltaksomfang) => oppdaterArbeidsrettetTiltak({ omfang: verdi })}
            disabled={!sporsmaalRedigerbare}
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
            disabled={!sporsmaalRedigerbare}
          >
            <Radio value="JA">Ja, bruker har en aktivitetsplan</Radio>
            <Radio value="NEI">Nei, bruker har ikke en aktivitetsplan</Radio>
          </RadioGroup>
        </VStack>
      </VStack>
      <ForlengetOmsPeriode>
        <VStack gap="space-16" maxWidth="30rem">
          <Textarea
            label="Begrunnelse"
            value={begrunnelse}
            onChange={(event) => oppdaterArbeidsrettetTiltak({ begrunnelse: event.target.value })}
            minRows={6}
            readOnly={!sporsmaalRedigerbare}
          />
        </VStack>
      </ForlengetOmsPeriode>
    </>
  )
}
