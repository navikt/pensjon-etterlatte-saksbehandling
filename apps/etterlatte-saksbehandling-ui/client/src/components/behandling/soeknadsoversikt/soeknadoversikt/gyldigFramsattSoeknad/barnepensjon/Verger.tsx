import { InfoWrapper } from '../../../styled'
import { Info } from '../../../Info'
import { VergemaalEllerFremtidsfullmakt } from '~components/person/typer'

interface Props {
  vergerOgFullmektige: VergemaalEllerFremtidsfullmakt[]
}

export const Verger = ({ vergerOgFullmektige }: Props) => {
  return (
    <InfoWrapper>
      {vergerOgFullmektige.map((it, index) => (
        <Info
          label="Verge"
          tekst={it.vergeEllerFullmektig.motpartsPersonident}
          undertekst={`${it.embete} (${it.type})`}
          key={index}
        />
      ))}
    </InfoWrapper>
  )
}
