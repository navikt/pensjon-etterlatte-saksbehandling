import { BodyShort, Box, Heading, HGrid, Label, Radio, RadioGroup, VStack } from '@navikt/ds-react'
import { addMonths, addYears } from 'date-fns'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { formaterDato } from '~utils/formatering/dato'
import { usePersonopplysningerOmsAvdoede } from '~components/person/usePersonopplysninger'
import { ForlengetOmsUtdanning } from './ForlengetOmsUtdanning'
import { ForlengetOmsArbeidsrettetTiltak } from './ForlengetOmsArbeidsrettetTiltak'
import { ResultatForlengetOms } from './ResultatForlengetOms'
import { ForlengetOmsProvider, useForlengetOms } from './ForlengetOmsContext'

export const ForlengetOmsAktivitet = ({ behandling }: { behandling: IBehandlingReducer }) => (
  <ForlengetOmsProvider key={behandling.id} behandling={behandling}>
    <ForlengetOmsAktivitetInnhold />
  </ForlengetOmsProvider>
)

const ForlengetOmsAktivitetInnhold = () => {
  const { aktivitetType, velgAktivitet, sporsmaalRedigerbare } = useForlengetOms()
  const avdoede = usePersonopplysningerOmsAvdoede()
  const doedsdato = avdoede?.opplysning.doedsdato
  const viktigeDatoer = [
    { tittel: 'Avdødes dødsdato', dato: doedsdato },
    { tittel: '6 mnd etter dødsfall', dato: doedsdato ? addMonths(doedsdato, 6) : undefined },
    { tittel: '12 mnd etter dødsfall', dato: doedsdato ? addMonths(doedsdato, 12) : undefined },
    { tittel: '3 år etter dødsfall', dato: doedsdato ? addYears(doedsdato, 3) : undefined },
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
        </VStack>
        <VStack gap="space-16">
          <RadioGroup
            legend="Tar bruker utdanning eller deltar i arbeidsrettet tiltak?"
            value={aktivitetType}
            onChange={velgAktivitet}
            disabled={!sporsmaalRedigerbare}
          >
            <Radio value="UTDANNING">Ja, bruker tar utdanning</Radio>
            <Radio value="ARBEIDSRETTET_TILTAK">Ja, bruker deltar i arbeidsrettet tiltak</Radio>
            <Radio value="NEI">Nei</Radio>
          </RadioGroup>
        </VStack>
      </VStack>
      {aktivitetType === 'UTDANNING' && <ForlengetOmsUtdanning />}
      {aktivitetType === 'ARBEIDSRETTET_TILTAK' && <ForlengetOmsArbeidsrettetTiltak />}
      <ResultatForlengetOms />
    </>
  )
}
