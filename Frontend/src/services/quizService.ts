import api from "./api";
import type { Quiz } from "../types/quiz";
import type { PaginatedResponse } from "../types/api";

export type QuestionType = "CHOIX" | "SAISIE_LIBRE";

export interface ReponseForm { text: string; estBonne: boolean; }
export interface QuestionForm {
    text: string;
    type: QuestionType;
    reponses: ReponseForm[];
}

// ── Lecture ──────────────────────────────────────────────────────────────────

export const getQuizzes = async (): Promise<Quiz[]> => {
    const res = await api.get<PaginatedResponse<Quiz>>("/quizs/");
    return res.data.content;
};

export const getQuizById = async (id: number): Promise<Quiz> => {
    const res = await api.get<Quiz>(`/quizs/${id}/`);
    return res.data;
};

// ── Création ─────────────────────────────────────────────────────────────────

export const createQuiz = async (
    titre: string,
    description: string,
    questions: QuestionForm[]
): Promise<Quiz> => {
    const res = await api.post<Quiz>("/quizs/full", { titre, description, questions });
    return res.data;
};

// ── Modification titre/description ────────────────────────────────────────────

export const updateQuizMeta = async (
    id: number,
    titre: string,
    description: string
): Promise<Quiz> => {
    const res = await api.put<Quiz>(`/quizs/${id}/`, { id, titre, description });
    return res.data;
};

// ── Suppression ───────────────────────────────────────────────────────────────

export const deleteQuiz = async (id: number): Promise<void> => {
    await api.delete(`/quizs/${id}/`);
};

// ── Questions ─────────────────────────────────────────────────────────────────

/** Ajoute une question complète (avec ses réponses) à un quiz existant. */
export const addQuestionToQuiz = async (
    quizId: number,
    question: QuestionForm
): Promise<Quiz> => {
    const res = await api.post<Quiz>(`/quizs/${quizId}/questions/full`, question);
    return res.data;
};

/** Remplace une question existante (texte + type + réponses). */
export const updateQuestionInQuiz = async (
    quizId: number,
    questionId: number,
    question: QuestionForm
): Promise<Quiz> => {
    const res = await api.put<Quiz>(`/quizs/${quizId}/questions/${questionId}/`, question);
    return res.data;
};

/** Supprime une question d'un quiz. */
export const removeQuestionFromQuiz = async (
    quizId: number,
    questionId: number
): Promise<Quiz> => {
    const res = await api.delete<Quiz>(`/quizs/${quizId}/questions/${questionId}/`);
    return res.data;
};
