import { useState } from 'react'
import { Alert, BodyShort, Heading, Radio, RadioGroup, Textarea, TextField, VStack } from '@navikt/ds-react'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { ForlengetOmsPeriodeOgResultat } from './ForlengetOmsPeriodeOgResultat'
import { ForlengetOmsVurdering } from '~shared/api/forlengetOms'
import { JaNei } from '~shared/types/ISvar'

export const ForlengetOmsUtdanning = ({
  behandling,
  redigerbar,
  vurdering,
  lagrer,
  onLagrerChange,
  onLagret,
}: {
  behandling: IBehandlingReducer
  redigerbar: boolean
  vurdering?: ForlengetOmsVurdering
  lagrer: boolean
  onLagrerChange: (lagrer: boolean) => void
  onLagret: (vurdering: ForlengetOmsVurdering) => void
}) => {
  const lagretAktivitet = vurdering?.aktivitet.type === 'UTDANNING' ? vurdering.aktivitet : undefined
  const [skole, setSkole] = useState(lagretAktivitet?.skole ?? '')
  const [studium, setStudium] = useState(lagretAktivitet?.studium ?? '')
  const [studererMinst50Prosent, setStudererMinst50Prosent] = useState<JaNei | ''>(
    lagretAktivitet?.studererMinst50Prosent ?? ''
  )
  const [visValideringsfeil, setVisValideringsfeil] = useState(false)
  const [resultatBegrunnelse, setResultatBegrunnelse] = useState(
    vurdering?.begrunnelse ?? behandling.vilkaarsvurdering?.resultat?.kommentar ?? ''
  )
  const vilkaarsvurdering = behandling.vilkaarsvurdering
  const sporsmaalRedigerbare = redigerbar && !lagrer && !vilkaarsvurdering?.resultat
  const begrunnelseTittel =
    studererMinst50Prosent === 'JA' ? 'Er utdanningen nødvendig og hensiktsmessig?' : 'Begrunn avslaget'

  const validerAktivitet = () => {
    setVisValideringsfeil(true)
    return (
      !!skole.trim() &&
      !!studium.trim() &&
      ['JA', 'NEI'].includes(studererMinst50Prosent) &&
      !!resultatBegrunnelse.trim()
    )
  }

  return (
    <>
      <VStack paddingInline="space-64 space-32" paddingBlock="space-24 space-0" gap="space-24">
        <VStack gap="space-16" maxWidth="30rem">
          <TextField
            label="Hvilken skole skal bruker studere ved?"
            value={skole}
            onChange={(event) => setSkole(event.target.value)}
            readOnly={!sporsmaalRedigerbare}
            error={visValideringsfeil && !skole.trim() ? 'Oppgi hvilken skole bruker skal studere ved.' : undefined}
          />
        </VStack>
        <VStack gap="space-16" maxWidth="30rem">
          <TextField
            label="Hva skal bruker studere?"
            value={studium}
            onChange={(event) => setStudium(event.target.value)}
            readOnly={!sporsmaalRedigerbare}
            error={visValideringsfeil && !studium.trim() ? 'Oppgi hva bruker skal studere.' : undefined}
          />
        </VStack>
      </VStack>
      <ForlengetOmsPeriodeOgResultat
        behandling={behandling}
        redigerbar={redigerbar}
        vurdering={vurdering}
        lagrer={lagrer}
        onLagrerChange={onLagrerChange}
        onLagret={onLagret}
        aktivitet={studererMinst50Prosent ? { type: 'UTDANNING', skole, studium, studererMinst50Prosent } : undefined}
        resultatBegrunnelse={{ tekst: resultatBegrunnelse, onChange: setResultatBegrunnelse }}
        validerAktivitet={validerAktivitet}
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
            error={
              visValideringsfeil && !studererMinst50Prosent ? 'Svar på om bruker studerer minst 50 prosent.' : undefined
            }
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
              label={begrunnelseTittel}
              description={
                studererMinst50Prosent === 'JA'
                  ? 'Begrunn hvorfor utdanningen er nødvendig og hensiktsmessig eller ikke'
                  : undefined
              }
              value={resultatBegrunnelse}
              onChange={(event) => setResultatBegrunnelse(event.target.value)}
              minRows={6}
              readOnly={!sporsmaalRedigerbare}
              error={visValideringsfeil && !resultatBegrunnelse.trim() ? 'Oppgi en begrunnelse.' : undefined}
            />
          </VStack>
        )}
      </ForlengetOmsPeriodeOgResultat>
    </>
  )
}
