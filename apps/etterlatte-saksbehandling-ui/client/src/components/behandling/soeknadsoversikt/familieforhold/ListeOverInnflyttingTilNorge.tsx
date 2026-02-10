import { InnflyttingDTO } from '~shared/types/Person'
import { ILand } from '~utils/kodeverk'
import { BodyShort, Box } from '@navikt/ds-react'
import { finnLandSomTekst } from '~components/person/personopplysninger/utils'
import { formaterDatoMedFallback } from '~utils/formatering/dato'

interface Props {
  innflyttingTilNorge: InnflyttingDTO[] | undefined
  alleLand: ILand[]
}

export const ListeOverInnflyttingTilNorge = ({ innflyttingTilNorge, alleLand }: Props) => {
  return !!innflyttingTilNorge ? (
    <Box width="fit-content" paddingInline="space-6 space-0">
      <ul>
        {innflyttingTilNorge.map((flytting, index) => (
          <li key={index}>
            {finnLandSomTekst(flytting.fraflyttingsland ?? '-', alleLand)} den {formaterDatoMedFallback(flytting.dato)}
          </li>
        ))}
      </ul>
    </Box>
  ) : (
    <BodyShort>Ingen innflytting</BodyShort>
  )
}
