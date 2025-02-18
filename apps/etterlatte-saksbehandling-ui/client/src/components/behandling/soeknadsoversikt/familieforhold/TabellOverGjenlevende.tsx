import { Personopplysning } from '~shared/types/grunnlag'
import { ILand } from '~utils/kodeverk'
import { Heading, HStack, Table, VStack } from '@navikt/ds-react'
import { PersonIcon } from '@navikt/aksel-icons'
import { formaterNavn, formaterSivilstatusTilLesbarStreng } from '~shared/types/Person'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'
import { ListeOverInnflyttingTilNorge } from '~components/behandling/soeknadsoversikt/familieforhold/ListeOverInnflyttingTilNorge'
import { ListeOverUtflyttingFraNorge } from '~components/behandling/soeknadsoversikt/familieforhold/ListeOverUtflyttingFraNorge'
import { finnLandSomTekst } from '~components/person/personopplysninger/utils'
import React from 'react'
import { AktivEllerSisteAdresse } from '~components/behandling/soeknadsoversikt/familieforhold/AktivEllerSisteAdresse'

interface Props {
  gjenlevende: Personopplysning[] | undefined
  alleLand: ILand[]
}

export const TabellOverGjenlevende = ({ gjenlevende, alleLand }: Props) => {
  return (
    <VStack gap="4">
      <HStack gap="4" justify="start" align="center" wrap={false}>
        <PersonIcon fontSize="1.5rem" aria-hidden />
        <Heading size="small" level="3">
          Gjenlevende
        </Heading>
      </HStack>
      <Table size="small">
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell scope="col">Navn</Table.HeaderCell>
            <Table.HeaderCell scope="col">Fødselsnummer</Table.HeaderCell>
            <Table.HeaderCell scope="col">Bostedsadresse</Table.HeaderCell>
            <Table.HeaderCell scope="col">Innflyttning</Table.HeaderCell>
            <Table.HeaderCell scope="col">Utflytning</Table.HeaderCell>
            <Table.HeaderCell scope="col">Statsborgerskap</Table.HeaderCell>
            <Table.HeaderCell scope="col">Sivilstand</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {!!gjenlevende?.length ? (
            gjenlevende.map((levende, index) => (
              <Table.Row key={index}>
                <Table.DataCell>{formaterNavn(levende.opplysning)}</Table.DataCell>
                <Table.DataCell>
                  <KopierbarVerdi value={levende.opplysning.foedselsnummer} iconPosition="right" />
                </Table.DataCell>
                <Table.DataCell>
                  <AktivEllerSisteAdresse person={levende} />
                </Table.DataCell>
                <Table.DataCell>
                  <ListeOverInnflyttingTilNorge
                    innflyttingTilNorge={levende.opplysning.utland?.innflyttingTilNorge}
                    alleLand={alleLand}
                  />
                </Table.DataCell>
                <Table.DataCell>
                  <ListeOverUtflyttingFraNorge
                    utflyttingFraNorge={levende.opplysning.utland?.utflyttingFraNorge}
                    alleLand={alleLand}
                  />
                </Table.DataCell>
                <Table.DataCell>
                  {!!levende.opplysning.statsborgerskap
                    ? finnLandSomTekst(levende.opplysning.statsborgerskap, alleLand)
                    : 'Ingen statsborgerskap'}
                </Table.DataCell>
                <Table.DataCell>{formaterSivilstatusTilLesbarStreng(levende.opplysning.sivilstatus)}</Table.DataCell>
              </Table.Row>
            ))
          ) : (
            <Table.Row>
              <Table.DataCell colSpan={7}>
                <Heading size="small"> Ingen gjenlevende</Heading>
              </Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    </VStack>
  )
}
