import { useState } from 'react'
import { Radio, RadioGroup, Textarea, TextField, VStack } from '@navikt/ds-react'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { ForlengetOmsPeriodeOgResultat } from './ForlengetOmsPeriodeOgResultat'

export const ForlengetOmsArbeidsrettetTiltak = ({
  behandling,
  redigerbar,
}: {
  behandling: IBehandlingReducer
  redigerbar: boolean
}) => {
  const [tiltak, setTiltak] = useState('')
  const [omfang, setOmfang] = useState('')
  const [aktivitetsplan, setAktivitetsplan] = useState('')
  const [begrunnelse, setBegrunnelse] = useState(behandling.vilkaarsvurdering?.resultat?.kommentar ?? '')
  const [visValideringsfeil, setVisValideringsfeil] = useState(false)
  const sporsmaalRedigerbare = redigerbar && !behandling.vilkaarsvurdering?.resultat

  const validerAktivitet = () => {
    setVisValideringsfeil(true)
    return (
      !!tiltak.trim() &&
      ['HELTID', 'DELTID'].includes(omfang) &&
      ['JA', 'NEI'].includes(aktivitetsplan) &&
      !!begrunnelse.trim()
    )
  }

  return (
    <>
      <VStack paddingInline="space-64 space-32" paddingBlock="space-24 space-0" gap="space-24">
        <VStack gap="space-16" maxWidth="30rem">
          <TextField
            label="Hvilket tiltak deltar bruker i?"
            value={tiltak}
            onChange={(event) => setTiltak(event.target.value)}
            readOnly={!sporsmaalRedigerbare}
            error={visValideringsfeil && !tiltak.trim() ? 'Oppgi hvilket tiltak bruker deltar i.' : undefined}
          />
        </VStack>
        <VStack gap="space-16">
          <RadioGroup
            legend="Deltar bruker i tiltaket på heltid eller deltid?"
            value={omfang}
            onChange={setOmfang}
            disabled={!sporsmaalRedigerbare}
            error={visValideringsfeil && !omfang ? 'Velg om bruker deltar på heltid eller deltid.' : undefined}
          >
            <Radio value="HELTID">Heltid</Radio>
            <Radio value="DELTID">Deltid</Radio>
          </RadioGroup>
        </VStack>
        <VStack gap="space-16">
          <RadioGroup
            legend="Finnes det en aktivitetsplan?"
            value={aktivitetsplan}
            onChange={setAktivitetsplan}
            disabled={!sporsmaalRedigerbare}
            error={visValideringsfeil && !aktivitetsplan ? 'Svar på om bruker har en aktivitetsplan.' : undefined}
          >
            <Radio value="JA">Ja, bruker har en aktivitetsplan</Radio>
            <Radio value="NEI">Nei, bruker har ikke en aktivitetsplan</Radio>
          </RadioGroup>
        </VStack>
      </VStack>
      <ForlengetOmsPeriodeOgResultat
        behandling={behandling}
        redigerbar={redigerbar}
        resultatBegrunnelse={{ tekst: begrunnelse, onChange: setBegrunnelse }}
        validerAktivitet={validerAktivitet}
      >
        <VStack gap="space-16" maxWidth="30rem">
          <Textarea
            label="Begrunnelse"
            value={begrunnelse}
            onChange={(event) => setBegrunnelse(event.target.value)}
            minRows={6}
            readOnly={!sporsmaalRedigerbare}
            error={visValideringsfeil && !begrunnelse.trim() ? 'Oppgi en begrunnelse.' : undefined}
          />
        </VStack>
      </ForlengetOmsPeriodeOgResultat>
    </>
  )
}
