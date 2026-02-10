import { formaterNavn, IPdlPerson } from '~shared/types/Person'
import { BodyShort, Heading, HStack, Table, VStack } from '@navikt/ds-react'
import { ChildHairEyesIcon } from '@navikt/aksel-icons'
import React from 'react'
import { hentAlderForDato } from '~components/behandling/felles/utils'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'
import { PdlPersonAktivEllerSisteAdresse } from '~components/behandling/soeknadsoversikt/familieforhold/AktivEllerSisteAdresse'
import { BarnAddressePeriode } from '~components/behandling/soeknadsoversikt/familieforhold/BarnAddressePeriode'
import { Personopplysninger } from '~shared/types/grunnlag'
import { SakType } from '~shared/types/sak'
import { DoedsdatoTag } from '~shared/tags/DoedsdatoTag'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'

interface Props {
  sakType: SakType
}

function erBarnTilPerson(person: IPdlPerson, barnIdent: string): boolean {
  return !!(person.familieRelasjon?.barn ?? []).find((ident) => ident == barnIdent)
}

function hvemErBarnetsForeldre(sakType: SakType, personopplysninger: Personopplysninger, barn: IPdlPerson): string {
  if (personopplysninger.avdoede.length >= 2) {
    const [avdoedEn, avdoedTo] = personopplysninger.avdoede
    const harAvdoedEnSomForelder = erBarnTilPerson(avdoedEn.opplysning, barn.foedselsnummer)
    const harAvdoedToSomForelder = erBarnTilPerson(avdoedTo.opplysning, barn.foedselsnummer)

    if (harAvdoedEnSomForelder && harAvdoedToSomForelder) {
      return 'Begge avdøde'
    } else if (harAvdoedEnSomForelder) {
      return `Kun ${formaterNavn(avdoedEn.opplysning)}`
    } else {
      return `Kun ${formaterNavn(avdoedTo.opplysning)}`
    }
  } else {
    // Hvis det er en omstillingsstønad-sak er søker gjenlevende
    const aktuelleGjenlevende =
      sakType === SakType.OMSTILLINGSSTOENAD
        ? [personopplysninger.soeker!, ...personopplysninger.gjenlevende]
        : personopplysninger.gjenlevende

    const harGjenlevendeSomForelder = aktuelleGjenlevende.some((gjenlevende) =>
      erBarnTilPerson(gjenlevende.opplysning, barn.foedselsnummer)
    )
    if (harGjenlevendeSomForelder) {
      if (sakType === SakType.BARNEPENSJON) {
        return 'Avdød og biologisk forelder'
      } else {
        return 'Avdød og gjenlevende'
      }
    } else {
      return 'Kun avdød'
    }
  }
}

export const TabellOverAvdoedesBarn = ({ sakType }: Props) => {
  const opplysninger = usePersonopplysninger()
  if (!opplysninger) {
    return null
  }

  // Filtrerer bort duplikate søsken
  const avdoedesBarn = opplysninger.avdoede
    .flatMap((avdoed) => avdoed.opplysning.avdoedesBarn ?? [])
    .filter((b, index, arr) => index === arr.findIndex((t) => t?.foedselsnummer === b.foedselsnummer))

  return (
    <VStack gap="space-4">
      <HStack gap="space-4" justify="start" align="center" wrap={false}>
        <ChildHairEyesIcon fontSize="1.75rem" aria-hidden />
        <Heading size="small" level="3">
          Avdødes barn
        </Heading>
      </HStack>
      <Table size="small">
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell scope="col">Navn</Table.HeaderCell>
            <Table.HeaderCell scope="col">Fødselsnummer</Table.HeaderCell>
            <Table.HeaderCell scope="col">Bostedsadresse</Table.HeaderCell>
            <Table.HeaderCell scope="col">Periode</Table.HeaderCell>
            <Table.HeaderCell scope="col">Foreldre</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {!!avdoedesBarn.length ? (
            avdoedesBarn.map((barn, index) => (
              <Table.Row key={index}>
                <Table.DataCell>
                  <HStack gap="space-2" justify="start" align="center" wrap={false}>
                    <BodyShort>{`${formaterNavn(barn)} (${hentAlderForDato(barn.foedselsdato)} år)`}</BodyShort>
                    <DoedsdatoTag doedsdato={barn.doedsdato} />
                  </HStack>
                </Table.DataCell>
                <Table.DataCell>
                  <KopierbarVerdi value={barn.foedselsnummer} iconPosition="right" />
                </Table.DataCell>
                <Table.DataCell>
                  <PdlPersonAktivEllerSisteAdresse person={barn} />
                </Table.DataCell>
                <Table.DataCell>
                  <BarnAddressePeriode barn={barn} />
                </Table.DataCell>
                <Table.DataCell>{hvemErBarnetsForeldre(sakType, opplysninger, barn)}</Table.DataCell>
              </Table.Row>
            ))
          ) : (
            <Table.Row>
              <Table.DataCell colSpan={5}>
                <Heading size="small">Avdøde har ingen barn</Heading>
              </Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    </VStack>
  )
}
