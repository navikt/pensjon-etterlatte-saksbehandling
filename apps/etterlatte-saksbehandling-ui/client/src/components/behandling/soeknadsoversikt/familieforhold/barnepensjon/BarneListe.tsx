import { ChildEyesIcon } from '@navikt/aksel-icons'
import { CopyButton } from '@navikt/ds-react'
import { Heading, Link, Table } from '@navikt/ds-react'
import { IFamilieforhold, IPdlPerson } from '~shared/types/Person'
import styled from 'styled-components'
import { IAdresse } from '~shared/types/IAdresse'
import { differenceInYears, format, parse } from 'date-fns'
import { DatoFormat, formaterFnr } from '~utils/formattering'
import { FlexHeader, IconWrapper, TableWrapper } from '~components/behandling/soeknadsoversikt/familieforhold/styled'
import { IconSize } from '~shared/types/Icon'

const FnrWrapper = styled.div`
  display: flex;
`

type Props = {
  familieforhold: IFamilieforhold
  soekerFnr: string
}

export const BarneListe = ({ familieforhold, soekerFnr }: Props) => {
  const barneListe = (familieforhold.avdoede?.opplysning.avdoedesBarn ?? []).filter(
    (person) => person.foedselsnummer !== soekerFnr
  )

  return (
    <div>
      <FlexHeader>
        <IconWrapper>
          <ChildEyesIcon fontSize={IconSize.DEFAULT} />
        </IconWrapper>
        <Heading size="small" level="3">
          Avdødes barn (søsken)
        </Heading>
      </FlexHeader>
      <TableWrapper>
        <Table size="small">
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell scope={'col'}>Navn</Table.HeaderCell>
              <Table.HeaderCell scope={'col'}>Fødselsnummer</Table.HeaderCell>
              <Table.HeaderCell scope={'col'}>Bostedsadresse</Table.HeaderCell>
              <Table.HeaderCell scope={'col'}>Periode</Table.HeaderCell>
              <Table.HeaderCell scope={'col'}>Foreldre</Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {barneListe.length ? (
              barneListe.map((barn, i) => {
                return <BarnRow key={i + barn.foedselsnummer} barn={barn} familieforhold={familieforhold} />
              })
            ) : (
              <Table.Row>
                <Table.DataCell colSpan={5} aria-colspan={5} align={'center'}>
                  Avdøde har ingen andre barn enn søker
                </Table.DataCell>
              </Table.Row>
            )}
          </Table.Body>
        </Table>
      </TableWrapper>
    </div>
  )
}

const BarnRow = ({ barn, familieforhold }: { barn: IPdlPerson; familieforhold: IFamilieforhold }) => {
  const foedselsdato = parse(String(barn.foedselsdato), DatoFormat.AAR_MAANED_DAG, new Date())
  const alder = differenceInYears(new Date(), foedselsdato)

  const navn = `${barn.fornavn} ${barn.etternavn} (${alder} år)`

  const aktivAdresse: IAdresse | undefined = barn.bostedsadresse?.find((adresse: IAdresse) => adresse.aktiv)
  const adresse = `${aktivAdresse?.adresseLinje1}, ${aktivAdresse?.postnr ?? ''} ${aktivAdresse?.poststed ?? ''}`
  const periode = aktivAdresse
    ? `${format(new Date(aktivAdresse!!.gyldigFraOgMed!!), DatoFormat.DAG_MAANED_AAR)} - ${
        aktivAdresse?.gyldigTilOgMed
          ? format(new Date(aktivAdresse!!.gyldigTilOgMed!!), DatoFormat.DAG_MAANED_AAR)
          : 'nå'
      }`
    : 'Mangler adresse'

  const gjenlevendeFnr = familieforhold.gjenlevende.opplysning.foedselsnummer
  const erGjenlevendesBarn = barn.familieRelasjon?.ansvarligeForeldre?.includes(gjenlevendeFnr)

  return (
    <Table.Row>
      <Table.DataCell>{navn}</Table.DataCell>
      <Table.DataCell>
        <FnrWrapper>
          <Link href={`/person/${barn.foedselsnummer}`} target="_blank" rel="noreferrer noopener">
            {formaterFnr(barn.foedselsnummer)}
          </Link>
          <CopyButton copyText={barn.foedselsnummer} size={'small'} />
        </FnrWrapper>
      </Table.DataCell>
      <Table.DataCell>{adresse}</Table.DataCell>
      <Table.DataCell>{periode}</Table.DataCell>
      <Table.DataCell>{erGjenlevendesBarn ? 'Gjenlevende og avdød' : 'Kun avdød'}</Table.DataCell>
    </Table.Row>
  )
}
