import express, { Request, Response } from "express";

export const proxy = express.Router();

proxy.use((req: Request, res: Response) => {
    console.log(process.env);
    return res.status(405).send("Ok");
});
