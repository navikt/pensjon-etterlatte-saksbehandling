import express, { Request, Response } from "express";

export const proxy = express.Router();

proxy.use((req: Request, res: Response) => {
    return res.status(405).send("Ok");
});
