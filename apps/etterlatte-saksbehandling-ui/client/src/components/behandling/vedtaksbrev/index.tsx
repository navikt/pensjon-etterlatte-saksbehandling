import { Content, ContentHeader } from '../../../shared/styled'
import { useEffect, useState } from 'react'
import { Heading, Select } from '@navikt/ds-react'
import { HeadingWrapper } from '../soeknadsoversikt/styled'
import { BehandlingsType } from '../fargetags/behandlingsType'
import { ISaksType, SaksType } from '../fargetags/saksType'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { genererPdf, opprettEllerOppdaterBrevForVedtak } from '../../../shared/api/brev'
import { useParams } from 'react-router-dom'
import { Soeknadsdato } from '../soeknadsoversikt/soeknadoversikt/Soeknadsdato'
import styled from 'styled-components'
import { useAppSelector } from '../../../store/Store'
import { SendTilAttesteringModal } from '../handlinger/sendTilAttesteringModal'
import { PdfVisning } from '../brev/pdf-visning'
import {
  hentVilkaarsvurdering,
  Vilkaarsvurdering,
  VilkaarsvurderingResultat,
  VurderingsResultat,
} from '../../../shared/api/vilkaarsvurdering'

interface VilkaarOption {
  value: string
  label: string
}

export const Vedtaksbrev = () => {
  const { behandlingId } = useParams()
  const { soeknadMottattDato, behandlingType } = useAppSelector((state) => state.behandlingReducer.behandling)

  const [fileURL, setFileURL] = useState<string>()
  const [vedtaksbrevId, setVedtaksbrevId] = useState<string>()
  const [vilkaarsvurdering, setVilkaarsvurdering] = useState<Vilkaarsvurdering>({ vilkaar: [] })
  const [ikkeOpfylteVilkaar, setIkkeOppfylteVilkaar] = useState<VilkaarOption[]>([])
  const [valgtVilkaarType, setValgtVilkaarType] = useState<string>()
  const [error, setError] = useState<string>()

  useEffect(() => {
    if (!vedtaksbrevId) return

    genererPdf(vedtaksbrevId)
      .then((file) => URL.createObjectURL(file))
      .then((url) => setFileURL(url))
      .catch((e) => setError(e.message))
      .finally(() => {
        if (fileURL) URL.revokeObjectURL(fileURL)
        // setHasLoaded(true)
      })
  }, [vedtaksbrevId])

  useEffect(() => {
    if (vilkaarsvurdering.resultat?.utfall === VilkaarsvurderingResultat.IKKE_OPPFYLT) {
      const hovedvilkaar: VilkaarOption[] = vilkaarsvurdering.vilkaar
        .filter((v) => v.hovedvilkaar.resultat === VurderingsResultat.IKKE_OPPFYLT)
        .map((v) => ({
          value: v.hovedvilkaar.type,
          label: `${v.hovedvilkaar.paragraf.paragraf}: ${v.hovedvilkaar.paragraf.tittel}`,
        }))

      const unntaksvilkaar = vilkaarsvurdering.vilkaar
        .flatMap((v) => v.unntaksvilkaar)
        .filter((v) => !!v && v.resultat === VurderingsResultat.IKKE_OPPFYLT)
        .map((v) => ({
          value: v!!.type,
          label: `${v!!.paragraf?.paragraf}: ${v!!.paragraf?.tittel}`,
        }))

      const vilkaar = [...hovedvilkaar, ...unntaksvilkaar]
      if (vilkaar.length === 1) {
        setValgtVilkaarType(vilkaar[0].value)
      } else {
        setIkkeOppfylteVilkaar([...hovedvilkaar, ...unntaksvilkaar])
      }
    }
  }, [vilkaarsvurdering])

  useEffect(() => {
    if (!behandlingId) throw new Error('Mangler behandlingsid')

    hentVilkaarsvurdering(behandlingId).then((response) => {
      console.log(response.status)
      if (response.status === 'ok') {
        setVilkaarsvurdering(response.data)
        console.log('vilkaarsvurder set')
      }
    })

    opprettEllerOppdaterBrevForVedtak(behandlingId!!)
      .then((res) => {
        console.log(res)
        setVedtaksbrevId(res)
      })
      .catch((e) => setError(e.message))
  }, [])

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
                <BehandlingsType type={behandlingType} />
                <SaksType type={ISaksType.BARNEPENSJON} />
              </div>
            </HeadingWrapper>
            <Soeknadsdato mottattDato={soeknadMottattDato} />
          </ContentHeader>

          {!!ikkeOpfylteVilkaar.length ? (
            <Select
              label={'Velg det vedtaket som er mest relevant'}
              onChange={(e) => setValgtVilkaarType(e.target.value)}
            >
              <option value=""></option>
              {ikkeOpfylteVilkaar.map((v) => (
                <option key={v.value} value={v.value}>
                  {v.label}
                </option>
              ))}
            </Select>
          ) : (
            <p>{valgtVilkaarType}</p>
          )}
        </Editor>

        <PdfVisning fileUrl={fileURL} error={error} />
      </BrevContent>

      <BrevContentFooter>
        <BehandlingHandlingKnapper>
          <SendTilAttesteringModal />
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
