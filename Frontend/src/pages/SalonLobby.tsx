import api from "./api";
import type { Salon } from "../types/salon";

export const createSalon = async (): Promise<Salon> => {
    const response = await api.post<Salon>("/salons/");
    return response.data;
};

export const getSalon = async (code: string): Promise<Salon> => {
    const response = await api.get<Salon>(`/salons/${code}/`);
    return response.data;
};

export const joinSalon = async (code: string): Promise<Salon> => {
    const response = await api.post<Salon>(`/salons/${code}/join`);
    return response.data;
};

export const leaveSalon = async (
    code: string,
    userId: number
): Promise<void> => {
    await api.delete(`/salons/${code}/users/${userId}/`);
};