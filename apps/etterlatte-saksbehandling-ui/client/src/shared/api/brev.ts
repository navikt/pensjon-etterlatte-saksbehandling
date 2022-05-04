import { IApiResponse } from './types'

const path = process.env.REACT_APP_VEDTAK_URL

export const hentBrev = async (id: string): Promise<IApiResponse<string>> =>
    await fetch(`${path}/pdf/${id}`, {
        method: 'GET'
    }).then(async (res: Response) => {
        const blob: any = await res.blob()

        return new Blob([blob], { type: 'application/pdf' })
    }).then((file: Blob) => {
        return {
            status: 200,
            data: URL.createObjectURL(file)
        }
    }).catch((e) => {
        console.error(e)
        return { status: 500 }
    })
