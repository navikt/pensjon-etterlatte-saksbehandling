import { Alert, BodyShort, Box, Heading, HGrid, Label, Radio, RadioGroup, VStack } from '@navikt/ds-react'
import { addMonths, addYears, isValid } from 'date-fns'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { formaterDato } from '~utils/formatering/dato'
import { usePersonopplysningerOmsAvdoede } from '~components/person/usePersonopplysninger'
import { ForlengetOmsUtdanning } from './ForlengetOmsUtdanning'
import { ForlengetOmsArbeidsrettetTiltak } from './ForlengetOmsArbeidsrettetTiltak'
import { ForlengetOmsIngenAktivitet } from './ForlengetOmsIngenAktivitet'
import { ResultatForlengetOms } from './ResultatForlengetOms'
import { mapFailure } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { ForlengetOmsProvider, useForlengetOms } from './ForlengetOmsContext'

export const ForlengetOmsAktivitet = ({ behandling }: { behandling: IBehandlingReducer }) => (
  <ForlengetOmsProvider key={behandling.id} behandling={behandling}>
    <ForlengetOmsAktivitetInnhold />
  </ForlengetOmsProvider>
)

const ForlengetOmsAktivitetInnhold = () => {
  const { utkast, velgAktivitet, sporsmaalRedigerbare, lagreStatus } = useForlengetOms()
  const avdoede = usePersonopplysningerOmsAvdoede()
  const doedsdato = avdoede?.opplysning.doedsdato
  const gyldigDoedsdato = doedsdato && isValid(new Date(doedsdato)) ? new Date(doedsdato) : undefined
  const viktigeDatoer = [
    { tittel: 'Avdødes dødsdato', dato: gyldigDoedsdato },
    { tittel: '6 mnd etter dødsfall', dato: gyldigDoedsdato ? addMonths(gyldigDoedsdato, 6) : undefined },
    { tittel: '12 mnd etter dødsfall', dato: gyldigDoedsdato ? addMonths(gyldigDoedsdato, 12) : undefined },
    { tittel: '3 år etter dødsfall', dato: gyldigDoedsdato ? addYears(gyldigDoedsdato, 3) : undefined },
  ]
  return (
    <>
      <Box paddingInline="space-64" paddingBlock="space-48 space-16">
        <Heading size="large" level="1">
          Vurdering av aktivitet
        </Heading>
      </Box>
      <VStack paddingInline="space-64 space-32" paddingBlock="space-16" gap="space-24">
        <VStack gap="space-16">
          <Label as="p">Viktige datoer</Label>
          <HGrid columns={4} gap="space-24">
            {viktigeDatoer.map(({ tittel, dato }) => (
              <VStack key={tittel} gap="space-8">
                <Label as="p">{tittel}</Label>
                <BodyShort>{dato ? formaterDato(dato) : 'Ukjent dato'}</BodyShort>
              </VStack>
            ))}
          </HGrid>
          {!gyldigDoedsdato && (
            <Alert variant="warning">
              Dødsdato mangler eller er ugyldig. Datoene etter dødsfall kan ikke beregnes.
            </Alert>
          )}
        </VStack>
        <VStack gap="space-16">
          <RadioGroup
            legend="Tar bruker utdanning eller deltar i arbeidsrettet tiltak?"
            value={utkast.aktivitetType}
            onChange={velgAktivitet}
            disabled={!sporsmaalRedigerbare}
          >
            <Radio value="UTDANNING">Ja, bruker tar utdanning</Radio>
            <Radio value="ARBEIDSRETTET_TILTAK">Ja, bruker deltar i arbeidsrettet tiltak</Radio>
            <Radio value="NEI">Nei</Radio>
          </RadioGroup>
        </VStack>
        {mapFailure(lagreStatus, (error) => (
          <ApiErrorAlert>Kunne ikke lagre vurderingen av aktivitet: {error.detail}</ApiErrorAlert>
        ))}
      </VStack>
      {utkast.aktivitetType === 'UTDANNING' && <ForlengetOmsUtdanning />}
      {utkast.aktivitetType === 'ARBEIDSRETTET_TILTAK' && <ForlengetOmsArbeidsrettetTiltak />}
      {utkast.aktivitetType === 'NEI' && <ForlengetOmsIngenAktivitet />}
      <ResultatForlengetOms />
    </>
  )
}
