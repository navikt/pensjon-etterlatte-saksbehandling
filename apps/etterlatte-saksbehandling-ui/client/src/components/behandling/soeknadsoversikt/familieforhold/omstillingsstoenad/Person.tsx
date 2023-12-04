import { PersonIcon } from '@navikt/aksel-icons'
import { format } from 'date-fns'
import { PersonInfoAdresse } from '../personer/personinfo/PersonInfoAdresse'
import { BodyShort, Detail, Heading, Label } from '@navikt/ds-react'
import styled from 'styled-components'
import { DatoFormat, formaterFnr } from '~utils/formattering'
import { IconSize } from '~shared/types/Icon'
import { GrunnlagKilde } from '~shared/types/grunnlag'
import { IPdlPerson } from '~shared/types/Person'
import { Utlandsopphold } from '~components/behandling/soeknadsoversikt/familieforhold/personer/personinfo/UtvandringInnvandring'
import { Statsborgerskap } from '~components/behandling/soeknadsoversikt/familieforhold/personer/personinfo/Statsborgerskap'

const PersonBorder = styled.div`
  padding: 1.2em 1em 1em 0em;
  display: flex;
`

const IconWrapper = styled.span`
  width: 2.5rem;
`

const PersonInfoWrapper = styled.div`
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
`

type Props = {
  person: IPdlPerson
  kilde: GrunnlagKilde
  avdoed?: boolean
}

export const Person = ({ person, kilde, avdoed = false }: Props) => {
  return (
    <PersonBorder>
      <IconWrapper>
        <PersonIcon fontSize={IconSize.DEFAULT} />
      </IconWrapper>
      <PersonInfoWrapper>
        <Heading size="small" level="3">
          {avdoed ? 'Avdød' : 'Gjenlevende'}
        </Heading>
        <BodyShort>
          {`${person.fornavn} ${person.etternavn}`} ({formaterFnr(person.foedselsnummer)})
        </BodyShort>
        <div>
          <PersonInfoAdresse adresser={person.bostedsadresse} visHistorikk={false} adresseDoedstidspunkt={avdoed} />
        </div>
        {person.utland && <Utlandsopphold utland={person.utland} />}
        <Statsborgerskap statsborgerskap={person.statsborgerskap} pdlStatsborgerskap={person.pdlStatsborgerskap} />
        <Detail>
          <Label size="small" as="p">
            Kilde
          </Label>
          {`${kilde.type.toUpperCase()} ${format(new Date(kilde.tidspunkt), DatoFormat.DAG_MAANED_AAR)}`}
        </Detail>
      </PersonInfoWrapper>
    </PersonBorder>
  )
}
