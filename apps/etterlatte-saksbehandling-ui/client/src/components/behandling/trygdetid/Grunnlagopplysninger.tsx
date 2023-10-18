import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { formaterStringDato } from '~utils/formattering'
import { IGrunnlagOpplysninger, IOpplysningsgrunnlag } from '~shared/api/trygdetid'
import styled from 'styled-components'

const InfoWrapperWithGap = styled.div`
  display: flex;
  gap: 20px;
  padding: 2em 0 2em 0;
`

export const Grunnlagopplysninger = ({ opplysninger }: { opplysninger: IGrunnlagOpplysninger }) => (
  <InfoWrapperWithGap>
    <Opplysningsgrunnlag label="Fødselsdato" opplysningsgrunnlag={opplysninger.avdoedFoedselsdato} />
    <Opplysningsgrunnlag label="16 år" opplysningsgrunnlag={opplysninger.avdoedFylteSeksten} />
    <Opplysningsgrunnlag label="Dødsdato" opplysningsgrunnlag={opplysninger.avdoedDoedsdato} />
    <Opplysningsgrunnlag label="66 år" opplysningsgrunnlag={opplysninger.avdoedFyllerSeksti} />
  </InfoWrapperWithGap>
)

const Opplysningsgrunnlag = ({
  label,
  opplysningsgrunnlag,
}: {
  label: string
  opplysningsgrunnlag: IOpplysningsgrunnlag
}) => (
  <Info
    label={label}
    tekst={opplysningsgrunnlag.opplysning ? formaterStringDato(opplysningsgrunnlag.opplysning) : 'n/a'}
    undertekst={opplysningsgrunnlag.kilde.type + ': ' + formaterStringDato(opplysningsgrunnlag.kilde.tidspunkt)}
  />
)
