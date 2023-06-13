import { SakType } from '~shared/types/sak'
import BeregningsgrunnlagBarnepensjon from '~components/behandling/beregningsgrunnlag/BeregningsgrunnlagBarnepensjon'
import BeregningsgrunnlagOmstillingsstoenad from '~components/behandling/beregningsgrunnlag/BeregningsgrunnlagOmstillingsstoenad'
import { HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { BodyShort, Heading } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { Content, ContentHeader } from '~shared/styled'
import { IBehandlingsType, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { formaterVedtaksResultat, useVedtaksResultat } from '~components/behandling/useVedtaksResultat'

const Beregningsgrunnlag = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const vedtaksresultat =
    behandling.behandlingType !== IBehandlingsType.MANUELT_OPPHOER ? useVedtaksResultat() : 'opphoer'
  const virkningstidspunkt = behandling.virkningstidspunkt?.dato
    ? formaterStringDato(behandling.virkningstidspunkt.dato)
    : undefined

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading spacing size={'large'} level={'1'}>
            Beregningsgrunnlag
          </Heading>
          <BodyShort spacing>
            Vilkårsresultat: <strong>{formaterVedtaksResultat(vedtaksresultat, virkningstidspunkt)}</strong>
          </BodyShort>
        </HeadingWrapper>
      </ContentHeader>
      {
        {
          [SakType.BARNEPENSJON]: <BeregningsgrunnlagBarnepensjon behandling={behandling} />,
          [SakType.OMSTILLINGSSTOENAD]: <BeregningsgrunnlagOmstillingsstoenad behandling={behandling} />,
        }[behandling.sakType]
      }
    </Content>
  )
}

export default Beregningsgrunnlag
