import { Personopplysning } from '~shared/types/grunnlag'
import { BodyShort, List } from '@navikt/ds-react'
import { ILand } from '~utils/kodeverk'
import { finnLandSomTekst } from '~components/person/personopplysninger/utils'

interface Props {
  personopplysning: Personopplysning
  alleLand: ILand[]
}

export const InnOgUtvandringer = ({ personopplysning, alleLand }: Props) => {
  const innflyttningerTilNorge = personopplysning.opplysning.utland?.innflyttingTilNorge
  const utflyttningerFraNorge = personopplysning.opplysning.utland?.utflyttingFraNorge
  return !!innflyttningerTilNorge?.length || !!utflyttningerFraNorge?.length ? (
    <List as="ul">
      {!!innflyttningerTilNorge?.length &&
        innflyttningerTilNorge.map((innflyttning, index) => (
          <List.Item key={index}>Inn fra {finnLandSomTekst(innflyttning.fraflyttingsland ?? '-', alleLand)}</List.Item>
        ))}
      {!!utflyttningerFraNorge?.length &&
        utflyttningerFraNorge.map((utflyttning, index) => (
          <List.Item key={index}>Ut til {finnLandSomTekst(utflyttning.tilflyttingsland ?? '-', alleLand)}</List.Item>
        ))}
    </List>
  ) : (
    <BodyShort>Ingen inn-/utvandringer</BodyShort>
  )
}
