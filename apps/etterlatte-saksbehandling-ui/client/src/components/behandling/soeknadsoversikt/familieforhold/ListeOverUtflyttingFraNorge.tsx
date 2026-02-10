import { ILand } from '~utils/kodeverk'
import { BodyShort, Box } from '@navikt/ds-react'
import { finnLandSomTekst } from '~components/person/personopplysninger/utils'
import { formaterDatoMedFallback } from '~utils/formatering/dato'

interface Props {
  utflyttingFraNorge: { tilflyttingsland?: string; dato?: string }[] | undefined
  alleLand: ILand[]
}

export const ListeOverUtflyttingFraNorge = ({ utflyttingFraNorge, alleLand }: Props) => {
  return !!utflyttingFraNorge ? (
    <Box width="fit-content" paddingInline="space-6 space-0">
      <ul>
        {utflyttingFraNorge.map((flytting, index) => (
          <li key={index}>
            {finnLandSomTekst(flytting.tilflyttingsland ?? '-', alleLand)} den {formaterDatoMedFallback(flytting.dato)}
          </li>
        ))}
      </ul>
    </Box>
  ) : (
    <BodyShort>Ingen utflytting</BodyShort>
  )
}
