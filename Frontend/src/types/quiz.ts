export interface Reponse {
    id: number;
    text: string;
    estBonne: boolean;
}

export type QuestionType = "CHOIX" | "SAISIE_LIBRE";

export interface Question {
    id: number;
    text: string;
    type: QuestionType;
    reponses: Reponse[];
}

export interface Quiz {
    id: number;
    titre: string;
    description: string;
    questions: Question[];
}