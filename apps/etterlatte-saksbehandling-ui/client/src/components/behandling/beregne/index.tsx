import { useContext } from 'react'
import { AppContext } from '../../../store/AppContext'
import { Content, ContentHeader } from '../../../shared/styled'
import { TypeStatusWrap } from '../soeknadsoversikt/styled'
import { Sammendrag } from './sammendrag'
import styled from 'styled-components'
import { FileIcon } from '../../../shared/icons/fileIcon'
import { OpplysningsType } from '../../../store/reducers/BehandlingReducer'
import { hentVirkningstidspunkt } from '../soeknadsoversikt/utils'
import { format } from 'date-fns'

export const Beregne = () => {
  const ctx = useContext(AppContext)
  const grunnlag = ctx.state.behandlingReducer.grunnlag

  const mottattDato: string = grunnlag.find((g) => g.opplysningType === OpplysningsType.soeknad_mottatt)?.opplysning
    ?.mottattDato

  const doedsdato: string = grunnlag.find((g) => g.opplysningType === OpplysningsType.avdoed_forelder_pdl)?.opplysning
    ?.doedsdato

  const virkningstidspunkt = format(new Date(hentVirkningstidspunkt(doedsdato, mottattDato)), 'dd.MM.yyyy')

  return (
    <Content>
      <ContentHeader>
        <h1>Beregning og vedtak</h1>
        <InfoWrapper>
          <DetailWrapper>
            <TypeStatusWrap type="barn">Barnepensjon</TypeStatusWrap>
            <TypeStatusWrap type="statsborgerskap">Nasjonal sak</TypeStatusWrap>
          </DetailWrapper>

          <div className="text">
            Vilkårsresultat: <strong>Innvilget fra {virkningstidspunkt}</strong>
          </div>
        </InfoWrapper>

        <Sammendrag />

        <VedtaksbrevWrapper>
          <FileIcon />
          <span className="text">Vis vedtaksbrev</span>
        </VedtaksbrevWrapper>
      </ContentHeader>
    </Content>
  )
}

export const InfoWrapper = styled.div`
  margin-top 5em;
  max-width: 500px;
  .text {
    margin: 2em 0em 5em 0em;
  }
`
export const DetailWrapper = styled.div`
  display: flex;
  max-width: 400px;
  margin-left: 0;
`
export const VedtaksbrevWrapper = styled.div`
  display: inline-flex;
  margin-top: 8em;
  cursor: pointer;

  .text {
    line-height: 1.5em;
    margin-left: 0.3em;
    color: #0056b4;
    font-size: 18px;
    font-weight: 600;
    text-decoration-line: underline;
  }
`
