import { IDetaljertBehandling, ITidligereFamiliepleier } from '~shared/types/IDetaljertBehandling'
import { LovtekstMedLenke } from '../LovtekstMedLenke'
import { Informasjon, Vurdering } from '../styled'
import { useState } from 'react'
import { BodyShort, Button, List, VStack } from '@navikt/ds-react'
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
    <LovtekstMedLenke
      tittel="Tidligere familiepleier"
      hjemler={[
        {
          lenke: 'https://lovdata.no/pro/lov/1997-02-28-19/§17-15',
          tittel: 'Folketrygdloven § 17-15',
        },
      ]}
      status={statusIkon(behandling.tidligereFamiliepleier)}
    >
      <Informasjon>
        <VStack gap="4">
          <BodyShort>
            Tidligere familiepleier kan innvilges når det har vært nødvendig med tilsyn og pleie av en nærstående i
            minst fem år, og det ikke er mulig å forsørge seg selv etter at pleieforholdet er opphørt.
          </BodyShort>
          <BodyShort>
            Tidligere familiepleier må oppfylle følgende vilkår for å ha rett til ytelsen etter dette kapitlet:
          </BodyShort>
          <List as="ul" title="For å ha rett til ytelsen må tidligere familiepleier">
            <List.Item>være medlem i trygden og ha vært det i minst fem år</List.Item>
            <List.Item>være ugift og ha vært ugift i minst fem år under pleieforholdet</List.Item>
            <List.Item>
              ha vært ute av stand til å forsørge seg selv med eget arbeid på grunn av pleieforholdet
            </List.Item>
            <List.Item>ikke hatt mulighet for å forsørge seg selv etter at pleieforholdet opphørte</List.Item>
            <List.Item>Ikke ha tilstrekkelige midler til livsopphold</List.Item>
          </List>
          <List as="ul" title="I tillegg er det et krav at:">
            <List.Item>pleieforholdet må ha vart i minst fem år</List.Item>
            <List.Item>
              den som er pleid må ha mottatt pensjon fra folketrygden eller vært medlem i trygden i minst fem år frem
              til pleieforholdet opphørte
            </List.Item>
          </List>
        </VStack>
      </Informasjon>
      <Vurdering>
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
      </Vurdering>
    </LovtekstMedLenke>
  )
}
