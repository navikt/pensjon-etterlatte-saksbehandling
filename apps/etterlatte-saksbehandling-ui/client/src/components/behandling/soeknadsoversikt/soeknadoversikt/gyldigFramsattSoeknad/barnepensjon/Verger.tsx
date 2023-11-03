import { InfoWrapper } from '../../../styled'
import { Info } from '../../../Info'
import { VergemaalEllerFremtidsfullmakt } from '~components/person/typer'

interface Props {
  vergerOgFullmektige: VergemaalEllerFremtidsfullmakt[]
}

export const Verger = ({ vergerOgFullmektige }: Props) => {
  function contents() {
    if (vergerOgFullmektige.length > 0) {
      return (
        <>
          {vergerOgFullmektige.map((it, index) => (
            <Info
              label="Verge"
              tekst={it.vergeEllerFullmektig.motpartsPersonident}
              undertekst={`${it.embete} (${it.type})`}
              key={index}
            />
          ))}
        </>
      )
    }
    return <Info label="Verge" tekst="Ingen verge registrert i PDL" />
  }

  return <InfoWrapper>{contents()}</InfoWrapper>
}
