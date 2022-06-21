import {BodyLong} from "@navikt/ds-react";
import {Column} from "../../../../shared/styled";
import {PdfViewer} from "./nytt-brev";

export const Forhaandsvisning = ({fileUrl, error}: {
    fileUrl?: string,
    error?: string
}) => {
    return (
        <Column style={{paddingLeft: '20px', marginTop: '100px'}}>
            {error && (
                <BodyLong>
                    En feil har oppstått ved henting av PDF:
                    <br/>
                    <code>{error}</code>
                </BodyLong>
            )}

            <div>{fileUrl && <PdfViewer src={fileUrl}/>}</div>
        </Column>
    )
}
