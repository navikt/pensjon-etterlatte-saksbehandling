import { useEffect, useState } from 'react'
import { ContentWrapper } from './styled'
import { Detail, BodyShort, Label } from '@navikt/ds-react'
import { InfoWrapper, DetailWrapper } from './styled'
import { IOpplysningProps } from './types'
import { hentPersonopplysninger } from '../../../shared/api/personopplysninger'
import { formatterDato, formatterTidspunkt } from '../../../utils/index'
import { Content, ContentHeader } from '../../../shared/styled'
import { Oppholdstillatelse } from './oppholdstillatelse'
import { Statsborgerskap } from './statsborgerskap'
import { Bostedsadresse } from './bostedsadresse'
import { Sivilstatus } from './sivilstatus'

export const Personopplysninger = () => {
  const [personopplysninger, setPersonopplysninger] = useState<IOpplysningProps>()

  useEffect(() => {
    hentPersonopplysninger().then((opplysninger: IOpplysningProps) => {
      setPersonopplysninger(opplysninger)
    })
  }, [])

  return (
    <Content>
      {personopplysninger && (
        <ContentHeader>
          <h1>Personopplynsinger</h1>
          <DetailWrapper>
            <BodyShort size="medium" spacing>
              Registeropplysninger
            </BodyShort>
            <Detail size="small">
              sist oppdatert i Folkeregisteret {formatterDato(personopplysninger.sistEndringIFolkeregister)}{' '}
              {formatterTidspunkt(personopplysninger.sistEndringIFolkeregister)}
            </Detail>
          </DetailWrapper>

          <DetailWrapper>
            <InfoWrapper>
              <BodyShort size="small">Statsborgerskap</BodyShort>
              <Label size="small">NO</Label>
            </InfoWrapper>

            <InfoWrapper>
              <BodyShort size="small">Personstatus</BodyShort>
              <Label size="small">Bosatt</Label>
            </InfoWrapper>
          </DetailWrapper>

          <ContentWrapper>
            <Sivilstatus innhold={personopplysninger.sivilstand} />
            {personopplysninger.oppholdstillatelse && (
              <Oppholdstillatelse innhold={personopplysninger?.oppholdstillatelse} />
            )}
            <Statsborgerskap innhold={personopplysninger.statsborgerskap} />
            <Bostedsadresse innhold={personopplysninger.bostedsadresse} />
          </ContentWrapper>
        </ContentHeader>
      )}
    </Content>
  )
}
