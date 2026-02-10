import { IDetaljertBehandling, ITidligereFamiliepleier } from '~shared/types/IDetaljertBehandling'
import { SoeknadVurdering } from '../SoeknadVurdering'
import { useState } from 'react'
import { BodyShort, Box, Button, List, ReadMore, VStack } from '@navikt/ds-react'
import { TidligereFamiliepleierVurdering } from '~components/behandling/soeknadsoversikt/tidligereFamiliepleier/TidligereFamiliepleierVurdering'

const statusIkon = (tidligereFamiliepleier: ITidligereFamiliepleier | null) => {
  if (tidligereFamiliepleier) {
    return 'success'
  }
  return 'warning'
}

export const TidligereFamiliepleier = ({
  behandling,
  redigerbar,
}: {
  behandling: IDetaljertBehandling
  redigerbar: boolean
}) => {
  const [vurdert, setVurdert] = useState<boolean>(!!behandling.tidligereFamiliepleier)

  return (
    <SoeknadVurdering
      tittel="Tidligere familiepleier"
      hjemler={[
        {
          lenke: 'https://lovdata.no/pro/lov/1997-02-28-19/§17-15',
          tittel: 'Folketrygdloven § 17-15',
        },
      ]}
      status={statusIkon(behandling.tidligereFamiliepleier)}
    >
      <VStack gap="space-4" marginBlock="space-2" marginInline="space-0" maxWidth="41rem">
        <BodyShort>
          Tidligere familiepleier kan innvilges når det har vært nødvendig med tilsyn og pleie av en nærstående i minst
          fem år, og det ikke er mulig å forsørge seg selv etter at pleieforholdet er opphørt.
        </BodyShort>

        <ReadMore header="Les mer om hvilke vilkår som må oppfylles">
          <BodyShort>For å ha rett til ytelsen må tidligere familiepleier</BodyShort>
          <Box marginBlock="space-16" asChild>
            <List data-aksel-migrated-v8 as="ul">
              <List.Item>være medlem i trygden og ha vært det i minst fem år</List.Item>
              <List.Item>være ugift og ha vært ugift i minst fem år under pleieforholdet</List.Item>
              <List.Item>
                ha vært ute av stand til å forsørge seg selv med eget arbeid på grunn av pleieforholdet
              </List.Item>
              <List.Item>ikke hatt mulighet for å forsørge seg selv etter at pleieforholdet opphørte</List.Item>
              <List.Item>ikke ha tilstrekkelige midler til livsopphold</List.Item>
            </List>
          </Box>
          <BodyShort>I tillegg er det et krav at</BodyShort>
          <Box marginBlock="space-16" asChild>
            <List data-aksel-migrated-v8 as="ul">
              <List.Item>pleieforholdet må ha vart i minst fem år</List.Item>
              <List.Item>
                den som er pleid må ha mottatt pensjon fra folketrygden eller vært medlem i trygden i minst fem år frem
                til pleieforholdet opphørte
              </List.Item>
            </List>
          </Box>
        </ReadMore>
      </VStack>
      <Box
        paddingInline="space-2 space-0"
        minWidth="18.75rem"
        width="10rem"
        borderWidth="0 0 0 2"
        borderColor="neutral-subtle"
      >
        {vurdert && (
          <TidligereFamiliepleierVurdering
            redigerbar={redigerbar}
            setVurdert={(visVurderingKnapp: boolean) => setVurdert(visVurderingKnapp)}
            behandlingId={behandling.id}
          />
        )}
        {!vurdert && redigerbar && (
          <Button variant="secondary" onClick={() => setVurdert(true)}>
            Legg til vurdering
          </Button>
        )}
      </Box>
    </SoeknadVurdering>
  )
}
