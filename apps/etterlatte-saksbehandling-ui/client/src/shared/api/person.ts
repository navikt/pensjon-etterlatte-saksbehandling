import { IApiResponse } from "./types";



const isDev = process.env.NODE_ENV !== "production"
const path = isDev ? "http://localhost:8080" : "https://etterlatte-saksbehandling.dev.intern.nav.no";


export const getPerson = async (fnr: string): Promise<IApiResponse<any>> => {
  try{
    const result: Response = await fetch(`${path}/api/personer/${fnr}`)
    return {
      status: result.status,
      data: await result.json()
    }
  } catch(e) {
    console.log(e);
    return {status: 500};
  }
}

interface Sak {
    id: number;
    ident: string;
    sakType: string;
}
export interface IPersonResult {
  person: {
    fornavn: string;
    etternavn: string;
    ident: string;
  },
  saker: { saker: Sak[] }
}

export const opprettSakPaaPerson = async (fnr: string): Promise<IApiResponse<IPersonResult>> => {
  try{
    const result: Response = await fetch(`${path}/api/personer/${fnr}/saker`, {
      method: "post"
    });
    
    return {
      status: result.status,
      data: await result.json()
    }
  } catch(e) {
    console.log(e);
    return {status: 500};
  }
}

export const opprettBehandlingPaaSak = async (sakId: number): Promise<IApiResponse<any>> => {

  try {
    const result: Response = await fetch(`${path}/api/saker/${sakId}/behandlinger`, {
      method: "post"
    });
    return {
      status: result.status,
      data: await result.json()
    }
  } catch(e) {
    console.log(e)
    return {status: 500}
  }
}