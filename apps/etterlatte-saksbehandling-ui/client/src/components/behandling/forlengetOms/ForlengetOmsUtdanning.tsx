import { useState } from 'react'
import { Alert, BodyShort, Heading, Radio, RadioGroup, Textarea, TextField, VStack } from '@navikt/ds-react'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { ForlengetOmsPeriodeOgResultat } from './ForlengetOmsPeriodeOgResultat'
import { ForlengetOmsVurdering, LagreForlengetOmsVurdering } from '~shared/api/forlengetOms'
import { JaNei } from '~shared/types/ISvar'

export const ForlengetOmsUtdanning = ({
  behandling,
  redigerbar,
  vurdering,
  lagrer,
  onLagre,
}: {
  behandling: IBehandlingReducer
  redigerbar: boolean
  vurdering?: ForlengetOmsVurdering
  lagrer: boolean
  onLagre: (request: LagreForlengetOmsVurdering) => void
}) => {
  const lagretAktivitet = vurdering?.aktivitet.type === 'UTDANNING' ? vurdering.aktivitet : undefined
  const [skole, setSkole] = useState(lagretAktivitet?.skole ?? '')
  const [studium, setStudium] = useState(lagretAktivitet?.studium ?? '')
  const [studererMinst50Prosent, setStudererMinst50Prosent] = useState<JaNei | ''>(
    lagretAktivitet?.studererMinst50Prosent ?? ''
  )
  const [resultatBegrunnelse, setResultatBegrunnelse] = useState(
    vurdering?.begrunnelse ?? behandling.vilkaarsvurdering?.resultat?.kommentar ?? ''
  )
  const vilkaarsvurdering = behandling.vilkaarsvurdering
  const sporsmaalRedigerbare = redigerbar && !lagrer && !vilkaarsvurdering?.resultat

  return (
    <>
      <VStack paddingInline="space-64 space-32" paddingBlock="space-24 space-0" gap="space-24">
        <VStack gap="space-16" maxWidth="30rem">
          <TextField
            label="Hvilken skole skal bruker studere ved?"
            value={skole}
            onChange={(event) => setSkole(event.target.value)}
            readOnly={!sporsmaalRedigerbare}
          />
        </VStack>
        <VStack gap="space-16" maxWidth="30rem">
          <TextField
            label="Hva skal bruker studere?"
            value={studium}
            onChange={(event) => setStudium(event.target.value)}
            readOnly={!sporsmaalRedigerbare}
          />
        </VStack>
      </VStack>
      <ForlengetOmsPeriodeOgResultat
        behandling={behandling}
        redigerbar={redigerbar}
        vurdering={vurdering}
        lagrer={lagrer}
        onLagre={onLagre}
        aktivitet={studererMinst50Prosent ? { type: 'UTDANNING', skole, studium, studererMinst50Prosent } : undefined}
        begrunnelse={resultatBegrunnelse}
        innvilgelseFeil={
          studererMinst50Prosent === 'NEI' ? 'Personen må studere minst 50% for å få stønad.' : undefined
        }
      >
        <VStack gap="space-16">
          <RadioGroup
            legend="Studerer bruker minst 50 prosent?"
            value={studererMinst50Prosent}
            onChange={setStudererMinst50Prosent}
            disabled={!sporsmaalRedigerbare}
          >
            <Radio value="JA">Ja, bruker studerer minst 50 prosent</Radio>
            <Radio value="NEI">Nei, prosenten er lavere</Radio>
          </RadioGroup>
          {studererMinst50Prosent === 'NEI' && (
            <Alert variant="error">
              <Heading size="small" level="3" spacing>
                Ikke rett til forlenget omstillingstønad
              </Heading>
              <BodyShort>Personen må studere minst 50% for å få stønad</BodyShort>
            </Alert>
          )}
        </VStack>
        {studererMinst50Prosent && (
          <VStack gap="space-16" maxWidth="30rem">
            <Textarea
              label={studererMinst50Prosent === 'JA' ? 'Er utdanningen nødvendig og hensiktsmessig?' : 'Begrunnelse'}
              description={
                studererMinst50Prosent === 'JA'
                  ? 'Begrunn hvorfor utdanningen er nødvendig og hensiktsmessig eller ikke'
                  : undefined
              }
              value={resultatBegrunnelse}
              onChange={(event) => setResultatBegrunnelse(event.target.value)}
              minRows={6}
              readOnly={!sporsmaalRedigerbare}
            />
          </VStack>
        )}
      </ForlengetOmsPeriodeOgResultat>
    </>
  )
}
