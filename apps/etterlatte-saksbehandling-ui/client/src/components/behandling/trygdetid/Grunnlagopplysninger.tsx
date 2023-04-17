import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { formaterStringDato } from '~utils/formattering'
import React from 'react'
import { InfoWrapper } from '~components/behandling/trygdetid/Trygdetid'
import { IGrunnlagOpplysninger, IOpplysningsgrunnlag } from '~shared/api/trygdetid'

export const Grunnlagopplysninger: React.FC<{ opplysninger: IGrunnlagOpplysninger }> = ({ opplysninger }) => (
  <InfoWrapper>
    <Opplysningsgrunnlag label={'Fødselsdato'} opplysningsgrunnlag={opplysninger.avdoedFoedselsdato} />
    <Opplysningsgrunnlag label={'Dødsdato'} opplysningsgrunnlag={opplysninger.avdoedDoedsdato} />
  </InfoWrapper>
)

const Opplysningsgrunnlag: React.FC<{
  label: string
  opplysningsgrunnlag: IOpplysningsgrunnlag
}> = ({ label, opplysningsgrunnlag }) => (
  <Info
    label={label}
    tekst={opplysningsgrunnlag.opplysning ? formaterStringDato(opplysningsgrunnlag.opplysning) : 'n/a'}
    undertekst={
      opplysningsgrunnlag.kilde.type + ': ' + formaterStringDato(opplysningsgrunnlag.kilde.tidspunktForInnhenting)
    }
  />
)
