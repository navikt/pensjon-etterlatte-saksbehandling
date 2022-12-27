import { Content, ContentHeader } from '~shared/styled'
import { useEffect, useState } from 'react'
import { Heading, Select, Tag } from '@navikt/ds-react'
import { HeadingWrapper } from '../soeknadsoversikt/styled'
import { ISaksType } from '../fargetags/saksType'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { genererPdf, opprettEllerOppdaterBrevForVedtak } from '~shared/api/brev'
import { useParams } from 'react-router-dom'
import { Soeknadsdato } from '../soeknadsoversikt/soeknadoversikt/Soeknadsdato'
import styled from 'styled-components'
import { useAppSelector } from '~store/Store'
import { SendTilAttesteringModal } from '../handlinger/sendTilAttesteringModal'
import { PdfVisning } from '../brev/pdf-visning'
import {
  hentVilkaarsvurdering,
  IVilkaarsvurdering,
  VilkaarsvurderingResultat,
  VurderingsResultat,
} from '~shared/api/vilkaarsvurdering'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'
import { tagColors, TagList } from "~shared/Tags";
import { formaterEnumTilLesbarString } from "~utils/formattering";

interface VilkaarOption {
  value: string
  label: string
}

export const Vedtaksbrev = () => {
  const { behandlingId } = useParams()
  const { sak, soeknadMottattDato, behandlingType, status } = useAppSelector(
    (state) => state.behandlingReducer.behandling
  )

  const [fileURL, setFileURL] = useState<string>()
  const [vedtaksbrevId, setVedtaksbrevId] = useState<string>()
  const [vilkaarsvurdering, setVilkaarsvurdering] = useState<IVilkaarsvurdering | undefined>(undefined)
  const [ikkeOpfylteVilkaar, setIkkeOppfylteVilkaar] = useState<VilkaarOption[]>([])
  const [valgtVilkaarType, setValgtVilkaarType] = useState<string>()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string>()

  useEffect(() => {
    if (!vedtaksbrevId) return

    genererPdf(vedtaksbrevId!!)
      .then((res) => {
        if (res.status === 'ok') {
          return new Blob([res.data], { type: 'application/pdf' })
        } else {
          throw Error(res.error)
        }
      })
      .then((file) => URL.createObjectURL(file!!))
      .then((url) => setFileURL(url))
      .catch((e) => setError(e.message))
      .finally(() => {
        if (fileURL) URL.revokeObjectURL(fileURL)

        setLoading(false)
      })
  }, [vedtaksbrevId])

  useEffect(() => {
    if (vilkaarsvurdering?.resultat?.utfall === VilkaarsvurderingResultat.IKKE_OPPFYLT) {
      const hovedvilkaar: VilkaarOption[] = vilkaarsvurdering.vilkaar
        .filter((v) => v.hovedvilkaar.resultat === VurderingsResultat.IKKE_OPPFYLT)
        .map((v) => ({
          value: v.hovedvilkaar.type,
          label: `${v.hovedvilkaar.lovreferanse.paragraf}: ${v.hovedvilkaar.tittel}`,
        }))

      const unntaksvilkaar = vilkaarsvurdering.vilkaar
        .flatMap((v) => v.unntaksvilkaar)
        .filter((v) => !!v && v.resultat === VurderingsResultat.IKKE_OPPFYLT)
        .map((v) => ({
          value: v!!.type,
          label: `${v!!.lovreferanse?.paragraf}: ${v!!.tittel}`,
        }))

      const vilkaar = [...hovedvilkaar, ...unntaksvilkaar]
      if (vilkaar.length === 1) {
        setValgtVilkaarType(vilkaar[0].value)
        console.log(valgtVilkaarType)
      } else {
        setIkkeOppfylteVilkaar([...hovedvilkaar, ...unntaksvilkaar])
      }
    }
  }, [vilkaarsvurdering])

  useEffect(() => {
    if (!behandlingId) throw new Error('Mangler behandlingsid')

    hentVilkaarsvurdering(behandlingId).then((response) => {
      if (response.status === 'ok') {
        setVilkaarsvurdering(response.data)
      }
    })

    const fetchVedtaksbrev = async () => {
      const brevResponse = await opprettEllerOppdaterBrevForVedtak(sak, behandlingId!!)

      if (brevResponse.status === 'ok') {
        setVedtaksbrevId(brevResponse.data)
      } else {
        setError(brevResponse.error)
        setLoading(false)
      }
    }
    fetchVedtaksbrev()
  }, [behandlingId])

  return (
    <Content>
      <BrevContent>
        <Editor>
          <ContentHeader>
            <HeadingWrapper>
              <Heading spacing size={'xlarge'} level={'5'}>
                Vedtaksbrev
              </Heading>
              <div className="details">
                <TagList>
                  <li>
                    <Tag variant={tagColors[behandlingType]} size={'small'}>
                      {formaterEnumTilLesbarString(behandlingType)}
                    </Tag>
                  </li>
                  <li>
                    <Tag variant={tagColors[ISaksType.BARNEPENSJON]} size={'small'}>
                      {formaterEnumTilLesbarString(ISaksType.BARNEPENSJON)}
                    </Tag>
                  </li>
                </TagList>
              </div>
            </HeadingWrapper>
            <Soeknadsdato mottattDato={soeknadMottattDato} />
          </ContentHeader>

          {!!ikkeOpfylteVilkaar.length && (
            <Select
              label={'Velg det vilkåret som er mest relevant'}
              onChange={(e) => setValgtVilkaarType(e.target.value)}
            >
              <option value=""></option>
              {ikkeOpfylteVilkaar.map((v) => (
                <option key={v.value} value={v.value}>
                  {v.label}
                </option>
              ))}
            </Select>
          )}
        </Editor>

        <PdfVisning fileUrl={fileURL} error={error} loading={loading} />
      </BrevContent>

      <BrevContentFooter>
        <BehandlingHandlingKnapper>
          {hentBehandlesFraStatus(status) && <SendTilAttesteringModal />}
        </BehandlingHandlingKnapper>
      </BrevContentFooter>
    </Content>
  )
}

const BrevContent = styled.div`
  display: flex;
  height: 75vh;
  max-height: 75vh;
`

const BrevContentFooter = styled.div`
  border-top: 1px solid #c6c2bf;
`

const Editor = styled.div`
  max-height: fit-content;
  min-width: 40%;
  width: 40%;
  border-right: 1px solid #c6c2bf;
`
